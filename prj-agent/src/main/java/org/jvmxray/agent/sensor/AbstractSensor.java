package org.jvmxray.agent.sensor;

import org.jvmxray.agent.guard.SensorGuard;
import org.jvmxray.platform.shared.property.AgentProperties;
import java.lang.instrument.Instrumentation;

/**
 * Abstract base class for sensors in the JVMXRay agent framework. Provides a common
 * implementation for sensor initialization, configuration, and shutdown behavior.
 * Sensors (e.g., {@link org.jvmxray.agent.sensor.http.HttpSensor},
 * {@link org.jvmxray.agent.sensor.io.FileIOSensor}) extend this class to integrate
 * with the agent's modular architecture for monitoring specific JVM activities.
 *
 * @author Milton Smith
 */
public abstract class AbstractSensor implements Sensor {

    protected final String propertySuffix; // Property key suffix (e.g., "http")

    /**
     * Constructs an AbstractSensor with the specified property suffix.
     *
     * @param propertySuffix The property key suffix (e.g., "http" from "jvmxray.sensor.http").
     */
    protected AbstractSensor(String propertySuffix) {
        this.propertySuffix = propertySuffix != null && !propertySuffix.isEmpty() ? propertySuffix : "unknown";
    }

    /**
     * Returns the unique GUID identifier for the sensor type. Must be implemented
     * by subclasses to provide a hardcoded GUID.
     *
     * @return The sensor's GUID.
     */
    public abstract String getIdentity();

    /**
     * Returns the human-readable display name for the sensor, formatted as
     * <propertySuffix>[<identity>]. Subclasses may override to customize the display name.
     *
     * @return The sensor's display name (e.g., "http[CCF1EE82-F58A-4866-A1D4-09A3B7B25A2D]").
     */
    @Override
    public String getDisplayName() {
        return propertySuffix + "[" + getIdentity() + "]";
    }

    /**
     * Initializes the sensor with the provided configuration properties.
     * Subclasses may override this method to perform setup tasks, such as starting
     * background threads or registering transformers. The default implementation
     * performs no operations.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // No operations by default
    }

    /**
     * Cleans up resources used by the sensor during shutdown.
     * Subclasses may override this method to release resources, such as stopping
     * threads or unregistering transformers. The default implementation performs
     * no operations.
     */
    @Override
    public void shutdown() {
        // No cleanup by default
    }
    
    // ================================================================================================
    // SENSOR GUARD METHODS - Prevent sensors from monitoring their own operations
    // ================================================================================================
    
    /**
     * Safe method for interceptors to execute logic with sensor guard protection.
     * This is the recommended pattern for all interceptor shouldCapture methods.
     * 
     * Prevents sensors from monitoring operations triggered by other sensors in the same thread.
     * This ensures we only monitor application behavior, not the monitoring infrastructure itself.
     * 
     * @param logic A supplier that returns whether the operation should be captured
     * @return true if the operation should be intercepted, false if another sensor is active
     */
    public static boolean executeSafely(java.util.function.BooleanSupplier logic) {
        // Check if any sensor is already active in this thread
        if (!SensorGuard.isSafeToContinue()) {
            return false; // Another sensor is active, skip interception
        }
        
        // Mark this thread as having an active sensor
        SensorGuard.enterSensor();
        try {
            // Execute the interceptor's logic safely
            return logic.getAsBoolean();
        } finally {
            // Always clear the sensor guard
            SensorGuard.exitSensor();
        }
    }
}