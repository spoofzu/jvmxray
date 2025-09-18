package org.jvmxray.agent.log.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jvmxray.platform.shared.schema.EventParser;
import org.jvmxray.platform.shared.schema.EventParser.ParsedEvent;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.jvmxray.platform.shared.util.GUID;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shaded SQLiteAppender for JVMXRay agent's isolated logback context.
 * 
 * This appender is specifically designed to work with the agent's shaded logback classes
 * (agent.shadow.logback.*) to maintain complete isolation from the application's logging.
 * 
 * Features:
 * - Extends agent.shadow.logback.core.AppenderBase (shaded)
 * - Parses JVMXRay event format: CONFIG_FILE | timestamp | thread | priority | namespace | keypairs
 * - Inserts events into STAGE0_EVENT table
 * - Uses connection pooling via HikariCP
 * - Asynchronous batch writes for performance
 * - Auto-creates database schema if needed
 * 
 * @author JVMXRay Agent Team
 */
public class ShadedSQLiteAppender extends AppenderBase<ILoggingEvent> {
    
    private static final Logger logger = Logger.getLogger(ShadedSQLiteAppender.class.getName());
    
    // Configuration properties
    private String databasePath;
    private int batchSize = 100;
    private long flushIntervalMs = 5000;
    private int queueSize = 10000;
    
    // Database components
    private HikariDataSource dataSource;
    private BlockingQueue<ParsedEvent> eventQueue;
    private Thread writerThread;
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    
    // Prepared statement for inserting events
    private static final String INSERT_EVENT_SQL = 
        "INSERT OR REPLACE INTO STAGE0_EVENT (EVENT_ID, CONFIG_FILE, TIMESTAMP, THREAD_ID, PRIORITY, NAMESPACE, AID, CID, KEYPAIRS) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    @Override
    public void start() {
        try {
            if (databasePath == null || databasePath.trim().isEmpty()) {
                addError("DatabasePath is required for ShadedSQLiteAppender");
                return;
            }
            
            // Ensure database directory exists
            File dbFile = new File(databasePath);
            File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            // Initialize connection pool
            initializeDataSource();
            
            // Initialize queue and worker thread
            eventQueue = new ArrayBlockingQueue<>(queueSize);
            writerThread = new Thread(this::writerLoop, "ShadedSQLiteAppender-Writer");
            writerThread.setDaemon(true);
            writerThread.start();
            
            super.start();
            logger.info("ShadedSQLiteAppender started successfully with database: " + databasePath);
            
        } catch (Exception e) {
            addError("Failed to start ShadedSQLiteAppender", e);
        }
    }
    
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        
        shutdown.set(true);
        
        try {
            // Wait for writer thread to finish
            if (writerThread != null) {
                writerThread.interrupt();
                writerThread.join(5000);
            }
            
            // Close data source
            if (dataSource != null) {
                dataSource.close();
            }
            
        } catch (Exception e) {
            addError("Error during ShadedSQLiteAppender shutdown", e);
        }
        
        super.stop();
        logger.info("ShadedSQLiteAppender stopped");
    }
    
    @Override
protected void append(ILoggingEvent eventObject) {
    if (!isStarted() || shutdown.get()) {
        return;
    }
    
    try {
        // Create ParsedEvent directly from logback event data
        ParsedEvent parsedEvent = new ParsedEvent();
        
        // Generate unique event ID
        parsedEvent.setEventId(GUID.generate());
        
        // Extract basic fields from logback event
        parsedEvent.setConfigFile("C:AP"); // Agent config file identifier
        parsedEvent.setTimestamp(eventObject.getTimeStamp());
        parsedEvent.setThreadId(eventObject.getThreadName());
        parsedEvent.setPriority(eventObject.getLevel().toString());
        parsedEvent.setNamespace(eventObject.getLoggerName());
        
        // Parse key-value pairs from MDC and message
        Map<String, String> keyPairs = new HashMap<>();
        
        // Add MDC properties if available
        if (eventObject.getMDCPropertyMap() != null) {
            keyPairs.putAll(eventObject.getMDCPropertyMap());
        }
        
        // Parse structured message format: "caller=..., message=..., AID=..., CID=..."
        String message = eventObject.getFormattedMessage();
        if (message != null && message.contains("=")) {
            parseMessageKeyPairs(message, keyPairs);
        }
        
        // Extract AID and CID for dedicated columns
        parsedEvent.setAid(keyPairs.get("AID"));
        parsedEvent.setCid(keyPairs.get("CID"));
        parsedEvent.setKeyPairs(keyPairs);
        
        // Try to add to queue (non-blocking)
        if (!eventQueue.offer(parsedEvent)) {
            addWarn("Event queue full, dropping event");
        }
        
    } catch (Exception e) {
        addError("Error processing event", e);
    }
}

    /**
     * Parse structured message format from agent sensors: "caller=..., message=..., AID=..., CID=..."
     * @param message The formatted message from the logging event
     * @param keyPairs Map to populate with extracted key-value pairs
     */
    private void parseMessageKeyPairs(String message, Map<String, String> keyPairs) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        try {
            // Split on ", " to get individual key-value pairs
            String[] pairs = message.split(", ");
            
            for (String pair : pairs) {
                if (pair != null && !pair.trim().isEmpty()) {
                    int equalsIndex = pair.indexOf('=');
                    if (equalsIndex > 0 && equalsIndex < pair.length() - 1) {
                        String key = pair.substring(0, equalsIndex).trim();
                        String value = pair.substring(equalsIndex + 1).trim();
                        keyPairs.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse message key-value pairs: " + message, e);
        }
    }
    
    private void initializeDataSource() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        
        // SQLite-specific settings
        config.addDataSourceProperty("enforceForeignKeys", "true");
        config.addDataSourceProperty("journalMode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        
        dataSource = new HikariDataSource(config);
        
        // Test connection and ensure schema exists
        try (Connection conn = dataSource.getConnection()) {
            ensureSchema(conn);
        }
    }
    
    private void ensureSchema(Connection conn) throws SQLException {
        // Create STAGE0_EVENT table if not exists
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS STAGE0_EVENT (" +
            "EVENT_ID TEXT PRIMARY KEY, " +
            "CONFIG_FILE TEXT, " +
            "TIMESTAMP INTEGER, " +
            "THREAD_ID TEXT, " +
            "PRIORITY TEXT, " +
            "NAMESPACE TEXT, " +
            "AID TEXT, " +
            "CID TEXT, " +
            "KEYPAIRS TEXT" +
            ")";
        
        try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.execute();
        }
        
        // Create indexes for performance
        String[] indexQueries = {
            "CREATE INDEX IF NOT EXISTS idx_stage0_event_timestamp ON STAGE0_EVENT(TIMESTAMP)",
            "CREATE INDEX IF NOT EXISTS idx_stage0_event_namespace ON STAGE0_EVENT(NAMESPACE)",
            "CREATE INDEX IF NOT EXISTS idx_stage0_event_aid ON STAGE0_EVENT(AID)",
            "CREATE INDEX IF NOT EXISTS idx_stage0_event_cid ON STAGE0_EVENT(CID)"
        };
        
        for (String indexSQL : indexQueries) {
            try (PreparedStatement stmt = conn.prepareStatement(indexSQL)) {
                stmt.execute();
            }
        }
    }
    
    private void writerLoop() {
        while (!shutdown.get() || !eventQueue.isEmpty()) {
            try {
                // Process events in batches
                processBatch();
                
                // Sleep between batches
                if (!shutdown.get()) {
                    Thread.sleep(Math.min(flushIntervalMs, 1000));
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in writer loop", e);
                try {
                    Thread.sleep(5000); // Back off on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void processBatch() throws SQLException {
        if (eventQueue.isEmpty()) {
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
                int count = 0;
                
                ParsedEvent event;
                while (count < batchSize && (event = eventQueue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                    stmt.setString(1, event.getEventId());
                    stmt.setString(2, event.getConfigFile());
                    stmt.setLong(3, event.getTimestamp());
                    stmt.setString(4, event.getThreadId());
                    stmt.setString(5, event.getPriority());
                    stmt.setString(6, event.getNamespace());
                    stmt.setString(7, event.getAid());
                    stmt.setString(8, event.getCid());
                    
                    // Serialize keypairs as JSON string
                    String keypairsJson = serializeKeypairs(event.getKeyPairs());
                    stmt.setString(9, keypairsJson);
                    
                    stmt.addBatch();
                    count++;
                }
                
                if (count > 0) {
                    stmt.executeBatch();
                    conn.commit();
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private String serializeKeypairs(Map<String, String> keypairs) {
        if (keypairs == null || keypairs.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : keypairs.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            json.append("\"").append(escapeJson(entry.getKey())).append("\"");
            json.append(": ");
            json.append("\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    // Configuration setters
    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }
    
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
    
    // Configuration getters
    public String getDatabasePath() {
        return databasePath;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
}