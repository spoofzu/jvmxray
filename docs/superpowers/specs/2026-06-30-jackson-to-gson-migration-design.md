# Migrate FasterXML Jackson → Gson

**Date:** 2026-06-30
**Status:** Approved (design)

## Goal

Remove all **FasterXML Jackson** (`com.fasterxml.jackson.*`) from the build, replacing the
one real consumer with Gson. After this change, `mvn dependency:tree` shows zero
`com.fasterxml.jackson` entries.

## Scope

### In scope
- Replace Jackson API usage in `XRCSVEncoder.java` with Gson.
- Remove the direct `jackson-databind` dependency from `pom.xml`.
- Remove the two `logback.contrib` add-ons that exist only to provide a Jackson-backed JSON
  layout: `logback-jackson` and `logback-json-classic`.
- Add `com.google.code.gson:gson` (version `2.10.1`, matching the mcp-server worktree).

### Out of scope
- **Legacy Codehaus Jackson** (`org.codehaus.jackson:jackson-core-asl:1.9.12`). This is a
  different, unrelated library pulled in transitively through the Cassandra driver
  (`com.datastax.oss:java-driver-core` → `com.esri.geometry:esri-geometry-api`). Removing it
  would require an exclusion on the Cassandra driver and risks breaking `esri-geometry-api` at
  runtime. Left untouched by explicit decision.
- **The `serialization` sensor package** (`JacksonInterceptor`, `SerializationInterceptor`,
  `SerializationSensor`). These reference Jackson by *string class name* to bytecode-instrument
  and detect Jackson usage in monitored target applications. Jackson is not a library
  dependency there; changing it would break detection. Left untouched.

## Background: how Jackson enters the build

Jackson reaches the build through two independent paths ("doors"):

- **Door A — direct declaration.** `pom.xml` lists `com.fasterxml.jackson.core:jackson-databind:2.17.1`
  directly. Its only consumer is `XRCSVEncoder`.
- **Door B — transitive via `logback-jackson`.** The `ch.qos.logback.contrib:logback-jackson:0.1.5`
  POM declares `jackson-databind` as a plain `compile` dependency. So Jackson comes along even if
  Door A is closed.

Logback itself does **not** depend on Jackson. The dependency layering is:

```
logback-core                 → (no Jackson)        ← the engine
logback-classic              → logback-core         (no Jackson)
logback-json-core (contrib)  → logback-core         (no Jackson)
logback-json-classic (contrib) → logback-classic, logback-json-core   (no Jackson)
logback-jackson (contrib)    → logback-json-core, jackson-databind     ← Jackson door
```

Closing both doors requires removing the direct `jackson-databind` **and** `logback-jackson`.
`logback-json-classic` only provided a JSON layout that `logback-jackson` formats; with the
formatter gone it has no purpose, so it is removed too.

## Why this is safe for logback

Verified against the codebase, not assumed:

1. `logback-classic` and `logback-core` are declared as their own direct dependencies and are
   untouched. They never depended on Jackson.
2. None of the 8 logback config files (`src/main/resources` + `src/test/resources`, including
   production / shaded / test variants) reference a JSON layout. A grep for
   `json|jackson|contrib` across all of them returns zero matches. Every appender is a stock
   `ConsoleAppender` / `FileAppender` / `RollingFileAppender` (or the project's custom
   SQLite/Cassandra appenders) using the default `PatternLayoutEncoder`.
3. `logback-jackson` / `logback-json-classic` depend *on* logback, not the reverse. Removing the
   leaves cannot affect the trunk.

The `logback.contrib` JSON layout is not Gson-pluggable — its formatter SPI is Jackson-specific
and there is no `logback-gson` adapter in this stack. Because no config uses the JSON layout,
nothing is lost by dropping it; we are not "swapping Gson into logback," we are removing dead
add-ons.

## Changes

### 1. `pom.xml`

- **Remove** `com.fasterxml.jackson.core:jackson-databind:2.17.1`
- **Remove** `ch.qos.logback.contrib:logback-jackson:0.1.5`
- **Remove** `ch.qos.logback.contrib:logback-json-classic:0.1.5`
- **Add** `com.google.code.gson:gson:2.10.1`

### 2. `XRCSVEncoder.java`

Mechanical 1:1 API swap inside the existing `toJson(...)` and `toCsv(...)` private methods. No
behavior change; output CSV is byte-for-byte identical (same fields, same order).

| Jackson | Gson |
|---|---|
| `ObjectMapper` + `mapper.createObjectNode()` | `new JsonObject()` |
| `node.put(k, v)` | `node.addProperty(k, v)` |
| `JsonNode` / `ObjectNode` types | `JsonObject` / `JsonElement` |
| `node.get("x").asLong()` | `node.get("x").getAsLong()` |
| `node.get("x").asText()` | `node.get("x").getAsString()` |
| `node.has("x")` | `node.has("x")` (unchanged) |

Imports change from `com.fasterxml.jackson.databind.*` to `com.google.gson.*`.

### Note on dead code

`XRCSVEncoder`, `logback-jackson`, and `logback-json-classic` all appear to be dead/unused — no
logback config or code wires them in. Per the project request this design **migrates**
`XRCSVEncoder` to Gson rather than deleting it. (Deleting the encoder instead would shrink the
work to the POM changes alone; not chosen.)

## Success criteria / verification

1. `mvn dependency:tree` shows **zero `com.fasterxml.jackson`** entries. (Codehaus 1.x may
   remain — out of scope.)
2. `mvn compile` succeeds.
3. The full `mvn test` suite passes. This exercises every logback config during initialization,
   empirically confirming the contrib removal broke nothing.
4. A small unit test for `XRCSVEncoder` asserts the produced CSV is correct (the class currently
   has no test and its only consumer is being rewritten).
