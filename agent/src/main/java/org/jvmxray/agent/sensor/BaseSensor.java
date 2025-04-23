package org.jvmxray.agent.sensor;

import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;

/**
 * Abstract base class for sensors in the JVMXRay agent framework. Provides a common
 * interface for sensor implementations to define their initialization, configuration,
 * and shutdown behavior. Sensors (e.g., {@link org.jvmxray.agent.sensor.sql.SQLSensor},
 * {@link org.jvmxray.agent.sensor.net.SocketSensor}) extend this class to integrate
 * with the agent's modular architecture for monitoring specific JVM activities.
 *
 * @author Milton Smith
 */
public abstract class BaseSensor {

    /**
     * Returns the unique identifier for the sensor, used for logging and configuration.
     * Must be implemented by subclasses to provide a specific sensor name.
     *
     * @return The sensor's name.
     */
    abstract String getName();

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
    void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // No operations by default
    }

    /**
     * Cleans up resources used by the sensor during shutdown.
     * Subclasses may override this method to release resources, such as stopping
     * threads or unregistering transformers. The default implementation performs
     * no operations.
     */
    void shutdown() {
        // No cleanup by default
    }
}