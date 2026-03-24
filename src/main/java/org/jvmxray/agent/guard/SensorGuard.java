package org.jvmxray.agent.guard;

/**
 * Simple thread-local guard to prevent sensors from monitoring operations triggered 
 * by other sensors in the same thread.
 * 
 * This prevents infinite recursion scenarios like:
 * - FileIOInterceptor monitoring Files.createDirectories()
 * - AgentInitializer calling Files.createDirectories() during initialization
 * - Files.createDirectories() triggering FileIOInterceptor again
 * 
 * Design principle: When ANY sensor is active in a thread, NO other sensor 
 * should monitor operations in that thread. This ensures we only monitor 
 * application behavior, not the monitoring infrastructure itself.
 * 
 * Thread-safe: Each thread maintains its own guard state.
 * Performance: Minimal overhead - just a ThreadLocal boolean check.
 * 
 * @author Milton Smith
 */
public class SensorGuard {
    
    // Per-thread flag indicating if any sensor is currently active
    private static final ThreadLocal<Boolean> sensorActive = 
        ThreadLocal.withInitial(() -> false);
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SensorGuard() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Checks if it's safe for a sensor to continue processing.
     * Returns false if any sensor is already active in this thread.
     * 
     * @return true if safe to continue, false if another sensor is active
     */
    public static boolean isSafeToContinue() {
        return !sensorActive.get();
    }
    
    /**
     * Marks that a sensor has started processing in this thread.
     * Should be called when a sensor begins intercepting operations.
     */
    public static void enterSensor() {
        sensorActive.set(true);
    }
    
    /**
     * Marks that sensor processing has ended in this thread.
     * Should be called in a finally block to ensure cleanup.
     */
    public static void exitSensor() {
        sensorActive.set(false);
        // Note: We don't need to remove the ThreadLocal since it's just a boolean
        // and won't cause memory leaks like collections would
    }
    
    /**
     * Checks if any sensor is currently active in this thread.
     * Useful for debugging or logging purposes.
     * 
     * @return true if a sensor is active, false otherwise
     */
    public static boolean isSensorActive() {
        return sensorActive.get();
    }
    
    /**
     * Clears the sensor guard state for the current thread.
     * Should only be used in exceptional circumstances or for testing.
     */
    public static void clear() {
        sensorActive.remove();
    }
}