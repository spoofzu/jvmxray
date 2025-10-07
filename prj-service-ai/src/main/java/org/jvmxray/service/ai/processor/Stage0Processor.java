package org.jvmxray.service.ai.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.schema.EventParser;
import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage0Processor - Migrates data from STAGE0_EVENT to STAGE1_EVENT.
 *
 * <p>This processor reads raw events from STAGE0_EVENT, parses the KEYPAIRS column,
 * and inserts the data into STAGE1_EVENT and STAGE1_EVENT_KEYPAIR tables using
 * the IS_STABLE flag pattern to ensure data consistency.</p>
 *
 * <p>Processing Flow:</p>
 * <ul>
 *   <li>Read batch of records from STAGE0_EVENT</li>
 *   <li>For each record:
 *     <ul>
 *       <li>Insert into STAGE1_EVENT with IS_STABLE=false</li>
 *       <li>Parse KEYPAIRS and insert into STAGE1_EVENT_KEYPAIR</li>
 *       <li>Update IS_STABLE=true</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class Stage0Processor extends AbstractStageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(Stage0Processor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // SQL queries
    private static final String SELECT_STAGE0_BATCH =
        "SELECT " + SchemaConstants.COL_EVENT_ID + ", " +
                    SchemaConstants.COL_CONFIG_FILE + ", " +
                    SchemaConstants.COL_TIMESTAMP + ", " +
                    SchemaConstants.COL_CURRENT_THREAD_ID + ", " +
                    SchemaConstants.COL_PRIORITY + ", " +
                    SchemaConstants.COL_NAMESPACE + ", " +
                    SchemaConstants.COL_AID + ", " +
                    SchemaConstants.COL_CID + ", " +
                    SchemaConstants.COL_KEYPAIRS +
        " FROM " + SchemaConstants.STAGE0_EVENT_TABLE +
        " LIMIT ?";

    private static final String INSERT_STAGE1_EVENT =
        "INSERT INTO " + SchemaConstants.STAGE1_EVENT_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_CONFIG_FILE + ", " +
        SchemaConstants.COL_TIMESTAMP + ", " +
        SchemaConstants.COL_CURRENT_THREAD_ID + ", " +
        SchemaConstants.COL_PRIORITY + ", " +
        SchemaConstants.COL_NAMESPACE + ", " +
        SchemaConstants.COL_AID + ", " +
        SchemaConstants.COL_CID + ", " +
        SchemaConstants.COL_KEYPAIRS + ", " +
        SchemaConstants.COL_IS_STABLE +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

    private static final String DELETE_STAGE0_EVENT =
        "DELETE FROM " + SchemaConstants.STAGE0_EVENT_TABLE +
        " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

    /**
     * Constructs a new Stage0Processor.
     *
     * @param properties Component properties for configuration
     */
    public Stage0Processor(PropertyBase properties) {
        super(properties);
    }

    @Override
    public String getProcessorName() {
        return "Stage0Processor";
    }

    @Override
    protected String getEnabledPropertyKey() {
        return "aiservice.stage0.enabled";
    }

    @Override
    public int processBatch(Connection connection, int batchSize) throws SQLException {
        if (!isEnabled()) {
            return 0;
        }

        logger.debug("Processing STAGE0 to STAGE1 batch, size: {}", batchSize);

        int processedCount = 0;
        List<Stage0Record> batch = readStage0Batch(connection, batchSize);

        if (batch.isEmpty()) {
            return 0;
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            for (Stage0Record record : batch) {
                try {
                    // Insert into STAGE1_EVENT with IS_STABLE=false
                    insertStage1Event(connection, record, false);

                    // Parse and insert keypairs
                    Map<String, String> keypairs = parseKeyPairs(record.getKeypairs());
                    for (Map.Entry<String, String> entry : keypairs.entrySet()) {
                        insertStage1Keypair(connection, record.getEventId(),
                                          entry.getKey(), entry.getValue());
                    }

                    // Update IS_STABLE to true
                    updateStage1Stable(connection, record.getEventId(), true);

                    // Delete from STAGE0 after successful migration
                    deleteStage0Event(connection, record.getEventId());

                    processedCount++;

                } catch (SQLException e) {
                    logger.error("Failed to process STAGE0 event {}: {}",
                               record.getEventId(), e.getMessage(), e);
                    connection.rollback();
                    throw e;
                }
            }

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

        logger.debug("Processed {} STAGE0 events", processedCount);
        return processedCount;
    }

    /**
     * Reads a batch of records from STAGE0_EVENT.
     */
    private List<Stage0Record> readStage0Batch(Connection conn, int limit) throws SQLException {
        List<Stage0Record> records = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SELECT_STAGE0_BATCH)) {
            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Stage0Record record = new Stage0Record();
                    record.setEventId(rs.getString(SchemaConstants.COL_EVENT_ID));
                    record.setConfigFile(rs.getString(SchemaConstants.COL_CONFIG_FILE));
                    record.setTimestamp(rs.getLong(SchemaConstants.COL_TIMESTAMP));
                    record.setThreadId(rs.getString(SchemaConstants.COL_CURRENT_THREAD_ID));
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
     * Inserts a record into STAGE1_EVENT.
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
            stmt.setString(9, record.getKeypairs());
            stmt.setBoolean(10, isStable);
            stmt.executeUpdate();
        }
    }

    /**
     * Inserts a keypair into STAGE1_EVENT_KEYPAIR.
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
     * Updates IS_STABLE flag for a STAGE1_EVENT record.
     */
    private void updateStage1Stable(Connection conn, String eventId, boolean isStable) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_STAGE1_STABLE)) {
            stmt.setBoolean(1, isStable);
            stmt.setString(2, eventId);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a record from STAGE0_EVENT after successful migration.
     */
    private void deleteStage0Event(Connection conn, String eventId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_STAGE0_EVENT)) {
            stmt.setString(1, eventId);
            stmt.executeUpdate();
        }
    }

    /**
     * Parses keypairs from JSON or legacy format.
     */
    private Map<String, String> parseKeyPairs(String keypairsString) {
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

                        // Special handling for "message" field
                        if ("message".equals(key) && value != null) {
                            Map<String, String> messageKeyPairs = parseMessageField(value);
                            if (!messageKeyPairs.isEmpty()) {
                                result.putAll(messageKeyPairs);
                            } else {
                                result.put(key, value);
                            }
                        } else {
                            result.put(key, value);
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse JSON keypairs: {}, falling back to legacy format", e.getMessage());
            }
        }

        // Fall back to legacy EventParser format
        try {
            result = EventParser.deserializeKeyPairs(trimmed);
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse keypairs in any format: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Parses message field to extract embedded key-value pairs.
     */
    private Map<String, String> parseMessageField(String messageValue) {
        Map<String, String> result = new HashMap<>();

        if (messageValue == null || messageValue.trim().isEmpty()) {
            return result;
        }

        String trimmed = messageValue.trim();
        String keyValuePart = trimmed;

        // Check for prefix pattern (e.g., "Environment Setting: KEY=VALUE")
        if (trimmed.contains(": ")) {
            String[] parts = trimmed.split(": ", 2);
            if (parts.length == 2) {
                keyValuePart = parts[1];
            }
        }

        // Look for KEY=VALUE pattern
        if (keyValuePart.contains("=")) {
            String[] keyValue = keyValuePart.split("(?<!\\\\)=", 2);

            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if (!key.isEmpty() && !value.isEmpty() && !key.contains(" ")) {
                    key = key.replace("\\=", "=").replace("\\, ", ", ");
                    value = value.replace("\\=", "=").replace("\\, ", ", ");
                    result.put(key, value);
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
    }
}
