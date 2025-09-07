package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Field.set() calls to detect sensitive field modifications.
 * 
 * @author Milton Smith
 */
public class FieldSetInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Field.set() calls to detect sensitive field modifications.
     */
    @Advice.OnMethodExit
    public static void fieldSet(@Advice.This Object field,
                               @Advice.Argument(0) Object instance,
                               @Advice.Argument(1) Object value,
                               @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "field_set");
            
            String fieldName = ReflectionUtils.extractFieldName(field);
            String className = ReflectionUtils.extractFieldClassName(field);
            
            metadata.put("field_name", fieldName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            metadata.put("value_provided", value != null ? "true" : "false");
            
            if (value != null) {
                metadata.put("value_type", value.getClass().getName());
            }
            
            // Check for sensitive field modification
            if (ReflectionUtils.isSensitiveField(fieldName, className)) {
                metadata.put("sensitive_field", "true");
                metadata.put("risk_level", "HIGH");
                metadata.put("threat_type", "privilege_escalation");
            }
            
            // Check for security manager modifications
            if (className.contains("System") && fieldName.toLowerCase().contains("security")) {
                metadata.put("security_manager_modification", "true");
                metadata.put("risk_level", "CRITICAL");
            }
            
            // Check for final field modification
            if (fieldName.equals("modifiers") && className.contains("Field")) {
                metadata.put("final_field_modification", "true");
                metadata.put("risk_level", "HIGH");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Analyze call stack
            String callContext = ReflectionUtils.analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".field_set", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}