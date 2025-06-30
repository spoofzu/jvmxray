package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.platform.shared.log.SecurityUtil;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * Sensor implementation for logging system environment details during application initialization.
 * Captures shell environment variables and Java system properties using {@link SecurityUtil} and
 * logs them via a dedicated logger. Integrates with the JVMXRay agent framework for system monitoring.
 *
 * @author Milton Smith
 */
public class AppInitSensor extends AbstractSensor implements Sensor {

    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.events.system.settings";
    // Singleton instance of LogProxy for logging
    private static final LogProxy logProxy = LogProxy.getInstance();
    // Static sensor identity.
    private static final String SENSOR_GUID = "7E2DFD81-7532-46E4-875F-DAA061F877A1"; // Generated via uuidgen
    public AppInitSensor(String propertySuffix) {
        super(propertySuffix);
    }

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's identity is, "7E2DFD81-7532-46E4-875F-DAA061F877A1".
     */
    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    /**
     * Initializes the sensor by logging shell environment variables and Java system properties.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // Create SecurityUtil instance with logProxy callback
        SecurityUtil util = new SecurityUtil(message ->
                logProxy.logMessage(NAMESPACE, "INFO", Map.of("message", message))
        );
        // Log shell environment variables
        util.logShellEnvironmentVariables();
        // Log Java system properties
        util.logJavaSystemProperties();
    }

    /**
     * Cleans up resources used by the sensor during shutdown.
     * This implementation performs no operations as no resources are held.
     */
    @Override
    public void shutdown() {
        // No cleanup required
    }
}