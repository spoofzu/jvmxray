package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for ObjectOutputStream.writeObject() to monitor serialization.
 * 
 * @author Milton Smith
 */
public class ObjectOutputInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts ObjectOutputStream.writeObject() to monitor serialization.
     */
    @Advice.OnMethodEnter
    public static void writeObject(@Advice.This Object objectOutputStream,
                                 @Advice.Argument(0) Object obj) {
        try {
            if (obj != null) {
                String className = obj.getClass().getName();
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("operation", "serialize");
                metadata.put("serialized_class", className);
                metadata.put("serialization_type", "java_native");
                
                // Check if serializing dangerous objects
                String dangerousClass = SerializationUtils.checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "HIGH");
                }
                
                logProxy.logMessage(NAMESPACE + ".serialize", "INFO", metadata);
            }
        } catch (Exception e) {
            // Fail silently
        }
    }
}