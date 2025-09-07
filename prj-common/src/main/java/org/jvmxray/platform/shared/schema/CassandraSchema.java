package org.jvmxray.platform.shared.schema;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.auth.AuthProvider;
import com.datastax.oss.driver.api.core.auth.ProgrammaticPlainTextAuthProvider;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Cassandra-specific implementation of database schema operations.
 * Handles creation, validation, and management of JVMXRay schema
 * in Cassandra NoSQL database.
 * 
 * @author Milton Smith
 */
public class CassandraSchema extends AbstractDatabaseSchema {
    
    private final String host;
    private final int port;
    private final String datacenter;
    private final int replicationFactor;
    private CqlSession session;
    
    /**
     * Constructor for Cassandra schema manager.
     * 
     * @param host Cassandra host
     * @param port Cassandra port (typically 9042)
     * @param username Database username
     * @param password Database password
     * @param keyspaceName Keyspace name
     * @param datacenter Datacenter name for load balancing
     * @param replicationFactor Replication factor for keyspace
     */
    public CassandraSchema(String host, int port, String username, String password, 
                          String keyspaceName, String datacenter, int replicationFactor) {
        super(String.format("cassandra://%s:%d/%s", host, port, keyspaceName), 
              username, password, keyspaceName);
        this.host = host;
        this.port = port;
        this.datacenter = datacenter;
        this.replicationFactor = replicationFactor;
        
        logger.info("Initialized Cassandra schema manager for keyspace: " + keyspaceName + 
                   " at " + host + ":" + port + " in datacenter: " + datacenter);
    }
    
    @Override
    protected Connection getConnection() throws SQLException {
        // Cassandra doesn't use JDBC, this method is not used
        throw new UnsupportedOperationException("Cassandra uses CqlSession, not JDBC Connection");
    }
    
    /**
     * Get or create CqlSession for Cassandra operations.
     * 
     * @return CqlSession instance
     * @throws SQLException if connection fails
     */
    protected CqlSession getCqlSession() throws SQLException {
        if (session == null || session.isClosed()) {
            try {
                AuthProvider authProvider = new ProgrammaticPlainTextAuthProvider(username, password);
                CqlSessionBuilder builder = CqlSession.builder();
                builder.addContactPoint(new InetSocketAddress(host, port));
                builder.withLocalDatacenter(datacenter);
                builder.withAuthProvider(authProvider);
                
                session = builder.build();
                logger.fine("Successfully connected to Cassandra cluster");
                
            } catch (Exception e) {
                throw new SQLException("Failed to connect to Cassandra: " + e.getMessage(), e);
            }
        }
        return session;
    }
    
    @Override
    protected void createDatabase() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            // Check if keyspace exists
            Optional<KeyspaceMetadata> keyspaceMetadata = 
                cqlSession.getMetadata().getKeyspace(databaseName);
            
            if (keyspaceMetadata.isEmpty()) {
                // Create keyspace with SimpleStrategy
                String createKeyspaceQuery = String.format(
                    "CREATE KEYSPACE IF NOT EXISTS %s WITH " +
                    "replication = {'class': 'SimpleStrategy', 'replication_factor': %d}",
                    databaseName, replicationFactor);
                
                cqlSession.execute(createKeyspaceQuery);
                logger.info("Created Cassandra keyspace: " + databaseName);
            } else {
                logger.info("Cassandra keyspace already exists: " + databaseName);
            }
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    @Override
    protected void createTables() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            // Create STAGE0_EVENT table
            String createEventTableQuery = String.format(
                SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_CASSANDRA, databaseName);
            cqlSession.execute(createEventTableQuery);
            logger.info("Created STAGE0_EVENT table in Cassandra");
            
            // Create STAGE0_EVENT_KEYPAIR table
            String createKeypairTableQuery = String.format(
                SchemaConstants.SQLTemplates.CREATE_STAGE0_EVENT_KEYPAIR_CASSANDRA, databaseName);
            cqlSession.execute(createKeypairTableQuery);
            logger.info("Created STAGE0_EVENT_KEYPAIR table in Cassandra");
            
            logger.info("Successfully created all Cassandra tables");
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    @Override
    protected void createIndexes() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            // Create secondary index on timestamp for range queries
            String createTimestampIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_timestamp ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_TIMESTAMP);
            cqlSession.execute(createTimestampIndex);
            
            // Create secondary index on namespace for filtering
            String createNamespaceIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_namespace ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_NAMESPACE);
            cqlSession.execute(createNamespaceIndex);
            
            // Create secondary index on AID for application filtering
            String createAidIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_aid ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_AID);
            cqlSession.execute(createAidIndex);
            
            // Create secondary index on CID for category filtering
            String createCidIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_cid ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_CID);
            cqlSession.execute(createCidIndex);
            
            // Create secondary index on IS_STABLE for consistency queries
            String createStableIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_stable ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_IS_STABLE);
            cqlSession.execute(createStableIndex);
            
            // Create secondary index on CONFIG_FILE for configuration filtering
            String createConfigIndex = String.format(
                "CREATE INDEX IF NOT EXISTS idx_stage0_event_config ON %s.%s (%s)",
                databaseName, SchemaConstants.STAGE0_EVENT_TABLE, SchemaConstants.COL_CONFIG_FILE);
            cqlSession.execute(createConfigIndex);
            
            logger.info("Successfully created all Cassandra indexes");
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    @Override
    protected void dropTables() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            // Drop tables (order doesn't matter much in Cassandra since no foreign keys)
            String dropKeypairTable = String.format(
                SchemaConstants.SQLTemplates.DROP_STAGE0_EVENT_KEYPAIR_CASSANDRA, databaseName);
            cqlSession.execute(dropKeypairTable);
            logger.info("Dropped STAGE0_EVENT_KEYPAIR table from Cassandra");
            
            String dropEventTable = String.format(
                SchemaConstants.SQLTemplates.DROP_STAGE0_EVENT_CASSANDRA, databaseName);
            cqlSession.execute(dropEventTable);
            logger.info("Dropped STAGE0_EVENT table from Cassandra");
            
            logger.info("Successfully dropped all Cassandra tables");
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    @Override
    protected boolean checkTablesExist() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            Optional<KeyspaceMetadata> keyspaceMetadata = 
                cqlSession.getMetadata().getKeyspace(databaseName);
            
            if (keyspaceMetadata.isEmpty()) {
                logger.warning("Keyspace does not exist: " + databaseName);
                return false;
            }
            
            // Check if STAGE0_EVENT table exists
            Optional<TableMetadata> eventTableMetadata = 
                keyspaceMetadata.get().getTable(SchemaConstants.STAGE0_EVENT_TABLE);
            boolean eventTableExists = eventTableMetadata.isPresent();
            
            // Check if STAGE0_EVENT_KEYPAIR table exists
            Optional<TableMetadata> keypairTableMetadata = 
                keyspaceMetadata.get().getTable(SchemaConstants.STAGE0_EVENT_KEYPAIR_TABLE);
            boolean keypairTableExists = keypairTableMetadata.isPresent();
            
            boolean allTablesExist = eventTableExists && keypairTableExists;
            
            if (allTablesExist) {
                logger.info("All required Cassandra tables exist");
            } else {
                logger.warning(String.format("Missing Cassandra tables - Event: %b, KeyPair: %b", 
                    eventTableExists, keypairTableExists));
            }
            
            return allTablesExist;
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    @Override
    protected boolean validateTableStructure() throws SQLException {
        CqlSession cqlSession = null;
        try {
            cqlSession = getCqlSession();
            
            Optional<KeyspaceMetadata> keyspaceMetadata = 
                cqlSession.getMetadata().getKeyspace(databaseName);
            
            if (keyspaceMetadata.isEmpty()) {
                logger.warning("Keyspace does not exist: " + databaseName);
                return false;
            }
            
            // Check STAGE0_EVENT table structure
            boolean eventTableValid = validateEventTableStructure(keyspaceMetadata.get());
            
            // Check STAGE0_EVENT_KEYPAIR table structure  
            boolean keypairTableValid = validateKeypairTableStructure(keyspaceMetadata.get());
            
            boolean structureValid = eventTableValid && keypairTableValid;
            
            if (structureValid) {
                logger.info("Cassandra table structures are valid");
            } else {
                logger.warning("Cassandra table structure validation failed");
            }
            
            return structureValid;
            
        } finally {
            // Don't close session here, we'll reuse it
        }
    }
    
    /**
     * Validate the structure of the STAGE0_EVENT table.
     */
    private boolean validateEventTableStructure(KeyspaceMetadata keyspaceMetadata) {
        try {
            Optional<TableMetadata> tableMetadata = 
                keyspaceMetadata.getTable(SchemaConstants.STAGE0_EVENT_TABLE);
            
            if (tableMetadata.isEmpty()) {
                return false;
            }
            
            TableMetadata table = tableMetadata.get();
            int columnCount = table.getColumns().size();
            
            // Should have 9 columns: EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, IS_STABLE
            return columnCount == 9;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to validate STAGE0_EVENT table structure", e);
            return false;
        }
    }
    
    /**
     * Validate the structure of the STAGE0_EVENT_KEYPAIR table.
     */
    private boolean validateKeypairTableStructure(KeyspaceMetadata keyspaceMetadata) {
        try {
            Optional<TableMetadata> tableMetadata = 
                keyspaceMetadata.getTable(SchemaConstants.STAGE0_EVENT_KEYPAIR_TABLE);
            
            if (tableMetadata.isEmpty()) {
                return false;
            }
            
            TableMetadata table = tableMetadata.get();
            int columnCount = table.getColumns().size();
            
            // Should have 3 columns: EVENT_ID, KEY, VALUE
            return columnCount == 3;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to validate STAGE0_EVENT_KEYPAIR table structure", e);
            return false;
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "Cassandra";
    }
    
    /**
     * Close the Cassandra session.
     */
    public void closeSession() {
        if (session != null && !session.isClosed()) {
            session.close();
            logger.info("Closed Cassandra session");
        }
    }
    
    /**
     * Test the Cassandra connection.
     * 
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try {
            CqlSession testSession = getCqlSession();
            return testSession != null && !testSession.isClosed();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Cassandra connection test failed", e);
            return false;
        }
    }
    
    /**
     * Execute a CQL statement and log the operation.
     * 
     * @param cqlStatement CQL statement to execute
     * @throws SQLException if execution fails
     */
    protected void executeCQL(String cqlStatement) throws SQLException {
        logger.info("Executing CQL: " + cqlStatement);
        try {
            CqlSession cqlSession = getCqlSession();
            SimpleStatement statement = SimpleStatement.newInstance(cqlStatement);
            cqlSession.execute(statement);
            logger.fine("Successfully executed CQL statement");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute CQL: " + cqlStatement, e);
            throw new SQLException("Failed to execute CQL statement", e);
        }
    }
    
    /**
     * Get the current keyspace name.
     * 
     * @return Keyspace name
     */
    public String getKeyspaceName() {
        return databaseName;
    }
    
    /**
     * Get the datacenter name.
     * 
     * @return Datacenter name
     */
    public String getDatacenter() {
        return datacenter;
    }
    
    /**
     * Get the replication factor.
     * 
     * @return Replication factor
     */
    public int getReplicationFactor() {
        return replicationFactor;
    }
}