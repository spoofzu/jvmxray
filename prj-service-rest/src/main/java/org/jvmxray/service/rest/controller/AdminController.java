package org.jvmxray.service.rest.controller;

import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST controller for administrative endpoints.
 * Manages API keys and other administrative functions.
 *
 * @author Milton Smith
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    @Autowired
    private DataSource dataSource;

    /**
     * Create a new API key.
     *
     * @param request Request containing app_name
     * @return The created API key
     */
    @PostMapping("/apikeys")
    public ResponseEntity<?> createApiKey(@RequestBody Map<String, String> request) {
        String appName = request.get("app_name");

        if (appName == null || appName.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "app_name is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Generate a random API key
        String apiKey = generateApiKey();

        String sql = "INSERT INTO " + SchemaConstants.API_KEY_TABLE +
                     " (" + SchemaConstants.COL_API_KEY + ", " +
                     SchemaConstants.COL_APP_NAME + ", " +
                     SchemaConstants.COL_IS_SUSPENDED + ", " +
                     SchemaConstants.COL_CREATED_AT + ", " +
                     SchemaConstants.COL_LAST_USED + ") VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();
            stmt.setString(1, apiKey);
            stmt.setString(2, appName);
            stmt.setBoolean(3, false);
            stmt.setLong(4, now);
            stmt.setLong(5, 0);

            stmt.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("api_key", apiKey);
            response.put("app_name", appName);
            response.put("created_at", now);

            logger.info("Created new API key for app: " + appName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating API key", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create API key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Suspend or unsuspend an API key.
     *
     * @param apiKey The API key to update
     * @param request Request containing suspend status
     * @return Updated API key information
     */
    @PutMapping("/apikeys/{apiKey}/suspend")
    public ResponseEntity<?> updateApiKeySuspension(@PathVariable String apiKey,
                                                    @RequestBody Map<String, Boolean> request) {
        Boolean suspend = request.get("suspend");

        if (suspend == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "suspend parameter is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String sql = "UPDATE " + SchemaConstants.API_KEY_TABLE +
                     " SET " + SchemaConstants.COL_IS_SUSPENDED + " = ?" +
                     " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, suspend);
            stmt.setString(2, apiKey);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated == 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "API key not found: " + apiKey);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("api_key", apiKey);
            response.put("is_suspended", suspend);

            logger.info((suspend ? "Suspended" : "Unsuspended") + " API key: " + apiKey);

            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating API key suspension", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update API key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * List all API keys.
     *
     * @return List of API keys
     */
    @GetMapping("/apikeys")
    public ResponseEntity<?> listApiKeys() {
        String sql = "SELECT * FROM " + SchemaConstants.API_KEY_TABLE +
                     " ORDER BY " + SchemaConstants.COL_CREATED_AT + " DESC";

        List<Map<String, Object>> apiKeys = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> keyInfo = new HashMap<>();
                keyInfo.put("api_key", rs.getString(SchemaConstants.COL_API_KEY));
                keyInfo.put("app_name", rs.getString(SchemaConstants.COL_APP_NAME));
                keyInfo.put("is_suspended", rs.getBoolean(SchemaConstants.COL_IS_SUSPENDED));
                keyInfo.put("created_at", rs.getLong(SchemaConstants.COL_CREATED_AT));
                keyInfo.put("last_used", rs.getLong(SchemaConstants.COL_LAST_USED));
                apiKeys.add(keyInfo);
            }

            logger.fine("Listed " + apiKeys.size() + " API keys");

            return ResponseEntity.ok(apiKeys);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error listing API keys", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to list API keys");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete an API key.
     *
     * @param apiKey The API key to delete
     * @return Success message
     */
    @DeleteMapping("/apikeys/{apiKey}")
    public ResponseEntity<?> deleteApiKey(@PathVariable String apiKey) {
        String sql = "DELETE FROM " + SchemaConstants.API_KEY_TABLE +
                     " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, apiKey);

            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted == 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "API key not found: " + apiKey);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "API key deleted successfully");

            logger.info("Deleted API key: " + apiKey);

            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting API key", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete API key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Generate a random API key.
     *
     * @return A random 32-character API key
     */
    private String generateApiKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(32);

        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}