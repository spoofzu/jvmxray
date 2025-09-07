# JVMXRay Code Architecture

## Module Structure
```
jvmxray/
├── prj-agent/           # Java agent with sensor injection
├── prj-service-event-aggregator/  # LogService for event collection
├── prj-common/          # Shared utilities and models
└── zzz-sensor/          # Additional sensor implementations
```

## Agent Architecture (prj-agent)
- **Entry Point**: `jvmxrayagent.java` - Java agent premain class
- **Sensors**: Modular sensor system monitoring different aspects:
  - `io/` - File I/O operations (FileIOSensor)
  - `net/` - Network operations (SocketSensor)  
  - `http/` - HTTP requests (HttpSensor)
  - `sql/` - SQL queries (SQLSensor)
  - `system/` - System calls, library loading (LibSensor, AppInitSensor)
  - `monitor/` - System monitoring (MonitorSensor)
  - `uncaughtexception/` - Exception handling
- **Transform**: ByteBuddy-based bytecode injection framework
- **Proxy**: Logging and management proxy layer

## Service Architecture (prj-service-event-aggregator)
- **LogService**: Socket-based event aggregation server
- **SocketServer**: Real-time event collection on port 9876

## Sensor Design Pattern
All sensors extend `AbstractSensor` and implement:
- **InjectableSensor**: Bytecode injection interface
- **Interceptor Classes**: Method interception logic
- **Event Generation**: Structured logging output

## Integration Test Framework
- **Turtle Test Application**: Exercises all sensors systematically
- **Maven Integration**: Failsafe plugin integration
- **Log Validation**: Pattern-based event validation

## Key Design Principles
- Modular sensor architecture for extensibility
- Structured event logging (not unstructured logs)
- Minimal performance impact via bytecode injection
- Real-time event streaming architecture
- Enterprise logging integration (Logback)