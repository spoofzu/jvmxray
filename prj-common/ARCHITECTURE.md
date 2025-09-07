# Common Module Architecture (prj-common)

## Overview
The prj-common module provides shared utilities, database schema management, event models, and component initialization patterns used across all JVMXRay modules.

## Module Architecture
```mermaid
graph TB
    subgraph "Database Management"
        SchemaManager[Schema Manager]
        DatabaseFactory[Database Factory]
        ConnectionManager[Connection Manager]
    end
    
    subgraph "Component Initialization"
        ComponentInitializer[Component Initializer]
        AgentInitializer[Agent Initializer]
        CommonInitializer[Common Initializer]
        IntegrationInitializer[Integration Initializer]
    end
    
    subgraph "Event Models"
        LogEvent[Log Event Model]
        EventKeyPair[Event Key Pair]
        SecurityEvent[Security Event]
    end
    
    subgraph "Shared Utilities"
        ConfigurationManager[Configuration Manager]
        LoggingUtils[Logging Utilities]
        ValidationUtils[Validation Utils]
        StringUtils[String Utilities]
    end
    
    SchemaManager --> DatabaseFactory
    DatabaseFactory --> ConnectionManager
    
    ComponentInitializer --> ConfigurationManager
    ComponentInitializer --> LoggingUtils
    
    AgentInitializer ---|extends| ComponentInitializer
    CommonInitializer ---|extends| ComponentInitializer
    IntegrationInitializer ---|extends| ComponentInitializer
```

## Schema Management Architecture
```mermaid
classDiagram
    class SchemaManager {
        +static main(String[] args)
        -DatabaseType databaseType
        -String connectionUrl
        -SchemaOperations operations
        +void createSchema()
        +void dropSchema()
        +void validateSchema()
        +void testConnection()
    }
    
    class DatabaseFactory {
        +static Database createDatabase(DatabaseType type)
        +static Connection getConnection(DatabaseConfig config)
    }
    
    class Database {
        <<interface>>
        +void createSchema()
        +void dropSchema()
        +boolean validateSchema()
        +Connection getConnection()
    }
    
    class SQLiteDatabase {
        -String dbPath
        +void createSchema()
        +void dropSchema()
        +boolean validateSchema()
    }
    
    class MySQLDatabase {
        -String host
        -int port
        -String database
        -String username
        -String password
        +void createSchema()
        +void dropSchema()
        +boolean validateSchema()
    }
    
    class CassandraDatabase {
        -String host
        -int port
        -String keyspace
        -String datacenter
        +void createSchema()
        +void dropSchema()
        +boolean validateSchema()
    }
    
    SchemaManager --> DatabaseFactory
    DatabaseFactory --> Database
    Database <|.. SQLiteDatabase
    Database <|.. MySQLDatabase
    Database <|.. CassandraDatabase
```

## Component Initialization System
```mermaid
sequenceDiagram
    participant App as Application
    participant CI as ComponentInitializer
    participant FS as File System
    participant Config as Configuration
    participant Logging as Logging System
    
    App->>+CI: getInstance()
    CI->>CI: Check singleton instance
    CI->>+FS: setupDirectories()
    FS->>FS: Create .jvmxray/{component}/
    FS->>FS: Create config/, logs/, data/
    FS-->>-CI: Directories created
    
    CI->>+FS: copyResources()
    FS->>FS: Check for existing logback.xml
    alt No existing config
        FS->>FS: Copy logback.xml2 to logback.xml
    else Existing config
        FS->>FS: Preserve existing configuration
    end
    FS-->>-CI: Resources copied
    
    CI->>+Config: initializeProperties()
    Config->>Config: Load {component}.properties
    Config->>Config: Apply system property overrides
    Config-->>-CI: Properties loaded
    
    CI->>+Logging: initializeLogging()
    Logging->>Logging: Set logback.configurationFile
    Logging->>Logging: Configure component logging
    Logging-->>-CI: Logging initialized
    
    CI-->>-App: Initialization complete
```

## Directory Structure Pattern
```mermaid
graph TD
    A[.jvmxray/] --> B[agent/]
    A --> C[common/]
    A --> D[integration/]
    
    B --> E[config/]
    B --> F[logs/]
    B --> G[data/]
    
    C --> H[config/]
    C --> I[logs/]
    C --> J[data/]
    
    D --> K[config/]
    D --> L[logs/]
    D --> M[data/]
    
    E --> N[logback.xml]
    E --> O[agent.properties]
    
    H --> P[logback.xml]
    H --> Q[common.properties]
    
    J --> R[jvmxray-test.db]
    
    subgraph "Component Initialization"
        S[ComponentInitializer.setupDirectories()]
        T[ComponentInitializer.copyResources()]
        U[ComponentInitializer.initializeProperties()]
    end
    
    S --> A
    T --> N
    T --> P
    U --> O
    U --> Q
```

## Database Schema Design
```mermaid
erDiagram
    STAGE0_EVENT {
        varchar EVENT_ID PK "UUID generated for each event"
        varchar CONFIG_FILE "Component that generated event"
        timestamp TIMESTAMP "Event creation timestamp"
        varchar THREAD_ID "Java thread identifier"
        varchar PRIORITY "Log level (INFO, WARN, ERROR)"
        varchar NAMESPACE "Event category namespace"
        varchar AID "Application identifier"
        varchar CID "Component identifier"  
        boolean IS_STABLE "Consistency flag for NoSQL"
    }
    
    STAGE0_EVENT_KEYPAIR {
        varchar EVENT_ID FK "References STAGE0_EVENT"
        varchar KEY "Parameter name from event message"
        varchar VALUE "Parameter value from event message"
    }
    
    STAGE0_EVENT ||--o{ STAGE0_EVENT_KEYPAIR : "contains extracted keypairs"
```

## Event Processing Flow
```mermaid
flowchart LR
    A[Raw Logback Event] --> B[Event Parser]
    B --> C[Extract Base Fields]
    C --> D[Extract Key-Value Pairs]
    D --> E[Generate EVENT_ID]
    E --> F[Set IS_STABLE Flag]
    F --> G[Create Event Objects]
    
    G --> H[STAGE0_EVENT Object]
    G --> I[STAGE0_EVENT_KEYPAIR Objects]
    
    H --> J[Database Storage]
    I --> J
    
    subgraph "Event Format"
        K["CONFIG_FILE | timestamp | thread | priority | namespace | keypairs"]
    end
    
    A --> K
    
    subgraph "Extracted Fields"
        L[EVENT_ID: UUID]
        M[TIMESTAMP: ISO format]
        N[THREAD_ID: Java thread]
        O[NAMESPACE: org.jvmxray.events.*]
        P[KEYPAIRS: caller=..., target=...]
    end
    
    C --> L
    C --> M
    C --> N
    C --> O
    D --> P
```

## Multi-Database Support
```mermaid
graph TD
    A[Database Abstraction] --> B[SQLite Implementation]
    A --> C[MySQL Implementation] 
    A --> D[Cassandra Implementation]
    
    B --> E[File-based Storage]
    B --> F[Development/Testing]
    B --> G[Single Node Deployment]
    
    C --> H[ACID Transactions]
    C --> I[Production Ready]
    C --> J[Read Replicas]
    
    D --> K[Distributed Storage]
    D --> L[High Availability]
    D --> M[Horizontal Scaling]
    
    subgraph "Schema Operations"
        N[Create Schema]
        O[Drop Schema]
        P[Validate Schema]
        Q[Test Connection]
    end
    
    B --> N
    C --> N
    D --> N
    
    subgraph "Connection Management"
        R[Connection Pooling]
        S[Health Monitoring]
        T[Retry Logic]
        U[Failover Support]
    end
```

## Configuration Management
```mermaid
sequenceDiagram
    participant App as Application
    participant CM as ConfigManager
    participant SP as System Properties
    participant CF as Config Files
    participant ENV as Environment
    participant DEF as Defaults
    
    App->>+CM: loadConfiguration()
    
    CM->>+SP: getSystemProperties()
    SP-->>-CM: System property values
    
    CM->>+CF: loadConfigFile()
    CF-->>-CM: File property values
    
    CM->>+ENV: getEnvironmentVariables()
    ENV-->>-CM: Environment values
    
    CM->>+DEF: getDefaultValues()
    DEF-->>-CM: Default values
    
    CM->>CM: mergeConfigurations()
    Note over CM: Priority: System Props > Config File > Environment > Defaults
    
    CM-->>-App: Merged configuration
```

## Event Model Architecture
```mermaid
classDiagram
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
        +void addKeyPair(String key, String value)
        +String getKeyPair(String key)
    }
    
    class EventKeyPair {
        +String eventId
        +String key
        +String value
        +EventKeyPair(String eventId, String key, String value)
    }
    
    class SecurityEvent {
        +String severity
        +List~String~ cweIds
        +List~String~ mitreTactics
        +double confidenceScore
        +Map~String,Object~ securityMetadata
        +void addSecurityMetadata(String key, Object value)
    }
    
    class EventBuilder {
        -LogEvent event
        +EventBuilder setEventId(String id)
        +EventBuilder setNamespace(String namespace)
        +EventBuilder addKeyPair(String key, String value)
        +LogEvent build()
    }
    
    LogEvent --> EventKeyPair : contains
    LogEvent --> SecurityEvent : enhanced by
    EventBuilder --> LogEvent : creates
```

## Validation and Sanitization
```mermaid
flowchart TD
    A[Input Data] --> B[Validation Layer]
    B --> C{Valid Format?}
    C -->|No| D[Validation Error]
    C -->|Yes| E[Sanitization Layer]
    
    E --> F[Remove Sensitive Data]
    F --> G[Escape Special Characters]
    G --> H[Truncate Long Strings]
    H --> I[Normalize Format]
    
    I --> J[Validated & Sanitized Data]
    
    subgraph "Validation Rules"
        K[Required Field Check]
        L[Data Type Validation]
        M[Length Limits]
        N[Format Patterns]
    end
    
    B --> K
    B --> L
    B --> M
    B --> N
    
    subgraph "Sanitization Rules"
        O[SQL Injection Prevention]
        P[XSS Prevention]
        Q[Path Traversal Prevention]
        R[Sensitive Data Masking]
    end
    
    F --> O
    G --> P
    G --> Q
    F --> R
```

## Component Lifecycle Management
```mermaid
stateDiagram-v2
    [*] --> Uninitialized
    Uninitialized --> Initializing : getInstance()
    Initializing --> SetupDirectories : setupDirectories()
    SetupDirectories --> CopyResources : copyResources()
    CopyResources --> LoadProperties : initializeProperties()
    LoadProperties --> SetupLogging : initializeLogging()
    SetupLogging --> Ready : initialization complete
    
    Ready --> ConfigUpdate : property changes
    ConfigUpdate --> Ready : configuration updated
    
    Ready --> Shutdown : application shutdown
    Shutdown --> [*]
    
    note right of SetupDirectories
        Create .jvmxray/{component}/
        config/, logs/, data/
    end note
    
    note right of CopyResources
        Copy .xml2 templates to .xml
        Preserve existing configs
    end note
    
    note right of LoadProperties
        Load {component}.properties
        Apply system property overrides
    end note
```

## System Properties Integration
```mermaid
graph LR
    A[System Properties] --> B{Property Type?}
    
    B -->|Home Directory| C[jvmxray.home]
    B -->|Test Directory| D[jvmxray.test.home]
    B -->|Component Logs| E[jvmxray.{component}.logs]
    B -->|Component Config| F[jvmxray.{component}.config]
    B -->|Database URL| G[jvmxray.database.url]
    
    C --> H[Production Mode]
    D --> I[Test Mode]
    
    H --> J[{jvmxray.home}/jvmxray/]
    I --> K[{jvmxray.test.home}/]
    
    E --> L[Override Default Paths]
    F --> L
    G --> M[Override Database Connection]
    
    subgraph "Validation"
        N[Mutual Exclusivity Check]
        O[Path Existence Check]
        P[Permission Check]
    end
    
    H --> N
    I --> N
    L --> O
    M --> P
```

## Module Directory Structure
```
prj-common/
├── src/main/java/org/jvmxray/platform/shared/
│   ├── bin/
│   │   └── SchemaManager.java        # CLI schema management
│   ├── database/
│   │   ├── Database.java             # Database interface
│   │   ├── DatabaseFactory.java     # Database factory
│   │   ├── SQLiteDatabase.java      # SQLite implementation
│   │   ├── MySQLDatabase.java       # MySQL implementation
│   │   └── CassandraDatabase.java   # Cassandra implementation
│   ├── model/
│   │   ├── LogEvent.java            # Event data model
│   │   ├── EventKeyPair.java        # Key-value pair model
│   │   ├── SecurityEvent.java       # Security metadata model
│   │   └── EventBuilder.java        # Event construction
│   ├── init/
│   │   ├── ComponentInitializer.java # Base initializer
│   │   ├── AgentInitializer.java    # Agent initialization
│   │   ├── CommonInitializer.java   # Common module init
│   │   └── IntegrationInitializer.java # Integration init
│   ├── config/
│   │   ├── ConfigurationManager.java # Configuration handling
│   │   ├── DatabaseConfig.java      # Database configuration
│   │   └── ComponentConfig.java     # Component configuration
│   ├── util/
│   │   ├── ValidationUtils.java     # Input validation
│   │   ├── SanitizationUtils.java   # Data sanitization
│   │   ├── StringUtils.java         # String utilities
│   │   └── LoggingUtils.java        # Logging helpers
│   └── exception/
│       ├── JVMXRayException.java    # Base exception
│       ├── DatabaseException.java   # Database errors
│       ├── ConfigurationException.java # Config errors
│       └── ValidationException.java # Validation errors
├── src/main/resources/
│   ├── schema/
│   │   ├── sqlite-schema.sql        # SQLite schema
│   │   ├── mysql-schema.sql         # MySQL schema
│   │   └── cassandra-schema.cql     # Cassandra schema
│   ├── logback.xml2                 # Logback template
│   └── common.properties            # Default properties
└── src/test/java/
    └── org/jvmxray/platform/shared/
        ├── database/                # Database implementation tests
        ├── init/                    # Initialization tests
        ├── model/                   # Model tests
        └── util/                    # Utility tests
```

## Error Handling Strategy
```mermaid
flowchart TD
    A[Component Operation] --> B{Exception Occurs?}
    B -->|No| C[Normal Operation]
    B -->|Yes| D{Exception Type?}
    
    D -->|Configuration| E[ConfigurationException]
    D -->|Database| F[DatabaseException]
    D -->|Validation| G[ValidationException]
    D -->|System| H[JVMXRayException]
    
    E --> I[Log Configuration Error]
    F --> J[Log Database Error]
    G --> K[Log Validation Error]
    H --> L[Log System Error]
    
    I --> M[Use Default Configuration]
    J --> N[Retry with Fallback]
    K --> O[Skip Invalid Data]
    L --> P[Graceful Degradation]
    
    M --> Q[Continue Operation]
    N --> Q
    O --> Q
    P --> Q
    
    C --> Q
```

## Testing Infrastructure
```mermaid
graph TB
    subgraph "Test Categories"
        A[Unit Tests]
        B[Integration Tests]
        C[Database Tests]
        D[Configuration Tests]
    end
    
    A --> E[Component Initialization Tests]
    A --> F[Model Validation Tests]
    A --> G[Utility Function Tests]
    
    B --> H[Cross-Module Integration]
    B --> I[Database Schema Tests]
    B --> J[Configuration Loading Tests]
    
    C --> K[SQLite Test Database]
    C --> L[MySQL Test Container]
    C --> M[Cassandra Test Container]
    
    D --> N[Property File Loading]
    D --> O[System Property Override]
    D --> P[Environment Variable Tests]
    
    subgraph "Test Data"
        Q[Test Properties Files]
        R[Test Database Schemas]
        S[Mock Event Data]
        T[Test Configuration Templates]
    end
```

## Performance Considerations
```mermaid
graph LR
    A[Performance Optimization] --> B[Database Operations]
    A --> C[Memory Management]
    A --> D[Configuration Loading]
    
    B --> E[Connection Pooling]
    B --> F[Prepared Statements]
    B --> G[Batch Operations]
    
    C --> H[Object Reuse]
    C --> I[String Interning]
    C --> J[Lazy Initialization]
    
    D --> K[Configuration Caching]
    D --> L[Property Validation]
    D --> M[Default Value Optimization]
    
    subgraph "Metrics"
        N[Initialization Time]
        O[Memory Footprint]
        P[Database Response Time]
        Q[Configuration Load Time]
    end
```

## See Also
- [Architecture Overview](../docs/architecture/README.md)
- [Database Schema Documentation](../docs/architecture/database-schema.md)
- [Component Initialization Guide](../docs/guides/component-initialization.md)
- [Configuration Reference](../docs/reference/configuration.md)