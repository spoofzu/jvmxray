package org.jvmxray.service.ai.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage1Processor - Processes library events from STAGE1_EVENT to STAGE2_LIBRARY.
 *
 * <p>This processor reads library loading events from STAGE1_EVENT (where namespace
 * is 'org.jvmxray.events.system.lib') and inserts basic library metadata into
 * STAGE2_LIBRARY. The Stage2Processor will then enrich these records with CVE
 * analysis and additional metadata.</p>
 *
 * <p>Processing Flow:</p>
 * <ul>
 *   <li>Read library events from STAGE1_EVENT (IS_STABLE=true, library namespace)</li>
 *   <li>Extract library information from KEYPAIRS column</li>
 *   <li>Insert basic library record into STAGE2_LIBRARY</li>
 *   <li>Delete processed events from STAGE1_EVENT</li>
 * </ul>
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class Stage1Processor extends AbstractStageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(Stage1Processor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LIBRARY_NAMESPACE = "org.jvmxray.events.system.lib";

    // SQL queries
    private static final String SELECT_LIBRARY_EVENTS_BATCH =
        "SELECT " + SchemaConstants.COL_EVENT_ID + ", " +
                    SchemaConstants.COL_AID + ", " +
                    SchemaConstants.COL_CID + ", " +
                    SchemaConstants.COL_TIMESTAMP + ", " +
                    SchemaConstants.COL_KEYPAIRS +
        " FROM " + SchemaConstants.STAGE1_EVENT_TABLE +
        " WHERE " + SchemaConstants.COL_NAMESPACE + " = ? " +
        "   AND " + SchemaConstants.COL_IS_STABLE + " = true " +
        " LIMIT ?";

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

    // SQLite-compatible upsert
    private static final String INSERT_STAGE2_LIBRARY_SQLITE =
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
        "ON CONFLICT(" + SchemaConstants.COL_LIBRARY_ID + ") DO UPDATE SET " +
        SchemaConstants.COL_LAST_SEEN + " = excluded." + SchemaConstants.COL_LAST_SEEN + ", " +
        SchemaConstants.COL_IS_ACTIVE + " = excluded." + SchemaConstants.COL_IS_ACTIVE;

    private static final String DELETE_STAGE1_EVENT =
        "DELETE FROM " + SchemaConstants.STAGE1_EVENT_TABLE +
        " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

    private boolean isSQLite = false;

    /**
     * Constructs a new Stage1Processor.
     *
     * @param properties Component properties for configuration
     */
    public Stage1Processor(PropertyBase properties) {
        super(properties);
    }

    @Override
    public String getProcessorName() {
        return "Stage1Processor";
    }

    @Override
    protected String getEnabledPropertyKey() {
        return "aiservice.stage1.enabled";
    }

    @Override
    protected void onInitialize() throws Exception {
        // Detect database type from JDBC URL
        String jdbcUrl = System.getProperty("aiservice.database.url", "");
        isSQLite = jdbcUrl.contains("sqlite");
    }

    @Override
    public int processBatch(Connection connection, int batchSize) throws SQLException {
        if (!isEnabled()) {
            return 0;
        }

        logger.debug("Processing STAGE1 to STAGE2 library batch, size: {}", batchSize);

        int processedCount = 0;

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement selectStmt = connection.prepareStatement(SELECT_LIBRARY_EVENTS_BATCH)) {
                selectStmt.setString(1, LIBRARY_NAMESPACE);
                selectStmt.setInt(2, batchSize);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            processLibraryEvent(connection, rs);
                            processedCount++;
                        } catch (Exception e) {
                            logger.error("Failed to process STAGE1 library event {}: {}",
                                       rs.getString(SchemaConstants.COL_EVENT_ID), e.getMessage(), e);
                            connection.rollback();
                            throw e;
                        }
                    }
                }
            }

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

        logger.debug("Processed {} STAGE1 library events", processedCount);
        return processedCount;
    }

    /**
     * Processes a single library event from STAGE1.
     */
    private void processLibraryEvent(Connection connection, ResultSet rs) throws SQLException {
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
            logger.warn("Skipping STAGE1 event {}: Missing required fields (jarPath={}, method={})",
                      eventId, jarPath, method);
            return;
        }

        if (sha256Hash == null || sha256Hash.trim().isEmpty()) {
            logger.warn("Skipping STAGE1 event {}: Missing SHA-256 hash from agent for {}",
                      eventId, jarPath);
            return;
        }

        // Extract library name from jar path
        String libraryName = extractLibraryName(jarPath);

        // Library ID is the SHA-256 hash
        String libraryId = sha256Hash;

        // Insert into STAGE2_LIBRARY
        String insertSql = isSQLite ? INSERT_STAGE2_LIBRARY_SQLITE : INSERT_STAGE2_LIBRARY;
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, libraryId);
            stmt.setString(2, eventId);
            stmt.setString(3, aid);
            stmt.setString(4, cid);
            stmt.setString(5, jarPath);
            stmt.setString(6, libraryName);
            stmt.setString(7, sha256Hash);
            stmt.setString(8, method);
            stmt.setLong(9, timestamp); // first_seen
            stmt.setLong(10, timestamp); // last_seen
            stmt.setBoolean(11, true); // is_active

            stmt.executeUpdate();
        }

        // Delete from STAGE1 after successful processing
        deleteStage1Event(connection, eventId);

        logger.debug("Processed STAGE1 library event: {} -> {} ({})",
                   eventId, libraryName, sha256Hash.substring(0, 8) + "...");
    }

    /**
     * Deletes a processed event from STAGE1_EVENT.
     */
    private void deleteStage1Event(Connection conn, String eventId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_STAGE1_EVENT)) {
            stmt.setString(1, eventId);
            stmt.executeUpdate();
        }
    }

    /**
     * Parses keypairs string into a map.
     */
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

    /**
     * Extracts library name from JAR path.
     */
    private String extractLibraryName(String jarPath) {
        if (jarPath == null) {
            return "unknown";
        }

        String fileName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        // Remove version pattern (e.g., -1.2.3, -1.2.3-SNAPSHOT)
        String nameOnly = fileName.replaceFirst("-\\d+\\..*$", "");
        return nameOnly.isEmpty() ? fileName : nameOnly;
    }
}
