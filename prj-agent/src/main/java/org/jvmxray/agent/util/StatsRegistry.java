package org.jvmxray.agent.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for collecting sensor statistics across the JVMXRay agent.
 *
 * <p>This utility provides a centralized location for sensors to publish their runtime
 * statistics for monitoring and diagnostics. Sensors update their stats on each lifecycle
 * event (enter/exit), and MonitorSensor reads the snapshot periodically for logging.</p>
 *
 * <p><strong>Thread Safety:</strong> Uses {@link ConcurrentHashMap} for lock-free updates
 * from multiple sensor threads. The {@link #getSnapshot()} method returns a defensive copy
 * safe to use across thread boundaries.</p>
 *
 * <p><strong>Usage Pattern:</strong></p>
 * <pre>{@code
 * // Sensor updates stats on each operation
 * activeContexts.incrementAndGet();
 * StatsRegistry.register("mcc_active_contexts", String.valueOf(activeContexts.get()));
 *
 * // MonitorSensor reads all stats periodically
 * Map<String, String> allStats = StatsRegistry.getSnapshot();
 * logProxy.logMessage(NAMESPACE, "INFO", allStats);
 * }</pre>
 *
 * @author Milton Smith
 */
public class StatsRegistry {

    // Thread-safe registry for sensor statistics
    private static final ConcurrentHashMap<String, String> registry = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private StatsRegistry() {
        throw new UnsupportedOperationException("StatsRegistry is a utility class and cannot be instantiated");
    }

    /**
     * Registers or updates a statistic in the registry.
     *
     * <p>If the key already exists, its value is replaced. This method is thread-safe
     * and can be called from multiple sensors concurrently without synchronization.</p>
     *
     * @param key the statistic key (e.g., "mcc_active_contexts", "lib_static_loaded")
     * @param value the statistic value as a string
     * @throws IllegalArgumentException if key or value is null
     */
    public static void register(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Stats key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Stats value cannot be null");
        }
        registry.put(key, value);
    }

    /**
     * Returns an immutable snapshot of all registered statistics.
     *
     * <p>This method creates a defensive copy of the registry, ensuring the returned
     * map is safe to pass across threads and will not reflect subsequent updates.</p>
     *
     * @return immutable copy of current statistics, never null
     */
    public static Map<String, String> getSnapshot() {
        // Defensive copy - safe to pass across threads
        return new HashMap<>(registry);
    }

    /**
     * Removes a statistic from the registry.
     *
     * <p>This is primarily used for cleanup or when a sensor is shut down and its
     * statistics are no longer relevant.</p>
     *
     * @param key the statistic key to remove
     */
    public static void remove(String key) {
        registry.remove(key);
    }

    /**
     * Clears all statistics from the registry.
     *
     * <p>This is primarily used for testing. Production code should rarely need
     * to clear the entire registry.</p>
     */
    public static void clear() {
        registry.clear();
    }

    /**
     * Returns the number of statistics currently registered.
     *
     * @return count of registered statistics
     */
    public static int size() {
        return registry.size();
    }
}
