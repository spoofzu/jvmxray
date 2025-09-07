package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Jackson ObjectMapper.readValue() for JSON deserialization.
 * 
 * @author Milton Smith
 */
public class JacksonInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Jackson ObjectMapper.readValue() for JSON deserialization.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void jacksonReadValue(@Advice.Argument(0) Object input,
                                      @Advice.Return Object result,
                                      @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "json_jackson");
            
            if (input != null) {
                metadata.put("input_type", input.getClass().getSimpleName());
                
                // Analyze input for suspicious patterns
                String inputStr = input.toString();
                if (inputStr.length() < 10000) { // Only analyze reasonably sized inputs
                    if (inputStr.contains("@type") || inputStr.contains("@class")) {
                        metadata.put("polymorphic_deserialization", "true");
                        metadata.put("risk_level", "HIGH");
                    }
                    
                    // Check for suspicious class references
                    for (String dangerousClass : SerializationUtils.DANGEROUS_CLASSES) {
                        if (inputStr.contains(dangerousClass)) {
                            metadata.put("dangerous_class_reference", dangerousClass);
                            metadata.put("risk_level", "CRITICAL");
                            break;
                        }
                    }
                }
            }
            
            if (result != null) {
                metadata.put("result_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".json", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}