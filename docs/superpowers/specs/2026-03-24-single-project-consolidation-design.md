# JVMXRay Single-Project Consolidation Design

**Date**: 2026-03-24
**Branch**: feature/project-slimdown
**Status**: Approved

---

## Problem

The current multi-module Maven structure (`prj-common`, `prj-agent`, `prj-mcp-client`) introduces
friction without clear benefit:

- `.jvmxray/` directory pollutes the repo root with runtime/test artifacts that persist across builds
- Cleaning requires explicit `maven-clean-plugin` custom fileset configuration — `mvn clean` alone is not sufficient
- Cross-module build ordering: `prj-common` must build before `prj-agent`; `prj-agent` JAR must be
  copied into `prj-common/target/` for integration tests
- Three POMs to maintain with inter-module dependency declarations
- All three modules are already tightly coupled; separation provides no deployment or reuse benefit

---

## Goal

Collapse all three modules into a single flat Maven project. No submodules. All source under one
`src/` tree. All build and test artifacts land in `target/` and are cleaned by `mvn clean`
automatically. Two deployable JARs produced from a single build.

---

## Constraints

- Package names are unchanged — no class renames, pure file moves
- Two distinct deployable artifacts must be produced: agent shaded JAR and MCP client standalone JAR
- MCP SDK SNAPSHOT JARs remain as system-scope deps from `libs/`

---

## Java Version

The single POM targets **Java 17**.

Both `libs/mcp-0.13.0-SNAPSHOT.jar` and `libs/mcp-json-jackson2-0.13.0-SNAPSHOT.jar` are compiled
at bytecode major version 61 (Java 17). A Java 11 compiler cannot link against them. Java 17 is
the minimum viable version for the consolidated project.

This is not a regression for the agent: the agent bytecode is still compiled at Java 17, which
runs on any JVM 17+. The agent JAR continues to work on all environments that previously ran it.
The original Java 11 target was chosen for "maximum cloud compatibility" — Java 17 LTS has
equivalent enterprise adoption and is widely available in all major cloud runtimes.

The optional Maven profile `include-mcp-client` (gated on `!skipMcpClient`) is removed. All code
is in one project; there is nothing to conditionally include.

---

## Source Tree

All Java source merges into one `src/main/java/` tree. Package roots are identical to today:

```
src/
├── main/
│   ├── java/org/jvmxray/
│   │   ├── agent/               ← from prj-agent
│   │   │   ├── bin/
│   │   │   ├── bootstrap/
│   │   │   ├── guard/
│   │   │   ├── init/
│   │   │   ├── log/             ← includes log/appender/ sub-package
│   │   │   ├── proxy/
│   │   │   ├── sensor/          ← includes sensor/monitor/ sub-package
│   │   │   └── util/
│   │   └── platform/
│   │       ├── shared/          ← from prj-common
│   │       │   ├── bin/
│   │       │   ├── classloader/
│   │       │   ├── init/
│   │       │   ├── log/
│   │       │   ├── property/
│   │       │   ├── schema/
│   │       │   ├── test/        ← TurtleTestExecutor stays in src/main/java (see note below)
│   │       │   └── util/
│   │       └── client/
│   │           └── mcp/bridge/  ← from prj-mcp-client (package: org.jvmxray.platform.client.mcp.bridge)
│   └── resources/               ← merged resources (all filenames are distinct, no conflicts)
│       ├── agent-logback-production.xml2   ← from prj-agent
│       ├── agent-logback-shaded.xml2       ← from prj-agent
│       ├── agent.properties                ← from prj-agent
│       ├── common-logback-production.xml   ← from prj-common
│       └── common-logback-test.xml2        ← from prj-common
└── test/
    └── java/org/jvmxray/shared/
        ├── integration/
        │   ├── TurtleTest.java           ← integration test runner
        │   └── turtle/
        │       └── TurtleIntegrationTest.java
        └── unittest/
```

Note: `org.jvmxray.platform.shared.test.TurtleTestExecutor` lives in `src/main/java/` because it
is a shared test-utility class used at runtime by the integration harness. It stays in
`src/main/java/` after consolidation.

---

## Build Artifacts

### Artifact 1 — Agent JAR
- **Output**: `target/jvmxray-0.0.1-agent.jar`
- **Classifier**: `agent`
- **Type**: Shaded fat JAR
- **Manifest**: `Premain-Class: org.jvmxray.agent.bootstrap.AgentBootstrap`, `Can-Redefine-Classes: true`, `Can-Retransform-Classes: true`
- **Relocations**: `org.slf4j` → `agent.shadow.slf4j`, `ch.qos.logback` → `agent.shadow.logback`
- **Excludes**: `logback.xml`, `logback-test.xml`, `META-INF/*.SF`, `META-INF/*.DSA`, `META-INF/*.RSA`
- **Contents**: All source classes + all runtime dependencies (ByteBuddy, Logback, Cassandra, SQLite, HikariCP, javax.servlet-api, etc.)

### Artifact 2 — MCP Client JAR
- **Output**: `target/jvmxray-0.0.1-mcp-client.jar`
- **Classifier**: `mcp-client`
- **Type**: Shaded fat JAR
- **Manifest**: `Main-Class: org.jvmxray.platform.client.mcp.bridge.JvmxrayMcpClient`
- **Relocations**: None
- **System-scope JAR handling**: `libs/mcp-0.13.0-SNAPSHOT.jar` and `libs/mcp-json-jackson2-0.13.0-SNAPSHOT.jar`
  are `system` scope. The shade plugin excludes system-scope JARs by default. The mcp-client shade
  execution must explicitly include them via `<artifactSet>` includes or a `<filter>` referencing
  the absolute system paths.
- **Contents**: All source classes + all runtime dependencies (MCP SDK, Reactor, Jackson, etc.)

Both artifacts are produced by two named executions of `maven-shade-plugin` in the single POM.

### Artifact 3 — VersionTool JAR
- **Output**: `target/jvmxray-0.0.1-version-tool.jar` (classifier `version-tool`)
- Retained from `prj-common`'s existing shade execution. The classifier is renamed from `shaded`
  to `version-tool` to disambiguate when three shade executions share a single POM.
- No relocations. `Main-Class: org.jvmxray.platform.shared.bin.VersionTool`.

---

## Test & Runtime Directories

### Before
```
.jvmxray/                    ← repo root, persists, requires explicit cleanup config
├── agent/config/logback.xml
├── agent/logs/
├── common/config/
├── common/data/jvmxray-test.db
└── common/logs/
```

### After
```
target/test-jvmxray/         ← inside target/, cleaned by mvn clean automatically
├── agent/config/logback.xml
├── agent/logs/
├── common/config/
├── common/data/jvmxray-test.db
└── common/logs/
```

`ComponentInitializer` Java code is unchanged — it reads `jvmxray.test.home` as a system property
set by Maven plugins, so the path change is entirely in the POM.

---

## POM Changes

### Single `pom.xml` replaces four POMs

**Compiler**: Java 17 (see Java Version section above).

**Dependencies**: Union of all three module dependency sets. Notes:
- `sqlite-jdbc`: Remove the explicit `3.42.0.0` version pin that was in `prj-common`. The parent
  POM's `dependencyManagement` entry of `3.50.3.0` then applies.
- `javax.servlet:javax.servlet-api:4.0.1` at `compile` scope — required for servlet-monitoring
  sensors. Was in `prj-agent`; must be present in the consolidated POM.
- No `dependencyManagement` block needed in the new single POM.

**Plugins retained**:
| Plugin | Purpose |
|--------|---------|
| `maven-compiler-plugin` | Java 17 |
| `maven-shade-plugin` | Three executions: `agent`, `mcp-client`, `version-tool` |
| `maven-jar-plugin` | Standard unshaded JAR |
| `maven-antrun-plugin` | Test dir creation + logback config seeding + agent JAR availability |
| `maven-surefire-plugin` | Unit tests; system props point to `target/test-jvmxray/` |
| `maven-failsafe-plugin` | TurtleTest integration; agent JAR path updated to local target |
| `git-commit-id-plugin` | Build version tracking |

**Plugins removed**:
| Plugin | Reason |
|--------|--------|
| `maven-clean-plugin` custom fileset | `.jvmxray/` no longer exists at repo root |
| `maven-resources-plugin` | Was in mcp-client only; not needed at project level |
| `maven-assembly-plugin` | Replaced by mcp-client shade execution |

### System property path changes

Complete inventory of affected properties in Surefire and Failsafe configurations:
- `jvmxray.test.home` → `${project.build.directory}/test-jvmxray`
- `jvmxray.common.logs` → `${project.build.directory}/test-jvmxray/common/logs`
- `jvmxray.common.config` → `${project.build.directory}/test-jvmxray/common/config`
- `jvmxray.common.data` → `${project.build.directory}/test-jvmxray/common/data`
- `logback.common.configurationFile` → `${project.build.directory}/test-jvmxray/common/config/logback.xml`
- **Add**: `project.basedir` → `${project.basedir}` (needed by `TurtleTestExecutor.createDatabaseConfig()` for external tooling — not required for `mvn clean install`)

Antrun `<copy>` destination for agent logback config changes from:
```
${project.basedir}/../.jvmxray/agent/config/logback.xml
```
to:
```
${project.build.directory}/test-jvmxray/agent/config/logback.xml
```

Agent JAR path in Failsafe changes from:
```
${project.basedir}/../prj-agent/target/prj-agent-${project.version}-shaded.jar
```
to:
```
${project.build.directory}/jvmxray-${project.version}-agent.jar
```

### `TurtleTestExecutor` Java fixes (two paths)

**Fix 1 — `resolveAgentJarPath()` fallback** (line ~128):

Current:
```java
agentJar = Paths.get(testHome).getParent()
    .resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar")
    .toString();
```

After consolidation, `testHome` is `target/test-jvmxray/` and `getParent()` returns `target/`.
The old path resolves to `target/prj-agent/target/...` which never exists. Fix:

```java
agentJar = Paths.get(testHome).getParent()
    .resolve("jvmxray-" + VERSION + "-agent.jar")
    .toString();
```

where `VERSION` is the project version constant (or read from a system property).
Failsafe sets `jvmxray.agent.jar` explicitly, so this fallback is a secondary guard only.

**Fix 2 — `createDatabaseConfig()` path** (line ~289, non-blocking for `mvn clean install`):

`createDatabaseConfig()` is a utility factory method not called by `TurtleTest.java` or any
Maven-executed test. It is used by external tooling only. This fix is a correctness improvement
for that tooling, not a prerequisite for the build to pass.

Current:
```java
agentLogbackDbConfig = Paths.get(testHome).getParent()
    .resolve("script/config/logback/agent-db.xml")
    .toString();
```

After consolidation, `testHome.getParent()` = `target/`, not the repo root. The `script/`
directory is at the repo root, not under `target/`. Fix: use `project.basedir` system property
to resolve repo-root-relative paths:

```java
String projectBasedir = System.getProperty("project.basedir");
if (projectBasedir != null) {
    agentLogbackDbConfig = Paths.get(projectBasedir)
        .resolve("script/config/logback/agent-db.xml")
        .toString();
}
```

---

## `.gitignore` Changes

Remove the `.jvmxray/` entry — that directory no longer lives at repo root.

---

## Migration Steps

1. Create new flat `pom.xml` at Java 17 with consolidated deps, three shade executions, and all
   system properties pointing to `target/test-jvmxray/`; add `project.basedir` system property to
   Surefire and Failsafe configurations
2. Move all Java source files to `src/main/java/` (no package renames)
3. Merge all `src/main/resources/` contents (all five resource files have distinct names — no conflicts)
4. Move test source to `src/test/java/`
5. Fix `TurtleTestExecutor.java`: update `resolveAgentJarPath()` fallback and `createDatabaseConfig()` path
6. Remove the four old POMs and three module directories
7. Verify `mvn clean install` passes including TurtleTest
8. Update `.gitignore` — remove `.jvmxray/` entry
9. Delete `.jvmxray/` from repo root
10. Update `CLAUDE.md` to reflect new single-module structure and Java 17

---

## Success Criteria

- `mvn clean install` passes with all tests including TurtleTest
- `mvn clean` removes all build and test artifacts — no manual cleanup needed
- `target/jvmxray-0.0.1-agent.jar` is a valid Java agent (Premain-Class in manifest, all deps bundled)
- `target/jvmxray-0.0.1-mcp-client.jar` is a valid standalone JAR (Main-Class in manifest, MCP SDK SNAPSHOT JARs included)
- `.jvmxray/` no longer exists at repo root
- No package or class names changed
- `TurtleTestExecutor` fallback paths resolve to valid locations
