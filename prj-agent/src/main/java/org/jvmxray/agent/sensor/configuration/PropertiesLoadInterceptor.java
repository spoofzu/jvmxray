package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Properties.load() to monitor configuration file loading.
 * 
 * @author Milton Smith
 */
public class PropertiesLoadInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Properties.load() to monitor configuration file loading.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void propertiesLoad(@Advice.This Object properties,
                                    @Advice.Argument(0) Object inputStream,
                                    @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "properties_load");
            metadata.put("input_stream_class", inputStream != null ? inputStream.getClass().getName() : "null");
            metadata.put("load_success", throwable == null ? "true" : "false");
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Try to get property count after loading
            if (throwable == null) {
                int propertyCount = ConfigurationUtils.getPropertyCount(properties);
                if (propertyCount > 0) {
                    metadata.put("property_count", String.valueOf(propertyCount));
                }
            }
            
            String context = ConfigurationUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".file", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}