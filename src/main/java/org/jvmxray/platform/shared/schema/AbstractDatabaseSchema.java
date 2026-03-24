package org.jvmxray.platform.shared.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Abstract base class for database schema operations.
 * Provides common functionality for creating, dropping, and validating
 * JVMXRay database schemas across different database systems.
 * 
 * @author Milton Smith
 */
public abstract class AbstractDatabaseSchema {
    
    protected static final Logger logger = Logger.getLogger(AbstractDatabaseSchema.class.getName());
    
    protected final String connectionUrl;
    protected final String username;
    protected final String password;
    protected final String databaseName;
    
    /**
     * Constructor for database schema manager.
     * 
     * @param connectionUrl JDBC connection URL
     * @param username Database username (null for embedded databases like SQLite)
     * @param password Database password (null for embedded databases like SQLite)
     * @param databaseName Database/keyspace name
     */
    public AbstractDatabaseSchema(String connectionUrl, String username, String password, String databaseName) {
        this.connectionUrl = connectionUrl;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }
    
    /**
     * Create the complete database schema including all tables.
     * This method should be called to set up a fresh database.
     * 
     * @throws SQLException if schema creation fails
     */
    public void createSchema() throws SQLException {
        logger.info("Creating JVMXRay database schema for " + getDatabaseType());
        
        try {
            createDatabase();
            createTables();
            createIndexes();
            logger.info("Successfully created JVMXRay database schema");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create database schema: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Drop the complete database schema including all tables.
     * This method should be used carefully as it will destroy all data.
     * 
     * @throws SQLException if schema deletion fails
     */
    public void dropSchema() throws SQLException {
        logger.info("Dropping JVMXRay database schema for " + getDatabaseType());
        
        try {
            dropTables();
            logger.info("Successfully dropped JVMXRay database schema");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to drop database schema: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Validate that the database schema exists and is correct.
     * 
     * @return true if schema is valid, false otherwise
     * @throws SQLException if validation fails
     */
    public boolean validateSchema() throws SQLException {
        logger.info("Validating JVMXRay database schema for " + getDatabaseType());
        
        try {
            boolean isValid = checkTablesExist() && validateTableStructure();
            if (isValid) {
                logger.info("Database schema validation passed");
            } else {
                logger.warning("Database schema validation failed");
            }
            return isValid;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to validate database schema: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get a database connection.
     * Subclasses should implement this to return appropriate connection types.
     * 
     * @return Database connection
     * @throws SQLException if connection fails
     */
    protected abstract Connection getConnection() throws SQLException;
    
    /**
     * Create the database/keyspace if it doesn't exist.
     * For some databases like SQLite, this might be a no-op.
     * 
     * @throws SQLException if database creation fails
     */
    protected abstract void createDatabase() throws SQLException;
    
    /**
     * Create all required tables for the JVMXRay schema.
     * 
     * @throws SQLException if table creation fails
     */
    protected abstract void createTables() throws SQLException;
    
    /**
     * Create any necessary indexes for optimal performance.
     * 
     * @throws SQLException if index creation fails
     */
    protected abstract void createIndexes() throws SQLException;
    
    /**
     * Drop all tables in the schema.
     * 
     * @throws SQLException if table dropping fails
     */
    protected abstract void dropTables() throws SQLException;
    
    /**
     * Check that all required tables exist.
     * 
     * @return true if all tables exist, false otherwise
     * @throws SQLException if check fails
     */
    protected abstract boolean checkTablesExist() throws SQLException;
    
    /**
     * Validate that table structures match expected schema.
     * 
     * @return true if table structures are correct, false otherwise
     * @throws SQLException if validation fails
     */
    protected abstract boolean validateTableStructure() throws SQLException;
    
    /**
     * Get the database type name for logging purposes.
     * 
     * @return Database type name (e.g., "MySQL", "SQLite", "Cassandra")
     */
    public abstract String getDatabaseType();
    
    /**
     * Execute a SQL statement and log the operation.
     * 
     * @param connection Database connection
     * @param sql SQL statement to execute
     * @throws SQLException if execution fails
     */
    protected void executeSQL(Connection connection, String sql) throws SQLException {
        logger.info("Executing SQL: " + sql);
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
            logger.fine("Successfully executed SQL statement");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute SQL: " + sql, e);
            throw e;
        }
    }
    
    /**
     * Execute a SQL statement that returns a count result.
     * 
     * @param connection Database connection
     * @param sql SQL statement to execute
     * @return Count result from the query
     * @throws SQLException if execution fails
     */
    protected int executeCountQuery(Connection connection, String sql) throws SQLException {
        logger.fine("Executing count query: " + sql);
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute count query: " + sql, e);
            throw e;
        }
    }
    
    /**
     * Execute a SQL statement that returns a boolean result.
     * 
     * @param connection Database connection
     * @param sql SQL statement to execute
     * @return true if query returns any results, false otherwise
     * @throws SQLException if execution fails
     */
    protected boolean executeExistsQuery(Connection connection, String sql) throws SQLException {
        logger.fine("Executing exists query: " + sql);
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute exists query: " + sql, e);
            throw e;
        }
    }
    
    /**
     * Close database connection safely.
     * 
     * @param connection Connection to close
     */
    protected void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.fine("Database connection closed");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to close database connection", e);
            }
        }
    }
}