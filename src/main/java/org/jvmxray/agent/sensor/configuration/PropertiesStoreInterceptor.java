package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Properties.store() to monitor configuration file writing.
 * 
 * @author Milton Smith
 */
public class PropertiesStoreInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Properties.store() to monitor configuration file writing.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void propertiesStore(@Advice.This Object properties,
                                     @Advice.Argument(0) Object outputStream,
                                     @Advice.Argument(1) String comments,
                                     @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "properties_store");
            metadata.put("output_stream_class", outputStream != null ? outputStream.getClass().getName() : "null");
            metadata.put("store_success", throwable == null ? "true" : "false");
            metadata.put("has_comments", comments != null ? "true" : "false");
            
            // Get property count being stored
            int propertyCount = ConfigurationUtils.getPropertyCount(properties);
            if (propertyCount > 0) {
                metadata.put("property_count", String.valueOf(propertyCount));
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Configuration modification is potentially risky
            metadata.put("config_modification", "true");
            metadata.put("risk_level", "MEDIUM");
            
            logProxy.logMessage(NAMESPACE + ".file", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}