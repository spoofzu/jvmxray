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
     * Returns the unique identifier for the sensor.  Sensor developer's must provide
     * a unique static GUID for the sensor at design time.  To generate a GUID from,
     * <code>
     * IntelliJ IDEA: use the plugin, "UUID Generator", to generate GUID's in code.
     * MacOS/Linux from the CLI: uuidgen
     * WindowsOS Powershell: [guid]::NewGuid().ToString()
     * </code>
     * Return the GUID you generated as the identity in your Sensor implementation
     * class.
     *
     * @return The sensor's identity as a {@code String}.  For example,
     * "CCF1EE82-F58A-4866-A1D4-09A3B7B25A2D".
     */
    String getIdentity();

    /**
     * Returns a human readable name for the sensor derrived from the sensors property
     * name.  For instance, if the sensor's property name is "jvmxray.sensor.http" then
     * the default display name for the sensor is 'http' and displayed in sensor logs.
     *
     * @return The sensor's name as a {@code String}.
     */
    String getDisplayName();

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