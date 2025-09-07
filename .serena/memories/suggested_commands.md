# JVMXRay Development Commands

## Build Commands
- `mvn clean compile` - Compile all modules
- `mvn clean package` - Build JARs with shaded agent JAR
- `mvn clean install` - Full build with tests and installation

## Testing Commands
- `mvn clean install -P integration-test` - Run integration tests (default profile)
- `./test-integration.sh` - Integration test runner script with validation
- `mvn test` - Unit tests only

## Key Build Artifacts
- `prj-agent/target/prj-agent-0.0.1-shaded.jar` - Deployable agent JAR
- `prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar` - LogService

## Integration Test Setup
Integration tests run automatically with Maven build using the integration-test profile. The test:
1. Starts LogService on port 9876
2. Runs Turtle test application with JVMXRay agent
3. Validates sensor events in log output
4. Stops LogService cleanly

## Development Workflow
1. Make changes
2. Run `mvn clean install` (includes integration tests)
3. Validate all sensors work via integration test output

## System Requirements
- Java 11+
- Maven 3.x
- Sufficient memory: set MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m"