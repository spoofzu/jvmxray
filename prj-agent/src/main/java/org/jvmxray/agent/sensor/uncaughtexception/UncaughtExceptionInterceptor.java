package org.jvmxray.agent.sensor.uncaughtexception;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.proxy.ManagementProxy;
import org.jvmxray.agent.util.SystemStateCollector;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Interceptor class for monitoring and logging uncaught exceptions in the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code dispatchUncaughtException}
 * method of the {@link Thread} class to log uncaught exception details with comprehensive diagnostics.
 * Provides bug tracking capabilities similar to commercial services like Bugsnag.
 *
 * @author Milton Smith
 */
public class UncaughtExceptionInterceptor {

    // Namespace for logging uncaught exception events
    public static final String NAMESPACE = "org.jvmxray.events.system.uncaughtexception";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code Thread.dispatchUncaughtException} method
     * to log uncaught exception details before the exception is handled.
     *
     * @param thread The {@code Thread} instance where the exception occurred.
     * @param throwable The uncaught exception.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This Thread thread, @Advice.Argument(0) Throwable throwable) {
        try {
            // Log the uncaught exception details
            logUncaughtException(thread, throwable);
        } catch (Throwable t) {
            // Ensure logging errors don't interfere with exception handling
            System.err.println("Error logging uncaught exception: " + t.getMessage());
        }
    }

    /**
     * Logs comprehensive details of an uncaught exception with level-based detail.
     * INFO level provides essential diagnostics for quick analysis.
     * DEBUG level includes comprehensive system state for deep debugging.
     * Provides diagnostics capabilities similar to commercial bug tracking services.
     *
     * @param thread    The {@code Thread} in which the uncaught exception occurred.
     * @param throwable The {@code Throwable} representing the uncaught exception.
     */
    public static void logUncaughtException(Thread thread, Throwable throwable) {
        // Check logging levels early to optimize data collection
        boolean isDebug = logProxy.isLoggingAtLevel(NAMESPACE, "DEBUG");
        boolean isInfo = logProxy.isLoggingAtLevel(NAMESPACE, "INFO");
        
        // Skip if neither INFO nor DEBUG is enabled
        if (!isInfo && !isDebug) {
            return;
        }
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        
        // === ALWAYS INCLUDED (INFO & DEBUG) ===
        
        // Basic thread and exception information
        metadata.put("thread_name", thread.getName());
        metadata.put("thread_id", String.valueOf(thread.getId()));
        metadata.put("thread_state", thread.getState().toString());
        metadata.put("thread_priority", String.valueOf(thread.getPriority()));
        metadata.put("thread_daemon", String.valueOf(thread.isDaemon()));
        
        // Thread group information
        ThreadGroup group = thread.getThreadGroup();
        if (group != null) {
            metadata.put("thread_group", group.getName());
        }
        
        metadata.put("exception_type", throwable.getClass().getName());
        metadata.put("exception_message", throwable.getMessage() != null ? 
                throwable.getMessage() : "No message");
        
        // Exception location - first non-JDK frame for quick identification
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > 0) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.startsWith("java.") && !className.startsWith("javax.") && 
                    !className.startsWith("sun.") && !className.startsWith("com.sun.")) {
                    metadata.put("exception_location", className + ":" + element.getLineNumber());
                    metadata.put("exception_method", element.getMethodName());
                    break;
                }
            }
            // Stack depth for quick assessment
            metadata.put("stack_depth", String.valueOf(stackTrace.length));
        }
        
        // Command line and basic memory (essential for diagnosis)
        metadata.putAll(SystemStateCollector.collectCommandLine());
        Map<String, String> memoryStats = SystemStateCollector.collectMemoryStats(false);
        metadata.putAll(memoryStats);
        
        // Incident tracking
        metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
        metadata.put("incident_id", java.util.UUID.randomUUID().toString());
        
        // === INFO LEVEL DATA ===
        if (isInfo || isDebug) {
            // Simplified stack trace (first 10 frames for INFO)
            StringJoiner stackTraceJoiner = new StringJoiner("\n    at ");
            stackTraceJoiner.add(""); // Start with empty to get "at" prefix on first line
            int maxFrames = isDebug ? stackTrace.length : Math.min(10, stackTrace.length);
            for (int i = 0; i < maxFrames; i++) {
                stackTraceJoiner.add(stackTrace[i].toString());
            }
            if (!isDebug && stackTrace.length > 10) {
                stackTraceJoiner.add("... " + (stackTrace.length - 10) + " more");
            }
            metadata.put("stack_trace", stackTraceJoiner.toString());
            
            // Root cause (essential for understanding)
            Throwable rootCause = throwable;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            if (rootCause != throwable) {
                metadata.put("root_cause_type", rootCause.getClass().getName());
                metadata.put("root_cause_message", 
                    rootCause.getMessage() != null ? rootCause.getMessage() : "No message");
            }
            
            // Basic thread state count
            metadata.putAll(SystemStateCollector.collectThreadStats(false));
            
            // Basic runtime info
            Map<String, String> runtimeInfo = SystemStateCollector.collectRuntimeInfo();
            metadata.put("jvm_uptime_ms", runtimeInfo.get("uptime_ms"));
        }
        
        // === DEBUG LEVEL DATA ===
        if (isDebug) {
            // Full cause chain analysis
            Throwable cause = throwable.getCause();
            int causeLevel = 0;
            while (cause != null && causeLevel < 10) { // Limit depth to prevent infinite loops
                causeLevel++;
                metadata.put("cause_" + causeLevel + "_type", cause.getClass().getName());
                metadata.put("cause_" + causeLevel + "_message", 
                        cause.getMessage() != null ? cause.getMessage() : "No message");
                // Include first frame of each cause
                if (cause.getStackTrace().length > 0) {
                    metadata.put("cause_" + causeLevel + "_location", 
                        cause.getStackTrace()[0].toString());
                }
                cause = cause.getCause();
            }
            metadata.put("cause_chain_length", String.valueOf(causeLevel));
        
            // Suppressed exceptions
            Throwable[] suppressed = throwable.getSuppressed();
            if (suppressed.length > 0) {
                metadata.put("suppressed_count", String.valueOf(suppressed.length));
                StringBuilder suppressedInfo = new StringBuilder();
                for (int i = 0; i < Math.min(suppressed.length, 5); i++) { // Limit to 5 suppressed exceptions
                    if (i > 0) suppressedInfo.append("; ");
                    suppressedInfo.append(suppressed[i].getClass().getSimpleName())
                            .append(": ").append(suppressed[i].getMessage());
                }
                metadata.put("suppressed_exceptions", suppressedInfo.toString());
            }

            // Detailed memory stats
            Map<String, String> detailedMemory = SystemStateCollector.collectMemoryStats(true);
            detailedMemory.forEach((key, value) -> {
                if (!metadata.containsKey(key)) { // Don't duplicate basic memory stats
                    metadata.put(key, value);
                }
            });
            
            // GC statistics
            metadata.putAll(SystemStateCollector.collectGCStats());
            
            // Class loading statistics
            metadata.putAll(SystemStateCollector.collectClassLoadingStats());
            
            // Operating system info
            metadata.putAll(SystemStateCollector.collectOSInfo());
            
            // Security info
            metadata.putAll(SystemStateCollector.collectSecurityInfo());
            
            // Disk space
            metadata.putAll(SystemStateCollector.collectDiskSpace());
            
            // Detailed thread information
            metadata.putAll(SystemStateCollector.collectThreadInfo(thread));
            metadata.putAll(SystemStateCollector.collectThreadStats(true));
            
            // Full runtime info
            Map<String, String> fullRuntime = SystemStateCollector.collectRuntimeInfo();
            fullRuntime.forEach((key, value) -> {
                if (!metadata.containsKey(key)) {
                    metadata.put(key, value);
                }
            });

            // Full system management information (processes, etc.)
            try {
                Map<String, String> managementInfo = ManagementProxy.getManagementInfo();
                if (managementInfo != null) {
                    managementInfo.forEach((key, value) -> {
                        if (key != null && value != null && !metadata.containsKey("system_" + key)) {
                            metadata.put("system_" + key, value);
                        }
                    });
                }
            } catch (Throwable t) {
                metadata.put("management_error", "Failed to get system info: " + t.getClass().getSimpleName());
            }
        }
        
        // Log at appropriate level
        String logLevel = isDebug ? "DEBUG" : "INFO";
        logProxy.logMessage(NAMESPACE, logLevel, metadata);
    }
}