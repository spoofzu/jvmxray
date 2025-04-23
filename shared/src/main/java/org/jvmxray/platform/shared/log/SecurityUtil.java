package org.jvmxray.platform.shared.log;

import org.slf4j.Logger;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Utility class for logging system properties, environment variables, and command-line
 * arguments in the JVMXRay framework. Supports redirecting {@code System.out} and
 * {@code System.err} streams to SLF4J loggers and provides customizable logging via
 * a {@link Consumer} handler.
 *
 * <p>This class integrates with JVMXRay's logging infrastructure to capture and log
 * system information, enhancing debugging and monitoring capabilities.</p>
 *
 * @author Milton Smith
 */
public class SecurityUtil {

    // Default logger for general logging operations
    private static final Logger defaultLogger = JVMXRayLogFactory.getInstance().getLogger(SecurityUtil.class.getName());
    // Logger for redirecting System.out
    private static Logger sysOutLogger = JVMXRayLogFactory.getInstance().getLogger(SecurityUtil.class.getName());
    // Logger for redirecting System.err
    private static Logger sysErrLogger = JVMXRayLogFactory.getInstance().getLogger(SecurityUtil.class.getName());

    // Original System.out stream for restoration
    public static final PrintStream sysout = System.out;
    // Original System.err stream for restoration
    public static final PrintStream syserr = System.err;

    // Handler for logging messages, allowing custom logging behavior
    private final Consumer<String> logMessageHandler;

    /**
     * Constructs a {@code SecurityUtil} instance with the default SLF4J logger
     * for logging messages.
     */
    public SecurityUtil() {
        // Use default logger with INFO level
        this(message -> defaultLogger.info(message));
    }

    /**
     * Constructs a {@code SecurityUtil} instance with a custom logging handler.
     *
     * @param logMessageHandler The {@link Consumer} to handle log messages, or null
     *                          to use the default logger.
     */
    public SecurityUtil(Consumer<String> logMessageHandler) {
        // Set the logging handler, falling back to default logger if null
        this.logMessageHandler = logMessageHandler != null ? logMessageHandler : message -> defaultLogger.info(message);
    }

    /**
     * Redirects {@code System.out} and {@code System.err} streams to SLF4J loggers
     * using the configured loggers for this instance.
     */
    public void bindSystemStreamsToSLF4J() {
        // Redirect System.out to SLF4J logger (INFO level)
        System.setOut(new PrintStream(new OutputStreamRedirector(sysOutLogger, false), true));
        // Redirect System.err to SLF4J logger (ERROR level)
        System.setErr(new PrintStream(new OutputStreamRedirector(sysErrLogger, true), true));
    }

    /**
     * Redirects {@code System.out} and {@code System.err} streams to the specified
     * SLF4J loggers, updating the configured loggers if provided.
     *
     * @param newSysOutLogger The {@link Logger} for {@code System.out}, or null to retain the current logger.
     * @param newSysErrLogger The {@link Logger} for {@code System.err}, or null to retain the current logger.
     */
    public void bindSystemStreamsToSLF4J(Logger newSysOutLogger, Logger newSysErrLogger) {
        // Update System.out logger if provided
        if (newSysOutLogger != null) {
            SecurityUtil.sysOutLogger = newSysOutLogger;
        }
        // Update System.err logger if provided
        if (newSysErrLogger != null) {
            SecurityUtil.sysErrLogger = newSysErrLogger;
        }
        // Redirect streams to SLF4J
        bindSystemStreamsToSLF4J();
    }

    /**
     * Restores the original {@code System.out} and {@code System.err} streams.
     */
    public void unbindSystemStreams() {
        // Restore original System.out
        System.setOut(sysout);
        // Restore original System.err
        System.setErr(syserr);
    }

    /**
     * Logs command-line arguments with their indices.
     *
     * @param args The command-line arguments to log.
     */
    public void logCommandLineArguments(String[] args) {
        // Skip if args is null or empty
        if (args == null || args.length < 1) {
            return;
        }
        // Log each argument with its index
        for (int i = 0; i < args.length; i++) {
            String arg = args[i] != null ? args[i] : "null";
            logMessage("Cmd line arg[" + i + "]=" + arg);
        }
    }

    /**
     * Logs shell environment variables associated with the Java process.
     */
    public void logShellEnvironmentVariables() {
        // Get environment variables
        Map<String, String> env = System.getenv();
        // Iterate over variable keys
        Iterator<String> keys = env.keySet().iterator();
        // Log each variable
        while (keys.hasNext()) {
            String key = keys.next();
            String value = env.get(key);
            logMessage("Environment Setting: " + key + "=" + value.trim());
        }
    }

    /**
     * Logs Java system properties.
     */
    public void logJavaSystemProperties() {
        // Get system properties
        Properties properties = System.getProperties();
        // Iterate over property keys
        Iterator<Object> keys = properties.keySet().iterator();
        // Log each property
        while (keys.hasNext()) {
            Object key = keys.next();
            Object value = properties.get(key);
            logMessage("System Property: " + key + "=" + value.toString().trim());
        }
    }

    /**
     * Logs a message using the configured or default logging handler.
     *
     * @param message The message to log.
     */
    public void logMessage(String message) {
        // Pass the message to the logging handler
        if (message == null) {
            return;
        }
        logMessageHandler.accept(message);
    }
}