package org.jvmxray.service.rest.repository;

import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.jvmxray.service.rest.model.Event;
import org.jvmxray.service.rest.model.PaginatedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for accessing STAGE1_EVENT table.
 *
 * @author Milton Smith
 */
@Repository
public class EventRepository {

    private static final Logger logger = Logger.getLogger(EventRepository.class.getName());

    @Autowired
    private DataSource dataSource;

    /**
     * Find an event by its ID.
     *
     * @param eventId The event ID
     * @return The event or null if not found
     */
    public Event findById(String eventId) {
        String sql = "SELECT * FROM " + SchemaConstants.STAGE1_EVENT_TABLE +
                     " WHERE " + SchemaConstants.COL_EVENT_ID + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvent(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding event by ID: " + eventId, e);
        }

        return null;
    }

    /**
     * Query events with filters and pagination.
     *
     * @param namespace Namespace pattern (supports wildcards)
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @param aid Application ID
     * @param cid Correlation ID
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return List of events
     */
    public List<Event> queryEvents(String namespace, Long startTime, Long endTime,
                                   String aid, String cid, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(SchemaConstants.STAGE1_EVENT_TABLE);
        sql.append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (namespace != null && !namespace.isEmpty()) {
            // Convert wildcard pattern to SQL LIKE pattern
            String pattern = namespace.replace("*", "%");
            sql.append(" AND ").append(SchemaConstants.COL_NAMESPACE).append(" LIKE ?");
            params.add(pattern);
        }

        if (startTime != null) {
            sql.append(" AND ").append(SchemaConstants.COL_TIMESTAMP).append(" >= ?");
            params.add(startTime);
        }

        if (endTime != null) {
            sql.append(" AND ").append(SchemaConstants.COL_TIMESTAMP).append(" <= ?");
            params.add(endTime);
        }

        if (aid != null && !aid.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_AID).append(" = ?");
            params.add(aid);
        }

        if (cid != null && !cid.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_CID).append(" = ?");
            params.add(cid);
        }

        sql.append(" ORDER BY ").append(SchemaConstants.COL_TIMESTAMP).append(" DESC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Event> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error querying events", e);
        }

        return events;
    }

    /**
     * Count events with filters (for pagination).
     *
     * @param namespace Namespace pattern
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @param aid Application ID
     * @param cid Correlation ID
     * @return Total count of matching events
     */
    public long countEvents(String namespace, Long startTime, Long endTime,
                           String aid, String cid) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(SchemaConstants.STAGE1_EVENT_TABLE);
        sql.append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (namespace != null && !namespace.isEmpty()) {
            String pattern = namespace.replace("*", "%");
            sql.append(" AND ").append(SchemaConstants.COL_NAMESPACE).append(" LIKE ?");
            params.add(pattern);
        }

        if (startTime != null) {
            sql.append(" AND ").append(SchemaConstants.COL_TIMESTAMP).append(" >= ?");
            params.add(startTime);
        }

        if (endTime != null) {
            sql.append(" AND ").append(SchemaConstants.COL_TIMESTAMP).append(" <= ?");
            params.add(endTime);
        }

        if (aid != null && !aid.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_AID).append(" = ?");
            params.add(aid);
        }

        if (cid != null && !cid.isEmpty()) {
            sql.append(" AND ").append(SchemaConstants.COL_CID).append(" = ?");
            params.add(cid);
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error counting events", e);
        }

        return 0;
    }

    /**
     * Find events with filters and pagination, returning a paginated response.
     *
     * @param namespace Namespace pattern (supports wildcards)
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @param aid Application ID
     * @param cid Correlation ID
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @param resultTruncated Whether result was truncated
     * @return PaginatedResponse containing events and pagination info
     */
    public PaginatedResponse<Event> findEvents(String namespace, Long startTime, Long endTime,
                                             String aid, String cid, int offset, int limit, boolean resultTruncated) {
        long startQueryTime = System.currentTimeMillis();

        // Get the events
        List<Event> events = queryEvents(namespace, startTime, endTime, aid, cid, offset, limit);

        // Get total count for pagination
        long totalElements = countEvents(namespace, startTime, endTime, aid, cid);

        long queryTime = System.currentTimeMillis() - startQueryTime;

        // Calculate page number (0-based)
        int page = offset / limit;

        // Create pagination info
        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(page, limit, totalElements);

        // Create metadata
        PaginatedResponse.ResponseMetadata metadata = new PaginatedResponse.ResponseMetadata(queryTime, resultTruncated);

        return new PaginatedResponse<>(events, pagination, metadata);
    }

    /**
     * Map a ResultSet row to an Event object.
     *
     * @param rs The ResultSet
     * @return Event object
     * @throws SQLException if mapping fails
     */
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getString(SchemaConstants.COL_EVENT_ID));
        event.setConfigFile(rs.getString(SchemaConstants.COL_CONFIG_FILE));
        event.setTimestamp(rs.getLong(SchemaConstants.COL_TIMESTAMP));
        event.setThreadId(rs.getString(SchemaConstants.COL_CURRENT_THREAD_ID));
        event.setPriority(rs.getString(SchemaConstants.COL_PRIORITY));
        event.setNamespace(rs.getString(SchemaConstants.COL_NAMESPACE));
        event.setAid(rs.getString(SchemaConstants.COL_AID));
        event.setCid(rs.getString(SchemaConstants.COL_CID));
        event.setIsStable(rs.getBoolean(SchemaConstants.COL_IS_STABLE));
        return event;
    }
}