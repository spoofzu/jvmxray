package org.jvmxray.service.rest.repository;

import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.jvmxray.service.rest.model.KeyPair;
import org.jvmxray.service.rest.model.PaginatedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for accessing STAGE1_EVENT_KEYPAIR table.
 *
 * @author Milton Smith
 */
@Repository
public class KeyPairRepository {

    private static final Logger logger = Logger.getLogger(KeyPairRepository.class.getName());

    @Autowired
    private DataSource dataSource;

    /**
     * Find all key-value pairs for a specific event.
     *
     * @param eventId The event ID
     * @return List of key-value pairs
     */
    public List<KeyPair> findByEventId(String eventId) {
        String sql = "SELECT * FROM " + SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE +
                     " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

        List<KeyPair> keyPairs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    keyPairs.add(mapResultSetToKeyPair(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding key pairs for event: " + eventId, e);
        }

        return keyPairs;
    }

    /**
     * Query key-value pairs with filters and pagination.
     *
     * @param key Key filter (exact match or pattern)
     * @param value Value filter (exact match or pattern)
     * @param eventId Optional event ID filter
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return List of key-value pairs
     */
    public List<KeyPair> queryKeyPairs(String key, String value, String eventId,
                                       int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE);
        sql.append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (key != null && !key.isEmpty()) {
            // Support wildcard patterns
            if (key.contains("*")) {
                String pattern = key.replace("*", "%");
                sql.append(" AND ").append(SchemaConstants.COL_KEY).append(" LIKE ?");
                params.add(pattern);
            } else {
                sql.append(" AND ").append(SchemaConstants.COL_KEY).append(" = ?");
                params.add(key);
            }
        }

        if (value != null && !value.isEmpty()) {
            // Support wildcard patterns
            if (value.contains("*")) {
                String pattern = value.replace("*", "%");
                sql.append(" AND ").append(SchemaConstants.COL_VALUE).append(" LIKE ?");
                params.add(pattern);
            } else {
                sql.append(" AND ").append(SchemaConstants.COL_VALUE).append(" = ?");
                params.add(value);
            }
        }

        if (eventId != null && !eventId.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_EVENT_ID).append(" = ?");
            params.add(eventId);
        }

        sql.append(" ORDER BY ").append(SchemaConstants.COL_EVENT_ID).append(", ")
           .append(SchemaConstants.COL_KEY);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<KeyPair> keyPairs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    keyPairs.add(mapResultSetToKeyPair(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error querying key pairs", e);
        }

        return keyPairs;
    }

    /**
     * Count key-value pairs with filters (for pagination).
     *
     * @param key Key filter
     * @param value Value filter
     * @param eventId Event ID filter
     * @return Total count of matching key-value pairs
     */
    public long countKeyPairs(String key, String value, String eventId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(SchemaConstants.STAGE1_EVENT_KEYPAIR_TABLE);
        sql.append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (key != null && !key.isEmpty()) {
            if (key.contains("*")) {
                String pattern = key.replace("*", "%");
                sql.append(" AND ").append(SchemaConstants.COL_KEY).append(" LIKE ?");
                params.add(pattern);
            } else {
                sql.append(" AND ").append(SchemaConstants.COL_KEY).append(" = ?");
                params.add(key);
            }
        }

        if (value != null && !value.isEmpty()) {
            if (value.contains("*")) {
                String pattern = value.replace("*", "%");
                sql.append(" AND ").append(SchemaConstants.COL_VALUE).append(" LIKE ?");
                params.add(pattern);
            } else {
                sql.append(" AND ").append(SchemaConstants.COL_VALUE).append(" = ?");
                params.add(value);
            }
        }

        if (eventId != null && !eventId.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_EVENT_ID).append(" = ?");
            params.add(eventId);
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, (String) params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error counting key pairs", e);
        }

        return 0;
    }

    /**
     * Find key-value pairs with filters and pagination, returning a paginated response.
     *
     * @param key Key filter (exact match or pattern)
     * @param value Value filter (exact match or pattern)
     * @param eventId Optional event ID filter
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @param resultTruncated Whether result was truncated
     * @return PaginatedResponse containing key-value pairs and pagination info
     */
    public PaginatedResponse<KeyPair> findKeyPairs(String key, String value, String eventId,
                                                 int offset, int limit, boolean resultTruncated) {
        long startQueryTime = System.currentTimeMillis();

        // Get the key-value pairs
        List<KeyPair> keyPairs = queryKeyPairs(key, value, eventId, offset, limit);

        // Get total count for pagination
        long totalElements = countKeyPairs(key, value, eventId);

        long queryTime = System.currentTimeMillis() - startQueryTime;

        // Calculate page number (0-based)
        int page = offset / limit;

        // Create pagination info
        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(page, limit, totalElements);

        // Create metadata
        PaginatedResponse.ResponseMetadata metadata = new PaginatedResponse.ResponseMetadata(queryTime, resultTruncated);

        return new PaginatedResponse<>(keyPairs, pagination, metadata);
    }

    /**
     * Map a ResultSet row to a KeyPair object.
     *
     * @param rs The ResultSet
     * @return KeyPair object
     * @throws SQLException if mapping fails
     */
    private KeyPair mapResultSetToKeyPair(ResultSet rs) throws SQLException {
        KeyPair keyPair = new KeyPair();
        keyPair.setEventId(rs.getString(SchemaConstants.COL_EVENT_ID));
        keyPair.setKey(rs.getString(SchemaConstants.COL_KEY));
        keyPair.setValue(rs.getString(SchemaConstants.COL_VALUE));
        return keyPair;
    }
}