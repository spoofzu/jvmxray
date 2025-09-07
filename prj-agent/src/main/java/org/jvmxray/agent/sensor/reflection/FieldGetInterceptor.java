package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Field.get() calls to detect sensitive field access.
 * 
 * @author Milton Smith
 */
public class FieldGetInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Field.get() calls to detect sensitive field access.
     */
    @Advice.OnMethodExit
    public static void fieldGet(@Advice.This Object field,
                               @Advice.Argument(0) Object instance,
                               @Advice.Return Object result,
                               @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "field_get");
            
            String fieldName = ReflectionUtils.extractFieldName(field);
            String className = ReflectionUtils.extractFieldClassName(field);
            
            metadata.put("field_name", fieldName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            
            // Check for sensitive field access
            if (ReflectionUtils.isSensitiveField(fieldName, className)) {
                metadata.put("sensitive_field", "true");
                metadata.put("risk_level", "HIGH");
                metadata.put("threat_type", "information_disclosure");
            }
            
            // Check for security-related field access
            if (fieldName.toLowerCase().contains("password") || 
                fieldName.toLowerCase().contains("secret") ||
                fieldName.toLowerCase().contains("key") ||
                fieldName.toLowerCase().contains("token")) {
                metadata.put("credential_access", "true");
                metadata.put("risk_level", "CRITICAL");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            if (result != null) {
                metadata.put("result_type", result.getClass().getName());
            }
            
            // Analyze call stack
            String callContext = ReflectionUtils.analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".field_get", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}