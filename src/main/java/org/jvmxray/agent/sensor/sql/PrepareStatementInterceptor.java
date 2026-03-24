package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.sql.PreparedStatement;

/**
 * Interceptor for Connection.prepareStatement() to capture SQL text at creation time.
 * Associates the SQL text with the PreparedStatement instance for later retrieval
 * when the statement is executed.
 *
 * @author Milton Smith
 */
public class PrepareStatementInterceptor {

    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the exit of prepareStatement methods to capture SQL text.
     *
     * @param sql The SQL text passed to prepareStatement
     * @param result The PreparedStatement instance returned
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Argument(0) String sql,
                            @Advice.Return PreparedStatement result) {
        try {
            if (result != null && sql != null && !sql.trim().isEmpty()) {
                // Skip agent's internal SQL operations
                String className = result.getClass().getName();
                if (className.contains("jvmxray") || className.contains("shaded")) {
                    return;
                }

                // Cache the SQL text for this PreparedStatement
                SQLStatementCache.put(result, sql);
            }
        } catch (Exception e) {
            // Silently ignore errors to avoid breaking the application
        }
    }
}
