package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.GUID;
import org.jvmxray.platform.shared.util.MCCScope;

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
     * Checks if this SQL operation is from the agent's internal database operations.
     * This filters out noise from the agent's own ShadedSQLiteAppender.
     */
    private static boolean isAgentInternalOperation(Object preparedStatement) {
        try {
            String className = preparedStatement.getClass().getName();
            // Filter shaded SQLite classes used by the agent's appender
            if (className.contains("jvmxray") || className.contains("shaded")) {
                return true;
            }

            // Also check the connection URL for agent's database
            if (preparedStatement instanceof PreparedStatement) {
                java.sql.Connection conn = ((PreparedStatement) preparedStatement).getConnection();
                String url = conn.getMetaData().getURL();
                if (url != null && url.contains("jvmxray")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors during check
        }
        return false;
    }

    /**
     * Extracts the SQL operation type from SQL text.
     */
    private static String extractSqlOperationType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("CREATE")) return "DDL";
        if (trimmed.startsWith("ALTER")) return "DDL";
        if (trimmed.startsWith("DROP")) return "DDL";
        if (trimmed.startsWith("TRUNCATE")) return "DDL";
        if (trimmed.startsWith("CALL")) return "CALL";
        if (trimmed.startsWith("MERGE")) return "MERGE";
        return "OTHER";
    }

    /**
     * Checks if the SQL is using a parameterized query (prepared statement pattern).
     */
    private static boolean isParameterized(String sql) {
        return sql != null && sql.contains("?");
    }

    /**
     * Counts the number of parameter placeholders in the SQL.
     */
    private static int countParameters(String sql) {
        if (sql == null) return 0;
        int count = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') count++;
        }
        return count;
    }

    /**
     * Computes a hash of normalized SQL for grouping similar queries.
     */
    private static String computeSqlHash(String sql) {
        if (sql == null) return null;
        try {
            // Normalize: remove extra whitespace, lowercase
            String normalized = sql.replaceAll("\\s+", " ").trim().toLowerCase();
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) { // Just first 8 bytes for brevity
                hexString.append(String.format("%02x", hash[i]));
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Intercepts method entry to log query details and record start time.
     * @param preparedStatement The PreparedStatement instance.
     * @return A context object containing start time and correlation ID.
     */
    @Advice.OnMethodEnter
    public static Object[] enter(@Advice.This Object preparedStatement) {
        // Enter MCC correlation scope
        MCCScope.enter("SQL");

        try {
            // Skip logging for agent's internal SQL operations
            if (isAgentInternalOperation(preparedStatement)) {
                return new Object[]{-1L, null, true}; // true = skip flag
            }
            String correlationId = GUID.generate();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("correlation_id", correlationId);
            metadata.put("class", preparedStatement.getClass().getName());

            // Retrieve SQL query from cache (captured at prepareStatement time)
            String sqlText = SQLStatementCache.get(preparedStatement);

            // Fallback to toString if not in cache (for directly created statements)
            if (sqlText == null || sqlText.isEmpty()) {
                try {
                    if (preparedStatement instanceof PreparedStatement) {
                        String toStringResult = preparedStatement.toString();
                        // Some drivers include SQL in toString, try to extract it
                        if (toStringResult != null && !toStringResult.contains("@")) {
                            sqlText = toStringResult;
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Add SQL text and derived metadata
            if (sqlText != null && !sqlText.isEmpty()) {
                metadata.put("sql_text", sqlText);
                metadata.put("sql_operation_type", extractSqlOperationType(sqlText));
                metadata.put("is_parameterized", String.valueOf(isParameterized(sqlText)));
                metadata.put("parameter_count", String.valueOf(countParameters(sqlText)));
                String sqlHash = computeSqlHash(sqlText);
                if (sqlHash != null) {
                    metadata.put("sql_hash", sqlHash);
                }
            } else {
                metadata.put("sql_text", "unavailable");
                metadata.put("sql_operation_type", "UNKNOWN");
            }

            // Add connection details if available
            if (preparedStatement instanceof PreparedStatement) {
                try {
                    java.sql.Connection conn = ((PreparedStatement) preparedStatement).getConnection();
                    java.sql.DatabaseMetaData dbMeta = conn.getMetaData();
                    metadata.put("db_url", dbMeta.getURL());
                    metadata.put("db_user", dbMeta.getUserName());

                    // Add schema name if available
                    try {
                        String schema = conn.getSchema();
                        if (schema != null) {
                            metadata.put("schema_name", schema);
                        }
                    } catch (Exception ignored) {
                        // Schema not supported by all drivers
                    }
                } catch (Exception ignored) {
                    // Ignore if connection metadata is unavailable
                }
            }

            logProxy.logMessage(SQL_NAMESPACE, "INFO", metadata);
            return new Object[]{System.nanoTime(), correlationId, false}; // false = don't skip
        } catch (Exception e) {
            // Log sensor error to platform log with full stacktrace
            Map<String, String> errorContext = new HashMap<>();
            errorContext.put("sensor", "SQLInterceptor");
            errorContext.put("phase", "enter");
            errorContext.put("class", preparedStatement.getClass().getName());
            logProxy.logPlatformError(errorContext, "Failed to process SQL query start", e);
            return new Object[]{-1L, null, false};
        }
    }

    /**
     * Intercepts method exit to log execution result and duration.
     * @param preparedStatement The PreparedStatement instance.
     * @param context The context object from enter (start time, correlation ID, skip flag).
     * @param result The method result (if any).
     * @param throwable The thrown exception, if any.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This Object preparedStatement,
                            @Advice.Enter Object[] context,
                            @Advice.Return Object result,
                            @Advice.Thrown Throwable throwable) {
        try {
            // Check skip flag - if this is an agent internal operation, skip logging
            if (context.length > 2 && context[2] != null && (Boolean) context[2]) {
                return;
            }

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
                String errorClass = throwable.getClass().getName();
                metadata.put("error_class", errorClass);

                // Add SQL state if available (for SQLExceptions)
                if (throwable instanceof java.sql.SQLException) {
                    java.sql.SQLException sqlEx = (java.sql.SQLException) throwable;
                    metadata.put("sql_state", sqlEx.getSQLState());
                    metadata.put("error_code", String.valueOf(sqlEx.getErrorCode()));
                }

                logProxy.logMessage(SQL_NAMESPACE, "ERROR", metadata);
            } else {
                metadata.put("status", "success");
                if (result instanceof java.sql.ResultSet) {
                    metadata.put("result_type", "ResultSet");
                } else if (result instanceof Integer) {
                    metadata.put("update_count", result.toString());
                    metadata.put("result_type", "UpdateCount");
                } else if (result instanceof int[]) {
                    // executeBatch returns int[]
                    int[] batchResult = (int[]) result;
                    metadata.put("batch_size", String.valueOf(batchResult.length));
                    metadata.put("result_type", "BatchResult");
                } else if (result instanceof Boolean) {
                    metadata.put("has_result_set", result.toString());
                    metadata.put("result_type", "Boolean");
                }
                logProxy.logMessage(SQL_NAMESPACE, "INFO", metadata);
            }

            // Log parameters at DEBUG level
            if (logProxy.isLoggingAtLevel(SQL_NAMESPACE, "DEBUG")) {
                Map<String, String> params = new HashMap<>();
                try {
                    // Parameter extraction is driver-specific and often not available
                    params.put("parameters", "unavailable");
                } catch (Exception e) {
                    params.put("param_error", "Unable to retrieve parameters: " + e.getMessage());
                }
                metadata.putAll(params);
                logProxy.logMessage(SQL_NAMESPACE, "DEBUG", metadata);
            }
        } catch (Exception e) {
            // Log sensor error to platform log with full stacktrace
            Map<String, String> errorContext = new HashMap<>();
            errorContext.put("sensor", "SQLInterceptor");
            errorContext.put("phase", "exit");
            errorContext.put("class", preparedStatement.getClass().getName());
            logProxy.logPlatformError(errorContext, "Failed to process SQL query exit", e);
        } finally {
            MCCScope.exit("SQL");
        }
    }

}