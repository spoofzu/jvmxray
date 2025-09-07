package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for ObjectInputStream.readObject() to detect deserialization attacks.
 * 
 * @author Milton Smith
 */
public class ObjectInputInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts ObjectInputStream.readObject() to detect deserialization attacks.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void readObject(@Advice.This Object objectInputStream,
                                @Advice.Return Object result,
                                @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "java_native");
            
            if (result != null) {
                String className = result.getClass().getName();
                metadata.put("deserialized_class", className);
                
                // Check if it's a dangerous class
                String dangerousClass = SerializationUtils.checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "CRITICAL");
                }
                
                // Check for suspicious patterns
                if (className.contains("Transformer") || 
                    className.contains("Handler") || 
                    className.contains("Factory")) {
                    metadata.put("suspicious_pattern", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "HIGH"));
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
                
                // Some attacks cause specific exceptions
                if (throwable instanceof ClassNotFoundException) {
                    metadata.put("potential_attack", "gadget_chain_attempt");
                }
            }
            
            // Analyze call stack for additional context
            String stackTrace = SerializationUtils.getRelevantStackTrace();
            metadata.put("call_context", SerializationUtils.analyzeCallContext(stackTrace));
            
            logProxy.logMessage(NAMESPACE + ".deserialize", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently to avoid disrupting deserialization
        }
    }
}