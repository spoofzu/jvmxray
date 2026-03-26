package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.asm.Advice;

/**
 * Interceptor for Statement.close() to clean up the SQL statement cache.
 * Removes the SQL text association when a statement is closed.
 *
 * @author Milton Smith
 */
public class StatementCloseInterceptor {

    /**
     * Intercepts the entry of close() to remove the statement from cache.
     * We do this on entry rather than exit to ensure cleanup happens even if close() throws.
     *
     * @param statement The Statement instance being closed
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This Object statement) {
        try {
            SQLStatementCache.remove(statement);
        } catch (Exception e) {
            // Silently ignore errors
        }
    }
}
