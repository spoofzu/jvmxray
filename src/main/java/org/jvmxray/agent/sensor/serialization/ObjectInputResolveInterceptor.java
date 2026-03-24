package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for ObjectInputStream.resolveClass() to detect class resolution attacks.
 * 
 * @author Milton Smith
 */
public class ObjectInputResolveInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts ObjectInputStream.resolveClass() to detect class resolution attacks.
     */
    @Advice.OnMethodEnter
    public static void resolveClass(@Advice.Argument(0) Object objectStreamClass) {
        try {
            if (objectStreamClass != null) {
                String className = objectStreamClass.toString();
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("operation", "resolve_class");
                metadata.put("class_name", className);
                
                // Check for dangerous classes being resolved
                String dangerousClass = SerializationUtils.checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "CRITICAL");
                    metadata.put("threat_type", "deserialization_gadget");
                }
                
                logProxy.logMessage(NAMESPACE + ".resolve", "INFO", metadata);
            }
        } catch (Exception e) {
            // Fail silently
        }
    }
}