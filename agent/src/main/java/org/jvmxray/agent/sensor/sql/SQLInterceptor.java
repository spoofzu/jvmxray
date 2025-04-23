package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging SQL query execution events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument methods of SQL-related classes (e.g., {@code PreparedStatement}),
 * capturing query details, execution duration, and parameters. Events are logged with contextual metadata using
 * the {@link LogProxy}. Note: This implementation is not yet fully operational and requires further development.
 *
 * @author Milton Smith
 */
public class SQLInterceptor {
    // Namespace for logging SQL query events
    public static final String SQL_NAMESPACE = "org.jvmxray.events.sql.query";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of a SQL-related method to log query details and record the start time.
     * Note: This implementation is not yet fully operational and may require adjustments for specific SQL classes.
     *
     * @param preparedStatement The SQL statement object (e.g., {@code PreparedStatement}) being executed.
     * @return The start time in nanoseconds, or -1L if an error occurs.
     */
    @Advice.OnMethodEnter
    public static long enter(@Advice.This Object preparedStatement) {
        try {
            // Initialize metadata for logging
            Map<String, String> metadata = new HashMap<>();

            // Retrieve SQL query, falling back to toString() if reflection fails
            String sqlQuery = getSqlQuery(preparedStatement);
            metadata.put("query", sqlQuery != null ? sqlQuery : "unknown");
            metadata.put("class", preparedStatement.getClass().getName());

            // Log the query start event
            logProxy.logEvent(SQL_NAMESPACE, "INFO", metadata);
            return System.nanoTime();
        } catch (Exception e) {
            // Log error if query processing fails
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process SQL query start: " + e.getMessage());
            errorMetadata.put("class", preparedStatement.getClass().getName());
            logProxy.logEvent(SQL_NAMESPACE, "ERROR", errorMetadata);
            return -1L;
        }
    }

    /**
     * Intercepts the exit of a SQL-related method to log the execution result and duration.
     * Captures both successful executions and exceptions, and logs parameters at DEBUG level.
     * Note: This implementation is not yet fully operational and may require adjustments for specific SQL classes.
     *
     * @param preparedStatement The SQL statement object (e.g., {@code PreparedStatement}) being executed.
     * @param startTime        The start time in nanoseconds, as returned by {@code enter}.
     * @param throwable        The {@code Throwable} thrown during execution, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This Object preparedStatement,
                            @Advice.Enter long startTime,
                            @Advice.Thrown Throwable throwable) {
        try {
            // Initialize metadata for logging
            Map<String, String> metadata = new HashMap<>();

            // Calculate execution duration if start time is valid
            if (startTime != -1L) {
                long durationNs = System.nanoTime() - startTime;
                double durationMs = durationNs / 1_000_000.0;
                metadata.put("duration_ms", String.format("%.2f", durationMs));
            } else {
                metadata.put("duration_ms", "unknown");
            }

            metadata.put("class", preparedStatement.getClass().getName());
            if (throwable != null) {
                // Log error status and details
                metadata.put("status", "error");
                metadata.put("error_message", throwable.getMessage());
                logProxy.logEvent(SQL_NAMESPACE, "ERROR", metadata);
            } else {
                // Log success status
                metadata.put("status", "success");
                logProxy.logEvent(SQL_NAMESPACE, "INFO", metadata);
            }

            // Log additional parameters at DEBUG level if enabled
            if (logProxy.isLoggingAtLevel(SQL_NAMESPACE, "DEBUG")) {
                Map<String, String> parameters = getParameters(preparedStatement);
                metadata.putAll(parameters);
                logProxy.logEvent(SQL_NAMESPACE, "DEBUG", metadata);
            }
        } catch (Exception e) {
            // Log error if exit processing fails
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process SQL query exit: " + e.getMessage());
            errorMetadata.put("class", preparedStatement.getClass().getName());
            logProxy.logEvent(SQL_NAMESPACE, "ERROR", errorMetadata);
        }
    }

    /**
     * Attempts to retrieve the SQL query string from the statement object using reflection.
     * Falls back to {@code toString()} if reflection fails.
     * Note: This method is not yet fully operational and may not work for all SQL implementations.
     *
     * @param preparedStatement The SQL statement object (e.g., {@code PreparedStatement}).
     * @return The SQL query string, or the object's {@code toString()} representation if retrieval fails.
     */
    private static String getSqlQuery(Object preparedStatement) {
        try {
            // Attempt to access the query via reflection (vendor-specific)
            java.lang.reflect.Field sqlField = preparedStatement.getClass().getDeclaredField("sql");
            sqlField.setAccessible(true);
            return (String) sqlField.get(preparedStatement);
        } catch (Exception e) {
            // Fallback to toString() if reflection fails
            return preparedStatement.toString();
        }
    }

    /**
     * Retrieves parameters from the SQL statement object.
     * Currently retrieves the query string again as a placeholder.
     * Note: This method is not yet fully operational and requires proper parameter extraction logic.
     *
     * @param preparedStatement The SQL statement object (e.g., {@code PreparedStatement}).
     * @return A map containing query details or an error message if retrieval fails.
     */
    private static Map<String, String> getParameters(Object preparedStatement) {
        Map<String, String> params = new HashMap<>();
        try {
            // Retrieve query string (placeholder for actual parameter extraction)
            String sqlQuery = getSqlQuery(preparedStatement);
            params.put("query_details", sqlQuery != null ? sqlQuery : "unknown");
        } catch (Exception e) {
            params.put("param_error", "Unable to retrieve parameters: " + e.getMessage());
        }
        return params;
    }
}