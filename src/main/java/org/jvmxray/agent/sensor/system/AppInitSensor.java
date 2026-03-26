package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.platform.shared.log.intervallogger.SecurityUtil;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.util.MCCScope;

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
    private static final String SENSOR_GUID = "C7B21F8F-3A48-4405-A8AB-2004E4603ADB"; // Generated via uuidgen
    public AppInitSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    /**
     * Initializes the sensor by logging system context, environment variables, and Java system properties.
     * Enhanced with JVM details, container detection, and cloud metadata.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        MCCScope.enter("AppInit");
        try {
            // Log enhanced system context (JVM, process, container, cloud)
            Map<String, String> systemContext = SystemContextUtils.getAllContext();
            systemContext.put("event_type", "system_context");
            logProxy.logMessage(NAMESPACE, "INFO", systemContext);

            // Create SecurityUtil instance with logProxy callback that includes sensitive value redaction
            SecurityUtil util = new SecurityUtil(message -> {
                // Check if this is a key=value format and redact if needed
                if (message.contains("=")) {
                    int idx = message.indexOf("=");
                    String key = message.substring(0, idx);
                    String value = message.substring(idx + 1);
                    String redactedValue = SystemContextUtils.redactSensitiveValue(key, value);
                    logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                            "message", key + "=" + redactedValue,
                            "is_redacted", String.valueOf(!value.equals(redactedValue))
                    ));
                } else {
                    logProxy.logMessage(NAMESPACE, "INFO", Map.of("message", message));
                }
            });

            // Log shell environment variables
            util.logShellEnvironmentVariables();

            // Log Java system properties
            util.logJavaSystemProperties();
        } finally {
            MCCScope.exit("AppInit");
        }
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