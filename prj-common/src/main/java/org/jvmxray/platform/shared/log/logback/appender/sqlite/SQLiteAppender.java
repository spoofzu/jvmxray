package org.jvmxray.platform.shared.log.logback.appender.sqlite;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jvmxray.platform.shared.schema.EventParser;
import org.jvmxray.platform.shared.schema.EventParser.ParsedEvent;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.jvmxray.platform.shared.schema.SQLiteSchema;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLiteAppender logs JVMXRay events to a SQLite database for offline processing/reporting.
 * Specifically designed for use with JVMXRay and logback logging framework.
 * 
 * This appender:
 * - Parses logback events using the JVMXRay format: CONFIG_FILE | timestamp | thread | priority | namespace | keypairs
 * - Inserts events into STAGE0_EVENT and STAGE0_EVENT_KEYPAIR tables
 * - Uses connection pooling via HikariCP for efficient database access
 * - Implements asynchronous batch writes to minimize performance impact
 * - Auto-creates database schema if not present
 * 
 * @author Milton Smith
 */
public class SQLiteAppender extends AppenderBase<ILoggingEvent> {
    
    private static final Logger logger = Logger.getLogger(SQLiteAppender.class.getName());
    
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
    
    // SQL statements
    private static final String INSERT_EVENT_SQL = 
        "INSERT OR IGNORE INTO " + SchemaConstants.STAGE0_EVENT_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_CONFIG_FILE + ", " +
        SchemaConstants.COL_TIMESTAMP + ", " +
        SchemaConstants.COL_THREAD_ID + ", " +
        SchemaConstants.COL_PRIORITY + ", " +
        SchemaConstants.COL_NAMESPACE + ", " +
        SchemaConstants.COL_AID + ", " +
        SchemaConstants.COL_CID + ", " +
        SchemaConstants.COL_IS_STABLE + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_KEYPAIR_SQL = 
        "INSERT OR IGNORE INTO " + SchemaConstants.STAGE0_EVENT_KEYPAIR_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_KEY + ", " +
        SchemaConstants.COL_VALUE + ") VALUES (?, ?, ?)";
    
    @Override
    public void start() {
        try {
            // Set default database path if not configured
            if (databasePath == null || databasePath.isEmpty()) {
                String jvmxrayHome = System.getProperty("jvmxray.home");
                if (jvmxrayHome == null) {
                    jvmxrayHome = System.getProperty("user.dir") + File.separator + ".jvmxray";
                }
                databasePath = jvmxrayHome + File.separator + "agent" + File.separator + 
                              "data" + File.separator + "jvmxray-events.db";
            }
            
            // Ensure database directory exists
            File dbFile = new File(databasePath);
            File dbDir = dbFile.getParentFile();
            if (!dbDir.exists() && !dbDir.mkdirs()) {
                addError("Failed to create database directory: " + dbDir.getAbsolutePath());
                return;
            }
            
            // Initialize database and schema
            initializeDatabase();
            
            // Initialize event queue
            eventQueue = new ArrayBlockingQueue<>(queueSize);
            
            // Start writer thread
            writerThread = new Thread(this::writerLoop, "SQLiteAppender-Writer");
            writerThread.setDaemon(true);
            writerThread.start();
            
            super.start();
            addInfo("SQLiteAppender started with database: " + databasePath);
            
        } catch (Exception e) {
            addError("Failed to start SQLiteAppender", e);
        }
    }
    
    @Override
    public void stop() {
        shutdown.set(true);
        
        // Flush remaining events
        if (writerThread != null) {
            try {
                writerThread.interrupt();
                writerThread.join(10000); // Wait up to 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close data source
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        
        super.stop();
        addInfo("SQLiteAppender stopped");
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        
        // Prevent recursive logging by filtering out events from this appender and related infrastructure
        String loggerName = event.getLoggerName();
        if (loggerName != null && (
            loggerName.equals(SQLiteAppender.class.getName()) ||
            loggerName.startsWith("org.jvmxray.platform.shared.schema") ||
            loggerName.startsWith("org.jvmxray.platform.shared.log") ||
            loggerName.equals("org.jvmxray.platform.shared.bin.SchemaManager") ||
            loggerName.startsWith("com.zaxxer.hikari") ||
            loggerName.startsWith("org.sqlite"))) {
            return;
        }
        
        // Also filter by message content to avoid processing non-JVMXRay messages
        String message = event.getMessage();
        if (message != null && (
            message.contains("VALUES (") ||
            message.contains("INSERT") ||
            message.contains("CREATE TABLE") ||
            message.contains("Sharing is only supported") ||
            message.startsWith("CONFIG_FILE") ||
            message.startsWith("TIMESTAMP"))) {
            logger.fine("Filtering non-JVMXRay message: " + message);
            return;
        }
        
        try {
            // Format the log message as expected by EventParser
            String formattedMessage = formatLogEvent(event);
            
            // Additional safety check on formatted message
            if (formattedMessage.contains("VALUES (") || 
                formattedMessage.contains("INSERT") ||
                formattedMessage.contains("CREATE TABLE")) {
                logger.fine("Skipping formatted message with SQL content: " + formattedMessage);
                return;
            }
            
            // Parse the event
            ParsedEvent parsedEvent = EventParser.parseEvent(formattedMessage);
            
            // Add to queue for async processing
            if (!eventQueue.offer(parsedEvent)) {
                // Use java.util.logging instead of addWarn to prevent recursion
                logger.warning("Event queue is full, dropping event");
            }
            
        } catch (IllegalArgumentException e) {
            // Silently ignore events that don't match expected format
            // These are likely infrastructure messages that shouldn't be processed as JVMXRay events
            logger.fine("Skipping non-JVMXRay event: " + e.getMessage());
        } catch (Exception e) {
            // Log other exceptions using java.util.logging to prevent recursion
            logger.log(Level.WARNING, "Unexpected error processing event", e);
        }
    }
    
    /**
     * Format ILoggingEvent into the expected format for EventParser.
     * Format: CONFIG_FILE | timestamp | thread | priority | namespace | keypairs
     */
    private String formatLogEvent(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        
        // CONFIG_FILE
        sb.append("C:AP");
        
        // Timestamp - format to match expected format: "2025.09.03 at 19:08:19 CDT"
        sb.append(" | ");
        java.time.Instant instant = java.time.Instant.ofEpochMilli(event.getTimeStamp());
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd 'at' HH:mm:ss z")
                                                      .withZone(zoneId);
        sb.append(formatter.format(instant));
        
        // Thread
        sb.append(" | ").append(event.getThreadName());
        
        // Priority (log level) - format with 5 characters like %5level
        sb.append(" | ").append(String.format("%5s", event.getLevel().toString()));
        
        // Namespace (logger name)
        sb.append(" | ").append(event.getLoggerName());
        
        // Key-value pairs section (combines MDC properties and message)
        sb.append(" | ");
        StringBuilder keyValuePairs = new StringBuilder();
        boolean hasKeyValuePairs = false;
        
        // Add MDC properties (these are the key-value pairs from sensors)
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                if (!first) keyValuePairs.append(", ");
                keyValuePairs.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
                hasKeyValuePairs = true;
            }
        }
        
        // Add message content as key-value pairs if it contains structured data
        String message = event.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            // Check if message contains key-value pairs (format: key=value, key2=value2)
            if (message.contains("=")) {
                if (hasKeyValuePairs) keyValuePairs.append(", ");
                keyValuePairs.append(message);
            } else {
                // If message doesn't contain key-value pairs, add it as a "message" key
                if (hasKeyValuePairs) keyValuePairs.append(", ");
                keyValuePairs.append("message=").append(message);
            }
        }
        
        sb.append(keyValuePairs.toString());
        
        return sb.toString();
    }
    
    /**
     * Initialize database connection pool and create schema if needed.
     */
    private void initializeDatabase() throws SQLException {
        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setMaximumPoolSize(1); // SQLite works best with single connection
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SQLiteAppender-Pool");
        
        // SQLite-specific settings for better concurrent access
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "30000");
        config.addDataSourceProperty("cache_size", "-2000"); // 2MB cache
        
        dataSource = new HikariDataSource(config);
        
        // Create schema if it doesn't exist
        try (Connection conn = dataSource.getConnection()) {
            SQLiteSchema schema = new SQLiteSchema("jdbc:sqlite:" + databasePath, null);
            if (!schema.validateSchema()) {
                logger.info("Creating SQLite schema at: " + databasePath);
                schema.createSchema();
            }
        }
    }
    
    /**
     * Background thread that writes events to the database in batches.
     */
    private void writerLoop() {
        ParsedEvent[] batch = new ParsedEvent[batchSize];
        int batchIndex = 0;
        long lastFlush = System.currentTimeMillis();
        
        while (!shutdown.get() || !eventQueue.isEmpty()) {
            try {
                // Wait for event with timeout
                ParsedEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                
                if (event != null) {
                    batch[batchIndex++] = event;
                }
                
                // Check if we should flush
                long now = System.currentTimeMillis();
                boolean shouldFlush = batchIndex >= batchSize || 
                                     (batchIndex > 0 && (now - lastFlush) >= flushIntervalMs) ||
                                     (shutdown.get() && batchIndex > 0);
                
                if (shouldFlush) {
                    writeBatch(batch, batchIndex);
                    batchIndex = 0;
                    lastFlush = now;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Flush any remaining events
                if (batchIndex > 0) {
                    writeBatch(batch, batchIndex);
                }
                break;
            } catch (Exception e) {
                addError("Error in writer loop", e);
            }
        }
    }
    
    /**
     * Write a batch of events to the database.
     */
    private void writeBatch(ParsedEvent[] batch, int count) {
        if (count == 0) return;
        
        Connection conn = null;
        PreparedStatement eventStmt = null;
        PreparedStatement keypairStmt = null;
        
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            
            eventStmt = conn.prepareStatement(INSERT_EVENT_SQL);
            keypairStmt = conn.prepareStatement(INSERT_KEYPAIR_SQL);
            
            for (int i = 0; i < count; i++) {
                ParsedEvent event = batch[i];
                if (event == null) continue;
                
                // Insert main event
                eventStmt.setString(1, event.getEventId());
                eventStmt.setString(2, event.getConfigFile());
                eventStmt.setLong(3, event.getTimestamp());
                eventStmt.setString(4, event.getThreadId());
                eventStmt.setString(5, event.getPriority());
                eventStmt.setString(6, event.getNamespace());
                eventStmt.setString(7, event.getAid());
                eventStmt.setString(8, event.getCid());
                eventStmt.setBoolean(9, true); // IS_STABLE
                eventStmt.addBatch();
                
                // Insert keypairs
                if (event.getKeyPairs() != null) {
                    for (Map.Entry<String, String> entry : event.getKeyPairs().entrySet()) {
                        keypairStmt.setString(1, event.getEventId());
                        keypairStmt.setString(2, entry.getKey());
                        keypairStmt.setString(3, entry.getValue());
                        keypairStmt.addBatch();
                    }
                }
                
                // Clear batch array entry
                batch[i] = null;
            }
            
            // Execute batches
            eventStmt.executeBatch();
            keypairStmt.executeBatch();
            conn.commit();
            
            logger.log(Level.FINE, "Successfully wrote " + count + " events to database");
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to write batch to database", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    logger.log(Level.WARNING, "Failed to rollback transaction", re);
                }
            }
        } finally {
            closeQuietly(keypairStmt);
            closeQuietly(eventStmt);
            closeQuietly(conn);
        }
    }
    
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // Getters and setters for configuration properties
    
    public String getDatabasePath() {
        return databasePath;
    }
    
    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }
    
    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
    
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}