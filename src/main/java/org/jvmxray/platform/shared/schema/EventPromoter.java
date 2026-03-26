package org.jvmxray.platform.shared.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Promotes events from STAGE0_EVENT to STAGE1_EVENT and STAGE1_EVENT_KEYPAIR.
 *
 * STAGE0_EVENT contains raw events with a serialized KEYPAIRS column.
 * This promoter copies event metadata into STAGE1_EVENT (with IS_STABLE flag)
 * and normalizes each key-value pair into individual STAGE1_EVENT_KEYPAIR rows.
 *
 * @author Milton Smith
 */
public class EventPromoter {

    private static final Logger logger = Logger.getLogger(EventPromoter.class.getName());

    // SQL for reading unprocessed stage0 events (not yet in stage1)
    private static final String SELECT_UNPROCESSED_EVENTS =
        "SELECT s0." + SchemaConstants.COL_EVENT_ID + ", " +
        "s0." + SchemaConstants.COL_CONFIG_FILE + ", " +
        "s0." + SchemaConstants.COL_TIMESTAMP + ", " +
        "s0." + SchemaConstants.COL_CURRENT_THREAD_ID + ", " +
        "s0." + SchemaConstants.COL_PRIORITY + ", " +
        "s0." + SchemaConstants.COL_NAMESPACE + ", " +
        "s0." + SchemaConstants.COL_AID + ", " +
        "s0." + SchemaConstants.COL_CID + ", " +
        "s0." + SchemaConstants.COL_KEYPAIRS +
        " FROM " + SchemaConstants.STAGE0_EVENT_TABLE + " s0" +
        " LEFT JOIN " + SchemaConstants.STAGE1_EVENT_TABLE + " s1" +
        " ON s0." + SchemaConstants.COL_EVENT_ID + " = s1." + SchemaConstants.COL_EVENT_ID +
        " WHERE s1." + SchemaConstants.COL_EVENT_ID + " IS NULL";

    // SQL for inserting into stage1_event (KEYPAIRS not stored here; normalized into STAGE1_EVENT_KEYPAIR)
    private static final String INSERT_STAGE1_EVENT =
        "INSERT OR IGNORE INTO " + SchemaConstants.STAGE1_EVENT_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_CONFIG_FILE + ", " +
        SchemaConstants.COL_TIMESTAMP + ", " +
        SchemaConstants.COL_CURRENT_THREAD_ID + ", " +
        SchemaConstants.COL_PRIORITY + ", " +
        SchemaConstants.COL_NAMESPACE + ", " +
        SchemaConstants.COL_AID + ", " +
        SchemaConstants.COL_CID + ", " +
        SchemaConstants.COL_IS_STABLE +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";

    // SQL for inserting into stage1_event_keypair
    private static final String INSERT_STAGE1_KEYPAIR =
        "INSERT OR IGNORE INTO " + SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE + " (" +
        SchemaConstants.COL_EVENT_ID + ", " +
        SchemaConstants.COL_KEY + ", " +
        SchemaConstants.COL_VALUE +
        ") VALUES (?, ?, ?)";

    // SQL to mark stage1 event as stable (keypairs fully written)
    private static final String UPDATE_STABLE =
        "UPDATE " + SchemaConstants.STAGE1_EVENT_TABLE +
        " SET " + SchemaConstants.COL_IS_STABLE + " = 1" +
        " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

    private EventPromoter() {
        // Utility class
    }

    /**
     * Promote all unprocessed STAGE0_EVENT rows into STAGE1_EVENT and STAGE1_EVENT_KEYPAIR.
     * Events are inserted with IS_STABLE=0, then marked IS_STABLE=1 after keypairs are written.
     *
     * @param connection JDBC connection (caller manages lifecycle)
     * @return number of events promoted
     * @throws SQLException if database operations fail
     */
    public static int promoteEvents(Connection connection) throws SQLException {
        int promoted = 0;
        boolean originalAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement selectStmt = connection.prepareStatement(SELECT_UNPROCESSED_EVENTS);
                 PreparedStatement insertEventStmt = connection.prepareStatement(INSERT_STAGE1_EVENT);
                 PreparedStatement insertKpStmt = connection.prepareStatement(INSERT_STAGE1_KEYPAIR);
                 PreparedStatement stableStmt = connection.prepareStatement(UPDATE_STABLE);
                 ResultSet rs = selectStmt.executeQuery()) {

                while (rs.next()) {
                    long eventId = rs.getLong(SchemaConstants.COL_EVENT_ID);
                    String configFile = rs.getString(SchemaConstants.COL_CONFIG_FILE);
                    long timestamp = rs.getLong(SchemaConstants.COL_TIMESTAMP);
                    String threadId = rs.getString(SchemaConstants.COL_CURRENT_THREAD_ID);
                    String priority = rs.getString(SchemaConstants.COL_PRIORITY);
                    String namespace = rs.getString(SchemaConstants.COL_NAMESPACE);
                    String aid = rs.getString(SchemaConstants.COL_AID);
                    String cid = rs.getString(SchemaConstants.COL_CID);
                    String keypairs = rs.getString(SchemaConstants.COL_KEYPAIRS);

                    // Insert into STAGE1_EVENT with IS_STABLE=0
                    insertEventStmt.setLong(1, eventId);
                    insertEventStmt.setString(2, configFile);
                    insertEventStmt.setLong(3, timestamp);
                    insertEventStmt.setString(4, threadId);
                    insertEventStmt.setString(5, priority);
                    insertEventStmt.setString(6, namespace);
                    insertEventStmt.setString(7, aid);
                    insertEventStmt.setString(8, cid);
                    insertEventStmt.executeUpdate();

                    // Parse keypairs and insert individual rows into STAGE1_EVENT_KEYPAIR
                    if (keypairs != null && !keypairs.trim().isEmpty()) {
                        Map<String, String> parsed = EventParser.deserializeKeyPairs(keypairs);
                        for (Map.Entry<String, String> entry : parsed.entrySet()) {
                            insertKpStmt.setLong(1, eventId);
                            insertKpStmt.setString(2, entry.getKey());
                            insertKpStmt.setString(3, entry.getValue());
                            insertKpStmt.executeUpdate();
                        }
                    }

                    // Mark event as stable
                    stableStmt.setLong(1, eventId);
                    stableStmt.executeUpdate();

                    promoted++;
                }
            }

            connection.commit();
            logger.info("Promoted " + promoted + " events from STAGE0 to STAGE1");

        } catch (SQLException e) {
            try {
                connection.rollback();
                logger.warning("Rolled back event promotion due to error");
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Failed to rollback promotion transaction", rollbackEx);
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to reset auto-commit", e);
            }
        }

        return promoted;
    }
}
