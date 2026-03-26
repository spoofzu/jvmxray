# Single-Project Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse three Maven modules (prj-agent, prj-common, prj-mcp-client) into one flat project so `mvn clean` removes all artifacts automatically and the build has zero inter-module dependencies.

**Architecture:** Single `pom.xml` at repo root. All Java source moves to `src/main/java/` with unchanged package names. Two shaded JARs (agent, version-tool) produced by `maven-shade-plugin`; one MCP client fat JAR produced by `maven-assembly-plugin`. Test artifacts move from `.jvmxray/` at repo root to `target/test-jvmxray/`.

**Tech Stack:** Java 17, Maven, ByteBuddy 1.14.17, Logback/SLF4J, maven-shade-plugin 3.6.1, maven-assembly-plugin 3.6.0, maven-failsafe-plugin 3.2.5

**Spec:** `docs/superpowers/specs/2026-03-24-single-project-consolidation-design.md`

---

## File Map

### Created
- `pom.xml` — replaces 4 existing POMs; contains all deps and all plugin config
- `src/main/java/org/jvmxray/agent/` — moved from `prj-agent/src/main/java/org/jvmxray/agent/`
- `src/main/java/org/jvmxray/platform/shared/` — moved from `prj-common/src/main/java/org/jvmxray/platform/shared/`
- `src/main/java/org/jvmxray/platform/client/` — moved from `prj-mcp-client/src/main/java/org/jvmxray/platform/client/`
- `src/main/resources/` — merged from all three modules (5 files, no name conflicts)
- `src/test/java/` — moved from `prj-common/src/test/java/`
- `src/test/resources/` — merged from prj-agent and prj-common test resources
- `src/assembly/standalone.xml` — moved from `prj-mcp-client/src/assembly/standalone.xml`

### Modified
- `src/main/java/org/jvmxray/platform/shared/test/TurtleTestExecutor.java` — fix two broken paths
- `.gitignore` — remove `.jvmxray/` entry

### Deleted
- `prj-agent/` — entire directory
- `prj-common/` — entire directory
- `prj-mcp-client/` — entire directory
- `.jvmxray/` — runtime artifacts no longer live at repo root

---

## Task 1: Create the consolidated pom.xml

**Files:**
- Create: `pom.xml` (replaces the existing parent POM)

This is the highest-risk step. The old parent `pom.xml` has `<packaging>pom</packaging>` and lists modules. The new one is a standard single-module `<packaging>jar</packaging>` POM. Do not delete the old module directories yet — that happens in Task 5.

> **Note — intentional deviation from spec on MCP client artifact:** The spec proposed `maven-shade-plugin` for the MCP client JAR, but `maven-shade-plugin` does not include `system`-scope dependencies by default and requires brittle workarounds. The `maven-assembly-plugin` already handles system-scope JARs correctly via the `<scope>system</scope>` dependencySet in `standalone.xml` and is the proven approach from `prj-mcp-client`. This plan retains `maven-assembly-plugin` for the MCP client JAR.

- [ ] **Step 1: Replace the root pom.xml with the consolidated version**

Replace the entire contents of `pom.xml` at repo root with the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.jvmxray</groupId>
  <artifactId>jvmxray</artifactId>
  <version>0.0.1</version>
  <packaging>jar</packaging>
  <name>jvmxray platform</name>
  <description>JVMXRay is a technology for monitoring access to system resources within the Java Virtual Machine.</description>
  <url>https://github.com/spoofzu/jvmxray/</url>

  <licenses>
    <license>
      <name>The Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>

  <developers>
    <developer>
      <name>Milton Smith</name>
      <email>milton.smith.rr@gmail.com</email>
      <organization>Self</organization>
      <organizationUrl>https://silentoctet.blogspot.com/p/milton-smith-welcome-and-thanks-for.html</organizationUrl>
    </developer>
  </developers>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>

    <!-- ── Agent: bytecode injection ─────────────────────────────────── -->
    <dependency>
      <groupId>net.bytebuddy</groupId>
      <artifactId>byte-buddy</artifactId>
      <version>1.14.17</version>
    </dependency>
    <dependency>
      <groupId>net.bytebuddy</groupId>
      <artifactId>byte-buddy-agent</artifactId>
      <version>1.14.17</version>
    </dependency>
    <dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>4.0.1</version>
      <scope>compile</scope>
    </dependency>

    <!-- ── Logging ──────────────────────────────────────────────────── -->
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.5.19</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-core</artifactId>
      <version>1.5.19</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.13</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback.contrib</groupId>
      <artifactId>logback-json-classic</artifactId>
      <version>0.1.5</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback.contrib</groupId>
      <artifactId>logback-jackson</artifactId>
      <version>0.1.5</version>
    </dependency>

    <!-- ── JSON ─────────────────────────────────────────────────────── -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.17.1</version>
    </dependency>

    <!-- ── Database drivers ─────────────────────────────────────────── -->
    <dependency>
      <groupId>com.datastax.oss</groupId>
      <artifactId>java-driver-core</artifactId>
      <version>4.13.0</version>
    </dependency>
    <dependency>
      <groupId>com.datastax.oss</groupId>
      <artifactId>java-driver-query-builder</artifactId>
      <version>4.13.0</version>
    </dependency>
    <dependency>
      <groupId>com.datastax.oss</groupId>
      <artifactId>java-driver-mapper-runtime</artifactId>
      <version>4.13.0</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <version>8.2.0</version>
    </dependency>
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>3.50.3.0</version>
    </dependency>
    <dependency>
      <groupId>com.zaxxer</groupId>
      <artifactId>HikariCP</artifactId>
      <version>5.0.1</version>
    </dependency>

    <!-- ── CLI ──────────────────────────────────────────────────────── -->
    <dependency>
      <groupId>commons-cli</groupId>
      <artifactId>commons-cli</artifactId>
      <version>1.5.0</version>
    </dependency>

    <!-- ── MCP SDK (system-scope SNAPSHOTs from libs/) ──────────────── -->
    <dependency>
      <groupId>io.modelcontextprotocol.sdk</groupId>
      <artifactId>mcp</artifactId>
      <version>0.13.0-SNAPSHOT</version>
      <scope>system</scope>
      <systemPath>${basedir}/libs/mcp-0.13.0-SNAPSHOT.jar</systemPath>
    </dependency>
    <dependency>
      <groupId>io.modelcontextprotocol.sdk</groupId>
      <artifactId>mcp-json-jackson2</artifactId>
      <version>0.13.0-SNAPSHOT</version>
      <scope>system</scope>
      <systemPath>${basedir}/libs/mcp-json-jackson2-0.13.0-SNAPSHOT.jar</systemPath>
    </dependency>

    <!-- ── MCP runtime support ──────────────────────────────────────── -->
    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpclient</artifactId>
      <version>4.5.14</version>
    </dependency>
    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpcore</artifactId>
      <version>4.4.16</version>
    </dependency>
    <dependency>
      <groupId>io.projectreactor</groupId>
      <artifactId>reactor-core</artifactId>
      <version>3.7.0</version>
    </dependency>

    <!-- ── Test ─────────────────────────────────────────────────────── -->
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.13.1</version>
      <scope>test</scope>
    </dependency>

  </dependencies>

  <build>
    <plugins>

      <!-- ── Compiler ─────────────────────────────────────────────── -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.10.1</version>
        <configuration>
          <source>17</source>
          <target>17</target>
        </configuration>
      </plugin>

      <!-- ── Git commit ID for version tracking ───────────────────── -->
      <plugin>
        <groupId>pl.project13.maven</groupId>
        <artifactId>git-commit-id-plugin</artifactId>
        <version>4.0.0</version>
        <executions>
          <execution>
            <id>get-the-git-infos</id>
            <goals><goal>revision</goal></goals>
            <phase>initialize</phase>
          </execution>
        </executions>
        <configuration>
          <generateGitPropertiesFile>false</generateGitPropertiesFile>
          <failOnNoGitDirectory>false</failOnNoGitDirectory>
        </configuration>
      </plugin>

      <!-- ── Unshaded JAR (standard output) ───────────────────────── -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.2.0</version>
        <configuration>
          <archive>
            <manifest>
              <mainClass>org.jvmxray.platform.shared.bin.VersionTool</mainClass>
            </manifest>
            <manifestEntries>
              <Implementation-Version>${project.version}</Implementation-Version>
              <Git-Commit>${git.commit.id.abbrev}</Git-Commit>
              <Build-Time>${maven.build.timestamp}</Build-Time>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>

      <!-- ── Shade: agent + version-tool ──────────────────────────── -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.1</version>
        <executions>

          <!-- Execution 1: agent shaded JAR -->
          <execution>
            <id>shade-agent</id>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
              <shadedArtifactAttached>true</shadedArtifactAttached>
              <shadedClassifierName>agent</shadedClassifierName>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <relocations>
                <relocation>
                  <pattern>org.slf4j</pattern>
                  <shadedPattern>agent.shadow.slf4j</shadedPattern>
                </relocation>
                <relocation>
                  <pattern>ch.qos.logback</pattern>
                  <shadedPattern>agent.shadow.logback</shadedPattern>
                </relocation>
              </relocations>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>logback.xml</exclude>
                    <exclude>logback-test.xml</exclude>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <manifestEntries>
                    <Premain-Class>org.jvmxray.agent.bootstrap.AgentBootstrap</Premain-Class>
                    <Can-Redefine-Classes>true</Can-Redefine-Classes>
                    <Can-Retransform-Classes>true</Can-Retransform-Classes>
                    <Implementation-Version>${project.version}</Implementation-Version>
                    <Git-Commit>${git.commit.id.abbrev}</Git-Commit>
                    <Build-Time>${maven.build.timestamp}</Build-Time>
                  </manifestEntries>
                </transformer>
              </transformers>
            </configuration>
          </execution>

          <!-- Execution 2: version-tool shaded JAR -->
          <execution>
            <id>shade-version-tool</id>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
              <shadedArtifactAttached>true</shadedArtifactAttached>
              <shadedClassifierName>version-tool</shadedClassifierName>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>org.jvmxray.platform.shared.bin.VersionTool</mainClass>
                  <manifestEntries>
                    <Implementation-Version>${project.version}</Implementation-Version>
                    <Git-Commit>${git.commit.id.abbrev}</Git-Commit>
                    <Build-Time>${maven.build.timestamp}</Build-Time>
                  </manifestEntries>
                </transformer>
              </transformers>
            </configuration>
          </execution>

        </executions>
      </plugin>

      <!-- ── Assembly: MCP client fat JAR (handles system-scope JARs) -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <version>3.6.0</version>
        <configuration>
          <descriptors>
            <descriptor>src/assembly/standalone.xml</descriptor>
          </descriptors>
          <finalName>jvmxray-mcp-client</finalName>
          <appendAssemblyId>false</appendAssemblyId>
          <archive>
            <manifest>
              <mainClass>org.jvmxray.platform.client.mcp.bridge.JvmxrayMcpClient</mainClass>
            </manifest>
            <manifestEntries>
              <Implementation-Version>${project.version}</Implementation-Version>
              <Git-Commit>${git.commit.id.abbrev}</Git-Commit>
              <Build-Time>${maven.build.timestamp}</Build-Time>
            </manifestEntries>
          </archive>
        </configuration>
        <executions>
          <execution>
            <id>make-mcp-client</id>
            <phase>package</phase>
            <goals><goal>single</goal></goals>
          </execution>
        </executions>
      </plugin>

      <!-- ── Antrun: create test dirs + seed configs + run SchemaManager -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-antrun-plugin</artifactId>
        <version>3.1.0</version>
        <executions>

          <!-- Setup agent test directories and logback config -->
          <execution>
            <id>prepare-agent-test-home</id>
            <phase>process-resources</phase>
            <goals><goal>run</goal></goals>
            <configuration>
              <target>
                <mkdir dir="${project.build.directory}/test-jvmxray/agent/config"/>
                <mkdir dir="${project.build.directory}/test-jvmxray/agent/logs"/>
                <copy file="${project.basedir}/src/main/resources/agent-logback-production.xml2"
                      tofile="${project.build.directory}/test-jvmxray/agent/config/logback.xml"
                      overwrite="false"/>
                <echo message="Seeded test-jvmxray/agent with config/logs"/>
              </target>
            </configuration>
          </execution>

          <!-- Create common test dirs and SQLite test schema -->
          <execution>
            <id>create-test-schema</id>
            <phase>process-test-resources</phase>
            <goals><goal>run</goal></goals>
            <configuration>
              <target>
                <mkdir dir="${project.build.directory}/test-jvmxray/common/data"/>
                <mkdir dir="${project.build.directory}/test-jvmxray/common/config"/>
                <mkdir dir="${project.build.directory}/test-jvmxray/common/logs"/>
                <echo message="Created test-jvmxray/common directory structure"/>
                <java classname="org.jvmxray.platform.shared.bin.SchemaManager"
                      fork="true"
                      failonerror="false">
                  <classpath>
                    <path refid="maven.compile.classpath"/>
                    <pathelement location="${project.build.outputDirectory}"/>
                    <pathelement location="${project.build.testOutputDirectory}"/>
                  </classpath>
                  <arg value="--create-schema"/>
                  <arg value="--database-type"/>
                  <arg value="sqlite"/>
                  <arg value="--connection-url"/>
                  <arg value="jdbc:sqlite:${project.build.directory}/test-jvmxray/common/data/jvmxray-test.db"/>
                  <sysproperty key="jvmxray.test.home" value="${project.build.directory}/test-jvmxray"/>
                </java>
                <echo message="Test database schema creation completed"/>
              </target>
            </configuration>
          </execution>

        </executions>
      </plugin>

      <!-- ── Surefire: unit tests ──────────────────────────────────── -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
        <configuration>
          <systemPropertyVariables>
            <jvmxray.test.home>${project.build.directory}/test-jvmxray</jvmxray.test.home>
            <jvmxray.common.logs>${project.build.directory}/test-jvmxray/common/logs</jvmxray.common.logs>
            <jvmxray.common.config>${project.build.directory}/test-jvmxray/common/config</jvmxray.common.config>
            <jvmxray.common.data>${project.build.directory}/test-jvmxray/common/data</jvmxray.common.data>
            <logback.common.configurationFile>${project.build.directory}/test-jvmxray/common/config/logback.xml</logback.common.configurationFile>
            <project.basedir>${project.basedir}</project.basedir>
          </systemPropertyVariables>
        </configuration>
      </plugin>

      <!-- ── Failsafe: TurtleTest integration ─────────────────────── -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <version>3.2.5</version>
        <executions>
          <execution>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
            <configuration>
              <includes>
                <include>**/TurtleTest.java</include>
              </includes>
              <systemPropertyVariables>
                <jvmxray.test.home>${project.build.directory}/test-jvmxray</jvmxray.test.home>
                <project.basedir>${project.basedir}</project.basedir>
                <jvmxray.agent.jar>${project.build.directory}/jvmxray-${project.version}-agent.jar</jvmxray.agent.jar>
                <jvmxray.common.logs>${project.build.directory}/test-jvmxray/common/logs</jvmxray.common.logs>
                <jvmxray.common.config>${project.build.directory}/test-jvmxray/common/config</jvmxray.common.config>
                <jvmxray.common.data>${project.build.directory}/test-jvmxray/common/data</jvmxray.common.data>
              </systemPropertyVariables>
            </configuration>
          </execution>
        </executions>
      </plugin>

    </plugins>
  </build>

</project>
```

- [ ] **Step 2: Verify the new POM parses**

```bash
mvn help:effective-pom -N 2>&1 | head -40
```

Expected: POM output starting with effective POM XML (no "Could not find artifact" errors about prj-common/prj-agent yet — the source doesn't exist yet so that's expected).

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "refactor: replace multi-module parent pom with single-project pom (Java 17)"
```

---

## Task 2: Create src/ tree and move main Java source

**Files:**
- Create directories: `src/main/java/`, `src/main/resources/`, `src/test/java/`, `src/test/resources/`, `src/assembly/`
- Move: all Java source files from three modules (no renames)

- [ ] **Step 1: Create the top-level src/ directory skeleton**

```bash
mkdir -p src/main/java src/main/resources src/test/java src/test/resources src/assembly
```

- [ ] **Step 2: Move the agent source tree**

```bash
git mv prj-agent/src/main/java/org/jvmxray/agent src/main/java/org/jvmxray/agent
```

Expected: no errors. Verify: `ls src/main/java/org/jvmxray/agent/` shows `bin bootstrap guard init log proxy sensor util`.

- [ ] **Step 3: Move the common (platform/shared) source tree**

```bash
git mv prj-common/src/main/java/org/jvmxray/platform src/main/java/org/jvmxray/platform
```

Expected: no errors. Verify: `ls src/main/java/org/jvmxray/platform/shared/` shows `bin classloader init log property schema test util`.

- [ ] **Step 4: Move the MCP client source**

```bash
mkdir -p src/main/java/org/jvmxray/platform/client/mcp/bridge
git mv prj-mcp-client/src/main/java/org/jvmxray/platform/client/mcp/bridge/JvmxrayMcpClient.java \
       src/main/java/org/jvmxray/platform/client/mcp/bridge/JvmxrayMcpClient.java
```

Expected: no errors. Verify: `ls src/main/java/org/jvmxray/platform/client/mcp/bridge/` shows `JvmxrayMcpClient.java`.

- [ ] **Step 5: Move resources (all 5 files, distinct names)**

```bash
git mv prj-agent/src/main/resources/agent-logback-production.xml2  src/main/resources/
git mv prj-agent/src/main/resources/agent-logback-shaded.xml2      src/main/resources/
git mv prj-agent/src/main/resources/agent.properties               src/main/resources/
git mv prj-common/src/main/resources/common-logback-production.xml  src/main/resources/
git mv prj-common/src/main/resources/common-logback-test.xml2       src/main/resources/
```

- [ ] **Step 6: Move test source**

```bash
git mv prj-common/src/test/java/org/jvmxray/shared src/test/java/org/jvmxray/shared
```

Expected: `ls src/test/java/org/jvmxray/shared/` shows `integration unittest`.

- [ ] **Step 7: Move test resources (both modules)**

```bash
git mv prj-agent/src/test/resources/agent-logback-test.xml2   src/test/resources/
git mv prj-common/src/test/resources/common-logback-test.xml2  src/test/resources/
```

Note: `common-logback-test.xml2` exists in both `prj-common/src/main/resources/` (moved to `src/main/resources/` in Step 5) and `prj-common/src/test/resources/` (moved to `src/test/resources/` here). Same filename but different directories — this is correct.

- [ ] **Step 8: Move the MCP assembly descriptor**

```bash
git mv prj-mcp-client/src/assembly/standalone.xml src/assembly/standalone.xml
```

- [ ] **Step 9: Verify file counts**

```bash
find src/main/java -name "*.java" | wc -l
```

Expected: **146** (103 from agent + 42 from common + 1 MCP client).

- [ ] **Step 10: Attempt compilation**

```bash
mvn clean compile -pl . 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`. The `TurtleTestExecutor` fix happens in Task 3 — that fix is a runtime path correction, not a syntax fix, so compilation succeeds before Task 3. If there are compile errors here, they indicate missing dependencies or incorrect paths in the POM; fix those before proceeding.

- [ ] **Step 11: Commit**

```bash
git add src/
git commit -m "refactor: move all Java source to flat src/main/java and src/test/java"
```

---

## Task 3: Fix TurtleTestExecutor.java

**Files:**
- Modify: `src/main/java/org/jvmxray/platform/shared/test/TurtleTestExecutor.java`

Two methods use `testHome.getParent()` to navigate to repo-root-relative paths. After consolidation `testHome` = `target/test-jvmxray/` so `getParent()` = `target/` — both paths break.

- [ ] **Step 1: Fix resolveAgentJarPath() fallback (line ~128)**

Find this block:
```java
agentJar = Paths.get(testHome).getParent()
    .resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar")
    .toString();
```

Replace with:
```java
agentJar = Paths.get(testHome).getParent()
    .resolve("jvmxray-0.0.1-agent.jar")
    .toString();
```

Also update the error message at the throw below it — replace "ensure 'mvn install' has been run on prj-agent module first" with "ensure 'mvn install' has been run first".

- [ ] **Step 2: Fix createDatabaseConfig() path (line ~289)**

Find this block:
```java
if (testHome != null) {
    agentLogbackDbConfig = Paths.get(testHome).getParent()
        .resolve("script/config/logback/agent-db.xml")
        .toString();
}
```

Replace with:
```java
String projectBasedir = System.getProperty("project.basedir");
if (projectBasedir != null) {
    agentLogbackDbConfig = Paths.get(projectBasedir)
        .resolve("script/config/logback/agent-db.xml")
        .toString();
}
```

- [ ] **Step 3: Compile to verify no syntax errors**

```bash
mvn clean compile -pl . 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/org/jvmxray/platform/shared/test/TurtleTestExecutor.java
git commit -m "fix: update TurtleTestExecutor paths for consolidated project structure"
```

---

## Task 4: Run unit tests

- [ ] **Step 1: Run unit tests only (skipping integration tests)**

```bash
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean test 2>&1 | tail -40
```

Expected: `BUILD SUCCESS`. All unit tests in `src/test/java/org/jvmxray/shared/unittest/` pass.

If tests fail, read the failure message carefully. Common issues:
- Missing system property → check Surefire `<systemPropertyVariables>` in pom.xml
- ClassNotFoundException → missing dependency in the consolidated pom.xml
- FileNotFoundException → a test resource path using the old `.jvmxray/` layout

- [ ] **Step 2: Fix any failures, then re-run**

```bash
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean test 2>&1 | tail -40
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit fixes (if any)**

```bash
git add -p
git commit -m "fix: resolve unit test failures after consolidation"
```

---

## Task 5: Delete old module directories

Only do this step after compilation and unit tests pass.

- [ ] **Step 1: Remove the three module directories**

```bash
git rm -r prj-agent prj-common prj-mcp-client
```

Expected: Many `delete mode` lines. No errors.

- [ ] **Step 2: Verify nothing important was missed**

```bash
git status
```

Expected: staged deletions only. Check that the deleted files were Java source (already moved in Task 2), POMs, and empty resource directories.

- [ ] **Step 3: Commit**

```bash
git commit -m "refactor: delete prj-agent, prj-common, prj-mcp-client module directories"
```

---

## Task 6: Run full build with integration tests

- [ ] **Step 1: Run full build**

```bash
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean install 2>&1 | tail -60
```

Expected: `BUILD SUCCESS` with TurtleTest passing and sensor events appearing in output.

- [ ] **Step 2: Verify the agent JAR is a valid Java agent**

```bash
jar tf target/jvmxray-0.0.1-agent.jar | grep -i "MANIFEST" && \
unzip -p target/jvmxray-0.0.1-agent.jar META-INF/MANIFEST.MF | grep -E "Premain|Can-Redefine|Can-Retransform"
```

Expected output:
```
Premain-Class: org.jvmxray.agent.bootstrap.AgentBootstrap
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```

- [ ] **Step 3: Verify the MCP client JAR has a Main-Class**

```bash
unzip -p target/jvmxray-mcp-client.jar META-INF/MANIFEST.MF | grep Main-Class
```

Expected:
```
Main-Class: org.jvmxray.platform.client.mcp.bridge.JvmxrayMcpClient
```

- [ ] **Step 4: Verify mvn clean removes all artifacts**

```bash
mvn clean && ls target/ 2>/dev/null && echo "target/ still exists after clean" || echo "target/ gone"
ls .jvmxray/ 2>/dev/null && echo ".jvmxray still exists" || echo ".jvmxray gone (expected — to be deleted in Task 7)"
```

Expected: `target/` is gone (or empty). `.jvmxray/` still exists — that's expected, it gets removed in Task 7.

- [ ] **Step 5: Fix any integration test failures, then re-run**

If TurtleTest fails, look for these causes first:
- Agent JAR path: verify `jvmxray.agent.jar` property in Failsafe points to the correct file
- Test home not set up: check antrun `prepare-agent-test-home` and `create-test-schema` executions ran
- Classpath issues: agent JAR might be missing a dependency

After fixing:
```bash
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean install 2>&1 | tail -60
```

- [ ] **Step 6: Commit fixes (if any)**

```bash
git add -p
git commit -m "fix: resolve integration test failures after consolidation"
```

---

## Task 7: Cleanup

- [ ] **Step 1: Remove .jvmxray/ from the filesystem**

`.jvmxray/` is listed in `.gitignore` so git does not track it — `git rm` will fail. Use a plain filesystem delete:

```bash
rm -rf .jvmxray/
```

- [ ] **Step 2: Update .gitignore — remove the .jvmxray/ entry**

Open `.gitignore` and delete the line containing `.jvmxray/`. Also add `target/` if not already present.

- [ ] **Step 3: Update CLAUDE.md — architecture section**

In `CLAUDE.md`, update the Multi-Module Maven Structure section to reflect the new layout:

```markdown
### Single-Module Maven Structure
```
jvmxray/
├── src/
│   ├── main/java/org/jvmxray/
│   │   ├── agent/          # Java agent with bytecode injection sensors
│   │   └── platform/
│   │       ├── shared/     # Shared utilities, models, database schema management
│   │       └── client/mcp/ # MCP (Model Context Protocol) client
│   └── test/               # Integration tests (TurtleTest) and unit tests
└── pom.xml                 # Single POM, Java 17
```

Update the Java version references:
- Change "Java 17 for most modules" to "Java 17 (single project)"
- Remove the exceptions listing Java 11 for prj-agent and Java 21 for prj-mcp-client

Update the Key Build Artifacts section:
- `target/jvmxray-0.0.1-agent.jar` — Deployable Java agent JAR (shaded, classifier: agent)
- `target/jvmxray-mcp-client.jar` — MCP client standalone JAR
- `target/jvmxray-0.0.1-version-tool.jar` — VersionTool standalone JAR (classifier: version-tool)

Update the Database Schema Commands section — the classpath construction changes from using `prj-common/pom.xml` to using the root `pom.xml`.

- [ ] **Step 4: Final build verification**

```bash
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean install 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit everything**

```bash
git add .gitignore CLAUDE.md
git commit -m "chore: remove .jvmxray/ from repo, update .gitignore and CLAUDE.md for single-project structure"
```

---

## Verification Checklist

After all tasks complete, confirm:

- [ ] `mvn clean install` passes with TurtleTest showing sensor events
- [ ] `mvn clean` removes `target/` — no leftover test artifacts
- [ ] `target/jvmxray-0.0.1-agent.jar` has `Premain-Class` in manifest
- [ ] `target/jvmxray-mcp-client.jar` has `Main-Class` in manifest
- [ ] `.jvmxray/` no longer exists at repo root
- [ ] No package or class names changed (verify with `git log --oneline --diff-filter=R` — renames only, no modifications to imports)
- [ ] `find . -name "pom.xml" | grep -v target` returns only `./pom.xml`
