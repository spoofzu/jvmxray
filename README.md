# JVMXRay: AI-Enhanced Java Security Monitoring

[![Black Hat Arsenal](https://raw.githubusercontent.com/toolswatch/badges/master/arsenal/usa/2020.svg?sanitize=true)](https://www.toolswatch.org/blackhat-arsenal-us-2020-archive/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Development Status](https://img.shields.io/badge/Status-Alpha-red.svg)](ZREADME-ALPHA.md)

<div align="left">
<picture>
  <source srcset="./media/logo-wht-on-blk.png?raw=true" media="(prefers-color-scheme: dark)">
  <source srcset="./media/logo-blk-on-wht.png?raw=true" media="(prefers-color-scheme: light)">
  <img src="https://github.com/spoofzu/jvmxray/blob/master/build/logo-blk-on-wht.png?raw=true" alt="JVMXRay Logo" width="600">
</picture>
</div>

**[Duke history](https://dev.java/duke/)**

JVMXRay monitors Java applications in real-time via bytecode injection, detecting vulnerabilities and suspicious activity without code changes. 19 modular sensors track file access, network connections, SQL queries, cryptographic operations, authentication, process execution, and more — generating structured, machine-readable security events with automatic cross-sensor correlation.

> **Alpha software** — for testing and evaluation only. [Details](ZREADME-ALPHA.md)

---

## Quick Start

```bash
git clone https://github.com/spoofzu/jvmxray.git
cd jvmxray
mvn clean install
```

Deploy as a Java agent:
```bash
java -javaagent:target/jvmxray-0.0.1-agent.jar -jar yourapp.jar
```

For advanced setup (data enrichment, AI integration): [Continue Setup](docs/setup-continued.md)

---

## Sensors

| Sensor | What It Monitors |
|--------|-----------------|
| **File I/O** | File reads, writes, deletes, permissions, symlinks with path resolution and aggregate I/O stats |
| **Network** | Socket connect/accept/bind/close with TLS detection, private IP classification, timing |
| **SQL** | Query capture with parameterization detection, operation type, duration, connection metadata |
| **HTTP** | Request/response with security header analysis, user/session correlation |
| **Process** | ProcessBuilder.start() and Runtime.exec() with command args, duration, exit status |
| **Crypto** | Cipher/KeyStore/MessageDigest/SSL with weak algorithm detection and key strength validation |
| **Authentication** | Session operations, JAAS login, Spring Security authenticate, principal queries with success/failure tracking |
| **API Call** | HttpClient.send() with URI, method, host, TLS, response status, timing |
| **Script Engine** | ScriptEngine.eval() with engine identification, content hashing, suspicious pattern detection, risk level |
| **Serialization** | Java native, Jackson, Gson, XStream deserialization with gadget chain detection |
| **Reflection** | Class.forName, Method.invoke, Field access, setAccessible with threat classification |
| **Configuration** | System properties, env vars, Properties files, Preferences with security annotations |
| **Library** | Static and dynamic JAR loading with SHA-256, Maven coordinates, package inventory |
| **Monitor** | JVM health: memory, threads, GC, CPU, open files, deadlock detection (60s interval) |
| **Uncaught Exception** | Crash diagnostics: full stack, memory state, thread dump, cause chain, incident ID |
| **App Init** | One-time startup capture: JVM version, OS, container detection, env vars (redacted) |
| **Data Transfer** | InputStream.read() byte counting (currently disabled — too broad) |
| **Thread** | Thread lifecycle monitoring (currently disabled — recursive logging fix needed) |
| **Memory** | Memory allocation tracking (currently disabled — recursive logging fix needed) |

## Event Correlation

Every sensor event includes correlation fields for reconstructing attack chains:

| Field | Example | Purpose |
|-------|---------|---------|
| `trace_id` | `a1b2c3d4e5f6` | Links all events in the same execution context |
| `scope_chain` | `HTTP>SQL>FileIO` | Nested sensor path — the "security stacktrace" |
| `parent_scope` | `SQL` | Immediate parent for tree reconstruction |
| `scope_depth` | `3` | Nesting level — depth 6+ warrants investigation |

A scope chain like `HTTP>Serialization>Reflection>Process` is a deserialization attack signature at a glance. The `TRACE_ID` column is indexed in the database for fast correlation queries.

## Architecture

```
java -javaagent:jvmxray-agent.jar -jar app.jar
         |
    AgentBootstrap (isolated classloader)
         |
    ByteBuddy installs sensor interceptors
         |
    Sensors fire on method entry/exit
         |
    LogProxy merges MCC correlation context
         |
    +----------+----------+----------+
    | SQLite   | File     | Socket   |
    | Appender | Appender | Appender |
    +----------+----------+----------+
```

- **Java 17**, **ByteBuddy 1.14.17** for bytecode injection
- **Logback + SLF4J** for enterprise logging (Splunk, ELK, DataDog compatible)
- **Multi-database**: SQLite (dev), MySQL, Cassandra (production)
- **MCC** (Mapped Correlation Context): thread-local scope tracking with TTL cleanup

## Documentation

- [Agent Reference](docs/prj-agent.md) — Sensor configuration, event fields, database schema
- [Common Components](docs/prj-common.md) — Database setup and utilities
- [Changelog](docs/CHANGELOG.md) — Detailed release changes
- [News Archive](ZREADME-NEWS.md) — Project history

## Contributors

Milton Smith — Project creator

Disclosure: JVMXRay is not approved, endorsed by, or affiliated with Oracle Corporation.
