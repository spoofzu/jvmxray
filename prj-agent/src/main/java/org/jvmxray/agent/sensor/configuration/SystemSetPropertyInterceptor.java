package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for System.setProperty() calls to monitor property modifications.
 * 
 * @author Milton Smith
 */
public class SystemSetPropertyInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts System.setProperty() calls to monitor property modifications.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void systemSetProperty(@Advice.Argument(0) String key,
                                       @Advice.Argument(1) String value,
                                       @Advice.Return String result,
                                       @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_setProperty");
            metadata.put("property_key", key != null ? key : "unknown");
            metadata.put("modification_success", throwable == null ? "true" : "false");
            
            if (key != null) {
                if (ConfigurationUtils.isSensitiveProperty(key)) {
                    metadata.put("sensitive_property_modification", "true");
                    metadata.put("risk_level", "HIGH");
                    metadata.put("threat_type", "privilege_escalation");
                }
                
                // Critical security property modifications
                if (key.equals("java.security.manager") || key.equals("java.security.policy")) {
                    metadata.put("critical_security_modification", "true");
                    metadata.put("risk_level", "CRITICAL");
                }
                
                // Path manipulation attempts
                if (key.contains("path") || key.contains("dir")) {
                    metadata.put("path_modification", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "MEDIUM"));
                }
            }
            
            if (result != null) {
                metadata.put("previous_value_existed", "true");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Don't log actual values for sensitive properties
            if (value != null && !ConfigurationUtils.isSensitiveProperty(key)) {
                metadata.put("new_value", ConfigurationUtils.truncateValue(value));
            }
            
            String context = ConfigurationUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".property", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}