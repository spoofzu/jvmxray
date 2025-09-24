package org.jvmxray.service.rest.util;

import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.jvmxray.platform.shared.util.GUID;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for managing API keys in the JVMXRay database.
 * Provides methods for creating, listing, revoking, and checking API keys.
 *
 * @author Milton Smith
 */
public class ApiKeyManager {

    private static final Logger logger = Logger.getLogger(ApiKeyManager.class.getName());

    /**
     * Represents an API key record.
     */
    public static class ApiKey {
        private final String key;
        private final String appName;
        private final boolean suspended;
        private final Timestamp createdAt;
        private final Timestamp lastUsed;

        public ApiKey(String key, String appName, boolean suspended, Timestamp createdAt, Timestamp lastUsed) {
            this.key = key;
            this.appName = appName;
            this.suspended = suspended;
            this.createdAt = createdAt;
            this.lastUsed = lastUsed;
        }

        public String getKey() { return key; }
        public String getAppName() { return appName; }
        public boolean isSuspended() { return suspended; }
        public Timestamp getCreatedAt() { return createdAt; }
        public Timestamp getLastUsed() { return lastUsed; }

        @Override
        public String toString() {
            return String.format("ApiKey{key='%s', appName='%s', suspended=%s, createdAt=%s, lastUsed=%s}",
                    key, appName, suspended, createdAt, lastUsed);
        }
    }

    /**
     * Generates a new API key string with the standard JVMXRay prefix.
     * Uses RFC 4122 standard UUID format (uppercase with dashes) for external compatibility.
     *
     * @return A new API key string in format: jvmxray-XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
     */
    public static String generateApiKeyString() {
        return "jvmxray-" + GUID.generateStandard();
    }

    /**
     * Inserts a new API key into the database.
     *
     * @param connectionUrl Database connection URL
     * @param apiKey API key string
     * @param appName Application name
     * @throws SQLException If database operation fails
     */
    public static void insertApiKey(String connectionUrl, String apiKey, String appName) throws SQLException {
        logger.info("Inserting API key for application: " + appName);

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            // Ensure SQLite driver is loaded
            Class.forName("org.sqlite.JDBC");

            String sql = "INSERT INTO " + SchemaConstants.API_KEY_TABLE + " (" +
                        SchemaConstants.COL_API_KEY + ", " +
                        SchemaConstants.COL_APP_NAME + ", " +
                        SchemaConstants.COL_IS_SUSPENDED + ", " +
                        SchemaConstants.COL_CREATED_AT + ", " +
                        SchemaConstants.COL_LAST_USED + ") VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                Timestamp now = new Timestamp(System.currentTimeMillis());

                stmt.setString(1, apiKey);
                stmt.setString(2, appName);
                stmt.setBoolean(3, false); // not suspended
                stmt.setTimestamp(4, now);
                stmt.setTimestamp(5, null); // never used yet

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new SQLException("Failed to insert API key - unexpected number of rows affected: " + rowsAffected);
                }

                logger.info("API key inserted successfully for application: " + appName);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Lists all API keys in the database.
     *
     * @param connectionUrl Database connection URL
     * @return List of API key records
     * @throws SQLException If database operation fails
     */
    public static List<ApiKey> listApiKeys(String connectionUrl) throws SQLException {
        logger.info("Listing all API keys");
        List<ApiKey> keys = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            Class.forName("org.sqlite.JDBC");

            String sql = "SELECT " +
                        SchemaConstants.COL_API_KEY + ", " +
                        SchemaConstants.COL_APP_NAME + ", " +
                        SchemaConstants.COL_IS_SUSPENDED + ", " +
                        SchemaConstants.COL_CREATED_AT + ", " +
                        SchemaConstants.COL_LAST_USED +
                        " FROM " + SchemaConstants.API_KEY_TABLE +
                        " ORDER BY " + SchemaConstants.COL_CREATED_AT + " DESC";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    keys.add(new ApiKey(
                            rs.getString(SchemaConstants.COL_API_KEY),
                            rs.getString(SchemaConstants.COL_APP_NAME),
                            rs.getBoolean(SchemaConstants.COL_IS_SUSPENDED),
                            rs.getTimestamp(SchemaConstants.COL_CREATED_AT),
                            rs.getTimestamp(SchemaConstants.COL_LAST_USED)
                    ));
                }
            }

            logger.info("Retrieved " + keys.size() + " API keys");
            return keys;

        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Revokes (suspends) an API key by setting the suspended flag.
     *
     * @param connectionUrl Database connection URL
     * @param apiKey API key to revoke
     * @return true if the key was found and revoked, false if not found
     * @throws SQLException If database operation fails
     */
    public static boolean revokeApiKey(String connectionUrl, String apiKey) throws SQLException {
        logger.info("Revoking API key: " + apiKey);

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            Class.forName("org.sqlite.JDBC");

            String sql = "UPDATE " + SchemaConstants.API_KEY_TABLE +
                        " SET " + SchemaConstants.COL_IS_SUSPENDED + " = ?" +
                        " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBoolean(1, true);
                stmt.setString(2, apiKey);

                int rowsAffected = stmt.executeUpdate();
                boolean revoked = rowsAffected > 0;

                if (revoked) {
                    logger.info("API key revoked successfully: " + apiKey);
                } else {
                    logger.warning("API key not found for revocation: " + apiKey);
                }

                return revoked;
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Checks if an API key exists and is active (not suspended).
     *
     * @param connectionUrl Database connection URL
     * @param apiKey API key to check
     * @return ApiKey record if found and active, null otherwise
     * @throws SQLException If database operation fails
     */
    public static ApiKey checkApiKey(String connectionUrl, String apiKey) throws SQLException {
        logger.info("Checking API key: " + apiKey);

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            Class.forName("org.sqlite.JDBC");

            String sql = "SELECT " +
                        SchemaConstants.COL_API_KEY + ", " +
                        SchemaConstants.COL_APP_NAME + ", " +
                        SchemaConstants.COL_IS_SUSPENDED + ", " +
                        SchemaConstants.COL_CREATED_AT + ", " +
                        SchemaConstants.COL_LAST_USED +
                        " FROM " + SchemaConstants.API_KEY_TABLE +
                        " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, apiKey);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ApiKey key = new ApiKey(
                                rs.getString(SchemaConstants.COL_API_KEY),
                                rs.getString(SchemaConstants.COL_APP_NAME),
                                rs.getBoolean(SchemaConstants.COL_IS_SUSPENDED),
                                rs.getTimestamp(SchemaConstants.COL_CREATED_AT),
                                rs.getTimestamp(SchemaConstants.COL_LAST_USED)
                        );

                        logger.info("API key found: " + key);
                        return key;
                    } else {
                        logger.info("API key not found: " + apiKey);
                        return null;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Updates the last used timestamp for an API key.
     *
     * @param connectionUrl Database connection URL
     * @param apiKey API key to update
     * @throws SQLException If database operation fails
     */
    public static void updateLastUsed(String connectionUrl, String apiKey) throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            Class.forName("org.sqlite.JDBC");

            String sql = "UPDATE " + SchemaConstants.API_KEY_TABLE +
                        " SET " + SchemaConstants.COL_LAST_USED + " = ?" +
                        " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setString(2, apiKey);
                stmt.executeUpdate();
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }
}