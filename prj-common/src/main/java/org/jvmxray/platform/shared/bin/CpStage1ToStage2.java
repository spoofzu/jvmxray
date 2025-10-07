package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.init.CommonInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CpStage1ToStage2 - Data enrichment tool for processing STAGE1_EVENT library events into STAGE2_LIBRARY table.
 *
 * <p><b>DEPRECATED:</b> This standalone tool has been superseded by the AI Service's integrated
 * Stage1Processor and Stage2Processor, which automatically enrich library data from STAGE1→STAGE2
 * as part of the multi-stage data pipeline. Use the AI Service instead for automated processing:
 * <pre>{@code
 * ./script/services/ai-service --start --interval 60
 * }</pre>
 * </p>
 *
 * <p>This tool is maintained for manual/one-time enrichment and backward compatibility.</p>
 *
 * <p>Enrichment Process:</p>
 * <ul>
 *   <li>Reads library events from STAGE1_EVENT where NAMESPACE = 'org.jvmxray.events.system.lib'</li>
 *   <li>For each record:
 *     <ul>
 *       <li>Extracts library information from KEYPAIRS column</li>
 *       <li>Generates library name from JAR path</li>
 *       <li>Uses agent-provided SHA-256 hash</li>
 *       <li>Inserts enriched data into STAGE2_LIBRARY</li>
 *     </ul>
 *   </li>
 *   <li>Uses transactions to ensure atomicity</li>
 * </ul>
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code -b, --batch-size <size>}: Number of records to process per batch (default: 100)</li>
 *   <li>{@code -v, --verbose}: Enable verbose output</li>
 *   <li>{@code --dry-run}: Show what would be enriched without making changes</li>
 *   <li>{@code -h, --help}: Display help information</li>
 * </ul>
 *
 * <p><b>Database Configuration:</b></p>
 * The tool reads the JDBC connection string from {@code common.properties}:
 * <pre>{@code
 * jvmxray.common.database.jdbc.connection=jdbc:sqlite:/path/to/database.db
 * }</pre>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * # Migrate 500 records per batch with verbose output
 * java org.jvmxray.platform.shared.bin.CpStage1ToStage2 -b 500 -v
 *
 * # Dry run to see what would be enriched
 * java org.jvmxray.platform.shared.bin.CpStage1ToStage2 --dry-run
 * }</pre>
 *
 * @author JVMXRay Development Team
 * @deprecated Use AI Service's integrated Stage1Processor and Stage2Processor instead
 */
@Deprecated
public class CpStage1ToStage2 {

    private static final Logger logger = LoggerFactory.getLogger(CpStage1ToStage2.class);

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

    // Library namespace for filtering events
    private static final String LIBRARY_NAMESPACE = "org.jvmxray.events.system.lib";

    // SQL queries
    private static final String SELECT_LIBRARY_EVENTS_COUNT =
        "SELECT COUNT(*) FROM " + SchemaConstants.STAGE1_EVENT_TABLE +
        " WHERE " + SchemaConstants.COL_NAMESPACE + " = ? AND " + SchemaConstants.COL_IS_STABLE + " = true";

    private static final String SELECT_LIBRARY_EVENTS_BATCH =
        "SELECT " + SchemaConstants.COL_EVENT_ID + ", " +
                    SchemaConstants.COL_AID + ", " +
                    SchemaConstants.COL_CID + ", " +
                    SchemaConstants.COL_TIMESTAMP + ", " +
                    SchemaConstants.COL_KEYPAIRS +
        " FROM " + SchemaConstants.STAGE1_EVENT_TABLE +
        " WHERE " + SchemaConstants.COL_NAMESPACE + " = ? AND " + SchemaConstants.COL_IS_STABLE + " = true" +
        " LIMIT ? OFFSET ?";

    private static final String INSERT_STAGE2_LIBRARY =
        "INSERT INTO " + SchemaConstants.STAGE2_LIBRARY_TABLE + " (" +
        SchemaConstants.COL_LIBRARY_ID + ", " +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_AID + ", " +
        SchemaConstants.COL_CID + ", " +
        SchemaConstants.COL_JARPATH + ", " +
        SchemaConstants.COL_LIBRARY_NAME + ", " +
        SchemaConstants.COL_SHA256_HASH + ", " +
        SchemaConstants.COL_METHOD + ", " +
        SchemaConstants.COL_FIRST_SEEN + ", " +
        SchemaConstants.COL_LAST_SEEN + ", " +
        SchemaConstants.COL_IS_ACTIVE +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        SchemaConstants.COL_LAST_SEEN + " = VALUES(" + SchemaConstants.COL_LAST_SEEN + "), " +
        SchemaConstants.COL_IS_ACTIVE + " = VALUES(" + SchemaConstants.COL_IS_ACTIVE + ")";

    public static void main(String[] args) {
        try {
            // Initialize the common module for logging and properties
            CommonInitializer.getInstance().initialize();

            // Parse command line arguments
            CommandLine cmd = parseArguments(args);

            if (cmd.hasOption(OPT_HELP_SHORT)) {
                printHelp();
                System.exit(0);
            }

            int batchSize = Integer.parseInt(cmd.getOptionValue(OPT_BATCH_SIZE_SHORT, String.valueOf(DEFAULT_BATCH_SIZE)));
            boolean verbose = cmd.hasOption(OPT_VERBOSE_SHORT);
            boolean dryRun = cmd.hasOption(OPT_DRY_RUN_LONG);

            if (verbose) {
                logger.info("Starting STAGE1 to STAGE2 library enrichment");
                logger.info("Batch size: {}", batchSize);
                logger.info("Dry run: {}", dryRun);
            }

            // Execute enrichment
            CpStage1ToStage2 enricher = new CpStage1ToStage2();
            enricher.enrich(batchSize, verbose, dryRun);

        } catch (Exception e) {
            logger.error("Enrichment failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static CommandLine parseArguments(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder(OPT_BATCH_SIZE_SHORT)
                .longOpt(OPT_BATCH_SIZE_LONG)
                .hasArg()
                .argName("size")
                .desc("Number of records to process per batch (default: " + DEFAULT_BATCH_SIZE + ")")
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
                .desc("Display help information")
                .build());

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private static void printHelp() {
        String header = "\nJVMXRay STAGE1 to STAGE2 Library Enrichment Tool\n" +
                       "Enriches library events from STAGE1_EVENT into STAGE2_LIBRARY\n\n";

        String footer = "\nExamples:\n" +
                       "  java " + CpStage1ToStage2.class.getName() + " -b 500 -v\n" +
                       "  java " + CpStage1ToStage2.class.getName() + " --dry-run\n";

        Options options = new Options();
        options.addOption(OPT_BATCH_SIZE_SHORT, OPT_BATCH_SIZE_LONG, true, "Batch size");
        options.addOption(OPT_VERBOSE_SHORT, OPT_VERBOSE_LONG, false, "Verbose output");
        options.addOption(null, OPT_DRY_RUN_LONG, false, "Dry run");
        options.addOption(OPT_HELP_SHORT, OPT_HELP_LONG, false, "Help");

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("CpStage1ToStage2", header, options, footer, true);
    }

    private void enrich(int batchSize, boolean verbose, boolean dryRun) throws Exception {
        CommonInitializer initializer = CommonInitializer.getInstance();
        PropertyBase properties = initializer.getProperties();
        String jdbcUrl = properties.getProperty("jvmxray.common.database.jdbc.connection");

        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalStateException("Database JDBC URL not found in common.properties. " +
                                          "Please set 'jvmxray.common.database.jdbc.connection'");
        }

        if (verbose) {
            logger.info("Using database: {}", jdbcUrl);
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            // Get total count of library events to enrich
            int totalEvents = getTotalLibraryEventCount(connection);

            if (verbose) {
                logger.info("Found {} library events to enrich", totalEvents);
            }

            if (totalEvents == 0) {
                logger.info("No library events found to enrich");
                return;
            }

            if (dryRun) {
                logger.info("DRY RUN: Would enrich {} library events in batches of {}", totalEvents, batchSize);
                return;
            }

            // Process in batches
            int offset = 0;
            int totalProcessed = 0;

            while (offset < totalEvents) {
                int processed = processBatch(connection, batchSize, offset, verbose);
                totalProcessed += processed;
                offset += batchSize;

                if (verbose && processed > 0) {
                    logger.info("Processed batch: {} events (total: {}/{})",
                               processed, totalProcessed, totalEvents);
                }

                if (processed == 0) {
                    break; // No more events to process
                }
            }

            logger.info("Enrichment completed: {} events processed", totalProcessed);
        }
    }

    private int getTotalLibraryEventCount(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_LIBRARY_EVENTS_COUNT)) {
            stmt.setString(1, LIBRARY_NAMESPACE);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int processBatch(Connection connection, int batchSize, int offset, boolean verbose) throws SQLException {
        int processed = 0;

        try (PreparedStatement selectStmt = connection.prepareStatement(SELECT_LIBRARY_EVENTS_BATCH)) {
            selectStmt.setString(1, LIBRARY_NAMESPACE);
            selectStmt.setInt(2, batchSize);
            selectStmt.setInt(3, offset);

            try (ResultSet rs = selectStmt.executeQuery()) {
                connection.setAutoCommit(false);

                try {
                    while (rs.next()) {
                        processLibraryEvent(connection, rs, verbose);
                        processed++;
                    }

                    connection.commit();

                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }

        return processed;
    }

    private void processLibraryEvent(Connection connection, ResultSet rs, boolean verbose) throws SQLException {
        String eventId = rs.getString(SchemaConstants.COL_EVENT_ID);
        String aid = rs.getString(SchemaConstants.COL_AID);
        String cid = rs.getString(SchemaConstants.COL_CID);
        long timestamp = rs.getLong(SchemaConstants.COL_TIMESTAMP);
        String keypairs = rs.getString(SchemaConstants.COL_KEYPAIRS);

        // Parse keypairs to extract library information
        Map<String, String> eventData = parseKeypairs(keypairs);
        String jarPath = eventData.get("jarPath");
        String method = eventData.get("method");
        String sha256Hash = eventData.get("sha256");

        if (jarPath == null || method == null) {
            if (verbose) {
                logger.warn("Skipping event {}: Missing required fields (jarPath={}, method={})",
                          eventId, jarPath, method);
            }
            return;
        }

        if (sha256Hash == null || sha256Hash.trim().isEmpty()) {
            if (verbose) {
                logger.warn("Skipping event {}: Missing SHA-256 hash from agent for {}", eventId, jarPath);
            }
            return;
        }

        // Extract library name from jar path
        String libraryName = extractLibraryName(jarPath);

        // Library ID is the SHA-256 hash
        String libraryId = sha256Hash;

        // Store library record
        try (PreparedStatement insertStmt = connection.prepareStatement(INSERT_STAGE2_LIBRARY)) {
            insertStmt.setString(1, libraryId);
            insertStmt.setString(2, eventId);
            insertStmt.setString(3, aid);
            insertStmt.setString(4, cid);
            insertStmt.setString(5, jarPath);
            insertStmt.setString(6, libraryName);
            insertStmt.setString(7, sha256Hash);
            insertStmt.setString(8, method);
            insertStmt.setLong(9, timestamp); // first_seen
            insertStmt.setLong(10, timestamp); // last_seen
            insertStmt.setBoolean(11, true); // is_active

            insertStmt.executeUpdate();

            if (verbose) {
                logger.debug("Processed library: {} -> {} ({})",
                           eventId, libraryName, sha256Hash.substring(0, 8) + "...");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseKeypairs(String keypairs) {
        if (keypairs == null || keypairs.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(keypairs, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse keypairs: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String extractLibraryName(String jarPath) {
        if (jarPath == null) {
            return "unknown";
        }

        String fileName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        // Remove version numbers (basic pattern matching)
        return fileName.replaceAll("-\\d+(\\.\\d+)*.*$", "");
    }

}