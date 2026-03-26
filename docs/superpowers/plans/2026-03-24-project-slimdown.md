# Project Slimdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove prj-service-log, prj-service-rest, and prj-service-ai modules plus their dead code, retaining prj-agent, prj-common (trimmed), and prj-mcp-client in a clean, buildable state.

**Architecture:** Pure deletion and cleanup — no new code is written. The three service Maven modules are removed entirely. Three service-pipeline bin tool classes are deleted from prj-common. Dead service-property references are removed from ComponentInitializer. All supporting scripts and docs for removed services are deleted or updated.

**Tech Stack:** Java 17/11, Maven multi-module build, JUnit 4, ByteBuddy (agent)

**Spec:** `~/Desktop/JVMXRay/2026-03-24-project-slimdown-design.md`

---

## Task 1: Delete Service Modules and Update Parent POM

**Files:**
- Delete: `prj-service-log/` (entire directory)
- Delete: `prj-service-rest/` (entire directory)
- Delete: `prj-service-ai/` (entire directory)
- Modify: `pom.xml` (root parent POM — remove three `<module>` entries)

- [ ] **Step 1: Delete the three service module directories**

```bash
rm -rf /Users/milton/github/jvmxray/prj-service-log
rm -rf /Users/milton/github/jvmxray/prj-service-rest
rm -rf /Users/milton/github/jvmxray/prj-service-ai
```

- [ ] **Step 2: Remove service module entries from parent pom.xml**

Open `pom.xml` at the project root. Find the `<modules>` block (around lines 26-32). It currently contains:

```xml
<modules>
    <module>prj-common</module>
    <module>prj-agent</module>
    <module>prj-service-log</module>
    <module>prj-service-rest</module>
    <module>prj-service-ai</module>
</modules>
```

Remove the three service lines, leaving:

```xml
<modules>
    <module>prj-common</module>
    <module>prj-agent</module>
</modules>
```

**Important:** Do NOT touch the `<profiles>` section further down — `prj-mcp-client` is activated via a profile (`!skipMcpClient`) and must remain there.

- [ ] **Step 3: Verify the module block looks correct**

```bash
grep -A 10 '<modules>' /Users/milton/github/jvmxray/pom.xml
```

Expected: only `prj-common` and `prj-agent` in the modules block. Profile block for mcp-client unchanged.

- [ ] **Step 4: Quick build check (compile only)**

```bash
cd /Users/milton/github/jvmxray && mvn clean compile -q
```

Expected: BUILD SUCCESS. No references to deleted modules should cause compile failure.

- [ ] **Step 5: Commit**

```bash
cd /Users/milton/github/jvmxray
git add -A
git commit -m "Remove prj-service-log, prj-service-rest, prj-service-ai modules"
```

---

## Task 2: Delete Service-Pipeline Bin Tools from prj-common

**Files:**
- Delete: `prj-common/src/main/java/org/jvmxray/platform/shared/bin/CpStage0ToStage1.java`
- Delete: `prj-common/src/main/java/org/jvmxray/platform/shared/bin/CpStage1ToStage2.java`
- Delete: `prj-common/src/main/java/org/jvmxray/platform/shared/bin/TestDataGenerator.java`

- [ ] **Step 1: Verify no other files import these classes**

```bash
grep -r "CpStage0ToStage1\|CpStage1ToStage2\|TestDataGenerator" \
  /Users/milton/github/jvmxray/prj-common/src \
  /Users/milton/github/jvmxray/prj-agent/src \
  /Users/milton/github/jvmxray/prj-mcp-client/src
```

Expected: no matches. These classes are self-contained entry points with no callers in the remaining modules.

- [ ] **Step 2: Delete the three files**

```bash
rm /Users/milton/github/jvmxray/prj-common/src/main/java/org/jvmxray/platform/shared/bin/CpStage0ToStage1.java
rm /Users/milton/github/jvmxray/prj-common/src/main/java/org/jvmxray/platform/shared/bin/CpStage1ToStage2.java
rm /Users/milton/github/jvmxray/prj-common/src/main/java/org/jvmxray/platform/shared/bin/TestDataGenerator.java
```

- [ ] **Step 3: Compile check**

```bash
cd /Users/milton/github/jvmxray && mvn clean compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /Users/milton/github/jvmxray
git add -A
git commit -m "Remove service-pipeline bin tools from prj-common"
```

---

## Task 3: Clean Dead Service Properties from ComponentInitializer

**Files:**
- Modify: `prj-common/src/main/java/org/jvmxray/platform/shared/init/ComponentInitializer.java`

The `ensureEarlySystemPropertiesInitialization()` method (lines ~416–430) sets system properties for `logservice` and `aiservice` — services that no longer exist. These must be removed.

- [ ] **Step 1: Remove dead service-property blocks**

Open `ComponentInitializer.java`. Find and delete lines 416–430 (the logservice and aiservice blocks):

```java
        // Set logservice properties early if not already set
        if (System.getProperty("jvmxray.logservice.logs") == null) {
            System.setProperty("jvmxray.logservice.logs", baseDir + "/logservice/logs");
        }
        if (System.getProperty("jvmxray.logservice.config") == null) {
            System.setProperty("jvmxray.logservice.config", baseDir + "/logservice/config");
        }

        // Set aiservice properties early if not already set
        if (System.getProperty("jvmxray.aiservice.logs") == null) {
            System.setProperty("jvmxray.aiservice.logs", baseDir + "/aiservice/logs");
        }
        if (System.getProperty("jvmxray.aiservice.config") == null) {
            System.setProperty("jvmxray.aiservice.config", baseDir + "/aiservice/config");
        }
```

The method should end after the `jvmxray.common.data` block (line ~414) with a closing `}`.

- [ ] **Step 2: Verify no remaining logservice/aiservice references in init/**

```bash
grep -r "logservice\|aiservice" /Users/milton/github/jvmxray/prj-common/src/main/java/org/jvmxray/platform/shared/init/
```

Expected: no matches.

- [ ] **Step 3: Compile check**

```bash
cd /Users/milton/github/jvmxray && mvn clean compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /Users/milton/github/jvmxray
git add prj-common/src/main/java/org/jvmxray/platform/shared/init/ComponentInitializer.java
git commit -m "Remove dead logservice/aiservice property initialization from ComponentInitializer"
```

---

## Task 4: Delete Service Scripts

**Files:**
- Delete: `script/services/ai-service`
- Delete: `script/services/log-service`
- Delete: `script/services/rest-service`
- Delete: `script/data/generate-test-data`
- Delete: `script/data/migrate-stage-data`
- Delete: `script/data/enrich-stage2-data`

- [ ] **Step 1: Delete service scripts**

```bash
rm /Users/milton/github/jvmxray/script/services/ai-service
rm /Users/milton/github/jvmxray/script/services/log-service
rm /Users/milton/github/jvmxray/script/services/rest-service
```

- [ ] **Step 2: Delete data pipeline scripts**

```bash
rm /Users/milton/github/jvmxray/script/data/generate-test-data
rm /Users/milton/github/jvmxray/script/data/migrate-stage-data
rm /Users/milton/github/jvmxray/script/data/enrich-stage2-data
```

- [ ] **Step 3: Verify script/services/ and script/data/ are now empty**

```bash
ls /Users/milton/github/jvmxray/script/services/
ls /Users/milton/github/jvmxray/script/data/
```

Expected: both directories empty (or show only hidden files).

- [ ] **Step 4: Commit**

```bash
cd /Users/milton/github/jvmxray
git add -A
git commit -m "Remove service and data pipeline scripts"
```

---

## Task 5: Delete Service Docs

**Files:**
- Delete: `docs/prj-service-ai.md`
- Delete: `docs/prj-service-log.md`
- Delete: `docs/prj-service-rest.md`
- Delete: `docs/code-complexity-report.md`

- [ ] **Step 1: Delete service documentation**

```bash
rm /Users/milton/github/jvmxray/docs/prj-service-ai.md
rm /Users/milton/github/jvmxray/docs/prj-service-log.md
rm /Users/milton/github/jvmxray/docs/prj-service-rest.md
rm /Users/milton/github/jvmxray/docs/code-complexity-report.md
```

- [ ] **Step 2: Commit**

```bash
cd /Users/milton/github/jvmxray
git add -A
git commit -m "Remove service docs and stale complexity report"
```

---

## Task 6: Update Docs — CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

Two specific items must be removed:

1. `LogServiceInitializer`, `RestServiceInitializer`, `AiServiceInitializer` in the **Key Components** list under Component Initialization Architecture
2. The `script/services/log-service` example in the **Script Design Principles** / Minimal Wrapper Pattern section
3. Any other references to the three removed modules throughout the file

- [ ] **Step 1: Find all service references in CLAUDE.md**

```bash
grep -n "service-log\|service-rest\|service-ai\|LogService\|RestService\|AiService\|log-service\|rest-service\|ai-service" /Users/milton/github/jvmxray/CLAUDE.md
```

Note each line number for targeted edits.

- [ ] **Step 2: Remove LogServiceInitializer, RestServiceInitializer, AiServiceInitializer from Key Components list**

Find the bullet list under "Key Components" in the Component Initialization Architecture section. Remove the three service initializer entries. Keep `ComponentInitializer`, `AgentInitializer`, `CommonInitializer`.

- [ ] **Step 3: Remove script/services/log-service example from Script Design Principles**

Find the Minimal Wrapper Pattern section. Remove the `script/services/log-service` example reference.

- [ ] **Step 4: Remove remaining service module references**

Remove references to `prj-service-log`, `prj-service-rest`, `prj-service-ai` from the module structure diagram and any other locations found in Step 1.

- [ ] **Step 5: Verify no remaining references**

```bash
grep -n "service-log\|service-rest\|service-ai\|LogServiceInitializer\|RestServiceInitializer\|AiServiceInitializer" /Users/milton/github/jvmxray/CLAUDE.md
```

Expected: no matches.

- [ ] **Step 6: Commit**

```bash
cd /Users/milton/github/jvmxray
git add CLAUDE.md
git commit -m "Update CLAUDE.md: remove service module references"
```

---

## Task 7: Update Docs — README.md

**Files:**
- Modify: `README.md`

README.md describes the full six-module platform. It needs to be updated to reflect the three-module slimmed project.

- [ ] **Step 1: Find service references in README.md**

```bash
grep -n "service-log\|service-rest\|service-ai\|prj-service\|LogService\|RestService\|AiService" /Users/milton/github/jvmxray/README.md | head -30
```

- [ ] **Step 2: Update the module structure / architecture sections**

Remove or update any sections describing the service modules. The focus should shift to:
- Agent (bytecode injection, sensors)
- MCP client (Claude Desktop integration)
- Common (shared utilities, schema management, turtle test)

Remove service doc links (lines ~231, 234 per spec review findings).

- [ ] **Step 3: Verify no broken service links remain**

```bash
grep -n "prj-service\|service-log\|service-rest\|service-ai" /Users/milton/github/jvmxray/README.md
```

Expected: no matches.

- [ ] **Step 4: Commit**

```bash
cd /Users/milton/github/jvmxray
git add README.md
git commit -m "Update README.md: remove service modules, focus on agent and MCP"
```

---

## Task 8: Update Docs — setup-continued.md and Module Docs

**Files:**
- Modify: `docs/setup-continued.md`
- Review/Modify: `docs/prj-common.md`
- Review/Modify: `docs/prj-agent.md` (likely no-op)
- Review/Modify: `docs/prj-mcp-client.md` (likely no-op)

- [ ] **Step 1: Trim docs/setup-continued.md**

The file contains Steps 5 and 6 that describe initializing and starting the AI Service, and references to `script/services/ai-service`. Remove all service setup steps. Keep only content relevant to the agent and MCP.

```bash
grep -n "aiservice\|logservice\|restservice\|ai-service\|log-service\|rest-service\|prj-service" /Users/milton/github/jvmxray/docs/setup-continued.md
```

Remove every section referencing those services.

- [ ] **Step 2: Clean docs/prj-common.md**

```bash
grep -n "prj-service\|CpStage\|TestDataGenerator\|LogServiceInitializer\|RestServiceInitializer\|AiServiceInitializer" /Users/milton/github/jvmxray/docs/prj-common.md
```

Remove all references found — specifically the known `prj-service-log` reference near line 801, plus any mentions of the deleted bin tools and service initializers.

- [ ] **Step 3: Check docs/prj-agent.md for service references**

```bash
grep -n "prj-service\|log-service\|rest-service\|ai-service" /Users/milton/github/jvmxray/docs/prj-agent.md
```

If any matches: remove them. If no matches: no action needed.

- [ ] **Step 4: Check docs/prj-mcp-client.md for service references**

```bash
grep -n "prj-service\|log-service\|rest-service\|ai-service" /Users/milton/github/jvmxray/docs/prj-mcp-client.md
```

If any matches: remove them. If no matches: no action needed.

- [ ] **Step 5: Commit**

```bash
cd /Users/milton/github/jvmxray
git add docs/
git commit -m "Update module docs: remove service references"
```

---

## Task 9: Full Build Verification

- [ ] **Step 1: Run full build with tests**

```bash
cd /Users/milton/github/jvmxray && MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean install 2>&1 | tail -50
```

Expected: `BUILD SUCCESS`. Turtle integration test should pass and show sensor events in output.

- [ ] **Step 2: Confirm turtle test ran and passed**

```bash
grep -A 5 "TurtleIntegrationTest\|Turtle" /Users/milton/github/jvmxray/prj-common/target/surefire-reports/*.txt 2>/dev/null | head -30
```

Expected: test results showing the turtle test ran with no failures.

- [ ] **Step 3: Verify no remaining references to removed modules in build output or source**

```bash
grep -r "prj-service-log\|prj-service-rest\|prj-service-ai" \
  /Users/milton/github/jvmxray/pom.xml \
  /Users/milton/github/jvmxray/prj-common \
  /Users/milton/github/jvmxray/prj-agent \
  /Users/milton/github/jvmxray/prj-mcp-client 2>/dev/null
```

Expected: no matches.

- [ ] **Step 4: Confirm prj-mcp-client builds cleanly**

```bash
cd /Users/milton/github/jvmxray && mvn clean install -pl prj-mcp-client -am -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Final commit if any cleanup was needed**

If any loose ends were cleaned up during verification, commit them:

```bash
cd /Users/milton/github/jvmxray
git add -A
git commit -m "Final cleanup after project slimdown verification"
```

---

## Known Pre-existing Issue (Do Not Fix in This Task)

`CommonInitializer.getResourcesToCopy()` requests `"common-logback-production.xml2"` but the actual resource file is named `"common-logback-production.xml"` (no `.xml2` suffix). This causes the production logback copy to silently fail when `jvmxray.test.home` is not set. This is a pre-existing bug — do not mask it or fix it here. Leave it for a follow-up task.

---

## Success Criteria

- [ ] `mvn clean install` passes with no errors
- [ ] Turtle integration test passes and shows sensor events in output
- [ ] `prj-mcp-client` builds cleanly
- [ ] No references to `prj-service-log`, `prj-service-rest`, or `prj-service-ai` in source or build files
- [ ] `ComponentInitializer` static block contains no `logservice` or `aiservice` property references
- [ ] `script/services/` and `script/data/` contain no scripts invoking deleted Java classes
