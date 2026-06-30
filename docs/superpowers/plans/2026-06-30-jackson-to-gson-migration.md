# Jackson → Gson Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all FasterXML Jackson (`com.fasterxml.jackson.*`) from the build, replacing the one real consumer (`XRCSVEncoder`) with Gson.

**Architecture:** Jackson reaches the build through two paths — a direct `jackson-databind` dependency used only by `XRCSVEncoder`, and a transitive pull from the `logback-jackson` contrib add-on. Both are closed: `XRCSVEncoder` is rewritten against the Gson API (a 1:1 mechanical swap with byte-identical CSV output), and the unused `logback-jackson` + `logback-json-classic` contrib modules are deleted from the POM. Core logback (`logback-classic`/`logback-core`) never depended on Jackson and is untouched.

**Tech Stack:** Java, Maven, JUnit 4.13.1, Gson 2.10.1, logback-classic 1.5.19.

## Global Constraints

- Gson version: exactly `2.10.1` (matches the mcp-server worktree).
- Do **not** touch the `org.jvmxray.agent.sensor.serialization` package — it references Jackson by string class name for bytecode instrumentation of monitored apps; it is not a library dependency.
- Do **not** add a Cassandra-driver exclusion — the transitive `org.codehaus.jackson:jackson-core-asl:1.9.12` (legacy, unrelated) is explicitly out of scope.
- Preserve `XRCSVEncoder`'s output exactly: same CSV fields, same order, byte-for-byte.
- Build must compile and the full `mvn test` suite must pass at each task's commit boundary.

---

## File Structure

- `pom.xml` — remove 3 Jackson-related dependency blocks, add 1 Gson block.
- `src/main/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoder.java` — swap Jackson API for Gson.
- `src/test/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoderTest.java` — new characterization test (none exists today).

---

## Task 1: Characterization test for XRCSVEncoder

Capture `XRCSVEncoder`'s current CSV output as a regression guard **before** changing anything. This is a refactor with identical output, so the test passes on the existing Jackson implementation (establishing the baseline) and must still pass after the Gson migration. It does not go "red" — that is correct for a behavior-preserving refactor.

**Files:**
- Test: `src/test/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoderTest.java`

**Interfaces:**
- Consumes: `XRCSVEncoder()` (no-arg ctor), `XRCSVEncoder.encode(ILoggingEvent): byte[]` (inherited public override).
- Produces: nothing for later tasks (test-only).

- [ ] **Step 1: Write the characterization test**

Create `src/test/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoderTest.java`:

```java
package org.jvmxray.platform.shared.log.logback.codec;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XRCSVEncoderTest {

    private XRCSVEncoder newStartedEncoder(LoggerContext context) {
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%msg");
        layout.start();

        XRCSVEncoder encoder = new XRCSVEncoder();
        encoder.setContext(context);
        encoder.setLayout(layout);
        encoder.start();
        return encoder;
    }

    @Test
    public void encodesCoreFieldsAsCsv() {
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("test.logger");
        XRCSVEncoder encoder = newStartedEncoder(context);

        LoggingEvent event = new LoggingEvent(
                "org.jvmxray.Fqcn", logger, Level.INFO, "hello world", null, null);
        event.setThreadName("main");
        event.setTimeStamp(1234567890L);
        // A hand-built LoggingEvent has no MDC map; real logback always sets one.
        // Without this, encode() NPEs iterating a null MDC map.
        event.setMDCPropertyMap(Collections.emptyMap());

        String csv = new String(encoder.encode(event));
        // CSV column order: timestamp,level,thread,logger,message,exception,<mdc...>
        // Columns 0-3 precede the message and contain no commas, so split(",") is safe for them.
        String[] cols = csv.split(",");

        assertEquals("1234567890", cols[0]);
        assertEquals("INFO", cols[1]);
        assertEquals("main", cols[2]);
        assertEquals("test.logger", cols[3]);
        assertTrue("CSV should end with newline", csv.endsWith("\n"));
    }

    @Test
    public void includesExceptionMessageWhenThrowablePresent() {
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("test.logger");
        XRCSVEncoder encoder = newStartedEncoder(context);

        LoggingEvent event = new LoggingEvent(
                "org.jvmxray.Fqcn", logger, Level.ERROR, "boom occurred",
                new RuntimeException("kaboom"), null);
        event.setThreadName("worker-1");
        event.setTimeStamp(42L);
        event.setMDCPropertyMap(Collections.emptyMap());

        String csv = new String(encoder.encode(event));

        // Column order: timestamp,level,thread,logger,message,exception(,mdc...).
        // With no MDC, the exception field is the last column, so the line ends with it.
        // This exercises the JSON exception branch (the only path that reads
        // node.has("exception") + the exception value back out).
        assertTrue("prefix", csv.startsWith("42,ERROR,worker-1,test.logger,"));
        assertTrue("exception column should carry the throwable message",
                csv.endsWith("kaboom\n"));
    }

    @Test
    public void nullExceptionMessageDoesNotCrash() {
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("test.logger");
        XRCSVEncoder encoder = newStartedEncoder(context);

        // A throwable whose getMessage() is null. Under Gson, addProperty(k, null) stores
        // JsonNull and getAsString() throws — so the encoder must guard this. The exception
        // column is emitted empty (byte-identical to the no-exception else branch).
        LoggingEvent event = new LoggingEvent(
                "org.jvmxray.Fqcn", logger, Level.ERROR, "boom", new RuntimeException(), null);
        event.setThreadName("worker-1");
        event.setTimeStamp(7L);
        event.setMDCPropertyMap(Collections.emptyMap());

        String csv = new String(encoder.encode(event));

        assertTrue("prefix", csv.startsWith("7,ERROR,worker-1,test.logger,"));
        assertTrue("line terminated", csv.endsWith("\n"));
    }
}
```

Note (added during execution): this third test is a true regression guard — it fails (throws
`UnsupportedOperationException`) against an unguarded Gson port and passes once the null-message
guard in `toJson` is in place.

- [ ] **Step 2: Run the test against the current (Jackson) implementation**

Run: `mvn test -Dtest=XRCSVEncoderTest`
Expected: PASS (2 tests). This confirms the test correctly characterizes current behavior.

If it fails, stop — the test assumptions are wrong and must be fixed before migrating, otherwise it cannot guard the refactor.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoderTest.java
git commit -m "test: add XRCSVEncoder characterization test before Gson migration

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Remove unused logback-jackson and logback-json-classic dependencies

These two `logback.contrib` add-ons exist only to provide a Jackson-backed JSON layout. No logback config references a JSON layout, so they are dead weight. `logback-jackson` is also Jackson's "Door B" (it pulls `jackson-databind` transitively). Removing them is an independent, code-free change verifiable on its own.

**Files:**
- Modify: `pom.xml` (the two `ch.qos.logback.contrib` dependency blocks, currently around lines 73–82)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing for later tasks.

- [ ] **Step 1: Remove the two contrib dependency blocks**

Delete these two blocks from `pom.xml`:

```xml
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
```

Leave the `logback-classic` and `logback-core` dependencies and the `<!-- JSON -->` `jackson-databind` block untouched (jackson-databind is handled in Task 3).

- [ ] **Step 2: Verify logback-jackson and logback-json-classic are gone from the tree**

Run: `mvn dependency:tree | grep -iE "logback-jackson|logback-json"`
Expected: no output (both removed). `logback-classic` and `logback-core` remain — verify with `mvn dependency:tree | grep -i "logback-c"` which should still list both.

- [ ] **Step 3: Run the full test suite to prove logging still initializes**

Run: `mvn test`
Expected: BUILD SUCCESS. Every logback config is loaded during tests; a green run confirms the contrib removal broke nothing.

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: remove unused logback-jackson and logback-json-classic deps

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Migrate XRCSVEncoder to Gson and swap the Jackson dependency

Atomic change: the encoder rewrite and the dependency swap must land together so the build always compiles (removing jackson-databind while the encoder still imports Jackson would break compilation, and vice versa).

**Files:**
- Modify: `pom.xml` (the `<!-- JSON -->` `jackson-databind` block)
- Modify: `src/main/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoder.java`

**Interfaces:**
- Consumes: `XRCSVEncoderTest` from Task 1 (the regression guard).
- Produces: `XRCSVEncoder` with unchanged public surface (`encode(ILoggingEvent): byte[]`).

- [ ] **Step 1: Swap the dependency in pom.xml**

Replace the `<!-- JSON -->` block:

```xml
    <!-- JSON -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.17.1</version>
    </dependency>
```

with:

```xml
    <!-- JSON -->
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.10.1</version>
    </dependency>
```

- [ ] **Step 2: Rewrite XRCSVEncoder against the Gson API**

Replace the entire contents of `src/main/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoder.java` with:

```java
package org.jvmxray.platform.shared.log.logback.codec;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.gson.JsonObject;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

/**
 * Encoder for CSVFiles ensures any data with commas is properly
 * escaped prior to logging.  It's intended to help cleanup
 * machine logs for better parsing.
 * @author Milton Smith
 */
public class XRCSVEncoder extends LayoutWrappingEncoder {

    private static final char CSV_DELIMITER = ',';
    private static Charset charset = Charset.forName("UTF-8");

    public XRCSVEncoder() {
        super();
    }

    public byte[] encode(ILoggingEvent event) {
        String formattedMessage = Arrays.toString(super.encode(event));
        return (toCsv(formattedMessage, toJson(event, event.getMDCPropertyMap()), event) + "\n").getBytes(charset);
    }

    private String toCsv(String formattedMessage, JsonObject node, ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(node.get("timestamp").getAsLong()).append(CSV_DELIMITER);
        builder.append(node.get("level").getAsString()).append(CSV_DELIMITER);
        builder.append(node.get("thread").getAsString()).append(CSV_DELIMITER);
        builder.append(node.get("logger").getAsString()).append(CSV_DELIMITER);
        builder.append(formattedMessage.replace(",", "\\,")).append(CSV_DELIMITER); // use formattedMessage here
        if (node.has("exception")) {
            builder.append(node.get("exception").getAsString().replace(",", "\\,")).append(CSV_DELIMITER);
        } else {
            builder.append(CSV_DELIMITER);
        }
        // add MDC values to CSV
        for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            builder.append(value.replace(",", "\\,")).append(CSV_DELIMITER);
        }
        builder.setLength(builder.length() - 1); // remove last delimiter
        return builder.toString();
    }

    private JsonObject toJson(ILoggingEvent event, Map<String, String> mdcProperties) {
        JsonObject node = new JsonObject();
        node.addProperty("timestamp", event.getTimeStamp());
        node.addProperty("level", event.getLevel().toString());
        node.addProperty("thread", event.getThreadName());
        node.addProperty("logger", event.getLoggerName());
        node.addProperty("message", event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            // Gson's addProperty(String, null) stores JsonNull, and getAsString() on it
            // throws (unlike Jackson, which stored a NullNode). A throwable with a null
            // message (e.g. new NullPointerException()) would otherwise crash the encoder.
            // Omitting the field yields a byte-identical empty exception column.
            String exceptionMessage = event.getThrowableProxy().getMessage();
            if (exceptionMessage != null) {
                node.addProperty("exception", exceptionMessage);
            }
        }
        if (mdcProperties != null) {
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                node.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return node;
    }


}
```

Note: the changes from the original are the three import lines (`com.fasterxml.jackson.databind.*` → `com.google.gson.JsonObject`), the `JsonNode`/`ObjectNode` types → `JsonObject`, `mapper.createObjectNode()` → `new JsonObject()`, `put(...)` → `addProperty(...)`, `asLong()` → `getAsLong()`, `asText()` → `getAsString()`, and the null-message guard on the exception field (Gson's `addProperty(k, null)` stores `JsonNull`, whose `getAsString()` throws — unlike Jackson's `NullNode`). The pre-existing unused `key` local is left as-is (surgical change — do not clean up pre-existing code).

- [ ] **Step 3: Verify zero FasterXML Jackson remains in the build**

Run: `mvn dependency:tree | grep -i "com.fasterxml.jackson"`
Expected: no output (zero entries).

Then confirm Gson is present:
Run: `mvn dependency:tree | grep -i "com.google.code.gson"`
Expected: one line showing `gson:jar:2.10.1`.

(The legacy `org.codehaus.jackson:jackson-core-asl:1.9.12` may still appear — out of scope, expected.)

- [ ] **Step 4: Run the characterization test to confirm identical behavior**

Run: `mvn test -Dtest=XRCSVEncoderTest`
Expected: PASS (2 tests) — same result as Task 1, proving the CSV output is unchanged.

- [ ] **Step 5: Run the full test suite**

Run: `mvn test`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/org/jvmxray/platform/shared/log/logback/codec/XRCSVEncoder.java
git commit -m "refactor: migrate XRCSVEncoder from Jackson to Gson

Removes the direct jackson-databind dependency; XRCSVEncoder now builds
its JSON view with Gson. CSV output is unchanged.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final verification

After all tasks:

- [ ] `mvn dependency:tree | grep -i "com.fasterxml.jackson"` → no output.
- [ ] `mvn dependency:tree | grep -iE "logback-jackson|logback-json"` → no output.
- [ ] `mvn test` → BUILD SUCCESS.
- [ ] `git log --oneline -4` shows the three migration commits plus the design-spec commit.
