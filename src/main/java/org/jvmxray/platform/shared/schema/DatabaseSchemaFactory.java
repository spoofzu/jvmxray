package org.jvmxray.platform.shared.schema;

import java.util.logging.Logger;

/**
 * Factory class for creating database schema implementations.
 * Provides a unified interface for instantiating schema managers
 * for different database types (Cassandra, MySQL, SQLite).
 * 
 * @author Milton Smith
 */
public class DatabaseSchemaFactory {
    
    private static final Logger logger = Logger.getLogger(DatabaseSchemaFactory.class.getName());
    
    /**
     * Supported database types.
     */
    public enum DatabaseType {
        CASSANDRA("cassandra"),
        MYSQL("mysql"), 
        SQLITE("sqlite");
        
        private final String typeName;
        
        DatabaseType(String typeName) {
            this.typeName = typeName;
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        /**
         * Parse database type from string.
         * 
         * @param typeStr Database type string
         * @return DatabaseType enum value
         * @throws IllegalArgumentException if type is not supported
         */
        public static DatabaseType fromString(String typeStr) {
            if (typeStr == null) {
                throw new IllegalArgumentException("Database type cannot be null");
            }
            
            String normalizedType = typeStr.trim().toLowerCase();
            for (DatabaseType type : DatabaseType.values()) {
                if (type.getTypeName().equals(normalizedType)) {
                    return type;
                }
            }
            
            throw new IllegalArgumentException("Unsupported database type: " + typeStr + 
                ". Supported types: " + getSupportedTypes());
        }
        
        /**
         * Get comma-separated list of supported database types.
         */
        public static String getSupportedTypes() {
            StringBuilder sb = new StringBuilder();
            for (DatabaseType type : DatabaseType.values()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(type.getTypeName());
            }
            return sb.toString();
        }
    }
    
    /**
     * Configuration class for database connection parameters.
     */
    public static class DatabaseConfig {
        private DatabaseType type;
        private String connectionUrl;
        private String host;
        private int port;
        private String username;
        private String password;
        private String databaseName;
        private String datacenter;
        private int replicationFactor = SchemaConstants.DEFAULT_REPLICATION_FACTOR;
        
        // Getters and setters
        public DatabaseType getType() { return type; }
        public void setType(DatabaseType type) { this.type = type; }
        
        public String getConnectionUrl() { return connectionUrl; }
        public void setConnectionUrl(String connectionUrl) { this.connectionUrl = connectionUrl; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        
        public String getDatacenter() { return datacenter; }
        public void setDatacenter(String datacenter) { this.datacenter = datacenter; }
        
        public int getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }
        
        /**
         * Validate that required configuration parameters are set.
         */
        public void validate() {
            if (type == null) {
                throw new IllegalArgumentException("Database type is required");
            }
            
            switch (type) {
                case SQLITE:
                    if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
                        throw new IllegalArgumentException("Connection URL is required for SQLite");
                    }
                    break;
                    
                case MYSQL:
                    if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
                        throw new IllegalArgumentException("Connection URL is required for MySQL");
                    }
                    if (username == null || username.trim().isEmpty()) {
                        throw new IllegalArgumentException("Username is required for MySQL");
                    }
                    if (password == null) {
                        throw new IllegalArgumentException("Password is required for MySQL");
                    }
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Database name is required for MySQL");
                    }
                    break;
                    
                case CASSANDRA:
                    if (host == null || host.trim().isEmpty()) {
                        throw new IllegalArgumentException("Host is required for Cassandra");
                    }
                    if (port <= 0) {
                        throw new IllegalArgumentException("Valid port is required for Cassandra");
                    }
                    if (username == null || username.trim().isEmpty()) {
                        throw new IllegalArgumentException("Username is required for Cassandra");
                    }
                    if (password == null) {
                        throw new IllegalArgumentException("Password is required for Cassandra");
                    }
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Keyspace name is required for Cassandra");
                    }
                    if (datacenter == null || datacenter.trim().isEmpty()) {
                        throw new IllegalArgumentException("Datacenter is required for Cassandra");
                    }
                    break;
            }
        }
        
        @Override
        public String toString() {
            return String.format("DatabaseConfig{type=%s, host=%s, port=%d, databaseName=%s, datacenter=%s}", 
                type, host, port, databaseName, datacenter);
        }
    }
    
    /**
     * Create a database schema implementation based on the provided configuration.
     * 
     * @param config Database configuration parameters
     * @return Appropriate AbstractDatabaseSchema implementation
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static AbstractDatabaseSchema createSchema(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }
        
        config.validate();
        
        logger.info("Creating database schema for: " + config.toString());
        
        switch (config.getType()) {
            case SQLITE:
                return createSQLiteSchema(config);
                
            case MYSQL:
                return createMySQLSchema(config);
                
            case CASSANDRA:
                return createCassandraSchema(config);
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + config.getType());
        }
    }
    
    /**
     * Create SQLite schema implementation.
     */
    private static SQLiteSchema createSQLiteSchema(DatabaseConfig config) {
        String databaseName = config.getDatabaseName();
        if (databaseName == null || databaseName.trim().isEmpty()) {
            // Extract database name from connection URL for SQLite
            databaseName = extractDatabaseNameFromSQLiteUrl(config.getConnectionUrl());
        }
        
        return new SQLiteSchema(config.getConnectionUrl(), databaseName);
    }
    
    /**
     * Create MySQL schema implementation.
     */
    private static MySQLSchema createMySQLSchema(DatabaseConfig config) {
        return new MySQLSchema(
            config.getConnectionUrl(),
            config.getUsername(),
            config.getPassword(),
            config.getDatabaseName()
        );
    }
    
    /**
     * Create Cassandra schema implementation.
     */
    private static CassandraSchema createCassandraSchema(DatabaseConfig config) {
        return new CassandraSchema(
            config.getHost(),
            config.getPort(),
            config.getUsername(),
            config.getPassword(),
            config.getDatabaseName(),
            config.getDatacenter(),
            config.getReplicationFactor()
        );
    }
    
    /**
     * Extract database name from SQLite connection URL.
     * 
     * @param connectionUrl SQLite JDBC URL
     * @return Database name (filename without path and extension)
     */
    private static String extractDatabaseNameFromSQLiteUrl(String connectionUrl) {
        try {
            // Expected format: jdbc:sqlite:/path/to/database.db
            String path = connectionUrl.substring("jdbc:sqlite:".length());
            
            // Extract filename from path
            String filename = path;
            if (path.contains("/")) {
                filename = path.substring(path.lastIndexOf("/") + 1);
            } else if (path.contains("\\")) {
                filename = path.substring(path.lastIndexOf("\\") + 1);
            }
            
            // Remove extension
            if (filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf("."));
            }
            
            return filename.isEmpty() ? "jvmxray" : filename;
            
        } catch (Exception e) {
            logger.warning("Failed to extract database name from SQLite URL: " + connectionUrl);
            return "jvmxray";
        }
    }
    
    /**
     * Create a database configuration for SQLite using the test database path.
     * This is a convenience method for testing scenarios.
     * 
     * @return DatabaseConfig configured for SQLite test database
     */
    public static DatabaseConfig createSQLiteTestConfig() {
        String jvmxrayHome = System.getProperty(SchemaConstants.Config.JVMXRAY_HOME_PROPERTY,
            SchemaConstants.Config.getDefaultJvmxrayHome());
        String testDbPath = jvmxrayHome + SchemaConstants.Config.TEST_DB_RELATIVE_PATH;
        String connectionUrl = "jdbc:sqlite:" + testDbPath;
        
        DatabaseConfig config = new DatabaseConfig();
        config.setType(DatabaseType.SQLITE);
        config.setConnectionUrl(connectionUrl);
        config.setDatabaseName("jvmxray-test");
        
        return config;
    }
    
    /**
     * Create a database configuration from command line parameters.
     * This is a convenience method for CLI tools.
     * 
     * @param databaseType Database type string
     * @param connectionUrl JDBC connection URL (for MySQL/SQLite)
     * @param host Database host (for Cassandra)
     * @param port Database port
     * @param username Database username
     * @param password Database password
     * @param databaseName Database/keyspace name
     * @param datacenter Datacenter name (for Cassandra)
     * @param replicationFactor Replication factor (for Cassandra)
     * @return Configured DatabaseConfig object
     */
    public static DatabaseConfig createConfigFromParameters(
            String databaseType, String connectionUrl, String host, Integer port,
            String username, String password, String databaseName, 
            String datacenter, Integer replicationFactor) {
        
        DatabaseConfig config = new DatabaseConfig();
        config.setType(DatabaseType.fromString(databaseType));
        
        if (connectionUrl != null && !connectionUrl.trim().isEmpty()) {
            config.setConnectionUrl(connectionUrl);
        }
        
        if (host != null && !host.trim().isEmpty()) {
            config.setHost(host);
        }
        
        if (port != null && port > 0) {
            config.setPort(port);
        }
        
        if (username != null && !username.trim().isEmpty()) {
            config.setUsername(username);
        }
        
        if (password != null) {
            config.setPassword(password);
        }
        
        if (databaseName != null && !databaseName.trim().isEmpty()) {
            config.setDatabaseName(databaseName);
        }
        
        if (datacenter != null && !datacenter.trim().isEmpty()) {
            config.setDatacenter(datacenter);
        }
        
        if (replicationFactor != null && replicationFactor > 0) {
            config.setReplicationFactor(replicationFactor);
        }
        
        return config;
    }
}