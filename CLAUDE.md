# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JVMXRay is an AI-enhanced security monitoring platform that watches Java applications in real-time, detecting vulnerabilities and suspicious activity without requiring code changes. It uses bytecode injection to install sensors that monitor file access, network connections, system calls, and other security-relevant operations.

## Build Commands

**Primary Build Command**: `mvn clean install` - Full build with unit and integration tests

### Database Schema Commands
- `java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager --help` - Show schema management options
- Schema creation examples:
  - SQLite: `--create-schema --database-type sqlite --connection-url jdbc:sqlite:/path/to/db.sqlite`
  - MySQL: `--create-schema --database-type mysql --connection-url jdbc:mysql://host:port/database --username user --password pass --database-name jvmxray`
  - Cassandra: `--create-schema --database-type cassandra --host host --port 9042 --username user --password pass --database-name jvmxray --datacenter datacenter1`

### Key Build Artifacts
- `prj-agent/target/prj-agent-0.0.1-shaded.jar` - Deployable Java agent JAR

## Architecture

### Multi-Module Maven Structure
```
jvmxray/
├── prj-agent/          # Java agent with bytecode injection sensors
├── prj-common/         # Shared utilities, models, and database schema management
├── prj-mcp-client/     # MCP (Model Context Protocol) client functionality
└── prj-service-log/    # Log service components
```

### Agent Architecture (prj-agent)
The agent uses ByteBuddy for bytecode injection to install sensors:

- **Entry Point**: `org.jvmxray.agent.bin.jvmxrayagent` (Java agent premain class)
- **Sensor Types**: Modular monitoring system
  - File I/O operations (`io/` package)
  - Network operations (`net/` package)
  - HTTP requests (`http/` package)
  - SQL queries (`sql/` package)
  - System calls and library loading (`system/` package)
  - System monitoring (`monitor/` package)
  - Exception handling (`uncaughtexception/` package)

### Database Schema Architecture (prj-common)
The project includes a comprehensive database schema management system for storing agent events:

- **SchemaManager**: CLI tool for creating, validating, and managing database schemas
- **Multi-Database Support**: Cassandra, MySQL, and SQLite implementations
- **Event Storage Tables**:
  - `STAGE0_EVENT`: Raw events with fields EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, IS_STABLE, KEYPAIRS
  - `STAGE0_EVENT_KEYPAIR`: Key-value pairs extracted from event messages (EVENT_ID, KEY, VALUE) - used for parsed/processed events
  - **Agent Events**: Agents post complete log messages with keypairs directly to STAGE0_EVENT.KEYPAIRS column (not STAGE0_EVENT_KEYPAIR table)
- **Consistency Management**: IS_STABLE flag ensures data consistency between tables for NoSQL databases
- **Test Integration**: SQLite test database automatically created during `mvn test` at `.jvmxray/common/data/jvmxray-test.db`
- **Event Format**: Parses logback format: `CONFIG_FILE | timestamp | thread | priority | namespace | keypairs`

### Component Initialization Architecture (prj-common)
JVMXRay uses a standardized component initialization system via `ComponentInitializer`:

#### **Component Directory Structure**
Each component follows the pattern:
```
.jvmxray/{component}/
├── config/
│   ├── logback.xml          # Logback configuration (copied from .xml2 template)
│   └── {component}.properties # Component-specific properties
├── logs/                     # Component log files
└── data/                     # Component data files (if needed)
```

#### **ComponentInitializer Pattern**
- **Base Class**: `ComponentInitializer` provides common initialization framework
- **Component-Specific**: Each module extends with `{Component}Initializer` (e.g., `IntegrationInitializer`, `CommonInitializer`)
- **Singleton Pattern**: Each initializer uses thread-safe singleton pattern with `getInstance()`

#### **Initialization Flow**
1. **setupDirectories()**: Creates component directory structure
2. **copyResources()**: Copies `.xml2` templates to `logback.xml` files (preserves existing configs)
3. **initializeProperties()**: Loads/creates component properties file
4. **initializeLogging()**: Sets `-Dlogback.configurationFile` system property for non-agent components

#### **Logging Configuration**
- **Standard Components**: Use `-Dlogback.configurationFile={component}/config/logback.xml`
- **Agent Exception**: Agent uses LogProxy instead of Logback (bootloader incompatibility)
- **Maven Integration**: Surefire/Failsafe plugins can set `logback.configurationFile` property
- **SLF4J Integration**: Components use standard `LoggerFactory.getLogger()` (not isolated LoggerContext)

#### **System Properties**
- `jvmxray.test.home`: Test mode base directory (e.g., `project/.jvmxray`)
- `jvmxray.home`: Production mode base directory (adds `/jvmxray/` subdirectory)
- `jvmxray.{component}.logs`: Component-specific logs directory path
- `jvmxray.{component}.config`: Component-specific config directory path

#### **Key Components**
- **IntegrationInitializer**: Sets up integration test logging and directories
- **CommonInitializer**: Manages common module logging and database connections
- **AgentInitializer**: Special case - manages agent-specific configuration without standard logging

## Integration Testing

Integration tests run automatically with Maven build via `mvn clean install`. The Turtle integration test:

1. Runs TurtleIntegrationTest in its own JVM with JVMXRay agent attached
2. Exercises all sensors through controlled operations  
3. Validates expected sensor events appear in logs
4. Located in `prj-common/src/test/java/org/jvmxray/shared/integration/`

**Memory Requirements**: Set `MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m"`

## Development Workflow

### Task Completion Checklist
When finishing development tasks:

1. Run `mvn clean install` (includes integration tests)
2. Verify Turtle integration test passes and shows sensor events in output
3. Ensure no build failures or test failures

### Sensor Development Pattern
When adding new sensors:

1. Extend `AbstractSensor` 
2. Implement `InjectableSensor` interface
3. Create Interceptor classes for method interception
4. Register sensor in agent initialization
5. Add test coverage in Turtle integration test
6. Validate structured event output format

## Technology Stack

### Core Technologies
- **Java 11+** (maven.compiler.source/target=11)
- **ByteBuddy 1.14.17** for bytecode injection

### Build & Testing
- **Maven** with parent POM dependency management
- **JUnit 4.13.1** for testing

### Logging & CLI
- **Logback + SLF4J** for enterprise logging integration
- **Apache Commons CLI** for command-line interfaces

## Key Design Principles

### Core Architecture Principles
- **Structured Events**: Generates machine-readable security events (not traditional unstructured logs)
- **Zero Application Changes**: Monitoring via bytecode injection
- **Modular Sensors**: Extensible sensor architecture for different monitoring aspects
- **AI-Ready**: Structured data enables intelligent analysis and threat detection

### Enterprise Integration
- **Compatible Logging**: Works with Splunk, ELK, DataDog logging infrastructure
- **Real-time Streaming**: SocketAppender for live event transmission

### Technical Constraints
- **Agent Logging**: ALL AGENT LOGGING MUST USE LOGPROXY CLASS (Logback cannot be used in bootloader where sensors operate)
- **Configuration Safety**: Existing configs are not overridden for safety
- **Test Environment**: TurtleIntegrationTest runs in its own JVM with agent attached

### Script Design Principles
- **Minimal Wrapper Pattern**: All shell scripts (e.g., `logserver.sh`) must be lightweight, minimal wrappers
- **Java-First Logic**: All business logic, dependency validation, environment setup, and process management must reside in Java code
- **Script Responsibilities Limited To**:
  - Classpath construction and dependency resolution
  - JVM option configuration
  - Java application execution (`exec java ...`)
- **Prohibited in Scripts**:
  - Complex business logic or control flow
  - Environment initialization beyond basic path resolution
  - Process management (PID handling, service lifecycle)
  - Dependency validation or error handling beyond basic existence checks
- **Modern Java APIs**: Use Java 11+ features like ProcessHandle API for cross-platform process management
- **Service Management**: Daemon mode, start/stop/restart, and status checking implemented in Java

### Configuration Notes
- **JVMXRay Home Pathing**:
  - Test mode (`jvmxray.test.home`): `{jvmxray.test.home}/agent/logs/` (no extra /jvmxray/)
  - Production mode (`jvmxray.home`): `{jvmxray.home}/jvmxray/agent/logs/`
  - Exception thrown if both properties are assigned