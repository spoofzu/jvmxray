package org.jvmxray.agent.proxy;

import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    // Metrics tracking
    private final AtomicLong discardCount = new AtomicLong(0);
    private final AtomicLong eventCount = new AtomicLong(0);
    private volatile long lastFlushTime = 0;
    private volatile int lastFlushEventCount = 0;

    /**
     * Enum defining the logging modes supported by {@code AgentLogger}.
     */
    public enum MODE {
        BUFFERED,    // Buffered mode with worker thread and queue
        SYSTEM,      // Direct output to System.out
        LOGBACK      // Direct output to SLF4J logger
    }

    /**
     * Enum defining the overflow strategies for buffer management.
     */
    public enum OverflowStrategy {
        WAIT("wait"),       // Block calling thread when buffer is full
        DISCARD("discard"); // Drop oldest events when buffer is full

        private final String value;

        OverflowStrategy(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static OverflowStrategy fromString(String value) {
            for (OverflowStrategy strategy : OverflowStrategy.values()) {
                if (strategy.value.equalsIgnoreCase(value)) {
                    return strategy;
                }
            }
            throw new IllegalArgumentException("Invalid overflow strategy: " + value + ". Valid values are: wait, discard");
        }
    }

    // Default logging mode
    private static final MODE ASSIGNEDMODE = MODE.BUFFERED;

    /**
     * Configuration class for {@code AgentLogger}, specifying buffer size, flush interval,
     * overflow strategy, and logging mode.
     */
    public static class Config {
        // Size of the event queue in BUFFERED mode
        private final int bufferSize;
        // Interval for flushing queued events (milliseconds)
        private final long flushIntervalMs;
        // Overflow strategy for buffer management
        private final OverflowStrategy overflowStrategy;
        // Logging mode (BUFFERED, SYSTEM, or LOGBACK)
        private final MODE mode;

        /**
         * Constructs a new {@code Config} with the specified parameters.
         *
         * @param bufferSize      The size of the event queue in BUFFERED mode.
         * @param flushIntervalMs The interval for flushing queued events (milliseconds).
         * @param overflowStrategy The overflow strategy for buffer management.
         * @param mode            The logging mode.
         */
        public Config(int bufferSize, long flushIntervalMs, OverflowStrategy overflowStrategy, MODE mode) {
            this.bufferSize = bufferSize;
            this.flushIntervalMs = flushIntervalMs;
            this.overflowStrategy = overflowStrategy;
            this.mode = mode;
        }

        /**
         * Returns the default configuration for {@code AgentLogger}.
         *
         * @return A {@code Config} instance with default settings.
         */
        public static Config defaultConfig() {
            return new Config(10000, 10, OverflowStrategy.WAIT, ASSIGNEDMODE);
        }

        /**
         * Creates configuration from agent properties.
         *
         * @param properties The agent properties to read configuration from.
         * @return A {@code Config} instance configured from properties.
         */
        public static Config fromProperties(org.jvmxray.platform.shared.property.AgentProperties properties) {
            int bufferSize = properties.getIntProperty("org.jvmxray.agent.logproxy.buffer.size", 10000);
            long flushInterval = properties.getLongProperty("org.jvmxray.agent.logproxy.buffer.flush.interval", 10);
            String strategyStr = properties.getProperty("org.jvmxray.agent.logproxy.buffer.overflow.strategy", "wait");
            OverflowStrategy strategy = OverflowStrategy.fromString(strategyStr);
            return new Config(bufferSize, flushInterval, strategy, ASSIGNEDMODE);
        }

        // Getters
        public int getBufferSize() { return bufferSize; }
        public long getFlushIntervalMs() { return flushIntervalMs; }
        public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
        public MODE getMode() { return mode; }
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

    // Mutable instance for reconfiguration
    private static volatile AgentLogger activeInstance = null;

    /**
     * Constructs a new {@code AgentLogger} with the specified configuration.
     *
     * @param config The configuration settings, or null to use default settings.
     */
    private AgentLogger(Config config) {
        // Use provided config or fall back to default
        this.config = config != null ? config : Config.defaultConfig();
        // Initialize SLF4J logger for internal errors
        this.logger = initializeLogger();
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
     * Configures the AgentLogger with the specified properties.
     * This should be called once during agent initialization.
     *
     * @param properties The agent properties containing buffer configuration.
     */
    public static void configure(org.jvmxray.platform.shared.property.AgentProperties properties) {
        if (activeInstance != null) {
            // Shutdown existing instance
            activeInstance.shutdown();
        }
        try {
            Config config = Config.fromProperties(properties);
            activeInstance = new AgentLogger(config);
            
            // Log the configuration for visibility
            long validationTime = 0; // Will be set by caller
            String configMessage = String.format(
                "LogProxy initialized: buffer=%d, flush=%dms, strategy=%s, validation=%.1fs",
                config.bufferSize,
                config.flushIntervalMs,
                config.overflowStrategy.getValue().toUpperCase(),
                validationTime / 1000.0
            );
            System.out.println(configMessage);
        } catch (Exception e) {
            System.err.println("AgentLogger: Failed to configure with properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the active AgentLogger instance.
     *
     * @return The {@code AgentLogger} instance.
     * @throws IllegalStateException If the singleton failed to initialize.
     */
    public static AgentLogger getInstance() {
        AgentLogger instance = activeInstance != null ? activeInstance : INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("AgentLogger singleton failed to initialize. Check startup logs.");
        }
        return instance;
    }

    /**
     * Logs an event with the specified namespace, level, and metadata.
     *
     * @param namespace The namespace for the event (e.g., sensor-specific identifier).
     * @param level     The log level (e.g., TRACE, DEBUG, INFO, WARN, ERROR).
     * @param metadata  The metadata key-value pairs for the event.
     */
    public void logEvent(String namespace, String level, Map<String, String> metadata) {
        // Don't modify the original.
        Map<String, String> clonedMetadata = new HashMap<>(metadata);
        // Add AID and CID to the meta.
        updateApplicationMeta(clonedMetadata);
        // Increment event counter
        eventCount.incrementAndGet();
        // Log all the sensor keyval pairs and also teh AID and CID properties.
        handleEvent0(namespace, level, clonedMetadata);
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
     * Enqueues an event for processing in BUFFERED mode using configured overflow strategy.
     *
     * @param namespace The namespace for the event.
     * @param level     The log level.
     * @param metadata  The metadata key-value pairs.
     */
    private void enqueueEvent(String namespace, String level, Map<String, String> metadata) {
        if (queue == null) return;
        
        LogEvent event = new LogEvent(namespace, level, metadata);
        
        switch (config.overflowStrategy) {
            case WAIT:
                try {
                    // Blocking put - waits if queue is full
                    queue.put(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting to enqueue event for namespace: {}", namespace);
                }
                break;
            case DISCARD:
                if (!queue.offer(event)) {
                    // Non-blocking offer - drops event if queue is full
                    discardCount.incrementAndGet();
                    logger.warn("Log queue full, dropping event for namespace: {} (strategy=DISCARD)", namespace);
                }
                break;
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
     * Initializes the SLF4J logger, handling any exceptions gracefully.
     * @return The logger instance, or null if initialization fails.
     */
    private static Logger initializeLogger() {
        try {
            return AgentInitializer.getInstance().getLogger(AgentLogger.class.getName());
        } catch (Exception e) {
            // Fallback to system out if AgentInitializer not available
            System.err.println("Warning: Could not initialize logger in AgentLogger, using System.out fallback");
            return null;
        }
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
        Logger eventLogger;
        try {
            eventLogger = AgentInitializer.getInstance().getLogger(namespace);
        } catch (Exception e) {
            System.err.println("Warning: Could not get logger for namespace: " + namespace);
            eventLogger = null;
        }
        if (eventLogger != null) {
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
        } else {
            // Fallback to system out
            System.out.println("[" + level + "] " + namespace + ": " + message);
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
        Logger eventLogger;
        try {
            eventLogger = AgentInitializer.getInstance().getLogger(namespace);
        } catch (Exception e) {
            System.err.println("Warning: Could not get logger for namespace: " + namespace);
            eventLogger = null;
        }
        boolean result = false;
        if (eventLogger != null) {
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
        } else {
            // If no logger available, assume logging is enabled (fall back to system out)
            result = true;
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

        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        
        LogEvent event;
        while ((event = queue.poll()) != null) {
            try {
                // Process each event using LOGBACK mode
                handleLogbackOutput(event.namespace, event.level, event.metadata);
                processedCount++;
            } catch (Exception e) {
                // Log processing errors
                logger.error("Failed to process log event: {}", event.namespace, e);
            }
        }
        
        // Update flush metrics
        if (processedCount > 0) {
            lastFlushTime = System.currentTimeMillis() - startTime;
            lastFlushEventCount = processedCount;
        }
    }

    /**
     * Adds server properties like application id (AID) and category id (CID) from
     * <codd>agent.properties</codd> to log messages.  Note: we need AID to identify
     * which cloud service produced an event.  The category is useful to separate
     * out events like unit-testing from production logs/events.
     * @param metadata Event meta provided by the caller.
     */
    private void updateApplicationMeta( Map<String, String> metadata ) {
        try {
            AgentInitializer agentInitializer = AgentInitializer.getInstance();
            AgentProperties properties = agentInitializer.getProperties();
            if (properties != null) {
                String aid = properties.getProperty("AID", "unknown");
                String cid = properties.getProperty("CID", "unknown");
                // Add if missing or replace it.
                metadata.put("AID", aid);
                metadata.put("CID", cid);
            }
        } catch (Exception e) {
            System.err.println("Error updating application meta data: " + e.getMessage());
            e.printStackTrace();
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
        
        // Process any remaining events in the queue to ensure they are logged
        if (queue != null) {
            processQueue();  // Flush remaining events
        }
        
        if (workerThread != null) {
            // Interrupt the worker thread
            workerThread.interrupt();
            try {
                // Wait for worker thread to finish (with timeout)
                workerThread.join(1000);  // Wait up to 1 second
            } catch (InterruptedException e) {
                // Restore interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Metrics class for LogProxy buffer statistics.
     */
    public static class LogProxyMetrics {
        private final String bufferUtilization;
        private final String currentQueueSize;
        private final String discardCount;
        private final String flushRate;
        private final String avgFlushTime;
        private final String overflowStrategy;
        private final String totalEvents;

        public LogProxyMetrics(String bufferUtilization, String currentQueueSize, String discardCount,
                              String flushRate, String avgFlushTime, String overflowStrategy, String totalEvents) {
            this.bufferUtilization = bufferUtilization;
            this.currentQueueSize = currentQueueSize;
            this.discardCount = discardCount;
            this.flushRate = flushRate;
            this.avgFlushTime = avgFlushTime;
            this.overflowStrategy = overflowStrategy;
            this.totalEvents = totalEvents;
        }

        // Getters
        public String getBufferUtilization() { return bufferUtilization; }
        public String getCurrentQueueSize() { return currentQueueSize; }
        public String getDiscardCount() { return discardCount; }
        public String getFlushRate() { return flushRate; }
        public String getAvgFlushTime() { return avgFlushTime; }
        public String getOverflowStrategy() { return overflowStrategy; }
        public String getTotalEvents() { return totalEvents; }
    }

    /**
     * Returns current buffer metrics for monitoring.
     *
     * @return LogProxyMetrics containing current statistics.
     */
    public LogProxyMetrics getMetrics() {
        if (queue == null || !MODE.BUFFERED.equals(config.mode)) {
            // Return empty metrics for non-buffered mode
            return new LogProxyMetrics("N/A", "N/A", "0", "N/A", "N/A", "N/A", String.valueOf(eventCount.get()));
        }

        int currentSize = queue.size();
        int bufferSize = config.bufferSize;
        int utilization = (int) ((currentSize * 100.0) / bufferSize);
        
        String flushRate = lastFlushEventCount > 0 && lastFlushTime > 0 ? 
            String.format("%.0f events/sec", (lastFlushEventCount * 1000.0) / lastFlushTime) : "N/A";
        
        String avgFlushTime = lastFlushTime > 0 ? String.format("%.1fms", (double) lastFlushTime) : "N/A";

        return new LogProxyMetrics(
            utilization + "%",
            currentSize + "/" + bufferSize,
            String.valueOf(discardCount.get()),
            flushRate,
            avgFlushTime,
            config.overflowStrategy.getValue().toUpperCase(),
            String.valueOf(eventCount.get())
        );
    }
}