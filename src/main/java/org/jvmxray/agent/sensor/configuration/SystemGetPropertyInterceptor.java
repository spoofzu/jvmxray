package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for System.getProperty() calls to monitor property access.
 * 
 * @author Milton Smith
 */
public class SystemGetPropertyInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts System.getProperty() calls to monitor property access.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void systemGetProperty(@Advice.Argument(0) String key,
                                       @Advice.Return String result,
                                       @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Config");
        try {
            // Skip logging for agent's own internal property reads to avoid noise
            if (key != null && key.startsWith("org.jvmxray")) {
                return;
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_getProperty");
            metadata.put("property_key", key != null ? key : "unknown");
            metadata.put("value_retrieved", result != null ? "true" : "false");

            if (key != null && ConfigurationUtils.isSensitiveProperty(key)) {
                metadata.put("sensitive_property", "true");
                metadata.put("risk_level", "MEDIUM");
            }

            // Flag access to security-related properties
            if (key != null && (key.contains("security") || key.contains("policy"))) {
                metadata.put("security_property_access", "true");
                metadata.put("risk_level", "HIGH");
            }

            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }

            // Don't log the actual value for sensitive properties
            if (result != null && !ConfigurationUtils.isSensitiveProperty(key)) {
                metadata.put("property_value", ConfigurationUtils.truncateValue(result));
            }

            logProxy.logMessage(NAMESPACE + ".property", "INFO", metadata);

        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Config");
        }
    }
}