package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.init.CommonInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.schema.EventParser;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CpStage0ToStage1 - Data migration tool for copying STAGE0_EVENT to STAGE1_EVENT tables.
 * 
 * This tool migrates data from STAGE0_EVENT to STAGE1_EVENT tables while maintaining data integrity
 * using the IS_STABLE flag. It reads the database connection from common.properties and supports
 * all database types (SQLite, MySQL, Cassandra) via JDBC connection strings.
 * 
 * <p>Migration Process:</p>
 * <ul>
 *   <li>Reads records from STAGE0_EVENT in batches</li>
 *   <li>For each record:
 *     <ul>
 *       <li>Inserts into STAGE1_EVENT with IS_STABLE=false</li>
 *       <li>Parses KEYPAIRS column and inserts into STAGE1_EVENT_KEYPAIR</li>
 *       <li>Updates IS_STABLE=true after successful keypair insertion</li>
 *     </ul>
 *   </li>
 *   <li>Uses transactions to ensure atomicity</li>
 * </ul>
 * 
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code -b, --batch-size <size>}: Number of records to process per batch (default: 100)</li>
 *   <li>{@code -v, --verbose}: Enable verbose output</li>
 *   <li>{@code --dry-run}: Show what would be migrated without making changes</li>
 *   <li>{@code -h, --help}: Display help information</li>
 * </ul>
 * 
 * <p><b>Database Configuration:</b></p>
 * The tool reads the JDBC connection string from {@code common.properties}:
 * <pre>{@code
 * jvmxray.common.database.jdbc.connection=jdbc:sqlite:/path/to/database.db
 * }</pre>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * # Basic migration
 * java org.jvmxray.platform.shared.bin.CpStage0ToStage1
 * 
 * # Larger batch size with verbose output
 * java org.jvmxray.platform.shared.bin.CpStage0ToStage1 -b 500 -v
 * 
 * # Dry run to test migration
 * java org.jvmxray.platform.shared.bin.CpStage0ToStage1 --dry-run
 * }</pre>
 * 
 * @author JVMXRay Development Team
 */
public class CpStage0ToStage1 {

    private static final Logger logger = LoggerFactory.getLogger(CpStage0ToStage1.class);
    
    // JSON parser for keypairs
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Command-line option constants
    private static final String OPT_BATCH_SIZE_SHORT = "b";
    private static final String OPT_BATCH_SIZE_LONG = "batch-size";
    private static final int DEFAULT_BATCH_SIZE = 100;

    private static final String OPT_VERBOSE_SHORT = "v";
    private static final String OPT_VERBOSE_LONG = "verbose";

    private static final String OPT_DRY_RUN_LONG = "dry-run";

    private static final String OPT_HELP_SHORT = "h";
    private static final String OPT_HELP_LONG = "help";

    // SQL queries
    private static final String SELECT_STAGE0_COUNT = 
        "SELECT COUNT(*) FROM " + SchemaConstants.STAGE0_EVENT_TABLE;
    
    private static final String SELECT_STAGE0_BATCH = 
        "SELECT " + SchemaConstants.COL_EVENT_ID + ", " + 
                    SchemaConstants.COL_CONFIG_FILE + ", " +
                    SchemaConstants.COL_TIMESTAMP + ", " +
                    SchemaConstants.COL_THREAD_ID + ", " +
                    SchemaConstants.COL_PRIORITY + ", " +
                    SchemaConstants.COL_NAMESPACE + ", " +
                    SchemaConstants.COL_AID + ", " +
                    SchemaConstants.COL_CID + ", " +
                    SchemaConstants.COL_KEYPAIRS +
        " FROM " + SchemaConstants.STAGE0_EVENT_TABLE + 
        " LIMIT ? OFFSET ?";

    private static final String INSERT_STAGE1_EVENT = 
        "INSERT INTO " + SchemaConstants.STAGE1_EVENT_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_CONFIG_FILE + ", " +
        SchemaConstants.COL_TIMESTAMP + ", " +
        SchemaConstants.COL_THREAD_ID + ", " +
        SchemaConstants.COL_PRIORITY + ", " +
        SchemaConstants.COL_NAMESPACE + ", " +
        SchemaConstants.COL_AID + ", " +
        SchemaConstants.COL_CID + ", " +
        SchemaConstants.COL_IS_STABLE +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_STAGE1_KEYPAIR = 
        "INSERT INTO " + SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_KEY + ", " +
        SchemaConstants.COL_VALUE +
        ") VALUES (?, ?, ?)";

    private static final String UPDATE_STAGE1_STABLE = 
        "UPDATE " + SchemaConstants.STAGE1_EVENT_TABLE + 
        " SET " + SchemaConstants.COL_IS_STABLE + " = ? " +
        " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

    // Configuration fields
    private int batchSize = DEFAULT_BATCH_SIZE;
    private boolean verbose = false;
    private boolean dryRun = false;
    private String jdbcConnection = null;

    /**
     * Main entry point for the STAGE0 to STAGE1 migration tool.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        new CpStage0ToStage1().run(args);
    }

    /**
     * Runs the data migration process.
     *
     * @param args Command-line arguments
     */
    private void run(String[] args) {
        try {
            // Parse command-line arguments
            parseCommandLineOptions(args);
            
            // Initialize environment
            initializeEnvironment();
            
            // Load database configuration
            loadDatabaseConfiguration();
            
            // Validate configuration
            validateConfiguration();
            
            // Execute migration
            executeDataMigration();
            
            System.out.println("✓ Data migration completed successfully");
            
        } catch (Exception e) {
            System.err.println("ERROR: Data migration failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to configure migration parameters.
     *
     * @param args Command-line arguments
     * @throws ParseException If arguments are invalid or cannot be parsed
     */
    private void parseCommandLineOptions(String[] args) throws ParseException {
        Options options = createCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            throw e;
        }

        // Display help if requested
        if (cmd.hasOption(OPT_HELP_SHORT)) {
            printUsage(options);
            System.exit(0);
        }

        // Parse batch size
        if (cmd.hasOption(OPT_BATCH_SIZE_SHORT)) {
            try {
                batchSize = Integer.parseInt(cmd.getOptionValue(OPT_BATCH_SIZE_SHORT));
                if (batchSize <= 0) {
                    System.err.println("Batch size must be positive. Using default: " + DEFAULT_BATCH_SIZE);
                    batchSize = DEFAULT_BATCH_SIZE;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid batch size format. Using default: " + DEFAULT_BATCH_SIZE);
                batchSize = DEFAULT_BATCH_SIZE;
            }
        }

        // Parse verbose flag
        verbose = cmd.hasOption(OPT_VERBOSE_SHORT);

        // Parse dry run flag
        dryRun = cmd.hasOption(OPT_DRY_RUN_LONG);

        // Log parsed configuration
        if (verbose) {
            System.out.println("Configuration:");
            System.out.println("  Batch Size: " + batchSize);
            System.out.println("  Verbose: " + verbose);
            System.out.println("  Dry Run: " + dryRun);
        }
    }

    /**
     * Creates command-line options definition.
     *
     * @return Options object with all defined command-line options
     */
    private Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(Option.builder(OPT_BATCH_SIZE_SHORT)
                .longOpt(OPT_BATCH_SIZE_LONG)
                .desc("Number of records to process per batch (default: " + DEFAULT_BATCH_SIZE + ")")
                .hasArg()
                .argName("SIZE")
                .build());

        options.addOption(Option.builder(OPT_VERBOSE_SHORT)
                .longOpt(OPT_VERBOSE_LONG)
                .desc("Enable verbose output")
                .build());

        options.addOption(Option.builder()
                .longOpt(OPT_DRY_RUN_LONG)
                .desc("Show what would be migrated without making changes")
                .build());

        options.addOption(Option.builder(OPT_HELP_SHORT)
                .longOpt(OPT_HELP_LONG)
                .desc("Display this help message")
                .build());

        return options;
    }

    /**
     * Prints usage information for the command-line options.
     *
     * @param options The command-line options
     */
    private void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java org.jvmxray.platform.shared.bin.CpStage0ToStage1 [options]",
                           "JVMXRay STAGE0 to STAGE1 Data Migration Tool",
                           options,
                           "Examples:\\n" +
                           "  java org.jvmxray.platform.shared.bin.CpStage0ToStage1\\n" +
                           "  java org.jvmxray.platform.shared.bin.CpStage0ToStage1 -b 500 -v\\n" +
                           "  java org.jvmxray.platform.shared.bin.CpStage0ToStage1 --dry-run");
    }

    /**
     * Initializes the environment and component directories.
     *
     * @throws Exception If environment initialization fails
     */
    private void initializeEnvironment() throws Exception {
        if (verbose) {
            System.out.println("Initializing JVMXRay environment...");
        }

        // Initialize CommonInitializer to set up directories and configuration
        CommonInitializer.getInstance();

        if (verbose) {
            System.out.println("✓ Environment initialized");
        }
    }

    /**
     * Loads database configuration from common.properties.
     *
     * @throws Exception If database configuration cannot be loaded
     */
    private void loadDatabaseConfiguration() throws Exception {
        if (verbose) {
            System.out.println("Loading database configuration...");
        }

        // Get the CommonInitializer instance to access properties
        CommonInitializer initializer = CommonInitializer.getInstance();
        PropertyBase propertyBase = initializer.getProperties();

        jdbcConnection = propertyBase.getProperty("jvmxray.common.database.jdbc.connection");
        if (jdbcConnection == null || jdbcConnection.trim().isEmpty()) {
            throw new IllegalStateException(
                "Property 'jvmxray.common.database.jdbc.connection' not found in common.properties. " +
                "Please configure the JDBC connection string.");
        }

        // Resolve relative SQLite database paths
        if (jdbcConnection.startsWith("jdbc:sqlite:") && !jdbcConnection.contains("/")) {
            String dbFile = jdbcConnection.substring("jdbc:sqlite:".length());
            String testHome = System.getProperty("jvmxray.test.home");
            if (testHome != null) {
                jdbcConnection = "jdbc:sqlite:" + testHome + "/common/data/" + dbFile;
            } else {
                jdbcConnection = "jdbc:sqlite:.jvmxray/common/data/" + dbFile;
            }
        }

        if (verbose) {
            System.out.println("✓ Database configuration loaded");
            System.out.println("  JDBC Connection: " + jdbcConnection);
        }
    }

    /**
     * Validates configuration and prerequisites.
     *
     * @throws Exception If validation fails
     */
    private void validateConfiguration() throws Exception {
        if (verbose) {
            System.out.println("Validating configuration...");
        }

        // Test database connection
        try (Connection conn = DriverManager.getConnection(jdbcConnection)) {
            if (verbose) {
                System.out.println("✓ Database connection successful");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to database: " + e.getMessage(), e);
        }

        if (verbose) {
            System.out.println("✓ Configuration validated");
        }
    }

    /**
     * Executes the data migration process.
     *
     * @throws Exception If migration fails
     */
    private void executeDataMigration() throws Exception {
        if (verbose || dryRun) {
            System.out.println("Starting STAGE0 to STAGE1 data migration...");
            System.out.println("Batch Size: " + batchSize);
            System.out.println("JDBC Connection: " + jdbcConnection);
            if (dryRun) {
                System.out.println("DRY RUN MODE - No changes will be made");
            }
            System.out.println();
        }

        long startTime = System.currentTimeMillis();
        int totalRecords = 0;
        int totalMigrated = 0;

        try (Connection conn = DriverManager.getConnection(jdbcConnection)) {
            if (!dryRun) {
                conn.setAutoCommit(false);
            }

            // Get total record count for progress reporting
            totalRecords = getTotalRecordCount(conn);
            if (verbose || dryRun) {
                System.out.println("Total STAGE0_EVENT records to migrate: " + totalRecords);
            }

            if (totalRecords == 0) {
                System.out.println("No records found in STAGE0_EVENT table. Nothing to migrate.");
                return;
            }

            if (dryRun) {
                System.out.println("DRY RUN: Would migrate " + totalRecords + " records from STAGE0_EVENT to STAGE1_EVENT");
                return;
            }

            // Process records in batches
            int offset = 0;
            while (offset < totalRecords) {
                List<Stage0Record> batch = readStage0Batch(conn, batchSize, offset);
                if (batch.isEmpty()) {
                    break;
                }

                // Process each record in the batch
                for (Stage0Record record : batch) {
                    try {
                        // Insert into STAGE1_EVENT with IS_STABLE=false
                        insertStage1Event(conn, record, false);

                        // Parse and insert keypairs
                        Map<String, String> keypairs = 
                            parseKeyPairs(record.getKeypairs(), verbose);

                        for (Map.Entry<String, String> entry : keypairs.entrySet()) {
                            insertStage1Keypair(conn, record.getEventId(), 
                                              entry.getKey(), entry.getValue());
                        }

                        // Update IS_STABLE to true after successful keypair insertion
                        updateStage1Stable(conn, record.getEventId(), true);

                        totalMigrated++;

                    } catch (SQLException e) {
                        logger.error("Failed to migrate record: " + record.getEventId(), e);
                        conn.rollback();
                        throw new RuntimeException("Migration failed for record: " + record.getEventId(), e);
                    }
                }

                // Commit the batch
                conn.commit();

                // Progress reporting
                if (verbose) {
                    int progressPercent = (int) ((double) totalMigrated / totalRecords * 100);
                    System.out.println("Progress: " + totalMigrated + "/" + totalRecords + 
                                     " (" + progressPercent + "%)");
                }

                offset += batchSize;
            }
        }

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        // Report results
        System.out.println();
        System.out.println("✓ Migration completed successfully");
        System.out.println("  Records migrated: " + totalMigrated);
        System.out.println("  Duration: " + String.format("%.2f", durationSeconds) + " seconds");
        if (totalMigrated > 0) {
            double recordsPerSecond = totalMigrated / durationSeconds;
            System.out.println("  Rate: " + String.format("%.2f", recordsPerSecond) + " records/second");
        }
    }

    /**
     * Gets the total number of records in STAGE0_EVENT table.
     *
     * @param conn Database connection
     * @return Total record count
     * @throws SQLException If query fails
     */
    private int getTotalRecordCount(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_STAGE0_COUNT);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Reads a batch of records from STAGE0_EVENT table.
     *
     * @param conn Database connection
     * @param limit Maximum number of records to read
     * @param offset Starting offset
     * @return List of Stage0Record objects
     * @throws SQLException If query fails
     */
    private List<Stage0Record> readStage0Batch(Connection conn, int limit, int offset) throws SQLException {
        List<Stage0Record> records = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SELECT_STAGE0_BATCH)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Stage0Record record = new Stage0Record();
                    record.setEventId(rs.getString(SchemaConstants.COL_EVENT_ID));
                    record.setConfigFile(rs.getString(SchemaConstants.COL_CONFIG_FILE));
                    record.setTimestamp(rs.getLong(SchemaConstants.COL_TIMESTAMP));
                    record.setThreadId(rs.getString(SchemaConstants.COL_THREAD_ID));
                    record.setPriority(rs.getString(SchemaConstants.COL_PRIORITY));
                    record.setNamespace(rs.getString(SchemaConstants.COL_NAMESPACE));
                    record.setAid(rs.getString(SchemaConstants.COL_AID));
                    record.setCid(rs.getString(SchemaConstants.COL_CID));
                    record.setKeypairs(rs.getString(SchemaConstants.COL_KEYPAIRS));
                    records.add(record);
                }
            }
        }

        return records;
    }

    /**
     * Inserts a record into STAGE1_EVENT table.
     *
     * @param conn Database connection
     * @param record Stage0 record to insert
     * @param isStable IS_STABLE flag value
     * @throws SQLException If insert fails
     */
    private void insertStage1Event(Connection conn, Stage0Record record, boolean isStable) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_STAGE1_EVENT)) {
            stmt.setString(1, record.getEventId());
            stmt.setString(2, record.getConfigFile());
            stmt.setLong(3, record.getTimestamp());
            stmt.setString(4, record.getThreadId());
            stmt.setString(5, record.getPriority());
            stmt.setString(6, record.getNamespace());
            stmt.setString(7, record.getAid());
            stmt.setString(8, record.getCid());
            stmt.setBoolean(9, isStable);
            stmt.executeUpdate();
        }
    }

    /**
     * Inserts a keypair into STAGE1_EVENT_KEYPAIR table.
     *
     * @param conn Database connection
     * @param eventId Event ID
     * @param key Key name
     * @param value Key value
     * @throws SQLException If insert fails
     */
    private void insertStage1Keypair(Connection conn, String eventId, String key, String value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_STAGE1_KEYPAIR)) {
            stmt.setString(1, eventId);
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.executeUpdate();
        }
    }

    /**
     * Updates the IS_STABLE flag for a record in STAGE1_EVENT table.
     *
     * @param conn Database connection
     * @param eventId Event ID
     * @param isStable IS_STABLE flag value
     * @throws SQLException If update fails
     */
    private void updateStage1Stable(Connection conn, String eventId, boolean isStable) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_STAGE1_STABLE)) {
            stmt.setBoolean(1, isStable);
            stmt.setString(2, eventId);
            stmt.executeUpdate();
        }
    }

    /**
     * Parses keypairs from JSON or legacy format, extracting all fields as simple key-value pairs.
     * Special handling for "message" field to extract embedded key-value data.
     *
     * @param keypairsString The keypairs string to parse (JSON or legacy format)
     * @param verbose Whether to log verbose parsing information
     * @return Map of key-value pairs with all fields flattened to simple keypairs
     */
    private Map<String, String> parseKeyPairs(String keypairsString, boolean verbose) {
        if (keypairsString == null || keypairsString.trim().isEmpty()) {
            return new HashMap<>();
        }

        String trimmed = keypairsString.trim();
        Map<String, String> result = new HashMap<>();

        // Try JSON parsing first (current format)
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, String> jsonData = objectMapper.readValue(trimmed,
                    new TypeReference<Map<String, String>>(){});

                if (jsonData != null) {
                    for (Map.Entry<String, String> entry : jsonData.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();

                        // Special handling for "message" field - extract embedded key-value pairs
                        if ("message".equals(key) && value != null) {
                            Map<String, String> messageKeyPairs = parseMessageField(value, verbose);
                            if (!messageKeyPairs.isEmpty()) {
                                // If we extracted key-value pairs from message, add them directly
                                result.putAll(messageKeyPairs);
                                if (verbose) {
                                    logger.info("Extracted {} keypairs from message field: {}",
                                              messageKeyPairs.size(), messageKeyPairs.keySet());
                                }
                            } else {
                                // If no key-value pairs found, add the message as-is
                                result.put(key, value);
                            }
                        } else {
                            // Add all other fields as simple keypairs
                            result.put(key, value);
                        }
                    }

                    if (verbose) {
                        logger.info("Parsed {} total keypairs from JSON format", result.size());
                    }
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse JSON keypairs: {}, falling back to legacy format", e.getMessage());
            }
        }

        // Fall back to legacy EventParser format (key=value, key2=value2)
        try {
            result = EventParser.deserializeKeyPairs(trimmed);
            if (verbose) {
                logger.info("Parsed {} keypairs from legacy format", result.size());
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse keypairs in any format: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Parses the message field to extract embedded key-value pairs.
     * Handles various message formats like "Environment Setting: KEY=VALUE", "KEY=VALUE", etc.
     *
     * @param messageValue The message field value to parse
     * @param verbose Whether to log verbose parsing information
     * @return Map of extracted key-value pairs, empty if no parseable pairs found
     */
    private Map<String, String> parseMessageField(String messageValue, boolean verbose) {
        Map<String, String> result = new HashMap<>();

        if (messageValue == null || messageValue.trim().isEmpty()) {
            return result;
        }

        // Look for key-value patterns in the message
        // Pattern 1: "Environment Setting: KEY=VALUE"
        // Pattern 2: "KEY=VALUE"
        // Pattern 3: "PREFIX: KEY=VALUE"

        String trimmed = messageValue.trim();

        // Check if message contains colon-separated prefix (like "Environment Setting: KEY=VALUE")
        String keyValuePart = trimmed;
        if (trimmed.contains(": ")) {
            String[] parts = trimmed.split(": ", 2);
            if (parts.length == 2) {
                keyValuePart = parts[1]; // Take the part after the colon
                if (verbose) {
                    logger.info("Found prefixed message format: '{}', extracting from: '{}'",
                              parts[0], keyValuePart);
                }
            }
        }

        // Look for KEY=VALUE pattern in the key-value part
        if (keyValuePart.contains("=")) {
            // Split on unescaped equals sign, taking only first occurrence for KEY=VALUE format
            String[] keyValue = keyValuePart.split("(?<!\\\\)=", 2);

            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Validate that we have reasonable key and value (not empty and key doesn't contain spaces)
                if (!key.isEmpty() && !value.isEmpty() && !key.contains(" ")) {
                    // Unescape special characters if needed
                    key = key.replace("\\=", "=").replace("\\, ", ", ");
                    value = value.replace("\\=", "=").replace("\\, ", ", ");

                    result.put(key, value);

                    if (verbose) {
                        logger.info("Extracted keypair from message: '{}' = '{}'", key, value);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Data holder class for STAGE0_EVENT records.
     */
    private static class Stage0Record {
        private String eventId;
        private String configFile;
        private long timestamp;
        private String threadId;
        private String priority;
        private String namespace;
        private String aid;
        private String cid;
        private String keypairs;

        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public String getConfigFile() { return configFile; }
        public void setConfigFile(String configFile) { this.configFile = configFile; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }

        public String getAid() { return aid; }
        public void setAid(String aid) { this.aid = aid; }

        public String getCid() { return cid; }
        public void setCid(String cid) { this.cid = cid; }

        public String getKeypairs() { return keypairs; }
        public void setKeypairs(String keypairs) { this.keypairs = keypairs; }

        @Override
        public String toString() {
            return String.format("Stage0Record{eventId='%s', configFile='%s', timestamp=%d, " +
                    "threadId='%s', priority='%s', namespace='%s', aid='%s', cid='%s'}",
                    eventId, configFile, timestamp, threadId, priority, namespace, aid, cid);
        }
    }
}