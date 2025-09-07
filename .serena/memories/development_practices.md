# JVMXRay Development Practices

## Code Style and Conventions
- **Java Version**: Java 11 minimum (maven.compiler.source/target=11)
- **Encoding**: UTF-8 (project.build.sourceEncoding)
- **Package Structure**: org.jvmxray.* namespace
- **Build Tool**: Maven with parent POM dependency management

## Testing Approach
- **Unit Tests**: JUnit 4.13.1 for module testing
- **Integration Tests**: Comprehensive end-to-end testing with Turtle application
- **Test Data**: Generated in SQLite DB during unit tests for AI integration demos

## Logging Architecture
- **Framework**: Logback with SLF4J
- **Event Format**: Structured security events (not traditional unstructured logs)
- **Real-time**: SocketAppender for live event streaming
- **Enterprise**: Compatible with Splunk, ELK, DataDog, etc.

## Build Configuration
- **Maven Shade Plugin**: Creates fat JARs with dependencies
- **Agent Manifest**: Configures Java agent properties (Can-Redefine-Classes, etc.)
- **Profiles**: Integration test profile enabled by default

## Security Considerations
- **Defensive Focus**: Security monitoring and detection only
- **No Malicious Code**: Strictly defensive security tools
- **Event Enrichment**: CWE/CVSS scoring for compliance reporting

## Development Completion Checklist
When completing tasks:
1. Run `mvn clean install` - includes integration tests
2. Verify integration test output shows expected sensor events
3. Check that all sensors are generating events in logs
4. Ensure LogService starts/stops cleanly
5. No build failures or test failures

## Sensor Development Pattern
When adding new sensors:
1. Extend AbstractSensor
2. Implement InjectableSensor interface  
3. Create Interceptor classes for method interception
4. Add to agent sensor registration
5. Update integration test to exercise new sensor
6. Validate structured event output