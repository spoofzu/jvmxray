package org.jvmxray.platform.shared.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.logging.Level;

/**
 * SQLite-specific implementation of database schema operations.
 * Handles creation, validation, and management of JVMXRay schema
 * in SQLite embedded database.
 * 
 * @author Milton Smith
 */
public class SQLiteSchema extends AbstractDatabaseSchema {
    
    private final String databasePath;
    
    /**
     * Constructor for SQLite schema manager.
     * 
     * @param connectionUrl JDBC SQLite connection URL (e.g., "jdbc:sqlite:/path/to/db.sqlite")
     * @param databaseName Database name (used for logging, not for connection)
     */
    public SQLiteSchema(String connectionUrl, String databaseName) {
        super(connectionUrl, null, null, databaseName); // SQLite doesn't use username/password
        
        // Extract database file path from connection URL
        if (connectionUrl.startsWith("jdbc:sqlite:")) {
            this.databasePath = connectionUrl.substring("jdbc:sqlite:".length());
        } else {
            throw new IllegalArgumentException("Invalid SQLite connection URL: " + connectionUrl);
        }
        
        logger.info("Initialized SQLite schema manager for database: " + databasePath);
    }
    
    @Override
    protected Connection getConnection() throws SQLException {
        try {
            // Ensure the parent directory exists
            Path dbPath = Paths.get(databasePath);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.info("Created parent directory: " + parentDir);
            }
            
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            Connection connection = DriverManager.getConnection(connectionUrl);
            
            // Enable foreign key constraints (though we won't use them for NoSQL compatibility)
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            
            logger.fine("Successfully connected to SQLite database: " + databasePath);
            return connection;
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory", e);
        }
    }
    
    @Override
    protected void createDatabase() throws SQLException {
        // SQLite creates the database file automatically when first accessed
        // Just verify we can connect
        Connection connection = null;
        try {
            connection = getConnection();
            logger.info("SQLite database file created/verified: " + databasePath);
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
            
            // Create STAGE0_EVENT table
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_SQLITE);
            logger.info("Created STAGE0_EVENT table");
            
            // Create STAGE0_EVENT_KEYPAIR table
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_KEYPAIR_SQLITE);
            logger.info("Created STAGE0_EVENT_KEYPAIR table");
            
            connection.commit();
            logger.info("Successfully created all SQLite tables");
            
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
            
            // Create index on timestamp for performance
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_timestamp ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_TIMESTAMP + ")");
            
            // Create index on namespace for filtering
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_namespace ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_NAMESPACE + ")");
            
            // Create index on AID for application filtering
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_aid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_AID + ")");
            
            // Create index on CID for category filtering
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_cid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_CID + ")");
            
            // Create index on IS_STABLE for consistency queries
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_stable ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_IS_STABLE + ")");
            
            logger.info("Successfully created all SQLite indexes");
            
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
            
            // Drop tables in reverse order (keypair first due to relationship)
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE0_EVENT_KEYPAIR);
            logger.info("Dropped STAGE0_EVENT_KEYPAIR table");
            
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE0_EVENT);
            logger.info("Dropped STAGE0_EVENT table");
            
            connection.commit();
            logger.info("Successfully dropped all SQLite tables");
            
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
        try {
            connection = getConnection();
            
            // Check if STAGE0_EVENT table exists
            boolean eventTableExists = checkTableExists(connection, SchemaConstants.STAGE0_EVENT_TABLE);
            
            // Check if STAGE0_EVENT_KEYPAIR table exists
            boolean keypairTableExists = checkTableExists(connection, SchemaConstants.STAGE0_EVENT_KEYPAIR_TABLE);
            
            boolean allTablesExist = eventTableExists && keypairTableExists;
            
            if (allTablesExist) {
                logger.info("All required SQLite tables exist");
            } else {
                logger.warning(String.format("Missing SQLite tables - Event: %b, KeyPair: %b", 
                    eventTableExists, keypairTableExists));
            }
            
            return allTablesExist;
            
        } finally {
            closeConnection(connection);
        }
    }
    
    @Override
    protected boolean validateTableStructure() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            
            // Check STAGE0_EVENT table structure
            boolean eventTableValid = validateEventTableStructure(connection);
            
            // Check STAGE0_EVENT_KEYPAIR table structure  
            boolean keypairTableValid = validateKeypairTableStructure(connection);
            
            boolean structureValid = eventTableValid && keypairTableValid;
            
            if (structureValid) {
                logger.info("SQLite table structures are valid");
            } else {
                logger.warning("SQLite table structure validation failed");
            }
            
            return structureValid;
            
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Validate the structure of the STAGE0_EVENT table.
     */
    private boolean validateEventTableStructure(Connection connection) throws SQLException {
        try {
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE0_EVENT_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);
            
            int columnCount = 0;
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE0_EVENT: " + columnName);
            }
            
            // Should have 9 columns: EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, IS_STABLE
            return columnCount == 9;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE0_EVENT table structure", e);
            return false;
        }
    }
    
    /**
     * Validate the structure of the STAGE0_EVENT_KEYPAIR table.
     */
    private boolean validateKeypairTableStructure(Connection connection) throws SQLException {
        try {
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE0_EVENT_KEYPAIR_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);
            
            int columnCount = 0;
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE0_EVENT_KEYPAIR: " + columnName);
            }
            
            // Should have 3 columns: EVENT_ID, KEY, VALUE
            return columnCount == 3;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE0_EVENT_KEYPAIR table structure", e);
            return false;
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "SQLite";
    }
    
    /**
     * Get the path to the SQLite database file.
     * 
     * @return Database file path
     */
    public String getDatabasePath() {
        return databasePath;
    }
    
    /**
     * Check if the SQLite database file exists.
     * 
     * @return true if database file exists, false otherwise
     */
    public boolean databaseFileExists() {
        return Files.exists(Paths.get(databasePath));
    }
    
    /**
     * Delete the SQLite database file.
     * This completely removes the database.
     * 
     * @return true if file was deleted, false if it didn't exist or couldn't be deleted
     */
    public boolean deleteDatabaseFile() {
        try {
            Path dbPath = Paths.get(databasePath);
            if (Files.exists(dbPath)) {
                Files.delete(dbPath);
                logger.info("Deleted SQLite database file: " + databasePath);
                return true;
            } else {
                logger.info("SQLite database file does not exist: " + databasePath);
                return false;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete SQLite database file: " + databasePath, e);
            return false;
        }
    }
    
    /**
     * Check if a specific table exists in the SQLite database.
     * 
     * @param connection Database connection
     * @param tableName Table name to check
     * @return true if table exists, false otherwise
     * @throws SQLException if query fails
     */
    private boolean checkTableExists(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement(SchemaConstants.SQLTemplates.CHECK_TABLE_EXISTS_SQLITE)) {
            statement.setString(1, tableName);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to check if table exists: " + tableName, e);
            return false;
        }
    }
}