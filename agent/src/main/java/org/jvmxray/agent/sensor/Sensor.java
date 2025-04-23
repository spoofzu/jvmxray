package org.jvmxray.agent.sensor;

import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;

/**
 * Base interface for all sensors in the JVMXRay agent framework. Defines the common
 * lifecycle methods for sensor initialization, operation, and shutdown. Sensors monitor
 * specific JVM activities (e.g., file I/O, network operations, SQL queries) and integrate
 * with the agent's modular architecture to log events or apply instrumentation.
 *
 * @author Milton Smith
 */
public interface Sensor {

    /**
     * Returns the unique identifier for the sensor, used for logging and configuration.
     * Implementations must provide a distinct name for the sensor.
     *
     * @return The sensor's name as a {@code String}.
     */
    String getName();

    /**
     * Initializes the sensor with the provided configuration properties. Implementations
     * may use this method to set up resources, such as background threads, class detection,
     * or instrumentation transformers.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    void initialize(AgentProperties properties, String agentArgs, Instrumentation inst);

    /**
     * Cleans up resources used by the sensor during shutdown. Implementations may use
     * this method to release resources, such as stopping threads or unregistering transformers.
     */
    void shutdown();
}