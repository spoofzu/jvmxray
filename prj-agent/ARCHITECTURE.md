# JVMXRay Agent Architecture

## Overview
The JVMXRay Agent is a Java agent that uses bytecode injection to install sensors for monitoring application security events in real-time without requiring code changes.

## Agent Lifecycle
```mermaid
stateDiagram-v2
    [*] --> JVMStart
    JVMStart --> PreMain : -javaagent parameter
    PreMain --> SensorRegistry : Initialize sensors
    SensorRegistry --> ByteBuddySetup : Configure transformations
    ByteBuddySetup --> ClassLoading : Install transformers
    ClassLoading --> MonitoringActive : Ready for events
    MonitoringActive --> EventGeneration : Method calls intercepted
    EventGeneration --> MonitoringActive : Continue monitoring
    MonitoringActive --> Shutdown : JVM shutdown
    Shutdown --> [*]
    
    note right of PreMain
        Entry point: JVMXRayAgent.premain()
        Initializes agent components
    end note
    
    note right of EventGeneration
        Sensors generate structured events
        Events sent to LogService
    end note
```

## Bytecode Injection Architecture
```mermaid
sequenceDiagram
    participant JVM as JVM
    participant Agent as JVMXRay Agent
    participant BB as ByteBuddy
    participant Sensor as File Sensor
    participant Interceptor as FileReadInterceptor
    participant App as Application Code
    
    JVM->>+Agent: premain() called
    Agent->>+BB: Create AgentBuilder
    Agent->>+Sensor: Initialize sensor
    Sensor->>+BB: Configure type matcher
    Sensor->>+BB: Configure method matcher
    BB->>+Interceptor: Install advice
    BB-->>-Agent: Transformer installed
    Agent-->>-JVM: Agent ready
    
    App->>+JVM: Load FileInputStream.class
    JVM->>+BB: Class loading event
    BB->>+Interceptor: Transform class
    Interceptor-->>-BB: Modified bytecode
    BB-->>-JVM: Return transformed class
    JVM-->>-App: Class loaded with monitoring
    
    Note over App,Interceptor: Application execution continues
    App->>+Interceptor: Method call (file read)
    Interceptor->>+Sensor: Generate security event
    Sensor-->>-Interceptor: Event logged
    Interceptor-->>-App: Continue execution
```

## Sensor Framework Architecture
```mermaid
classDiagram
    class JVMXRayAgent {
        +static premain(String args, Instrumentation inst)
        +static agentmain(String args, Instrumentation inst)
        -initializeSensors(Instrumentation inst)
    }
    
    class SensorRegistry {
        -List~AbstractSensor~ sensors
        +registerSensor(AbstractSensor sensor)
        +initializeAllSensors()
        +injectAllSensors(Instrumentation inst)
        +shutdownAllSensors()
    }
    
    class AbstractSensor {
        <<abstract>>
        #String sensorName
        #boolean initialized
        +String getName()
        +void initialize()
        +void shutdown()
        #void logEvent(String namespace, String message)
        #String formatEvent(Map~String,String~ params)
    }
    
    class InjectableSensor {
        <<interface>>
        +void inject(Instrumentation inst)
        +ElementMatcher~TypeDescription~ getTypeMatcher()
        +ElementMatcher~MethodDescription~ getMethodMatcher()
    }
    
    class FileSensor {
        -static final String SENSOR_NAME
        -static final String EVENT_NAMESPACE
        +void inject(Instrumentation inst)
        +void onFileRead(String path, String caller)
        +void onFileWrite(String path, String caller)
        +void onFileDelete(String path, String caller)
    }
    
    class NetworkSensor {
        +void inject(Instrumentation inst)
        +void onConnection(String host, int port, String caller)
        +void onBind(String address, int port, String caller)
    }
    
    class SQLSensor {
        +void inject(Instrumentation inst)
        +void onQuery(String sql, String caller)
        +void onExecute(String sql, Object[] params, String caller)
    }
    
    JVMXRayAgent --> SensorRegistry
    SensorRegistry --> AbstractSensor
    AbstractSensor <|-- FileSensor
    AbstractSensor <|-- NetworkSensor
    AbstractSensor <|-- SQLSensor
    InjectableSensor <|.. FileSensor
    InjectableSensor <|.. NetworkSensor
    InjectableSensor <|.. SQLSensor
```

## Method Interception Pattern
```mermaid
flowchart TD
    A[Method Call] --> B{Is Target Method?}
    B -->|No| C[Normal Execution]
    B -->|Yes| D[ByteBuddy Interceptor]
    D --> E[OnMethodEnter Advice]
    E --> F[Extract Method Parameters]
    F --> G[Generate Security Event]
    G --> H[Log to SocketAppender]
    H --> I[OnMethodExit Advice]
    I --> J[Handle Return/Exception]
    J --> K[Continue Normal Execution]
    C --> L[Method Complete]
    K --> L
    
    subgraph "Error Handling"
        M[Sensor Exception]
        N[Log Error Internally]
        O[Never Break App Flow]
        M --> N
        N --> O
        O --> K
    end
    
    G -.->|Exception| M
```

## Event Generation Flow
```mermaid
sequenceDiagram
    participant App as Application
    participant Interceptor as Method Interceptor
    participant Sensor as Sensor Instance
    participant Formatter as Event Formatter
    participant Logger as LogProxy
    participant Appender as SocketAppender
    participant LogService as LogService
    
    App->>+Interceptor: Method call intercepted
    Interceptor->>Interceptor: Extract method context
    Interceptor->>+Sensor: Trigger event generation
    Sensor->>+Formatter: Format structured event
    Formatter->>Formatter: Create key-value pairs
    Formatter-->>-Sensor: Formatted event string
    Sensor->>+Logger: Log event with namespace
    Logger->>+Appender: Send to configured appenders
    Appender->>+LogService: Stream via TCP socket
    LogService-->>-Appender: Acknowledge receipt
    Appender-->>-Logger: Event delivered
    Logger-->>-Sensor: Logging complete
    Sensor-->>-Interceptor: Event processed
    Interceptor-->>-App: Continue execution
```

## Component Directory Structure
```
prj-agent/
├── src/main/java/org/jvmxray/agent/
│   ├── bin/
│   │   └── JVMXRayAgent.java          # Main agent entry point
│   ├── core/
│   │   ├── AbstractSensor.java        # Base sensor implementation
│   │   ├── InjectableSensor.java      # Sensor injection interface
│   │   ├── SensorRegistry.java       # Sensor management
│   │   └── EventFormatter.java       # Event formatting utilities
│   ├── sensors/
│   │   ├── io/
│   │   │   ├── FileSensor.java        # File I/O monitoring
│   │   │   └── SerializationSensor.java # Object serialization
│   │   ├── net/
│   │   │   ├── NetworkSensor.java     # Network connections
│   │   │   └── HttpSensor.java        # HTTP requests
│   │   ├── sql/
│   │   │   └── SQLSensor.java         # Database queries
│   │   ├── system/
│   │   │   ├── ProcessSensor.java     # System processes
│   │   │   ├── LibrarySensor.java     # Dynamic loading
│   │   │   └── ReflectionSensor.java  # Reflection operations
│   │   ├── monitor/
│   │   │   └── MonitorSensor.java     # System monitoring
│   │   └── uncaughtexception/
│   │       └── UncaughtExceptionSensor.java # Exception handling
│   ├── interceptors/
│   │   ├── FileInterceptor.java       # File I/O interception
│   │   ├── NetworkInterceptor.java    # Network interception
│   │   └── SQLInterceptor.java        # SQL interception
│   └── util/
│       ├── LogProxy.java              # Agent logging proxy
│       ├── StackTraceUtil.java        # Call stack analysis
│       └── AgentInitializer.java      # Agent initialization
├── src/main/resources/
│   └── logback.xml2                   # Logback template
└── src/test/java/
    └── org/jvmxray/agent/
        ├── sensors/                   # Sensor unit tests
        └── integration/               # Integration tests
```

## Sensor Implementation Details

### File I/O Sensor
```mermaid
graph TD
    A[File I/O Operations] --> B{Operation Type}
    B -->|Read| C[FileInputStream Interception]
    B -->|Write| D[FileOutputStream Interception]
    B -->|Delete| E[File.delete() Interception]
    
    C --> F[Extract File Path]
    D --> F
    E --> F
    
    F --> G[Extract Caller Context]
    G --> H[Generate Event]
    H --> I[Log to SocketAppender]
    
    subgraph "Event Format"
        J["caller=ClassName:lineNumber"]
        K["target=/path/to/file"]
        L["status=success|denied|error"]
        M["operation=read|write|delete"]
    end
    
    H --> J
    H --> K
    H --> L
    H --> M
```

### Network Sensor
```mermaid
graph TD
    A[Network Operations] --> B{Operation Type}
    B -->|Connect| C[Socket.connect() Interception]
    B -->|Bind| D[ServerSocket.bind() Interception]
    B -->|Accept| E[ServerSocket.accept() Interception]
    
    C --> F[Extract Destination]
    D --> G[Extract Bind Address]
    E --> H[Extract Client Info]
    
    F --> I[Generate Connection Event]
    G --> J[Generate Bind Event]
    H --> K[Generate Accept Event]
    
    I --> L[Log to SocketAppender]
    J --> L
    K --> L
```

### SQL Sensor
```mermaid
graph TD
    A[SQL Operations] --> B{Statement Type}
    B -->|PreparedStatement| C[PreparedStatement Interception]
    B -->|Statement| D[Statement Interception]
    B -->|CallableStatement| E[CallableStatement Interception]
    
    C --> F[Extract SQL + Parameters]
    D --> G[Extract SQL Query]
    E --> H[Extract Procedure Call]
    
    F --> I[Sanitize Sensitive Data]
    G --> I
    H --> I
    
    I --> J[Detect Injection Patterns]
    J --> K[Generate SQL Event]
    K --> L[Log to SocketAppender]
```

## Performance Optimization
```mermaid
flowchart LR
    A[Method Call] --> B{High Frequency?}
    B -->|Yes| C[Sampling Strategy]
    B -->|No| D[Full Monitoring]
    
    C --> E[Sample 1 in N calls]
    D --> F[Process Every Call]
    
    E --> G[Minimal Processing]
    F --> H[Standard Processing]
    
    G --> I[Async Event Logging]
    H --> I
    
    I --> J[Batch Processing]
    J --> K[Non-blocking Return]
    
    subgraph "Memory Management"
        L[Object Pooling]
        M[String Builder Reuse]
        N[Weak References]
    end
    
    G --> L
    H --> L
    L --> M
    M --> N
```

## Agent Configuration
```mermaid
graph TD
    A[Agent Startup] --> B[Load Configuration]
    B --> C{Configuration Source?}
    
    C -->|System Properties| D[JVM System Properties]
    C -->|Config File| E[agent.properties]
    C -->|Environment| F[Environment Variables]
    
    D --> G[Merge Configurations]
    E --> G
    F --> G
    
    G --> H[Initialize Components]
    H --> I[Setup Logging]
    I --> J[Register Sensors]
    J --> K[Install ByteBuddy Transformers]
    
    subgraph "Configuration Properties"
        L["jvmxray.home"]
        M["jvmxray.test.home"] 
        N["jvmxray.debug"]
        O["logservice.port"]
        P["sensor.enabled.*"]
    end
    
    G --> L
    G --> M
    G --> N
    G --> O
    G --> P
```

## Error Handling Strategy
```mermaid
flowchart TD
    A[Sensor Exception] --> B{Exception Type?}
    
    B -->|RuntimeException| C[Log Error Locally]
    B -->|Error| D[Log Critical Error]
    B -->|Throwable| E[Log Unknown Error]
    
    C --> F[Continue Application]
    D --> F
    E --> F
    
    F --> G{Multiple Failures?}
    G -->|Yes| H[Disable Sensor]
    G -->|No| I[Continue Monitoring]
    
    H --> J[Log Sensor Disabled]
    I --> K[Normal Operation]
    J --> K
    
    subgraph "Never Break Application"
        L[All Exceptions Caught]
        M[No Application Interruption]
        N[Minimal Performance Impact]
    end
    
    F --> L
    L --> M
    M --> N
```

## Memory Management
```mermaid
graph LR
    A[Agent Memory Usage] --> B[Static Components]
    A --> C[Dynamic Components]
    
    B --> D[Sensor Instances]
    B --> E[ByteBuddy Infrastructure]
    B --> F[Configuration Data]
    
    C --> G[Event Objects]
    C --> H[String Buffers]
    C --> I[Thread Local Storage]
    
    G --> J[Object Pooling]
    H --> K[StringBuilder Reuse]
    I --> L[Cleanup on Thread Death]
    
    J --> M[Memory Bounds Check]
    K --> M
    L --> M
    
    M --> N{Memory Limit Exceeded?}
    N -->|Yes| O[Reduce Monitoring]
    N -->|No| P[Continue Normal Operation]
```

## Thread Safety Design
```mermaid
graph TD
    A[Concurrent Method Calls] --> B[Thread Local Context]
    B --> C[Immutable Event Data]
    C --> D[Atomic Operations]
    
    D --> E{Shared State?}
    E -->|Yes| F[Synchronization]
    E -->|No| G[Lock-Free Processing]
    
    F --> H[Minimal Lock Scope]
    G --> I[Event Generation]
    H --> I
    
    I --> J[Thread-Safe Logging]
    J --> K[Async Event Delivery]
    
    subgraph "Concurrency Patterns"
        L[ThreadLocal Storage]
        M[Copy-on-Write Collections]
        N[Compare-and-Swap Operations]
        O[Message Passing]
    end
    
    B --> L
    C --> M
    D --> N
    K --> O
```

## See Also
- [Sensor Development Guide](../docs/guides/sensor-development.md)
- [Architecture Overview](../docs/architecture/README.md)
- [Event Data Flow](../docs/architecture/data-flow.md)
- [Performance Tuning](../docs/guides/performance-tuning.md)