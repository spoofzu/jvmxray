# JVMXRay Data Flow Architecture

## Overview
This document details how data flows through the JVMXRay system from method interception to AI-enhanced security analysis.

## Event Lifecycle Overview
```mermaid
flowchart LR
    A[Method Call] --> B[Sensor Intercept]
    B --> C[Event Creation]
    C --> D[LogService]
    D --> E[Database Storage]
    D --> F[Real-time Stream]
    F --> G[MCP Server]
    G --> H[AI Analysis]
    
    subgraph "Event Structure"
        I[Timestamp]
        J[Thread ID]
        K[Namespace]
        L[Key-Value Pairs]
        M[Security Context]
    end
    
    C --> I
    C --> J
    C --> K
    C --> L
    C --> M
```

## Detailed Event Processing Flow
```mermaid
sequenceDiagram
    participant App as Java Application
    participant ByteBuddy as ByteBuddy Transformer
    participant Sensor as Sensor Instance
    participant EventFactory as Event Factory
    participant SocketAppender as Socket Appender
    participant LogService as LogService
    participant Parser as Event Parser
    participant DB as Database
    participant MCP as MCP Server
    
    App->>+ByteBuddy: Method call intercepted
    ByteBuddy->>+Sensor: Delegate to sensor
    Sensor->>+EventFactory: Create structured event
    EventFactory->>EventFactory: Add metadata (timestamp, thread, etc.)
    EventFactory-->>-Sensor: Return formatted event
    Sensor->>+SocketAppender: Send via Logback
    SocketAppender->>+LogService: Stream event (port 9876)
    LogService->>+Parser: Parse logback format
    Parser->>Parser: Extract EVENT_ID, keypairs
    Parser->>+DB: Store in STAGE0_EVENT
    Parser->>+DB: Store keypairs in STAGE0_EVENT_KEYPAIR
    DB-->>-Parser: Acknowledge storage
    Parser-->>-LogService: Processing complete
    LogService-->>-SocketAppender: ACK
    SocketAppender-->>-Sensor: Event delivered
    Sensor-->>-ByteBuddy: Continue execution
    ByteBuddy-->>-App: Return control
    
    Note over MCP: Real-time query capability
    MCP->>+DB: Query events
    DB-->>-MCP: Return structured data
```

## Event Format Transformation
```mermaid
flowchart TD
    A[Raw Java Method Call] --> B[Sensor Interception]
    B --> C[Structured Event Creation]
    
    subgraph "Event Creation Process"
        C --> D[Add Timestamp]
        D --> E[Add Thread Context]
        E --> F[Add Security Namespace]
        F --> G[Add Method Metadata]
        G --> H[Add Custom Keypairs]
    end
    
    H --> I[Logback Format]
    I --> J["CONFIG_FILE | timestamp | thread | priority | namespace | keypairs"]
    
    subgraph "Database Storage"
        J --> K[Parse Logback Format]
        K --> L[Extract Base Fields]
        K --> M[Extract Keypairs]
        L --> N[STAGE0_EVENT Table]
        M --> O[STAGE0_EVENT_KEYPAIR Table]
    end
    
    subgraph "Output Formats"
        N --> P[MCP JSON Response]
        N --> Q[Grafana Dashboard]
        N --> R[Splunk Integration]
        N --> S[AI Analysis]
    end
```

## Database Storage Pattern
```mermaid
erDiagram
    STAGE0_EVENT {
        varchar EVENT_ID PK "UUID for event"
        varchar CONFIG_FILE "Source component"
        timestamp TIMESTAMP "Event creation time"
        varchar THREAD_ID "Java thread identifier"
        varchar PRIORITY "Log level (INFO, WARN, ERROR)"
        varchar NAMESPACE "Event category namespace"
        varchar AID "Application identifier"
        varchar CID "Component identifier"
        boolean IS_STABLE "Consistency flag for NoSQL"
    }
    
    STAGE0_EVENT_KEYPAIR {
        varchar EVENT_ID FK "References STAGE0_EVENT"
        varchar KEY "Parameter name"
        varchar VALUE "Parameter value"
    }
    
    STAGE0_EVENT ||--o{ STAGE0_EVENT_KEYPAIR : "contains keypairs"
```

## Event Categories and Namespaces
```mermaid
graph TD
    Events[JVMXRay Events] --> IO[org.jvmxray.events.io.*]
    Events --> NET[org.jvmxray.events.net.*]
    Events --> SQL[org.jvmxray.events.sql.*]
    Events --> HTTP[org.jvmxray.events.http.*]
    Events --> SYSTEM[org.jvmxray.events.system.*]
    Events --> MONITOR[org.jvmxray.events.monitor.*]
    
    IO --> FileRead[fileread]
    IO --> FileWrite[filewrite]
    IO --> FileDelete[filedelete]
    
    NET --> Connect[connect]
    NET --> Bind[bind]
    NET --> Accept[accept]
    
    SQL --> Query[query]
    SQL --> Execute[execute]
    
    SYSTEM --> Exec[exec]
    SYSTEM --> LoadLibrary[lib]
    SYSTEM --> UncaughtException[uncaughtexception]
    
    MONITOR --> SystemHealth[health]
    MONITOR --> Performance[performance]
```

## Real-Time Data Streaming
```mermaid
sequenceDiagram
    participant Sensor as Sensor
    participant Logback as Logback Framework
    participant SocketAppender as Socket Appender
    participant LogService as LogService
    participant Buffer as Event Buffer
    participant Processor as Event Processor
    participant DB as Database
    
    Sensor->>+Logback: Generate log event
    Logback->>+SocketAppender: Route via configuration
    SocketAppender->>+LogService: TCP connection (port 9876)
    LogService->>+Buffer: Add to processing queue
    Buffer->>+Processor: Batch process events
    
    loop Event Processing
        Processor->>Processor: Parse logback format
        Processor->>Processor: Generate EVENT_ID
        Processor->>Processor: Extract keypairs
        Processor->>+DB: Insert STAGE0_EVENT
        Processor->>+DB: Insert STAGE0_EVENT_KEYPAIR records
        DB-->>-Processor: Confirm storage
    end
    
    Processor-->>-Buffer: Batch complete
    Buffer-->>-LogService: Processing complete
    LogService-->>-SocketAppender: ACK batch
    SocketAppender-->>-Logback: Event delivered
    Logback-->>-Sensor: Logging complete
```

## MCP Server Query Processing
```mermaid
flowchart TD
    A[AI Client Request] --> B[MCP Server]
    B --> C{Query Type?}
    
    C -->|Time-based| D[Filter by timestamp range]
    C -->|Application-based| E[Filter by AID/namespace]
    C -->|Security-focused| F[Apply security filters]
    C -->|Event-type| G[Filter by event category]
    
    D --> H[Database Query]
    E --> H
    F --> H
    G --> H
    
    H --> I[Join STAGE0_EVENT + KEYPAIRS]
    I --> J[Apply pagination/limits]
    J --> K[Format JSON response]
    K --> L[AI Enhancement Layer]
    
    subgraph "AI Enhancement"
        L --> M[Pattern Recognition]
        L --> N[Threat Classification]
        L --> O[CWE/CVSS Assignment]
        L --> P[Timeline Analysis]
    end
    
    M --> Q[Enhanced Response]
    N --> Q
    O --> Q
    P --> Q
    
    Q --> R[Return to AI Client]
```

## Performance Optimization Flow
```mermaid
graph LR
    subgraph "Sensor Layer"
        A[Method Interception] --> B[Minimal Processing]
        B --> C[Async Event Creation]
    end
    
    subgraph "Transport Layer"
        C --> D[SocketAppender Buffer]
        D --> E[Batch Transmission]
    end
    
    subgraph "Processing Layer"
        E --> F[LogService Queue]
        F --> G[Batch Processing]
        G --> H[Database Batch Insert]
    end
    
    subgraph "Query Layer"
        H --> I[Indexed Tables]
        I --> J[Optimized Queries]
        J --> K[Result Caching]
    end
```

## Error Handling and Recovery
```mermaid
stateDiagram-v2
    [*] --> NormalOperation
    NormalOperation --> ErrorDetected : Exception occurs
    ErrorDetected --> LogServiceDown : Connection failed
    ErrorDetected --> DatabaseError : DB unavailable
    ErrorDetected --> ParsingError : Format issue
    
    LogServiceDown --> BufferEvents : Store locally
    BufferEvents --> RetryConnection : Periodic retry
    RetryConnection --> NormalOperation : Connection restored
    RetryConnection --> BufferEvents : Still unavailable
    
    DatabaseError --> EventQueue : Queue events
    EventQueue --> RetryDatabase : Periodic retry
    RetryDatabase --> NormalOperation : DB restored
    RetryDatabase --> EventQueue : Still unavailable
    
    ParsingError --> LogError : Record issue
    LogError --> NormalOperation : Continue processing
    
    note right of BufferEvents
        Events buffered to prevent loss
        during LogService outages
    end note
    
    note right of EventQueue
        Events queued during
        database maintenance
    end note
```

## Integration Points
```mermaid
graph TB
    subgraph "Data Sources"
        A[File I/O Operations]
        B[Network Connections]
        C[SQL Queries]
        D[HTTP Requests]
        E[System Calls]
        F[Exception Events]
    end
    
    subgraph "JVMXRay Core"
        G[Sensor Framework]
        H[Event Processing]
        I[LogService]
        J[Database Storage]
    end
    
    subgraph "External Integrations"
        K[Splunk/ELK]
        L[Grafana Dashboards]
        M[AI/ML Platforms]
        N[SIEM Systems]
        O[Alert Management]
    end
    
    A --> G
    B --> G
    C --> G
    D --> G
    E --> G
    F --> G
    
    G --> H
    H --> I
    I --> J
    
    J --> K
    J --> L
    J --> M
    J --> N
    J --> O
```

## See Also
- [Architecture Overview](README.md)
- [Database Schema Documentation](database-schema.md)
- [MCP Server API](../api/mcp-server.md)
- [Event Format Reference](../reference/all-events.md)