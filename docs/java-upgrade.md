# Updating jvmxray for a New Java Release

Use this when a new JDK is installed or released, or when updating Byte Buddy.

## Rules

- **Bytecode stays at Java 17.** The pom sets `maven.compiler.release` to 17, so the agent attaches to apps on Java 17 and later. Don't raise it unless you deliberately drop support for older JVMs.
- **Byte Buddy must support the newest JDK you run on.** Byte Buddy reads the JDK's own classes to instrument them. If it doesn't recognize the JDK's class-file version, instrumentation fails and the agent records nothing.
- **A passing build proves nothing here.** When Byte Buddy is too old, `mvn verify` still passes. Check the agent's sensor output (steps 3 and 4).

## Procedure

### 1. Check the installed JDKs

```bash
java -version
/usr/libexec/java_home -V    # macOS: lists all installed JDKs
```

### 2. Update Byte Buddy to the latest release

Find the latest version (ignore `-jdk5` variants):

```bash
curl -s https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/maven-metadata.xml \
  | grep -o '<version>[0-9.]*</version>' | tail -1
```

Set that version for **both** `byte-buddy` and `byte-buddy-agent` in `pom.xml`. Also update the version listed in `README.md`.

### 3. Build and check for instrumentation errors

```bash
mvn clean verify > build.log 2>&1; echo "exit=$?"
grep -c "is not supported by the current version of Byte Buddy" build.log   # must be 0
grep -o "Error transforming [^:]*: [^,]\{0,80\}" build.log | sort | uniq -c
```

Known errors that are unrelated to the Java version (as of Byte Buddy 1.18.17):

- `Error transforming com.google.gson.Gson: Usage of interface net.bytebuddy.asm.Advice$Thrown is not allowed`
- `Failed to load class for transformation` for Hikari and SQLite prepared-statement classes

Any other error kind is new and needs investigating.

### 4. Check that the sensors record events

```bash
for f in target/test-jvmxray/agent/logs/*.log; do
  printf "%-32s %s\n" "$(basename "$f")" "$(wc -l < "$f")"
done
```

Reference counts on JDK 25 with Byte Buddy 1.18.17:

| Sensor log | Lines |
|---|---|
| CONFIG | ~80–120 |
| CRYPTO | 2 |
| IO | 7–8 |
| SERIALIZATION | 7 |
| SYSTEM | ~180 |
| MONITOR | 1 |

API, AUTH, EXCEPTION, HTTP, NET, REFLECTION, SCRIPT and SQL log 0 on every JDK, because the Turtle integration test doesn't exercise them. If CONFIG, CRYPTO, IO or SERIALIZATION drop to 0, instrumentation is broken.

### 5. Repeat on Java 17 and 21

This confirms the new Byte Buddy still works on older JVMs. Run steps 3 and 4 with each JDK:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean verify > build-j17.log 2>&1
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean verify > build-j21.log 2>&1
```

### 6. Confirm the bytecode is still Java 17

```bash
javap -v -cp target/classes org.jvmxray.agent.bootstrap.AgentBootstrap | grep "major version"
# expect: major version: 61   (61 = Java 17)
```

### 7. Commit on a branch and open a PR

Include the per-JDK sensor counts in the PR description.

## Stopgap

If a JDK is released before Byte Buddy supports it, start the monitored app with `-Dnet.bytebuddy.experimental=true`. Byte Buddy will then attempt the unknown class-file version. It usually works, but failures can be subtle, so upgrade Byte Buddy as soon as a supporting release ships.
