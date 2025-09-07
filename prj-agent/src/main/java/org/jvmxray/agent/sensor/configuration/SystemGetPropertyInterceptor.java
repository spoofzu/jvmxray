package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

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
        try {
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
            
            String context = ConfigurationUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".property", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}