# Complete Command Reference

## Overview
This document consolidates all JVMXRay commands including build, deployment, testing, and management operations.

## Build Commands

### Essential Build Commands
```bash
# Compile all modules
mvn clean compile

# Build JARs including shaded agent JAR
mvn clean package

# Full build with unit and integration tests (recommended)
mvn clean install

# Build with specific memory settings
MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m" mvn clean install
```

### Testing Commands
```bash
# Run with integration tests (default profile)
mvn clean install -P integration-test

# Unit tests only
mvn test

# Standalone integration test runner with validation
./test-integration.sh

# Run specific test class
mvn test -Dtest=FileSensorTest

# Run with debug output
mvn test -X
```

## Database Schema Management

### Schema Manager Commands
```bash
# Show schema management help
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager --help
```

### SQLite Schema Operations
```bash
# Create SQLite schema
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --create-schema \
  --database-type sqlite \
  --connection-url jdbc:sqlite:/path/to/db.sqlite

# Validate existing schema
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --validate-schema \
  --database-type sqlite \
  --connection-url jdbc:sqlite:/path/to/db.sqlite
```

### MySQL Schema Operations
```bash
# Create MySQL schema
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --create-schema \
  --database-type mysql \
  --connection-url jdbc:mysql://host:port/database \
  --username user \
  --password pass \
  --database-name jvmxray

# Drop and recreate MySQL schema
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --drop-schema \
  --create-schema \
  --database-type mysql \
  --connection-url jdbc:mysql://host:port/database \
  --username user \
  --password pass \
  --database-name jvmxray
```

### Cassandra Schema Operations
```bash
# Create Cassandra schema
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --create-schema \
  --database-type cassandra \
  --host host \
  --port 9042 \
  --username user \
  --password pass \
  --database-name jvmxray \
  --datacenter datacenter1

# List Cassandra keyspaces
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" org.jvmxray.platform.shared.bin.SchemaManager \
  --list-schemas \
  --database-type cassandra \
  --host host \
  --port 9042 \
  --username user \
  --password pass
```

## Deployment Commands

### Agent Deployment
```bash
# Deploy agent with Java application
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar -jar your-application.jar

# Deploy agent with specific configuration
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -Djvmxray.home=/opt/jvmxray \
     -jar your-application.jar

# Deploy in test mode
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -Djvmxray.test.home=./test-jvmxray \
     -jar your-application.jar
```

### LogService Commands
```bash
# Start LogService manually
java -cp prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar \
     org.jvmxray.service.eventAggregator.bin.LogService

# Start LogService with custom port
java -cp prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar \
     -Dlogservice.port=9877 \
     org.jvmxray.service.eventAggregator.bin.LogService

# Start LogService in background
nohup java -cp prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar \
     org.jvmxray.service.eventAggregator.bin.LogService > logservice.log 2>&1 &
```

## MCP Server Commands

### API Key Management
```bash
# Generate new API key
./scripts/generate-api-key.sh

# Generate API key with custom name
./scripts/generate-api-key.sh --name "development-key"

# List existing API keys
./scripts/list-api-keys.sh

# Revoke API key
./scripts/revoke-api-key.sh --key "jvmx_abc123def456"
```

### MCP Server Operations
```bash
# Start MCP Server
./scripts/start-mcp-server.sh

# Start MCP Server with custom database
./scripts/start-mcp-server.sh --database /path/to/custom.db

# Start MCP Server in debug mode
./scripts/start-mcp-server.sh --debug

# Stop MCP Server
./scripts/stop-mcp-server.sh

# Check MCP Server status
./scripts/mcp-server-status.sh
```

## Integration Testing

### Turtle Integration Test
```bash
# Run full integration test
./test-integration.sh

# Run integration test with verbose output
./test-integration.sh --verbose

# Run integration test with custom timeout
./test-integration.sh --timeout 300

# Run integration test with specific sensors
./test-integration.sh --sensors "file,network,sql"
```

### Manual Integration Testing
```bash
# Start LogService for testing
java -cp prj-service-event-aggregator/target/prj-service-event-aggregator-0.0.1.jar \
     org.jvmxray.service.eventAggregator.bin.LogService &

# Run test application with agent
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -Djvmxray.test.home=./test-output \
     -cp prj-integration/target/test-classes \
     org.jvmxray.integration.TurtleIntegrationTest

# Stop LogService
pkill -f LogService
```

## Development Commands

### Code Quality
```bash
# Run static analysis
mvn spotbugs:check

# Generate code coverage report
mvn jacoco:report

# Run dependency vulnerability scan
mvn dependency-check:check

# Format code
mvn formatter:format

# Check code style
mvn checkstyle:check
```

### Debugging Commands
```bash
# Enable JVMXRay debug logging
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -Djvmxray.debug=true \
     -jar your-application.jar

# Debug with remote JVM debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -jar your-application.jar

# Capture heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
jmap -dump:format=b,file=heap.hprof <pid>

# Analyze thread dumps
jstack <pid> > thread-dump.txt
```

### Performance Analysis
```bash
# Enable JFR profiling
java -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=jvmxray-profile.jfr \
     -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -jar your-application.jar

# Monitor with JConsole
jconsole <pid>

# Use VisualVM
visualvm --jdkhome $JAVA_HOME
```

## System Properties

### JVMXRay Configuration Properties
```bash
# Set JVMXRay home directory (production mode)
-Djvmxray.home=/opt/jvmxray

# Set JVMXRay test home (test mode)
-Djvmxray.test.home=./test-jvmxray

# Component-specific log directories
-Djvmxray.agent.logs=/var/log/jvmxray/agent
-Djvmxray.common.logs=/var/log/jvmxray/common
-Djvmxray.integration.logs=/var/log/jvmxray/integration

# Component-specific config directories
-Djvmxray.agent.config=/etc/jvmxray/agent
-Djvmxray.common.config=/etc/jvmxray/common

# Enable debug mode
-Djvmxray.debug=true

# Set LogService port
-Dlogservice.port=9876

# Database connection settings
-Djvmxray.database.url=jdbc:sqlite:/path/to/jvmxray.db
-Djvmxray.database.type=sqlite
```

### Logback Configuration Properties
```bash
# Specify logback configuration file
-Dlogback.configurationFile=/path/to/logback.xml

# Set logging level
-Djvmxray.log.level=INFO

# Enable console logging
-Djvmxray.log.console=true
```

## Docker Commands

### Build Docker Images
```bash
# Build JVMXRay base image
docker build -t jvmxray:latest .

# Build with specific JDK version
docker build --build-arg JDK_VERSION=11 -t jvmxray:jdk11 .

# Build development image
docker build -f Dockerfile.dev -t jvmxray:dev .
```

### Run JVMXRay in Docker
```bash
# Run with SQLite database
docker run -d \
  --name jvmxray-logservice \
  -p 9876:9876 \
  -v $(pwd)/data:/data \
  jvmxray:latest

# Run with MySQL database
docker run -d \
  --name jvmxray-logservice \
  -p 9876:9876 \
  -e DB_TYPE=mysql \
  -e DB_URL=jdbc:mysql://mysql-host:3306/jvmxray \
  -e DB_USER=jvmxray \
  -e DB_PASS=password \
  jvmxray:latest

# Run MCP Server in Docker
docker run -d \
  --name jvmxray-mcp \
  -p 8080:8080 \
  -v $(pwd)/data:/data \
  jvmxray:latest mcp-server
```

## Kubernetes Commands

### Deploy to Kubernetes
```yaml
# Apply JVMXRay deployment
kubectl apply -f k8s/jvmxray-deployment.yaml

# Create config maps
kubectl create configmap jvmxray-config --from-file=config/

# Create secrets for database
kubectl create secret generic jvmxray-db-secret \
  --from-literal=username=jvmxray \
  --from-literal=password=secure-password

# Scale deployment
kubectl scale deployment jvmxray-logservice --replicas=3

# Check deployment status
kubectl rollout status deployment/jvmxray-logservice

# View logs
kubectl logs -f deployment/jvmxray-logservice

# Port forward for local access
kubectl port-forward service/jvmxray-logservice 9876:9876
```

## Monitoring Commands

### Health Checks
```bash
# Check LogService health
curl http://localhost:9876/health

# Check MCP Server health
curl http://localhost:8080/health

# Check database connectivity
java -cp "prj-common/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout -f prj-common/pom.xml)" \
  org.jvmxray.platform.shared.bin.SchemaManager \
  --test-connection \
  --database-type sqlite \
  --connection-url jdbc:sqlite:.jvmxray/common/data/jvmxray-test.db
```

### Log Analysis
```bash
# Tail JVMXRay agent logs
tail -f .jvmxray/agent/logs/agent.log

# Search for specific events
grep "org.jvmxray.events.io.fileread" .jvmxray/*/logs/*.log

# Count events by type
grep -o "org\.jvmxray\.events\.[^|]*" .jvmxray/*/logs/*.log | sort | uniq -c

# Find error events
grep "ERROR" .jvmxray/*/logs/*.log | head -20

# Monitor real-time events
tail -f .jvmxray/*/logs/*.log | grep --line-buffered "org.jvmxray.events"
```

## Troubleshooting Commands

### Diagnostic Information
```bash
# Java version and environment
java -version
echo $JAVA_HOME
echo $PATH

# JVMXRay build artifacts
ls -la prj-agent/target/prj-agent-*-shaded.jar
ls -la prj-service-event-aggregator/target/prj-service-event-aggregator-*.jar

# Check port usage
netstat -an | grep 9876
lsof -i :9876

# Process information
ps aux | grep java
ps aux | grep LogService

# Disk usage
du -sh .jvmxray/
df -h
```

### Reset and Cleanup
```bash
# Clean Maven build
mvn clean

# Reset JVMXRay directories
rm -rf .jvmxray/

# Clean Docker resources
docker system prune -a

# Reset database (SQLite)
rm -f .jvmxray/common/data/jvmxray-test.db

# Kill all JVMXRay processes
pkill -f jvmxray
pkill -f LogService
```

## See Also
- [Quick Start Guide](../guides/quick-start.md)
- [Troubleshooting Guide](../guides/troubleshooting.md)
- [Configuration Reference](configuration.md)
- [Performance Tuning](../guides/performance-tuning.md)