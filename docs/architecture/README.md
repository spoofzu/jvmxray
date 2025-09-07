# JVMXRay Architecture Overview

## Overview
JVMXRay uses bytecode injection to install sensors that monitor Java applications in real-time, generating structured security events without requiring application code changes.

## System Architecture
```mermaid
graph TB
    App[Java Application] --> Agent[JVMXRay Agent]
    Agent --> Sensors[Sensor Framework]
    Sensors --> Events[Event Generation]
    Events --> LogService[LogService :9876]
    Events --> DB[(Database)]
    LogService --> MCP[MCP Server]
    MCP --> AI[AI Clients]
    
    subgraph "Sensor Types"
        FileIO[File I/O]
        Network[Network]
        SQL[SQL]
        Monitor[System Monitor]
        HTTP[HTTP]
        Exception[Exception Handler]
    end
    
    Sensors --> FileIO
    Sensors --> Network
    Sensors --> SQL
    Sensors --> Monitor
    Sensors --> HTTP
    Sensors --> Exception
    
    subgraph "Data Storage"
        SQLite[(SQLite - Testing)]
        MySQL[(MySQL - Production)]
        Cassandra[(Cassandra - Scale)]
    end
    
    DB --> SQLite
    DB --> MySQL
    DB --> Cassandra
```

## Component Interaction Flow
```mermaid
sequenceDiagram
    participant App as Java Application
    participant Agent as JVMXRay Agent
    participant Sensor as File I/O Sensor
    participant LogService as LogService
    participant DB as Database
    participant MCP as MCP Server
    participant AI as AI Client
    
    App->>+Agent: Application starts with -javaagent
    Agent->>+Sensor: Install bytecode injection
    Note over Agent,Sensor: ByteBuddy transforms classes
    
    App->>+Sensor: File read operation intercepted
    Sensor->>Sensor: Generate structured event
    Sensor->>+LogService: Send event via SocketAppender
    LogService->>+DB: Store event with metadata
    DB-->>-LogService: Acknowledge storage
    LogService-->>-Sensor: Acknowledge receipt
    Sensor-->>-App: Continue normal execution
    
    AI->>+MCP: Query security events
    MCP->>+DB: Fetch events with filters
    DB-->>-MCP: Return structured data
    MCP->>MCP: Apply AI analysis
    MCP-->>-AI: Enhanced security intelligence
```

## Multi-Module Architecture
```mermaid
graph LR
    subgraph "prj-agent"
        AgentMain[JVMXRay Agent]
        SensorFramework[Sensor Framework]
        Interceptors[Method Interceptors]
    end
    
    subgraph "prj-service-event-aggregator"
        LogService[LogService Server]
        EventProcessor[Event Processor]
    end
    
    subgraph "prj-common"
        SchemaManager[Schema Manager]
        EventModels[Event Models]
        ComponentInit[Component Initializer]
    end
    
    subgraph "prj-mcp-client"
        MCPServer[MCP Server]
        QueryEngine[Query Engine]
        AIInterface[AI Interface]
    end
    
    AgentMain --> SensorFramework
    SensorFramework --> Interceptors
    Interceptors --> EventProcessor
    EventProcessor --> LogService
    SchemaManager --> EventModels
    ComponentInit --> EventModels
    MCPServer --> QueryEngine
    QueryEngine --> EventModels
```

## Data Flow Architecture
```mermaid
flowchart TD
    A[Method Call] --> B[Sensor Intercept]
    B --> C[Event Creation]
    C --> D[Structured Format]
    D --> E[LogService]
    E --> F[Database Storage]
    E --> G[Real-time Stream]
    G --> H[MCP Server]
    H --> I[AI Analysis]
    I --> J[Security Intelligence]
    
    subgraph "Event Structure"
        K[Timestamp]
        L[Thread ID]
        M[Namespace]
        N[Key-Value Pairs]
        O[Security Metadata]
    end
    
    C --> K
    C --> L
    C --> M
    C --> N
    C --> O
    
    subgraph "Storage Layer"
        P[STAGE0_EVENT]
        Q[STAGE0_EVENT_KEYPAIR]
    end
    
    F --> P
    F --> Q
```

## Database Schema Design
```mermaid
erDiagram
    STAGE0_EVENT {
        varchar EVENT_ID PK
        varchar CONFIG_FILE
        timestamp TIMESTAMP
        varchar THREAD_ID
        varchar PRIORITY
        varchar NAMESPACE
        varchar AID
        varchar CID
        boolean IS_STABLE
    }
    
    STAGE0_EVENT_KEYPAIR {
        varchar EVENT_ID FK
        varchar KEY
        varchar VALUE
    }
    
    STAGE0_EVENT ||--o{ STAGE0_EVENT_KEYPAIR : contains
```

## Component Initialization System
```mermaid
stateDiagram-v2
    [*] --> ComponentStart
    ComponentStart --> SetupDirectories
    SetupDirectories --> CopyResources
    CopyResources --> InitializeProperties
    InitializeProperties --> InitializeLogging
    InitializeLogging --> Ready
    Ready --> [*]
    
    note right of SetupDirectories
        Creates .jvmxray/{component}/
        config/, logs/, data/
    end note
    
    note right of CopyResources
        Copies .xml2 templates
        to logback.xml files
    end note
    
    note right of InitializeLogging
        Sets logback.configurationFile
        system property
    end note
```

## Key Architectural Principles

### Zero Application Impact
- **Bytecode Injection**: Uses ByteBuddy to modify classes at load time
- **Non-Intrusive**: Applications run normally with monitoring layer
- **Performance Optimized**: Minimal overhead (<3% typical)

### Structured Event Generation
- **Machine-Readable**: YAML/JSON compatible event format
- **Consistent Schema**: All events follow standardized structure
- **AI-Ready**: Structured data enables intelligent analysis

### Enterprise Integration
- **Logging Framework**: Built on Logback/SLF4J standards
- **Database Agnostic**: Supports SQLite, MySQL, Cassandra
- **Scalable Design**: From single apps to enterprise deployments

### Real-Time Intelligence
- **Live Streaming**: SocketAppender for immediate event delivery
- **AI Enhancement**: MCP server provides intelligent analysis
- **Pattern Detection**: Automated threat and vulnerability identification

## Component Directory Structure
```
.jvmxray/
├── agent/
│   ├── config/
│   │   ├── logback.xml
│   │   └── agent.properties
│   ├── logs/
│   └── data/
├── common/
│   ├── config/
│   │   ├── logback.xml
│   │   └── common.properties
│   ├── logs/
│   └── data/
│       └── jvmxray-test.db
└── integration/
    ├── config/
    ├── logs/
    └── data/
```

## See Also
- [Sensor Development Guide](../guides/sensor-development.md)
- [Database Schema Documentation](database-schema.md)
- [Event Data Flow](data-flow.md)
- [MCP Server API](../api/mcp-server.md)