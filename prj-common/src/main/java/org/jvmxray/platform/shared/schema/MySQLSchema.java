package org.jvmxray.platform.shared.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * MySQL-specific implementation of database schema operations.
 * Handles creation, validation, and management of JVMXRay schema
 * in MySQL database.
 * 
 * @author Milton Smith
 */
public class MySQLSchema extends AbstractDatabaseSchema {
    
    private final String host;
    private final int port;
    
    /**
     * Constructor for MySQL schema manager.
     * 
     * @param connectionUrl JDBC MySQL connection URL
     * @param username Database username
     * @param password Database password  
     * @param databaseName Database name
     */
    public MySQLSchema(String connectionUrl, String username, String password, String databaseName) {
        super(connectionUrl, username, password, databaseName);
        
        // Extract host and port from connection URL for database creation
        this.host = extractHostFromUrl(connectionUrl);
        this.port = extractPortFromUrl(connectionUrl);
        
        logger.info("Initialized MySQL schema manager for database: " + databaseName + 
                   " at " + host + ":" + port);
    }
    
    @Override
    protected Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Connection connection = DriverManager.getConnection(connectionUrl, username, password);
            
            logger.fine("Successfully connected to MySQL database: " + databaseName);
            return connection;
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }
    
    /**
     * Get connection to MySQL server (without specific database) for database creation.
     */
    private Connection getServerConnection() throws SQLException {
        try {
            // Build server connection URL (without database name)
            String serverUrl = String.format("jdbc:mysql://%s:%d/", host, port);
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(serverUrl, username, password);
            
            logger.fine("Successfully connected to MySQL server");
            return connection;
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }
    
    @Override
    protected void createDatabase() throws SQLException {
        Connection connection = null;
        try {
            connection = getServerConnection();
            
            // Create database if it doesn't exist
            String createDbSql = "CREATE DATABASE IF NOT EXISTS " + databaseName + 
                               " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            executeSQL(connection, createDbSql);
            
            logger.info("MySQL database created/verified: " + databaseName);
            
        } finally {
            closeConnection(connection);
        }
    }
    
    @Override
    protected void createTables() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false); // Use transaction
            
            // Use the specific database
            executeSQL(connection, "USE " + databaseName);
            
            // Create STAGE0_EVENT table (raw events with KEYPAIRS column)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_MYSQL);
            logger.info("Created STAGE0_EVENT table");
            
            // Create STAGE1_EVENT table (processed events with IS_STABLE)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE1_EVENT_MYSQL);
            logger.info("Created STAGE1_EVENT table");
            
            // Create STAGE1_EVENT_KEYPAIR table (normalized keypairs)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE1_EVENT_KEYPAIR_MYSQL);
            logger.info("Created STAGE1_EVENT_KEYPAIR table");
            
            connection.commit();
            logger.info("Successfully created all MySQL tables");
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    logger.warning("Rolled back table creation due to error");
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to reset auto-commit", e);
                }
                closeConnection(connection);
            }
        }
    }
    
    @Override
    protected void createIndexes() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Use the specific database
            executeSQL(connection, "USE " + databaseName);
            
            // STAGE0_EVENT indexes (raw events)
            executeSQL(connection, 
                "CREATE INDEX idx_stage0_event_timestamp ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_TIMESTAMP + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage0_event_namespace ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_NAMESPACE + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage0_event_aid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_AID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage0_event_cid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_CID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage0_event_config ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_CONFIG_FILE + ")");
            
            // STAGE1_EVENT indexes (processed events)
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_timestamp ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_TIMESTAMP + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_namespace ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_NAMESPACE + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_aid ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_AID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_cid ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_CID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_config ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_CONFIG_FILE + ")");
            
            // Create index on IS_STABLE for consistency queries (only on STAGE1)
            executeSQL(connection, 
                "CREATE INDEX idx_stage1_event_stable ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_IS_STABLE + ")");
            
            logger.info("Successfully created all MySQL indexes");
            
        } finally {
            closeConnection(connection);
        }
    }
    
    @Override
    protected void dropTables() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false); // Use transaction
            
            // Use the specific database
            executeSQL(connection, "USE " + databaseName);
            
            // Drop tables in reverse order (keypair first due to relationship)
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE1_EVENT_KEYPAIR);
            logger.info("Dropped STAGE1_EVENT_KEYPAIR table");
            
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE1_EVENT);
            logger.info("Dropped STAGE1_EVENT table");
            
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE0_EVENT);
            logger.info("Dropped STAGE0_EVENT table");
            
            connection.commit();
            logger.info("Successfully dropped all MySQL tables");
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    logger.warning("Rolled back table dropping due to error");
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to reset auto-commit", e);
                }
                closeConnection(connection);
            }
        }
    }
    
    @Override
    protected boolean checkTablesExist() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            
            // Check if STAGE0_EVENT table exists
            statement = connection.prepareStatement(SchemaConstants.SQLTemplates.CHECK_TABLE_EXISTS_MYSQL);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE0_EVENT_TABLE);
            int stage0EventCount = executeCountQuery(connection, statement);
            boolean stage0EventExists = stage0EventCount > 0;
            
            // Check if STAGE1_EVENT table exists
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_TABLE);
            int stage1EventCount = executeCountQuery(connection, statement);
            boolean stage1EventExists = stage1EventCount > 0;
            
            // Check if STAGE1_EVENT_KEYPAIR table exists
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE);
            int stage1KeypairCount = executeCountQuery(connection, statement);
            boolean stage1KeypairExists = stage1KeypairCount > 0;
            
            boolean allTablesExist = stage0EventExists && stage1EventExists && stage1KeypairExists;
            
            if (allTablesExist) {
                logger.info("All required MySQL tables exist");
            } else {
                logger.warning(String.format("Missing MySQL tables - Stage0Event: %b, Stage1Event: %b, Stage1KeyPair: %b", 
                    stage0EventExists, stage1EventExists, stage1KeypairExists));
            }
            
            return allTablesExist;
            
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to close prepared statement", e);
                }
            }
            closeConnection(connection);
        }
    }
    
    /**
     * Execute a count query using a prepared statement.
     */
    private int executeCountQuery(Connection connection, PreparedStatement statement) throws SQLException {
        try (var resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }
    
    @Override
    protected boolean validateTableStructure() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Check STAGE0_EVENT table structure (should have KEYPAIRS column, no IS_STABLE)
            boolean stage0EventValid = validateStage0EventTableStructure(connection);
            
            // Check STAGE1_EVENT table structure (should have IS_STABLE column, no KEYPAIRS)
            boolean stage1EventValid = validateStage1EventTableStructure(connection);
            
            // Check STAGE1_EVENT_KEYPAIR table structure  
            boolean stage1KeypairValid = validateStage1KeypairTableStructure(connection);
            
            boolean structureValid = stage0EventValid && stage1EventValid && stage1KeypairValid;
            
            if (structureValid) {
                logger.info("MySQL table structures are valid");
            } else {
                logger.warning("MySQL table structure validation failed");
            }
            
            return structureValid;
            
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Validate the structure of the STAGE0_EVENT table (should have KEYPAIRS column, no IS_STABLE).
     */
    private boolean validateStage0EventTableStructure(Connection connection) throws SQLException {
        try {
            // Check column count
            String countSql = "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE table_schema = ? AND table_name = ?";
            PreparedStatement statement = connection.prepareStatement(countSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE0_EVENT_TABLE);
            int columnCount = executeCountQuery(connection, statement);
            
            // Check for KEYPAIRS column
            String keypairsSql = "SELECT COUNT(*) FROM information_schema.columns " +
                               "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            statement = connection.prepareStatement(keypairsSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE0_EVENT_TABLE);
            statement.setString(3, SchemaConstants.COL_KEYPAIRS);
            boolean hasKeypairsColumn = executeCountQuery(connection, statement) > 0;
            
            // Check for IS_STABLE column (should NOT exist)
            String stableSql = "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            statement = connection.prepareStatement(stableSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE0_EVENT_TABLE);
            statement.setString(3, SchemaConstants.COL_IS_STABLE);
            boolean hasIsStableColumn = executeCountQuery(connection, statement) > 0;
            
            // Should have 9 columns with KEYPAIRS but without IS_STABLE
            return columnCount == 9 && hasKeypairsColumn && !hasIsStableColumn;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE0_EVENT table structure", e);
            return false;
        }
    }
    
    /**
     * Validate the structure of the STAGE1_EVENT table (should have IS_STABLE column, no KEYPAIRS).
     */
    private boolean validateStage1EventTableStructure(Connection connection) throws SQLException {
        try {
            // Check column count
            String countSql = "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE table_schema = ? AND table_name = ?";
            PreparedStatement statement = connection.prepareStatement(countSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_TABLE);
            int columnCount = executeCountQuery(connection, statement);
            
            // Check for KEYPAIRS column (should NOT exist)
            String keypairsSql = "SELECT COUNT(*) FROM information_schema.columns " +
                               "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            statement = connection.prepareStatement(keypairsSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_TABLE);
            statement.setString(3, SchemaConstants.COL_KEYPAIRS);
            boolean hasKeypairsColumn = executeCountQuery(connection, statement) > 0;
            
            // Check for IS_STABLE column
            String stableSql = "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            statement = connection.prepareStatement(stableSql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_TABLE);
            statement.setString(3, SchemaConstants.COL_IS_STABLE);
            boolean hasIsStableColumn = executeCountQuery(connection, statement) > 0;
            
            // Should have 9 columns with IS_STABLE but without KEYPAIRS
            return columnCount == 9 && !hasKeypairsColumn && hasIsStableColumn;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE1_EVENT table structure", e);
            return false;
        }
    }
    
    /**
     * Validate the structure of the STAGE1_EVENT_KEYPAIR table.
     */
    private boolean validateStage1KeypairTableStructure(Connection connection) throws SQLException {
        try {
            // Query information_schema to check columns
            String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, databaseName);
            statement.setString(2, SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE);
            
            int columnCount = executeCountQuery(connection, statement);
            
            // Should have 3 columns: EVENT_ID, KEY, VALUE
            return columnCount == 3;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE1_EVENT_KEYPAIR table structure", e);
            return false;
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "MySQL";
    }
    
    /**
     * Extract host from JDBC URL.
     */
    private String extractHostFromUrl(String url) {
        try {
            // Expected format: jdbc:mysql://host:port/database
            String hostPart = url.substring(url.indexOf("://") + 3);
            if (hostPart.contains("/")) {
                hostPart = hostPart.substring(0, hostPart.indexOf("/"));
            }
            if (hostPart.contains(":")) {
                return hostPart.substring(0, hostPart.indexOf(":"));
            }
            return hostPart;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to extract host from URL: " + url, e);
            return "localhost";
        }
    }
    
    /**
     * Extract port from JDBC URL.
     */
    private int extractPortFromUrl(String url) {
        try {
            // Expected format: jdbc:mysql://host:port/database
            String hostPart = url.substring(url.indexOf("://") + 3);
            if (hostPart.contains("/")) {
                hostPart = hostPart.substring(0, hostPart.indexOf("/"));
            }
            if (hostPart.contains(":")) {
                String portStr = hostPart.substring(hostPart.indexOf(":") + 1);
                return Integer.parseInt(portStr);
            }
            return 3306; // Default MySQL port
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to extract port from URL: " + url, e);
            return 3306;
        }
    }
    
    /**
     * Test the database connection.
     * 
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        Connection connection = null;
        try {
            connection = getConnection();
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database connection test failed", e);
            return false;
        } finally {
            closeConnection(connection);
        }
    }
}