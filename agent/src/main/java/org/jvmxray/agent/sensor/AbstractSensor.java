package org.jvmxray.agent.sensor;

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
}