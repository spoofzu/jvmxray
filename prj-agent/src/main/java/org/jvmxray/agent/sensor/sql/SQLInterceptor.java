package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interceptor for monitoring SQL query execution in JDBC PreparedStatement implementations.
 * Instruments execute methods to capture query details, execution duration, and errors.
 * Logs events with contextual metadata using LogProxy.
 *
 * @author Milton Smith
 */
public class SQLInterceptor {
    private static final String SQL_NAMESPACE = "org.jvmxray.events.sql.query";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts method entry to log query details and record start time.
     * @param preparedStatement The PreparedStatement instance.
     * @return A context object containing start time and correlation ID.
     */
    @Advice.OnMethodEnter
    public static Object[] enter(@Advice.This Object preparedStatement) {
        try {
            String correlationId = UUID.randomUUID().toString();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("correlation_id", correlationId);
            metadata.put("class", preparedStatement.getClass().getName());

            // Retrieve SQL query safely
            String sqlQuery = null;
            try {
                // Use driver-specific methods or toString as a last resort
                if (preparedStatement instanceof PreparedStatement) {
                    // Some drivers provide proprietary methods; fallback to toString
                    sqlQuery = preparedStatement.toString(); // Replace with driver-specific logic if needed
                }
            } catch (Exception e) {
                //todo sink exception for now, revisit.
            }
            metadata.put("query", sqlQuery != null ? sqlQuery : "unknown");

            // Add connection details if available
            if (preparedStatement instanceof PreparedStatement) {
                try {
                    java.sql.Connection conn = ((PreparedStatement) preparedStatement).getConnection();
                    metadata.put("db_url", conn.getMetaData().getURL());
                    metadata.put("db_user", conn.getMetaData().getUserName());
                } catch (Exception ignored) {
                    // Ignore if connection metadata is unavailable
                }
            }

            logProxy.logMessage(SQL_NAMESPACE, "INFO", metadata);
            return new Object[]{System.nanoTime(), correlationId};
        } catch (Exception e) {
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process SQL query start: " + e.getMessage());
            errorMetadata.put("class", preparedStatement.getClass().getName());
            logProxy.logMessage(SQL_NAMESPACE, "ERROR", errorMetadata);
            return new Object[]{-1L, null};
        }
    }

    /**
     * Intercepts method exit to log execution result and duration.
     * @param preparedStatement The PreparedStatement instance.
     * @param context The context object from enter (start time, correlation ID).
     * @param result The method result (if any).
     * @param throwable The thrown exception, if any.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This Object preparedStatement,
                            @Advice.Enter Object[] context,
                            @Advice.Return Object result,
                            @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            long startTime = (Long) context[0];
            String correlationId = (String) context[1];
            metadata.put("correlation_id", correlationId);
            metadata.put("class", preparedStatement.getClass().getName());

            // Calculate duration
            if (startTime != -1L) {
                double durationMs = (System.nanoTime() - startTime) / 1_000_000.0;
                metadata.put("duration_ms", String.format("%.2f", durationMs));
            } else {
                metadata.put("duration_ms", "unknown");
            }

            // Log execution result
            if (throwable != null) {
                metadata.put("status", "error");
                metadata.put("error_message", throwable.getMessage());
                logProxy.logMessage(SQL_NAMESPACE, "ERROR", metadata);
            } else {
                metadata.put("status", "success");
                if (result instanceof java.sql.ResultSet) {
                    metadata.put("result_type", "ResultSet");
                } else if (result instanceof Integer) {
                    metadata.put("update_count", result.toString());
                }
                logProxy.logMessage(SQL_NAMESPACE, "INFO", metadata);
            }

            // Log parameters at DEBUG level
            if (logProxy.isLoggingAtLevel(SQL_NAMESPACE, "DEBUG")) {
                Map<String, String> params = new HashMap<>();
                try {
                    // todo: Implement driver-specific parameter extraction
                    params.put("parameters", "unavailable");
                } catch (Exception e) {
                    params.put("param_error", "Unable to retrieve parameters: " + e.getMessage());
                }
                metadata.putAll(params);
                logProxy.logMessage(SQL_NAMESPACE, "DEBUG", metadata);
            }
        } catch (Exception e) {
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process SQL query exit: " + e.getMessage());
            errorMetadata.put("class", preparedStatement.getClass().getName());
            logProxy.logMessage(SQL_NAMESPACE, "ERROR", errorMetadata);
        }
    }

}