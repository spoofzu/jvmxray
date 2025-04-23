package org.jvmxray.agent.util.log;

import org.jvmxray.platform.shared.log.JVMXRayLogFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton logger for the JVMXRay agent framework, responsible for handling logging
 * of events with configurable modes: buffered (via a worker thread), direct to
 * {@code System.out}, or direct to an SLF4J logger. Supports asynchronous event
 * logging with a queue in buffered mode and provides methods to check logging levels.
 *
 * @author Milton Smith
 */
public class AgentLogger {

    // Singleton instance of the logger
    private static final AgentLogger INSTANCE;
    // Queue for buffered log events (used in BUFFERED mode)
    private final BlockingQueue<LogEvent> queue;
    // Worker thread for processing buffered log events
    private final Thread workerThread;
    // Flag to control the worker thread's lifecycle
    private final AtomicBoolean running = new AtomicBoolean(true);
    // Configuration settings for logging mode, buffer size, and flush interval
    private final Config config;
    // SLF4J logger for internal AgentLogger errors and warnings
    private final Logger logger;

    /**
     * Enum defining the logging modes supported by {@code AgentLogger}.
     */
    public enum MODE {
        BUFFERED,    // Buffered mode with worker thread and queue
        SYSTEM,      // Direct output to System.out
        LOGBACK      // Direct output to SLF4J logger
    }

    // Default logging mode
    private static final MODE ASSIGNEDMODE = MODE.BUFFERED;

    /**
     * Configuration class for {@code AgentLogger}, specifying buffer size, flush interval,
     * and logging mode.
     */
    public static class Config {
        // Size of the event queue in BUFFERED mode
        private final int bufferSize;
        // Interval for flushing queued events (milliseconds)
        private final long flushIntervalMs;
        // Logging mode (BUFFERED, SYSTEM, or LOGBACK)
        private final MODE mode;

        /**
         * Constructs a new {@code Config} with the specified parameters.
         *
         * @param bufferSize      The size of the event queue in BUFFERED mode.
         * @param flushIntervalMs The interval for flushing queued events (milliseconds).
         * @param mode            The logging mode.
         */
        public Config(int bufferSize, long flushIntervalMs, MODE mode) {
            this.bufferSize = bufferSize;
            this.flushIntervalMs = flushIntervalMs;
            this.mode = mode;
        }

        /**
         * Returns the default configuration for {@code AgentLogger}.
         *
         * @return A {@code Config} instance with default settings.
         */
        public static Config defaultConfig() {
            return new Config(1000, 100, ASSIGNEDMODE);
        }
    }

    /**
     * Represents a log event with a namespace, log level, and metadata.
     */
    private static class LogEvent {
        // Namespace for the log event (e.g., sensor-specific identifier)
        final String namespace;
        // Log level (e.g., TRACE, DEBUG, INFO, WARN, ERROR)
        final String level;
        // Metadata key-value pairs for the event
        final Map<String, String> metadata;

        /**
         * Constructs a new {@code LogEvent}.
         *
         * @param namespace The namespace for the event.
         * @param level     The log level.
         * @param metadata  The metadata key-value pairs.
         */
        LogEvent(String namespace, String level, Map<String, String> metadata) {
            this.namespace = namespace;
            this.level = level;
            this.metadata = metadata;
        }
    }

    /**
     * Initializes the singleton instance of {@code AgentLogger}.
     */
    static {
        AgentLogger tempInstance = null;
        try {
            tempInstance = new AgentLogger(Config.defaultConfig());
        } catch (Exception e) {
            // Log initialization failure to stderr
            System.err.println("AgentLogger: Failed to initialize singleton: " + e.getMessage());
            e.printStackTrace();
        }
        INSTANCE = tempInstance;
    }

    /**
     * Constructs a new {@code AgentLogger} with the specified configuration.
     *
     * @param config The configuration settings, or null to use default settings.
     */
    private AgentLogger(Config config) {
        // Use provided config or fall back to default
        this.config = config != null ? config : Config.defaultConfig();
        // Initialize SLF4J logger for internal errors
        this.logger = JVMXRayLogFactory.getInstance().getLogger(AgentLogger.class.getName());
        // Initialize queue for BUFFERED mode
        this.queue = MODE.BUFFERED.equals(this.config.mode) ?
                new ArrayBlockingQueue<>(this.config.bufferSize) : null;
        // Initialize worker thread for BUFFERED mode
        this.workerThread = MODE.BUFFERED.equals(this.config.mode) ?
                new Thread(this::workerLoop, "jvmxray.sensor-1") : null;
        if (workerThread != null) {
            // Start daemon thread for processing queued events
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    /**
     * Returns the singleton instance of {@code AgentLogger}.
     *
     * @return The {@code AgentLogger} instance.
     * @throws IllegalStateException If the singleton failed to initialize.
     */
    public static AgentLogger getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AgentLogger singleton failed to initialize. Check startup logs.");
        }
        return INSTANCE;
    }

    /**
     * Logs an event with the specified namespace, level, and metadata.
     *
     * @param namespace The namespace for the event (e.g., sensor-specific identifier).
     * @param level     The log level (e.g., TRACE, DEBUG, INFO, WARN, ERROR).
     * @param metadata  The metadata key-value pairs for the event.
     */
    public void logEvent(String namespace, String level, Map<String, String> metadata) {
        handleEvent0(namespace, level, metadata);
    }

    /**
     * Checks if logging is enabled at the specified level for the given namespace.
     *
     * @param namespace The namespace to check.
     * @param level     The log level to check (e.g., TRACE, DEBUG, INFO, WARN, ERROR).
     * @return {@code true} if logging is enabled at the specified level, {@code false} otherwise.
     */
    public boolean isLoggingAtLevel(String namespace, String level) {
        return isLoggingAtLevel0(namespace, level);
    }

    /**
     * Internal method to check if logging is enabled at the specified level.
     *
     * @param namespace The namespace to check.
     * @param level     The log level to check.
     * @return {@code true} if logging is enabled, {@code false} otherwise.
     */
    private boolean isLoggingAtLevel0(String namespace, String level) {
        boolean result = false;
        switch (config.mode) {
            case BUFFERED:
                // Check SLF4J logger for BUFFERED mode
                result = handleIsLogbackLoggingAtLevel(namespace, level);
                break;
            case SYSTEM:
                // TODO: Implement command-line setting for SYSTEM mode
                result = false;
                break;
            case LOGBACK:
                // Check SLF4J logger for LOGBACK mode
                result = handleIsLogbackLoggingAtLevel(namespace, level);
                break;
        }
        return result;
    }

    /**
     * Internal method to handle event logging based on the configured mode.
     *
     * @param namespace The namespace for the event.
     * @param level     The log level.
     * @param metadata  The metadata key-value pairs.
     */
    private void handleEvent0(String namespace, String level, Map<String, String> metadata) {
        switch (config.mode) {
            case BUFFERED:
                // Enqueue event for asynchronous processing
                enqueueEvent(namespace, level, metadata);
                break;
            case SYSTEM:
                // Log directly to System.out
                handleSystemOutput(namespace, level, metadata);
                break;
            case LOGBACK:
                // Log directly to SLF4J logger
                handleLogbackOutput(namespace, level, metadata);
                break;
        }
    }

    /**
     * Enqueues an event for processing in BUFFERED mode.
     *
     * @param namespace The namespace for the event.
     * @param level     The log level.
     * @param metadata  The metadata key-value pairs.
     */
    private void enqueueEvent(String namespace, String level, Map<String, String> metadata) {
        if (queue != null && !queue.offer(new LogEvent(namespace, level, metadata))) {
            // Log warning if queue is full
            logger.warn("Log queue full, dropping event for namespace: {}", namespace);
        }
    }

    /**
     * Logs an event directly to {@code System.out} in SYSTEM mode.
     *
     * @param namespace The namespace for the event.
     * @param level     The log level.
     * @param metadata  The metadata key-value pairs.
     */
    private void handleSystemOutput(String namespace, String level, Map<String, String> metadata) {
        String message = String.format("%s [%s] %s", namespace, level, formatMetadata(metadata));
        System.out.println(message);
    }

    /**
     * Logs an event using an SLF4J logger in LOGBACK mode.
     *
     * @param namespace The namespace for the event.
     * @param level     The log level.
     * @param metadata  The metadata key-value pairs.
     */
    private void handleLogbackOutput(String namespace, String level, Map<String, String> metadata) {
        String message = String.format("%s", formatMetadata(metadata));
        Logger eventLogger = JVMXRayLogFactory.getInstance().getLogger(namespace);
        switch (level.toUpperCase()) {
            case "TRACE":
                eventLogger.trace(message);
                break;
            case "DEBUG":
                eventLogger.debug(message);
                break;
            case "INFO":
                eventLogger.info(message);
                break;
            case "WARN":
                eventLogger.warn(message);
                break;
            case "ERROR":
                eventLogger.error(message);
                break;
            default:
                eventLogger.info(message);
        }
    }

    /**
     * Checks if an SLF4J logger is enabled for the specified level.
     *
     * @param namespace The namespace for the logger.
     * @param level     The log level to check.
     * @return {@code true} if the level is enabled, {@code false} otherwise.
     */
    private boolean handleIsLogbackLoggingAtLevel(String namespace, String level) {
        Logger eventLogger = JVMXRayLogFactory.getInstance().getLogger(namespace);
        boolean result = false;
        switch (level.toUpperCase()) {
            case "TRACE":
                result = eventLogger.isTraceEnabled();
                break;
            case "DEBUG":
                result = eventLogger.isDebugEnabled();
                break;
            case "INFO":
                result = eventLogger.isInfoEnabled();
                break;
            case "WARN":
                result = eventLogger.isWarnEnabled();
                break;
            case "ERROR":
                result = eventLogger.isErrorEnabled();
                break;
        }
        return result;
    }

    /**
     * Runs the worker thread loop for processing queued events in BUFFERED mode.
     */
    private void workerLoop() {
        while (running.get()) {
            try {
                // Process queued events
                processQueue();
                // Sleep for configured flush interval
                Thread.sleep(config.flushIntervalMs);
            } catch (InterruptedException e) {
                // Restore interrupted status and stop the loop
                Thread.currentThread().interrupt();
                running.set(false);
            } catch (Exception e) {
                // Log worker errors
                logger.error("Error in log worker", e);
            }
        }
    }

    /**
     * Processes all available events in the queue.
     */
    private void processQueue() {
        if (queue == null) return;

        LogEvent event;
        while ((event = queue.poll()) != null) {
            try {
                // Process each event using LOGBACK mode
                handleLogbackOutput(event.namespace, event.level, event.metadata);
            } catch (Exception e) {
                // Log processing errors
                logger.error("Failed to process log event: {}", event.namespace, e);
            }
        }
    }

    /**
     * Formats metadata key-value pairs into a string for logging.
     *
     * @param metadata The metadata key-value pairs.
     * @return A formatted string of the metadata.
     */
    private String formatMetadata(Map<String, String> metadata) {
        StringBuilder buff = new StringBuilder();
        Iterator<String> keys = metadata.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = metadata.get(key);
            if (buff.length() > 0) {
                buff.append(", ");
            }
            buff.append(key).append('=').append(value);
        }
        return buff.toString();
    }

    /**
     * Shuts down the logger, stopping the worker thread and clearing resources.
     */
    public void shutdown() {
        // Stop the worker loop
        running.set(false);
        if (workerThread != null) {
            // Interrupt the worker thread
            workerThread.interrupt();
        }
    }
}