package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.platform.shared.log.JVMXRayLogFactory;
import org.jvmxray.platform.shared.log.SecurityUtil;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;

/**
 * Sensor implementation for logging system environment details during application initialization.
 * Captures shell environment variables and Java system properties using {@link SecurityUtil} and
 * logs them via a dedicated logger. Integrates with the JVMXRay agent framework for system monitoring.
 *
 * @author Milton Smith
 */
public class AppInitSensor implements Sensor {

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's name, "AppInitSensor".
     */
    @Override
    public String getName() {
        return "AppInitSensor";
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
        // Initialize logger for system settings events
        Logger logger = JVMXRayLogFactory.getInstance().getLogger("org.jvmxray.events.system.settings");
        // Create SecurityUtil instance with logger callback
        SecurityUtil util = new SecurityUtil(message -> logger.info(message));
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