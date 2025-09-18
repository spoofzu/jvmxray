package org.jvmxray.service.rest.filter;

import org.jvmxray.platform.shared.schema.SchemaConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter to authenticate API requests using API keys.
 * Validates API keys against the database and checks if they are suspended.
 *
 * @author Milton Smith
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(ApiKeyAuthenticationFilter.class.getName());
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PARAM = "api_key";

    private DataSource dataSource;

    /**
     * Set the data source for database connections.
     *
     * @param dataSource The data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract API key from header or query parameter
        String apiKey = extractApiKey(request);

        if (apiKey == null || apiKey.isEmpty()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "API key is required");
            return;
        }

        // Validate API key
        try {
            ApiKeyInfo keyInfo = validateApiKey(apiKey);

            if (keyInfo == null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid API key");
                return;
            }

            if (keyInfo.isSuspended) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "API key is suspended");
                return;
            }

            // Update last used timestamp
            updateLastUsed(apiKey);

            // Add app name to request attributes for logging
            request.setAttribute("app_name", keyInfo.appName);

            // Continue with the request
            filterChain.doFilter(request, response);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during API key validation", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication service error");
        }
    }

    /**
     * Extract API key from request header or query parameter.
     *
     * @param request HTTP request
     * @return API key or null if not found
     */
    private String extractApiKey(HttpServletRequest request) {
        // First check header
        String apiKey = request.getHeader(API_KEY_HEADER);

        // If not in header, check query parameter
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = request.getParameter(API_KEY_PARAM);
        }

        return apiKey;
    }

    /**
     * Validate API key against the database.
     *
     * @param apiKey The API key to validate
     * @return ApiKeyInfo if valid, null if not found
     * @throws SQLException if database error occurs
     */
    private ApiKeyInfo validateApiKey(String apiKey) throws SQLException {
        String sql = "SELECT " + SchemaConstants.COL_APP_NAME + ", " + SchemaConstants.COL_IS_SUSPENDED +
                     " FROM " + SchemaConstants.API_KEY_TABLE +
                     " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, apiKey);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ApiKeyInfo info = new ApiKeyInfo();
                    info.appName = rs.getString(SchemaConstants.COL_APP_NAME);
                    info.isSuspended = rs.getBoolean(SchemaConstants.COL_IS_SUSPENDED);
                    return info;
                }
            }
        }

        return null;
    }

    /**
     * Update the last used timestamp for an API key.
     *
     * @param apiKey The API key to update
     * @throws SQLException if database error occurs
     */
    private void updateLastUsed(String apiKey) throws SQLException {
        String sql = "UPDATE " + SchemaConstants.API_KEY_TABLE +
                     " SET " + SchemaConstants.COL_LAST_USED + " = ?" +
                     " WHERE " + SchemaConstants.COL_API_KEY + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, apiKey);
            stmt.executeUpdate();
        }
    }

    /**
     * Send an error response with the specified status and message.
     *
     * @param response HTTP response
     * @param status HTTP status code
     * @param message Error message
     * @throws IOException if writing response fails
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    /**
     * Inner class to hold API key information.
     */
    private static class ApiKeyInfo {
        String appName;
        boolean isSuspended;
    }
}