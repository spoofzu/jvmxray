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
            
            // Create STAGE0_EVENT table (raw events with KEYPAIRS column)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_SQLITE);
            logger.info("Created STAGE0_EVENT table");
            
            // Create STAGE1_EVENT table (processed events with IS_STABLE)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE1_EVENT_SQLITE);
            logger.info("Created STAGE1_EVENT table");
            
            // Create STAGE1_EVENT_KEYPAIR table (normalized keypairs)
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE1_EVENT_KEYPAIR_SQLITE);
            logger.info("Created STAGE1_EVENT_KEYPAIR table");

            // Create API_KEY table for REST service authentication
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_API_KEY_SQLITE);
            logger.info("Created API_KEY table");

            // Create STAGE2_LIBRARY table for library enrichment
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE2_LIBRARY_SQLITE);
            logger.info("Created STAGE2_LIBRARY table");

            // Create STAGE2_LIBRARY_CVE table for CVE associations
            executeSQL(connection, SchemaConstants.SQLTemplates.CREATE_STAGE2_LIBRARY_CVE_SQLITE);
            logger.info("Created STAGE2_LIBRARY_CVE table");

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
            
            // STAGE0_EVENT indexes (raw events)
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_timestamp ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_TIMESTAMP + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_namespace ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_NAMESPACE + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_aid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_AID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_cid ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_CID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_config ON " + 
                SchemaConstants.STAGE0_EVENT_TABLE + "(" + SchemaConstants.COL_CONFIG_FILE + ")");
            
            // STAGE1_EVENT indexes (processed events)
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_timestamp ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_TIMESTAMP + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_namespace ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_NAMESPACE + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_aid ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_AID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_cid ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_CID + ")");
            
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_config ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_CONFIG_FILE + ")");
            
            // Create index on IS_STABLE for consistency queries (only on STAGE1)
            executeSQL(connection, 
                "CREATE INDEX IF NOT EXISTS idx_stage1_event_stable ON " + 
                SchemaConstants.STAGE1_EVENT_TABLE + "(" + SchemaConstants.COL_IS_STABLE + ")");
            
            // STAGE2_LIBRARY indexes
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_sha256 ON " +
                SchemaConstants.STAGE2_LIBRARY_TABLE + "(" + SchemaConstants.COL_SHA256_HASH + ")");
            
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_jarpath ON " +
                SchemaConstants.STAGE2_LIBRARY_TABLE + "(" + SchemaConstants.COL_JARPATH + ")");
            
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_first_seen ON " +
                SchemaConstants.STAGE2_LIBRARY_TABLE + "(" + SchemaConstants.COL_FIRST_SEEN + ")");
            
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_last_seen ON " +
                SchemaConstants.STAGE2_LIBRARY_TABLE + "(" + SchemaConstants.COL_LAST_SEEN + ")");
            
            // STAGE2_LIBRARY_CVE indexes (based on actual table structure)
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_cve_severity ON " +
                SchemaConstants.STAGE2_LIBRARY_CVE_TABLE + "(" + SchemaConstants.COL_CVSS_SEVERITY + ")");
            
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_cve_published ON " +
                SchemaConstants.STAGE2_LIBRARY_CVE_TABLE + "(" + SchemaConstants.COL_PUBLISHED_DATE + ")");
            
            executeSQL(connection,
                "CREATE INDEX IF NOT EXISTS idx_stage2_library_cve_cvss_v3 ON " +
                SchemaConstants.STAGE2_LIBRARY_CVE_TABLE + "(" + SchemaConstants.COL_CVSS_V3 + ")");
            
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
            
            // Drop tables in reverse order (dependent tables first)
            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE2_LIBRARY_CVE);
            logger.info("Dropped STAGE2_LIBRARY_CVE table");

            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE2_LIBRARY);
            logger.info("Dropped STAGE2_LIBRARY table");

            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_API_KEY);
            logger.info("Dropped API_KEY table");

            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE1_EVENT_KEYPAIR);
            logger.info("Dropped STAGE1_EVENT_KEYPAIR table");

            executeSQL(connection, SchemaConstants.SQLTemplates.DROP_STAGE1_EVENT);
            logger.info("Dropped STAGE1_EVENT table");

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
            boolean stage0EventExists = checkTableExists(connection, SchemaConstants.STAGE0_EVENT_TABLE);
            
            // Check if STAGE1_EVENT table exists
            boolean stage1EventExists = checkTableExists(connection, SchemaConstants.STAGE1_EVENT_TABLE);
            
            // Check if STAGE1_EVENT_KEYPAIR table exists
            boolean stage1KeypairExists = checkTableExists(connection, SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE);

            // Check if API_KEY table exists
            boolean apiKeyExists = checkTableExists(connection, SchemaConstants.API_KEY_TABLE);

            // Check if STAGE2_LIBRARY table exists
            boolean stage2LibraryExists = checkTableExists(connection, SchemaConstants.STAGE2_LIBRARY_TABLE);

            // Check if STAGE2_LIBRARY_CVE table exists
            boolean stage2LibraryCveExists = checkTableExists(connection, SchemaConstants.STAGE2_LIBRARY_CVE_TABLE);

            boolean allTablesExist = stage0EventExists && stage1EventExists && stage1KeypairExists &&
                                   apiKeyExists && stage2LibraryExists && stage2LibraryCveExists;

            if (allTablesExist) {
                logger.info("All required SQLite tables exist");
            } else {
                logger.warning(String.format("Missing SQLite tables - Stage0Event: %b, Stage1Event: %b, Stage1KeyPair: %b, ApiKey: %b, Stage2Library: %b, Stage2LibraryCve: %b",
                    stage0EventExists, stage1EventExists, stage1KeypairExists, apiKeyExists, stage2LibraryExists, stage2LibraryCveExists));
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
            
            // Check STAGE0_EVENT table structure (should have KEYPAIRS column, no IS_STABLE)
            boolean stage0EventValid = validateStage0EventTableStructure(connection);
            
            // Check STAGE1_EVENT table structure (should have IS_STABLE column, no KEYPAIRS)
            boolean stage1EventValid = validateStage1EventTableStructure(connection);
            
            // Check STAGE1_EVENT_KEYPAIR table structure
            boolean stage1KeypairValid = validateStage1KeypairTableStructure(connection);

            // Check STAGE2_LIBRARY table structure
            boolean stage2LibraryValid = validateStage2LibraryTableStructure(connection);

            // Check STAGE2_LIBRARY_CVE table structure
            boolean stage2LibraryCveValid = validateStage2LibraryCveTableStructure(connection);

            boolean structureValid = stage0EventValid && stage1EventValid && stage1KeypairValid &&
                                   stage2LibraryValid && stage2LibraryCveValid;
            
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
     * Validate the structure of the STAGE0_EVENT table (should have KEYPAIRS column, no IS_STABLE).
     */
    private boolean validateStage0EventTableStructure(Connection connection) throws SQLException {
        try {
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE0_EVENT_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);
            
            int columnCount = 0;
            boolean hasKeypairsColumn = false;
            boolean hasIsStableColumn = false;
            
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE0_EVENT: " + columnName);
                
                if (SchemaConstants.COL_KEYPAIRS.equals(columnName)) {
                    hasKeypairsColumn = true;
                }
                if (SchemaConstants.COL_IS_STABLE.equals(columnName)) {
                    hasIsStableColumn = true;
                }
            }
            
            // Should have 9 columns with KEYPAIRS but without IS_STABLE
            // EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, KEYPAIRS
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
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE1_EVENT_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);
            
            int columnCount = 0;
            boolean hasKeypairsColumn = false;
            boolean hasIsStableColumn = false;
            
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE1_EVENT: " + columnName);
                
                if (SchemaConstants.COL_KEYPAIRS.equals(columnName)) {
                    hasKeypairsColumn = true;
                }
                if (SchemaConstants.COL_IS_STABLE.equals(columnName)) {
                    hasIsStableColumn = true;
                }
            }
            
            // Should have 9 columns with IS_STABLE but without KEYPAIRS
            // EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, IS_STABLE
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
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);
            
            int columnCount = 0;
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE1_EVENT_KEYPAIR: " + columnName);
            }
            
            // Should have 3 columns: EVENT_ID, KEY, VALUE
            return columnCount == 3;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE1_EVENT_KEYPAIR table structure", e);
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
     * Validate the structure of the STAGE2_LIBRARY table.
     */
    private boolean validateStage2LibraryTableStructure(Connection connection) throws SQLException {
        try {
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE2_LIBRARY_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);

            int columnCount = 0;
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE2_LIBRARY: " + columnName);
            }

            // Should have 11 columns: LIBRARY_ID, EVENT_ID, AID, CID, JARPATH, LIBRARY_NAME,
            // SHA256_HASH, METHOD, FIRST_SEEN, LAST_SEEN, REMOVED_ON
            return columnCount == 11;

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE2_LIBRARY table structure", e);
            return false;
        }
    }

    /**
     * Validate the structure of the STAGE2_LIBRARY_CVE table.
     */
    private boolean validateStage2LibraryCveTableStructure(Connection connection) throws SQLException {
        try {
            // Query table info to check columns
            String sql = "PRAGMA table_info(" + SchemaConstants.STAGE2_LIBRARY_CVE_TABLE + ")";
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);

            int columnCount = 0;
            while (resultSet.next()) {
                columnCount++;
                String columnName = resultSet.getString("name");
                logger.fine("Found column in STAGE2_LIBRARY_CVE: " + columnName);
            }

            // Should have 4 columns: LIBRARY_ID, CVE_ID, CVE_SCORE, FIRST_DETECTED
            return columnCount == 4;

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to validate STAGE2_LIBRARY_CVE table structure", e);
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