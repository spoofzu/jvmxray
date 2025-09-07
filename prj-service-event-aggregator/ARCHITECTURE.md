# LogService Architecture (prj-service-event-aggregator)

## Overview
The LogService is a real-time event aggregation server that receives structured security events from JVMXRay agents via TCP socket connections and stores them in the database for analysis.

## LogService Architecture
```mermaid
graph TB
    Agent[JVMXRay Agent] -->|SocketAppender| TCP[TCP Socket :9876]
    TCP --> Listener[Socket Listener]
    Listener --> Queue[Event Queue]
    Queue --> Processor[Event Processor]
    Processor --> Parser[Event Parser]
    Parser --> DB[(Database)]
    
    subgraph "LogService Components"
        Listener --> Manager[Connection Manager]
        Manager --> Pool[Thread Pool]
        Pool --> Worker1[Worker Thread 1]
        Pool --> Worker2[Worker Thread 2]
        Pool --> WorkerN[Worker Thread N]
    end
    
    subgraph "Event Processing Pipeline"
        Parser --> Validator[Event Validator]
        Validator --> Generator[ID Generator]
        Generator --> Extractor[KeyPair Extractor]
        Extractor --> Storage[Database Storage]
    end
    
    Storage --> EventTable[STAGE0_EVENT]
    Storage --> KeyPairTable[STAGE0_EVENT_KEYPAIR]
```

## Event Processing Flow
```mermaid
sequenceDiagram
    participant Agent as JVMXRay Agent
    participant Socket as TCP Socket
    participant Listener as Socket Listener
    participant Queue as Event Queue
    participant Processor as Event Processor
    participant Parser as LogEvent Parser
    participant DB as Database
    
    Agent->>+Socket: Connect to port 9876
    Socket->>+Listener: Accept connection
    Listener->>Listener: Create client handler
    Listener-->>-Socket: Connection established
    
    loop Event Streaming
        Agent->>+Socket: Send logback event
        Socket->>+Queue: Add to processing queue
        Queue->>+Processor: Dequeue for processing
        Processor->>+Parser: Parse logback format
        
        Parser->>Parser: Extract base fields
        Parser->>Parser: Extract key-value pairs
        Parser->>Parser: Generate EVENT_ID
        
        Parser->>+DB: INSERT STAGE0_EVENT
        Parser->>+DB: INSERT STAGE0_EVENT_KEYPAIR(s)
        DB-->>-Parser: Storage complete
        Parser-->>-Processor: Event processed
        Processor-->>-Queue: Processing complete
        Queue-->>-Socket: ACK (optional)
    end
    
    Agent->>Socket: Disconnect
    Socket->>Listener: Close connection
```

## Component Architecture
```mermaid
classDiagram
    class LogService {
        +static main(String[] args)
        -ServerSocket serverSocket
        -ExecutorService threadPool
        -EventProcessor processor
        +void start()
        +void stop()
        +void handleClient(Socket client)
    }
    
    class SocketListener {
        -ServerSocket serverSocket
        -int port
        -boolean running
        +void start()
        +void stop()
        +void acceptConnections()
    }
    
    class ClientHandler {
        -Socket clientSocket
        -BufferedReader reader
        -EventProcessor processor
        +void run()
        +void processEvents()
        +void handleDisconnect()
    }
    
    class EventProcessor {
        -BlockingQueue~String~ eventQueue
        -DatabaseManager dbManager
        -int batchSize
        +void processEvent(String logEvent)
        +void processBatch(List~String~ events)
    }
    
    class LogEventParser {
        -Pattern logPattern
        +LogEvent parse(String logLine)
        +Map~String,String~ extractKeyPairs(String message)
        +String generateEventId()
    }
    
    class DatabaseManager {
        -Connection connection
        -PreparedStatement eventInsert
        -PreparedStatement keyPairInsert
        +void storeEvent(LogEvent event)
        +void storeBatch(List~LogEvent~ events)
        +void initializeConnection()
    }
    
    class LogEvent {
        +String eventId
        +String configFile
        +Timestamp timestamp
        +String threadId
        +String priority
        +String namespace
        +String aid
        +String cid
        +boolean isStable
        +Map~String,String~ keyPairs
    }
    
    LogService --> SocketListener
    LogService --> EventProcessor
    SocketListener --> ClientHandler
    ClientHandler --> EventProcessor
    EventProcessor --> LogEventParser
    EventProcessor --> DatabaseManager
    LogEventParser --> LogEvent
    DatabaseManager --> LogEvent
```

## Socket Connection Management
```mermaid
stateDiagram-v2
    [*] --> ServerStart
    ServerStart --> Listening : bind(9876)
    Listening --> AcceptConnection : client connects
    AcceptConnection --> ClientConnected
    ClientConnected --> ProcessingEvents : receive events
    ProcessingEvents --> ProcessingEvents : more events
    ProcessingEvents --> ClientDisconnected : connection lost/closed
    ClientDisconnected --> Listening : continue accepting
    Listening --> ServerShutdown : stop() called
    ServerShutdown --> [*]
    
    note right of ClientConnected
        Each client gets dedicated
        handler thread from pool
    end note
    
    note right of ProcessingEvents
        Events processed in batches
        for optimal performance
    end note
```

## Event Parsing Pipeline
```mermaid
flowchart TD
    A[Raw Log Event] --> B[Logback Format Detection]
    B --> C{Valid Format?}
    C -->|No| D[Log Parse Error]
    C -->|Yes| E[Extract Base Fields]
    
    E --> F[Extract CONFIG_FILE]
    F --> G[Extract TIMESTAMP]
    G --> H[Extract THREAD_ID]
    H --> I[Extract PRIORITY]
    I --> J[Extract NAMESPACE]
    J --> K[Extract Message Body]
    
    K --> L[Parse Key-Value Pairs]
    L --> M[Generate EVENT_ID]
    M --> N[Set IS_STABLE flag]
    N --> O[Create LogEvent Object]
    
    O --> P[Validate Event]
    P --> Q{Valid?}
    Q -->|No| R[Log Validation Error]
    Q -->|Yes| S[Store in Database]
    
    D --> T[Continue Processing]
    R --> T
    S --> T
    
    subgraph "Logback Format"
        U["CONFIG_FILE | timestamp | thread | priority | namespace | message"]
    end
    
    B --> U
```

## Database Storage Strategy
```mermaid
sequenceDiagram
    participant Parser as Event Parser
    participant Validator as Event Validator
    participant BatchManager as Batch Manager
    participant TxnManager as Transaction Manager
    participant DB as Database
    
    Parser->>+Validator: Validate parsed event
    Validator->>Validator: Check required fields
    Validator->>Validator: Validate data types
    Validator-->>-Parser: Validation result
    
    alt Valid Event
        Parser->>+BatchManager: Add to batch
        BatchManager->>BatchManager: Check batch size
        
        alt Batch Ready
            BatchManager->>+TxnManager: Begin transaction
            
            loop For Each Event in Batch
                TxnManager->>+DB: INSERT STAGE0_EVENT
                TxnManager->>+DB: INSERT STAGE0_EVENT_KEYPAIR(s)
            end
            
            TxnManager->>+DB: COMMIT transaction
            DB-->>-TxnManager: Transaction complete
            TxnManager-->>-BatchManager: Batch stored
        end
        
        BatchManager-->>-Parser: Event queued/stored
    else Invalid Event
        Parser->>Parser: Log validation error
        Parser->>Parser: Continue processing
    end
```

## Thread Pool Management
```mermaid
graph TD
    A[Server Start] --> B[Initialize Thread Pool]
    B --> C{Pool Type?}
    
    C -->|Fixed| D[Fixed Thread Pool]
    C -->|Cached| E[Cached Thread Pool]
    C -->|Custom| F[Custom Thread Pool]
    
    D --> G[Core Pool Size: 10]
    E --> H[Dynamic Scaling]
    F --> I[Configurable Parameters]
    
    G --> J[Client Connections]
    H --> J
    I --> J
    
    J --> K[Assign Handler Thread]
    K --> L[Process Events]
    L --> M{Connection Active?}
    M -->|Yes| L
    M -->|No| N[Release Thread]
    N --> O[Return to Pool]
    
    subgraph "Thread Pool Configuration"
        P[Core Pool Size: 10]
        Q[Maximum Pool Size: 50]
        R[Keep Alive Time: 60s]
        S[Queue Capacity: 1000]
    end
```

## Event Batching Strategy
```mermaid
flowchart LR
    A[Individual Events] --> B[Batch Collector]
    B --> C{Batch Size Reached?}
    C -->|No| D[Wait for More Events]
    C -->|Yes| E[Process Batch]
    
    D --> F{Timeout Reached?}
    F -->|No| G[Continue Collecting]
    F -->|Yes| H[Process Partial Batch]
    
    E --> I[Begin Transaction]
    H --> I
    
    I --> J[Insert Events]
    J --> K[Insert KeyPairs]
    K --> L[Commit Transaction]
    L --> M[Batch Complete]
    
    G --> B
    M --> N[Reset Batch]
    N --> B
    
    subgraph "Batch Configuration"
        O[Batch Size: 100 events]
        P[Timeout: 5 seconds]
        Q[Max Batch Size: 1000]
    end
```

## Connection Lifecycle Management
```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Connecting : client connects
    Connecting --> Connected : handshake complete
    Connected --> Streaming : receiving events
    Streaming --> Streaming : process events
    Streaming --> Disconnecting : client disconnects
    Streaming --> Error : network error
    Error --> Disconnecting : cleanup
    Disconnecting --> Cleanup : close resources
    Cleanup --> Idle : ready for new connection
    
    note right of Connected
        Connection established
        Reader/Writer initialized
    end note
    
    note right of Streaming
        Primary operational state
        Events processed continuously
    end note
    
    note right of Cleanup
        Close sockets, streams
        Release thread resources
    end note
```

## Performance Optimization
```mermaid
graph TD
    A[Performance Optimization] --> B[Connection Level]
    A --> C[Processing Level]
    A --> D[Database Level]
    
    B --> E[Connection Pooling]
    B --> F[Keep-Alive Settings]
    B --> G[Buffer Sizes]
    
    C --> H[Event Batching]
    C --> I[Parallel Processing]
    C --> J[Memory Management]
    
    D --> K[Prepared Statements]
    D --> L[Transaction Batching]
    D --> M[Connection Pooling]
    
    E --> N[Reuse Connections]
    H --> O[Batch Insert Operations]
    K --> P[Avoid SQL Parsing Overhead]
    
    subgraph "Configuration Tuning"
        Q[TCP Buffer Sizes]
        R[Thread Pool Sizing]
        S[Batch Size Optimization]
        T[Database Connection Pool]
    end
```

## Error Handling and Recovery
```mermaid
flowchart TD
    A[Error Detection] --> B{Error Type?}
    
    B -->|Network Error| C[Handle Connection Loss]
    B -->|Parse Error| D[Log and Continue]
    B -->|Database Error| E[Handle DB Failure]
    B -->|Resource Error| F[Handle Resource Exhaustion]
    
    C --> G[Close Connection]
    G --> H[Notify Client Disconnect]
    H --> I[Release Resources]
    
    D --> J[Log Error Details]
    J --> K[Skip Invalid Event]
    K --> L[Continue Processing]
    
    E --> M{Connection Available?}
    M -->|No| N[Attempt Reconnection]
    M -->|Yes| O[Queue Events]
    N --> P{Reconnect Success?}
    P -->|Yes| Q[Process Queued Events]
    P -->|No| R[Wait and Retry]
    
    F --> S[Free Memory]
    S --> T[Reduce Batch Sizes]
    T --> U[Log Resource Warning]
    
    I --> V[Continue Accepting]
    L --> V
    Q --> V
    R --> W[Service Degraded Mode]
    U --> V
    W --> V
```

## Configuration Management
```mermaid
graph LR
    A[Configuration Sources] --> B[System Properties]
    A --> C[Configuration File]
    A --> D[Environment Variables]
    A --> E[Command Line Args]
    
    B --> F[Merge Configuration]
    C --> F
    D --> F
    E --> F
    
    F --> G[Validate Settings]
    G --> H{Valid?}
    H -->|No| I[Use Defaults]
    H -->|Yes| J[Apply Configuration]
    
    I --> K[Log Default Usage]
    J --> L[Start Service]
    K --> L
    
    subgraph "Configuration Properties"
        M["logservice.port=9876"]
        N["logservice.batch.size=100"]
        O["logservice.thread.pool.size=10"]
        P["database.url=jdbc:sqlite:..."]
        Q["database.batch.timeout=5000"]
    end
```

## Monitoring and Metrics
```mermaid
graph TD
    A[LogService Metrics] --> B[Connection Metrics]
    A --> C[Processing Metrics]
    A --> D[Database Metrics]
    A --> E[System Metrics]
    
    B --> F[Active Connections]
    B --> G[Connection Rate]
    B --> H[Connection Errors]
    
    C --> I[Events Processed/sec]
    C --> J[Processing Latency]
    C --> K[Parse Errors]
    
    D --> L[Database Response Time]
    D --> M[Transaction Rate]
    D --> N[Storage Errors]
    
    E --> O[Memory Usage]
    E --> P[CPU Usage]
    E --> Q[Thread Pool Status]
    
    subgraph "Health Checks"
        R[Service Status: UP/DOWN]
        S[Database Connectivity]
        T[Resource Availability]
        U[Error Rate Thresholds]
    end
```

## Directory Structure
```
prj-service-event-aggregator/
├── src/main/java/org/jvmxray/service/eventAggregator/
│   ├── bin/
│   │   └── LogService.java           # Main service entry point
│   ├── server/
│   │   ├── SocketListener.java       # TCP socket management
│   │   ├── ClientHandler.java        # Client connection handling
│   │   └── ConnectionManager.java    # Connection pool management
│   ├── processor/
│   │   ├── EventProcessor.java       # Event processing pipeline
│   │   ├── LogEventParser.java       # Logback format parsing
│   │   ├── EventValidator.java       # Event validation
│   │   └── BatchProcessor.java       # Batch processing logic
│   ├── database/
│   │   ├── DatabaseManager.java      # Database operations
│   │   ├── EventStorage.java         # Event storage logic
│   │   └── ConnectionPoolManager.java # DB connection pooling
│   ├── config/
│   │   ├── ServiceConfiguration.java # Configuration management
│   │   └── MetricsConfiguration.java # Monitoring setup
│   └── util/
│       ├── ServiceInitializer.java   # Service initialization
│       ├── MetricsCollector.java     # Performance metrics
│       └── HealthChecker.java        # Service health monitoring
├── src/main/resources/
│   ├── logback.xml2                  # Logback template
│   └── service.properties            # Default configuration
└── src/test/java/
    └── org/jvmxray/service/eventAggregator/
        ├── server/                   # Server component tests
        ├── processor/                # Event processing tests
        └── integration/              # Integration tests
```

## Deployment Architecture
```mermaid
graph TB
    subgraph "Production Deployment"
        LB[Load Balancer] --> LS1[LogService Instance 1]
        LB --> LS2[LogService Instance 2]
        LB --> LS3[LogService Instance 3]
        
        LS1 --> DB1[(Primary Database)]
        LS2 --> DB1
        LS3 --> DB1
        
        DB1 --> DB2[(Read Replica)]
        DB1 --> DB3[(Read Replica)]
    end
    
    subgraph "Development Deployment"
        DEV_LS[LogService] --> DEV_DB[(SQLite Database)]
    end
    
    subgraph "High Availability"
        HA_LB[HA Load Balancer]
        HA_LS1[LogService Primary]
        HA_LS2[LogService Standby]
        HA_DB1[(Database Cluster)]
        
        HA_LB --> HA_LS1
        HA_LB --> HA_LS2
        HA_LS1 --> HA_DB1
        HA_LS2 --> HA_DB1
    end
```

## See Also
- [Architecture Overview](../docs/architecture/README.md)
- [Data Flow Documentation](../docs/architecture/data-flow.md)
- [Database Schema](../docs/architecture/database-schema.md)
- [Performance Tuning](../docs/guides/performance-tuning.md)