# Migrate FasterXML Jackson → Gson

**Date:** 2026-06-30
**Status:** Approved (design)

## Goal

Remove the project's **direct** dependency on FasterXML Jackson, replacing the one real
consumer with Gson. After this change, the project declares no Jackson dependency of its own
and no project code imports the Jackson API.

> **Scope clarification (added during execution):** The original goal stated "`mvn
> dependency:tree` shows zero `com.fasterxml.jackson` entries." That is not achievable here and
> is not the real objective. Removing our direct `jackson-databind:2.17.1` unmasked a
> *transitive* FasterXML Jackson (`jackson-core`/`jackson-databind:2.12.2`) that the Cassandra
> driver (`com.datastax.oss:java-driver-core:4.13.0`) declares for its own use — Maven's
> nearest-wins mediation had hidden it behind our direct, newer copy. Per project policy,
> **direct** Jackson dependencies are removed (this migration); **transitive** Jackson owned by
> 3rd-party jars is a *separate* effort addressed by upgrading or replacing the offending jar,
> proposed and reviewed with the user as a tradeoff — not by blanket Maven exclusions. See
> "Follow-up: transitive Jackson" below.

## Scope

### In scope
- Replace Jackson API usage in `XRCSVEncoder.java` with Gson.
- Remove the direct `jackson-databind` dependency from `pom.xml`.
- Remove the two `logback.contrib` add-ons that exist only to provide a Jackson-backed JSON
  layout: `logback-jackson` and `logback-json-classic`.
- Add `com.google.code.gson:gson` (version `2.10.1`, matching the mcp-server worktree).

### Out of scope (transitive Jackson — see follow-up)
- **FasterXML Jackson `2.12.2` via the Cassandra driver** (`com.datastax.oss:java-driver-core:4.13.0`
  → `jackson-core`/`jackson-databind`). The driver depends on Jackson for its own JSON handling.
  Transitive; addressed separately by upgrading/replacing the driver, not by this migration.
- **Legacy Codehaus Jackson** (`org.codehaus.jackson:jackson-core-asl:1.9.12`). A different,
  unrelated library pulled in transitively through the Cassandra driver
  (`com.datastax.oss:java-driver-core` → `com.esri.geometry:esri-geometry-api`). Transitive;
  same follow-up strategy.
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

1. `pom.xml` declares **no** `com.fasterxml.jackson` dependency and no project code imports the
   Jackson API. (`grep -rn "com.fasterxml.jackson" src/main pom.xml` returns only the
   `serialization` sensor's string class-name references, which are out of scope.) Any remaining
   `com.fasterxml.jackson` in `mvn dependency:tree` is transitive via 3rd-party jars (Cassandra
   driver) — tracked under Follow-up, not a blocker here.
2. `mvn dependency:tree` shows `com.google.code.gson:gson:2.10.1` present.
3. `mvn compile` succeeds.
4. The full `mvn test` suite passes. This exercises every logback config during initialization,
   empirically confirming the contrib removal broke nothing.
5. A small unit test for `XRCSVEncoder` asserts the produced CSV is correct (the class currently
   has no test and its only consumer is being rewritten).

## Follow-up: transitive Jackson (separate effort)

After this migration, FasterXML Jackson `2.12.2` (and legacy Codehaus `jackson-core-asl:1.9.12`)
remain in the tree transitively, both owned by `com.datastax.oss:java-driver-core:4.13.0` (the
Codehaus one via its `esri-geometry-api` dependency). Project policy is to treat Jackson as
systemically vulnerable and avoid it where practical, but transitive copies are not removed by
blanket Maven `<exclusion>`s. They are addressed as a **separate, user-reviewed tradeoff**:

- **Option A — upgrade the Cassandra driver** to a newer `java-driver-core` that bundles a newer,
  less-vulnerable Jackson (and may drop the Codehaus path). Lower-risk, stays on DataStax.
- **Option B — replace the driver** with an equivalent Cassandra client that doesn't pull Jackson.
  Larger change; eliminates Jackson but higher migration cost/risk.

This is to be proposed and reviewed with the user before any action; it is intentionally **not**
part of this migration.
