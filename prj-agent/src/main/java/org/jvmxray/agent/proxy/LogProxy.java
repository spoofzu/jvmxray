package org.jvmxray.agent.proxy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Proxy class for logging events via reflection to the AgentLogger singleton instance.
 * Provides a layer of abstraction to interact with the AgentLogger, handling initialization,
 * logging, and shutdown operations.
 *
 * @author Milton Smith
 */
public class LogProxy {
    // Singleton instance of LogProxy
    private static final LogProxy INSTANCE = new LogProxy();
    // Package prefix for JVMXRay classes
    private static final String AGENT_PACKAGE = "org.jvmxray";
    // Framework prefixes to filter out from stack traces
    private static final Set<String> FRAMEWORK_PREFIXES = new HashSet<>(Set.of(
            "java.",
            "javax.",
            "sun.",
            "jdk.",
            "net.bytebuddy"
    ));

    // Reference to AgentLogger class
    private static Class<?> agentLoggerClass;
    // AgentLogger singleton instance
    private static Object agentLoggerInstance;
    // Method reference for logging events
    private static java.lang.reflect.Method logEventMethod;
    // Method reference for shutting down the logger
    private static java.lang.reflect.Method shutdownMethod;
    // Method reference for checking log level
    private static java.lang.reflect.Method isLoggingAtLevelMethod;

    /**
     * Static initializer to load and configure the AgentLogger class and its methods.
     */
    static {
        try {
            System.out.println("LogProxy: Attempting to load AgentLogger...");
            // Load AgentLogger class
            agentLoggerClass = ClassLoader.getSystemClassLoader()
                    .loadClass("org.jvmxray.agent.proxy.AgentLogger");
            System.out.println("LogProxy: AgentLogger loaded successfully.");

            // Get the singleton instance via getInstance()
            java.lang.reflect.Method getInstanceMethod = agentLoggerClass.getMethod("getInstance");
            agentLoggerInstance = getInstanceMethod.invoke(null);

            // Initialize method references
            logEventMethod = agentLoggerClass.getMethod("logEvent",
                    String.class, String.class, Map.class);
            shutdownMethod = agentLoggerClass.getMethod("shutdown");
            isLoggingAtLevelMethod = agentLoggerClass.getMethod("isLoggingAtLevel",
                    String.class, String.class);
            System.out.println("LogProxy: Methods initialized successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("LogProxy: Failed to find AgentLogger class: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("LogProxy: Failed to find method in AgentLogger: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("LogProxy: Unexpected error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private LogProxy() {
        // Private constructor for singleton
    }

    /**
     * Returns the singleton instance of LogProxy.
     *
     * @return The LogProxy instance.
     */
    public static LogProxy getInstance() {
        return INSTANCE;
    }

    /**
     * Shuts down the AgentLogger instance, releasing any resources.
     */
    public static synchronized void shutdown() {
        try {
            // Invoke shutdown method if initialized
            if (shutdownMethod != null && agentLoggerInstance != null) {
                shutdownMethod.invoke(agentLoggerInstance);
            } else {
                System.err.println("LogProxy: Cannot shutdown - shutdownMethod or instance not initialized. Check startup logs.");
            }
        } catch (Exception e) {
            System.err.println("LogProxy: Failed to shutdown AgentLogger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs an event using the AgentLogger instance.  Log messages are enriched with caller information,
     * application id, and category id.
     *
     * <emphasis>Note: this is a convenience method for platform logging messages.  Sensors should
     * normally log using logEvent(String,String,Map) to log keyvalue pairs.</emphasis>
     *
     * @param namespace The logging namespace.
     * @param level The log level (e.g., DEBUG, INFO, ERROR).
     * @param message Message to log.
     */
    public synchronized void logMessage(String namespace, String level, String message) {
        try {
            // Capture caller information
            String caller = captureCallerInfo(namespace);
            // Enrich metadata with caller info
            Map<String, String> metadata = new HashMap<>();
            metadata.put("caller", caller);
            metadata.put("message",message);
            // Invoke logEvent method if initialized
            if (logEventMethod != null && agentLoggerInstance != null) {
                logEventMethod.invoke(agentLoggerInstance, namespace, level, metadata);
            } else {
                System.err.println("LogProxy: Cannot log event - logEventMethod or instance not initialized. Check startup logs.");
            }
        } catch (Exception e) {
            System.err.println("LogProxy: Failed to log event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs an event with an exception using the AgentLogger instance. Log messages are enriched with
     * caller information, application id, category id, and exception details.
     *
     * <emphasis>Note: this is a convenience method for platform logging messages with exceptions.
     * Sensors should normally log using logEvent(String,String,Map) to log keyvalue pairs.</emphasis>
     *
     * @param namespace The logging namespace.
     * @param level The log level (e.g., DEBUG, INFO, ERROR).
     * @param message Message to log.
     * @param exception The exception to log.
     */
    public synchronized void logMessageWithException(String namespace, String level, String message, Throwable exception) {
        try {
            // Capture caller information
            String caller = captureCallerInfo(namespace);
            // Enrich metadata with caller info and exception details
            Map<String, String> metadata = new HashMap<>();
            metadata.put("caller", caller);
            metadata.put("message", message);
            metadata.put("exception", exception.getClass().getName() + ": " + exception.getMessage());
            metadata.put("stacktrace", getStackTraceAsString(exception));
            // Invoke logEvent method if initialized
            if (logEventMethod != null && agentLoggerInstance != null) {
                logEventMethod.invoke(agentLoggerInstance, namespace, level, metadata);
            } else {
                System.err.println("LogProxy: Cannot log event - logEventMethod or instance not initialized. Check startup logs.");
            }
        } catch (Exception e) {
            System.err.println("LogProxy: Failed to log event with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs an event using the AgentLogger instance.  Log messages are enriched with caller information,
     * application id, category id, and MCC (Mapped Correlation Context) data.
     *
     * <p>MCC integration: All correlation context from the current thread (trace_id, user_id, session_id, etc.)
     * is automatically merged into the event metadata, enabling event correlation across execution paths.</p>
     *
     * @param namespace The logging namespace.
     * @param level The log level (e.g., DEBUG, INFO, ERROR).
     * @param metadata Metadata associated with the log event.
     */
    public synchronized void logMessage(String namespace, String level, Map<String, String> metadata) {
        try {
            // Capture caller information
            String caller = captureCallerInfo(namespace);
            // Enrich metadata with caller info
            Map<String, String> enrichedMetadata = new HashMap<>(metadata);
            enrichedMetadata.put("caller", caller);

            // Merge MCC (Mapped Correlation Context) data
            // MCC provides correlation fields like trace_id, user_id, session_id, parent_thread_id
            // These enable event correlation for security analysis and forensics
            // Note: Sensors manage MCC lifecycle via enterScope/exitScope - LogProxy only reads context
            try {
                Class<?> mccClass = Class.forName("org.jvmxray.platform.shared.util.MCC");

                // Get MCC context and merge into event metadata (passive read only)
                java.lang.reflect.Method getCopyOfContextMethod = mccClass.getMethod("getCopyOfContext");
                @SuppressWarnings("unchecked")
                Map<String, String> mccContext = (Map<String, String>) getCopyOfContextMethod.invoke(null);

                if (mccContext != null && !mccContext.isEmpty()) {
                    // MCC values do not override explicit sensor metadata
                    // This allows sensors to provide specific values that take precedence
                    for (Map.Entry<String, String> entry : mccContext.entrySet()) {
                        enrichedMetadata.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            } catch (ClassNotFoundException e) {
                // MCC not available - this is expected in minimal deployments
                // No action needed, events will just lack correlation context
            } catch (Exception e) {
                // MCC integration failed - log but don't fail the event
                System.err.println("LogProxy: Warning - Failed to merge MCC context: " + e.getMessage());
            }

            // Invoke logEvent method if initialized
            if (logEventMethod != null && agentLoggerInstance != null) {
                logEventMethod.invoke(agentLoggerInstance, namespace, level, enrichedMetadata);
            } else {
                System.err.println("LogProxy: Cannot log event - logEventMethod or instance not initialized. Check startup logs.");
            }
        } catch (Exception e) {
            System.err.println("LogProxy: Failed to log event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if logging is enabled for the specified namespace and log level.
     *
     * @param namespace The logging namespace.
     * @param level The log level to check (e.g., DEBUG, INFO, ERROR).
     * @return {@code true} if logging is enabled at the specified level, {@code false} otherwise.
     */
    public synchronized boolean isLoggingAtLevel(String namespace, String level) {
        boolean result = false;
        try {
            // Invoke isLoggingAtLevel method if initialized
            if (isLoggingAtLevelMethod != null && agentLoggerInstance != null) {
                if (namespace != null && level != null) {
                    result = (boolean) isLoggingAtLevelMethod.invoke(agentLoggerInstance, namespace, level);
                } else {
                    System.err.println("LogProxy: Null param(s) passed to isLoggingAtLevel. namespace=" + namespace + " level=" + level);
                }
            } else {
                System.err.println("LogProxy: isLoggingAtLevel method or instance not initialized. Check startup logs.");
            }
        } catch (Exception e) {
            System.err.println("LogProxy: Failed to retrieve log priority level: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Captures caller information from the stack trace, excluding framework and agent classes.
     *
     * Filters out:
     * - JDK packages (java.*, javax.*, sun.*, jdk.*)
     * - ByteBuddy framework (net.bytebuddy.*)
     * - JVMXRay agent code (org.jvmxray.* except test/integration packages)
     *
     * Keeps:
     * - User application code
     * - Test code (org.jvmxray packages containing .test. or .integration.)
     *
     * @param namespace The logging namespace (for context, not used in logic).
     * @return A string in the format "className:lineNumber" representing the caller, or "unknown:0" if not found.
     */
    private String captureCallerInfo(String namespace) {
        String result = "unknown:0";
        // Get stack trace
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            boolean isFrameworkFrame = false;

            // Check if the class belongs to a framework package
            for (String prefix : FRAMEWORK_PREFIXES) {
                if (className.startsWith(prefix)) {
                    isFrameworkFrame = true;
                    break;
                }
            }

            // Check if it's JVMXRay code
            boolean isJvmxrayCode = className.startsWith(AGENT_PACKAGE);
            boolean isTestCode = false;

            if (isJvmxrayCode) {
                // Keep test and integration packages
                isTestCode = className.contains(".test.") || className.contains(".integration.");
            }

            // Select the first non-framework class that is either:
            // 1. Not JVMXRay code, OR
            // 2. JVMXRay test/integration code
            if (!isFrameworkFrame && (!isJvmxrayCode || isTestCode)) {
                result = className + ":" + element.getLineNumber();
                break;
            }
        }
        return result;
    }

    /**
     * Converts an exception's stack trace to a string.
     *
     * @param throwable The exception to convert.
     * @return The stack trace as a string.
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

}