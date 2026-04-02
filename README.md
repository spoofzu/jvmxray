# JVMXRay: Java Application Security Monitoring

[![Black Hat Arsenal](https://raw.githubusercontent.com/toolswatch/badges/master/arsenal/usa/2020.svg?sanitize=true)](https://www.toolswatch.org/blackhat-arsenal-us-2020-archive/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Development Status](https://img.shields.io/badge/Status-POC-red.svg)](#)

<div align="left">
<picture>
  <source srcset="./media/logo-wht-on-blk.png?raw=true" media="(prefers-color-scheme: dark)">
  <source srcset="./media/logo-blk-on-wht.png?raw=true" media="(prefers-color-scheme: light)">
  <img src="https://github.com/spoofzu/jvmxray/blob/master/build/logo-blk-on-wht.png?raw=true" alt="JVMXRay Logo" width="600">
</picture>
</div>

**[Duke history](https://dev.java/duke/)**

[Documentation](docs/prj-agent.md) | [Changes from Upstream](docs/CHANGES.md) |  [Project Blog](https://www.sipsjava.com/blog/)

JVMXRay monitors Java applications in real-time via bytecode injection, detecting vulnerabilities and suspicious activity without code changes. 19 modular sensors track file access, network connections, SQL queries, cryptographic operations, authentication, process execution, and more — generating structured, machine-readable security events with automatic cross-sensor correlation.

---

## Quick Start

```bash
git clone https://github.com/spoofzu/jvmxray.git
cd jvmxray
mvn clean install
```

During Maven's test phase a small program called Turtle runs under the freshly built JVMXRay agent. Turtle performs file I/O, cryptography, process execution, serialization, reflection, and other activities that trigger the sensors — so a successful build already gives you real security event logs to explore in `./target/test-jvmxray/`. A SQLite database is also created at `./target/test-jvmxray/common/data/jvmxray-test.db` for quick experimentation. See the [docs](docs/) for further details.

Deploy as a Java agent in your own applications:
```bash
java -javaagent:target/jvmxray-0.0.1-agent.jar -jar yourapp.jar
```

---

## Benefits

- **Zero code changes** — Attaches to any Java application at launch, like a debugger. No source modifications, no recompilation, no library dependencies to add.
- **AI-ready structured events** — Sensors generate machine-readable security events, not unstructured log lines. Use your AI tools to analyze application behavior like you application logs.
- **Fits your logging infrastructure** — Events are Logback log messages. Route them to Splunk, ELK, DataDog, Kafka, or any destination the Logback ecosystem supports.
- **Attack chain reconstruction** — Correlated events with `trace_id` and `scope_chain` let SOC teams trace an incident from HTTP request through SQL injection to data exfiltration in a single query.
- **Tunable overhead** — Standard Logback log-level properties control which sensors fire and at what verbosity. Turn sensors on or off per environment without rebuilding.

Keep in mind, JVMXRay is a source of truth for what a Java application is doing, not an analysis engine.  Analysis is provided by your centralized logging solution and/or your AI tooling projects.

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
- [Changelog](docs/CHANGES.md) — Detailed release changes
- [News Archive](docs/CHANGES.md) — Project history

## Acknowledgements
This project was developed using ideas, architecture, and code in some cases from the following,
- Claude Code CLI by Anthropic, [Anthropic](https://www.anthropic.com/claude-code)
- Byte-code architecture based in part from ideas by Terse Systems, [Terse Systems](https://tersesystems.com/)
- Logback logging framework by QOS.ch Sarl Sweitzerland, [QOS.ch](https://www.qos.ch/)
- Apache Software Foundation, [Apache Software Foundation](https://www.apache.org/)

## Contributor(s)

Milton Smith — Project creator, leader

Disclosure: JVMXRay is not approved, endorsed by, or affiliated with Oracle Corporation.
