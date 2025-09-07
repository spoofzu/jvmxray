# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JVMXRay is an AI-enhanced security monitoring platform that watches Java applications in real-time, detecting vulnerabilities and suspicious activity without requiring code changes. It uses bytecode injection to install sensors that monitor file access, network connections, system calls, and other security-relevant operations.

## Build Commands

### Essential Build Commands
- `mvn clean compile` - Compile all modules
- `mvn clean package` - Build JARs including shaded agent JAR
- `mvn clean install` - Full build with unit and integration tests (recommended)

### Testing Commands
- `mvn clean install -P integration-test` - Run with integration tests (default profile)
- `./test-integration.sh` - Standalone integration test runner with validation
- `mvn test` - Unit tests only

### Database Schema Commands
- `java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager --help` - Show schema management options
- Schema creation examples:
  - SQLite: `--create-schema --database-type sqlite --connection-url jdbc:sqlite:/path/to/db.sqlite`
  - MySQL: `--create-schema --database-type mysql --connection-url jdbc:mysql://host:port/database --username user --password pass --database-name jvmxray`
  - Cassandra: `--create-schema --database-type cassandra --host host --port 9042 --username user --password pass --database-name jvmxray --datacenter datacenter1`

### Key Build Artifacts
- `prj-agent/target/prj-agent-0.0.1-shaded.jar` - Deployable Java agent JAR
- `prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar` - LogService

## Architecture

### Multi-Module Maven Structure
```
jvmxray/
├── prj-agent/                      # Java agent with bytecode injection sensors
├── prj-service-event-aggregator/   # LogService for real-time event aggregation  
└── prj-common/                     # Shared utilities, models, and database schema management
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

### Service Architecture (prj-service-event-aggregator)
- **LogService**: Socket-based event aggregation server (port 9876)
- **Real-time Events**: SocketAppender streams events from agent to LogService

### Database Schema Architecture (prj-common)
The project includes a comprehensive database schema management system for storing agent events:

- **SchemaManager**: CLI tool for creating, validating, and managing database schemas
- **Multi-Database Support**: Cassandra, MySQL, and SQLite implementations
- **Event Storage Tables**:
  - `STAGE0_EVENT`: Raw events with fields EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, IS_STABLE
  - `STAGE0_EVENT_KEYPAIR`: Key-value pairs extracted from event messages (EVENT_ID, KEY, VALUE)
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

Integration tests run automatically with Maven build. The system:

1. Starts LogService on port 9876
2. Runs Turtle test application with JVMXRay agent attached
3. Exercises all sensors through controlled operations
4. Validates expected sensor events appear in logs
5. Stops LogService cleanly

**Memory Requirements**: Set `MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m"`

## Development Workflow

### Task Completion Checklist
When finishing development tasks:

1. Run `mvn clean install` (includes integration tests)
2. Verify integration test passes and shows sensor events in output
3. Check LogService starts/stops without errors
4. Ensure no build failures or test failures

### Sensor Development Pattern
When adding new sensors:

1. Extend `AbstractSensor` 
2. Implement `InjectableSensor` interface
3. Create Interceptor classes for method interception
4. Register sensor in agent initialization
5. Add test coverage in Turtle integration test
6. Validate structured event output format

## Technology Stack

- **Java 11+** (maven.compiler.source/target=11)
- **Maven** with parent POM dependency management
- **ByteBuddy 1.14.17** for bytecode injection
- **Logback + SLF4J** for enterprise logging integration
- **JUnit 4.13.1** for testing
- **Apache Commons CLI** for command-line interfaces

## Key Design Principles

- **Structured Events**: Generates machine-readable security events (not traditional unstructured logs)
- **Zero Application Changes**: Monitoring via bytecode injection
- **Enterprise Integration**: Compatible with Splunk, ELK, DataDog logging infrastructure
- **Real-time Streaming**: SocketAppender for live event transmission
- **Modular Sensors**: Extensible sensor architecture for different monitoring aspects
- **AI-Ready**: Structured data enables intelligent analysis and threat detection
- ALL AGENT LOGGING MUST USE LOGPROXY CLASS.  REASON: LOGBACK CANNOT BE USED IN BOOTLOADER WHERE SENSORS OPERATE
- IF LOGBACK CONFIG APPEARS STALE, CHECK LOG CONFIGS IN {JVMXRAY-HOME}.  EXISTING CONFIGS ARE NOT OVERRIDDEN FOR SAFETY.
- Jvmxray home pathing: test mode (jvmxray.test.home), uses {jvmxray.test.home}/agent/logs/ (without the extra /jvmxray/).  Production mode (jvmxray.home), uses {jvmxray.home}/jvmxray/agent/logs/.  If both properties are assigned, jvmxray will throw an execption and print an appropriate message.
- TurtleIntegrationTest runs in it's own JVM with the agent attached.  As such, test configurations must apply to TurtleIntegrationTest, not it's wrapper.