package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Gson.fromJson() for JSON deserialization.
 * 
 * @author Milton Smith
 */
public class GsonInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Gson.fromJson() for JSON deserialization.
     */
    @Advice.OnMethodExit
    public static void gsonFromJson(@Advice.Argument(0) String json,
                                  @Advice.Return Object result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "json_gson");
            
            if (json != null && json.length() < 10000) {
                // Analyze JSON for suspicious patterns
                if (json.contains("@type") || json.contains("class")) {
                    metadata.put("polymorphic_deserialization", "true");
                    metadata.put("risk_level", "MEDIUM");
                }
            }
            
            if (result != null) {
                metadata.put("result_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".gson", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}