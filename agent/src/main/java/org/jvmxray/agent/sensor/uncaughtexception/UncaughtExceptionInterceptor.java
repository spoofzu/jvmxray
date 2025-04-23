package org.jvmxray.agent.sensor.uncaughtexception;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.proxy.ManagementProxy;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Interceptor class for monitoring and logging uncaught exceptions in the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code setUncaughtExceptionHandler}
 * method of the {@link Thread} class, wrapping the provided handler to log uncaught exception details.
 * Events are logged with contextual metadata using the {@link LogProxy}.
 * Note: This implementation is not yet fully operational and requires further development.
 *
 * @author Milton Smith
 */
public class UncaughtExceptionInterceptor {

    // Namespace for logging uncaught exception events
    public static final String NAMESPACE = "org.jvmxray.events.system.uncaughtexception";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code Thread.setUncaughtExceptionHandler} method to wrap the provided
     * handler with a logging handler. Note: This implementation is not yet fully operational and may
     * require adjustments to ensure proper handler wrapping and exception logging.
     *
     * @param thread The {@code Thread} instance on which the handler is being set.
     * @param handler The {@code UncaughtExceptionHandler} being set, or null if clearing the handler.
     */
    @Advice.OnMethodEnter
    public static void enter(
            @Advice.This Thread thread,
            @Advice.Argument(value = 0, optional = true) UncaughtExceptionHandler handler) {
        // Wrap the provided handler with a logging handler
        UncaughtExceptionHandler wrappedHandler = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // Log the uncaught exception details
                logUncaughtException(t, e);
                if (handler != null) {
                    // Delegate to the original handler if present
                    handler.uncaughtException(t, e);
                }
            }
        };
        // Set the wrapped handler on the thread
        thread.setUncaughtExceptionHandler(wrappedHandler);
    }

    /**
     * Logs details of an uncaught exception, including thread information, exception details,
     * stack trace, system management info, and active threads. Note: This implementation is not
     * yet fully operational and may require robust error handling and testing.
     *
     * @param thread    The {@code Thread} in which the uncaught exception occurred.
     * @param throwable The {@code Throwable} representing the uncaught exception.
     */
    private static void logUncaughtException(Thread thread, Throwable throwable) {
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("thread_name", thread.getName());
        metadata.put("thread_id", String.valueOf(thread.getId()));
        metadata.put("exception_type", throwable.getClass().getName());
        metadata.put("exception_message", throwable.getMessage() != null ?
                throwable.getMessage() : "No message");

        // Build stack trace as a string
        StringJoiner stackTrace = new StringJoiner("-->");
        for (StackTraceElement element : throwable.getStackTrace()) {
            stackTrace.add(element.toString());
        }
        metadata.put("stack_trace", stackTrace.toString());

        // Add system management information
        try {
            Map<String, String> managementInfo = ManagementProxy.getManagementInfo();
            managementInfo.forEach(metadata::put);
        } catch (Throwable t) {
            t.printStackTrace();
            metadata.put("management_error", "Error: " + t.toString());
        }

        // Add information about all active threads
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        StringBuilder threadsInfo = new StringBuilder();
        for (Thread t : allThreads) {
            threadsInfo.append(String.format("Thread[name=%s, id=%d, state=%s], ",
                    t.getName(), t.getId(), t.getState().toString()));
        }
        metadata.put("threads", threadsInfo.toString());

        // Log the uncaught exception event
        logProxy.logEvent(NAMESPACE, "INFO", metadata);
    }
}