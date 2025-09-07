package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Class.forName() calls to detect dynamic class loading.
 * 
 * @author Milton Smith
 */
public class ClassForNameInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Class.forName() calls to detect dynamic class loading.
     */
    @Advice.OnMethodExit
    public static void classForName(@Advice.Argument(0) String className,
                                  @Advice.Return Class<?> result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "class_forName");
            metadata.put("class_name", className != null ? className : "unknown");
            
            if (className != null) {
                // Check for suspicious classes
                for (String suspiciousClass : ReflectionUtils.SUSPICIOUS_CLASSES) {
                    if (className.equals(suspiciousClass) || className.contains(suspiciousClass)) {
                        metadata.put("suspicious_class", "true");
                        metadata.put("risk_level", "HIGH");
                        metadata.put("threat_type", "privilege_escalation");
                        break;
                    }
                }
                
                // Check for dynamic bytecode generation frameworks
                if (className.contains("asm.") || 
                    className.contains("bytebuddy") ||
                    className.contains("cglib") ||
                    className.contains("javassist")) {
                    metadata.put("bytecode_framework", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "MEDIUM"));
                }
            }
            
            if (result != null) {
                metadata.put("loaded_successfully", "true");
                metadata.put("class_loader", result.getClassLoader() != null ? 
                    result.getClassLoader().getClass().getName() : "bootstrap");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Analyze call stack for context
            String callContext = ReflectionUtils.analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".class_forname", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently to avoid disrupting reflection operations
        }
    }
}