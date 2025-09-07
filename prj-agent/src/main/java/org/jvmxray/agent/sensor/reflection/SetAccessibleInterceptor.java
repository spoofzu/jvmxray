package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for AccessibleObject.setAccessible() calls to detect privilege escalation.
 * 
 * @author Milton Smith
 */
public class SetAccessibleInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts AccessibleObject.setAccessible() calls to detect privilege escalation.
     */
    @Advice.OnMethodExit
    public static void setAccessible(@Advice.This Object accessibleObject,
                                   @Advice.Argument(0) boolean flag,
                                   @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "set_accessible");
            metadata.put("accessible_flag", String.valueOf(flag));
            
            String objectInfo = ReflectionUtils.extractAccessibleObjectInfo(accessibleObject);
            metadata.put("accessible_object", objectInfo);
            
            // Only log when making things accessible (true flag)
            if (flag) {
                metadata.put("privilege_escalation", "true");
                metadata.put("risk_level", "MEDIUM");
                metadata.put("threat_type", "access_control_bypass");
                
                // Check for particularly dangerous accessibility modifications
                if (objectInfo.contains("System") || 
                    objectInfo.contains("SecurityManager") ||
                    objectInfo.contains("Runtime") ||
                    objectInfo.contains("sun.misc.Unsafe") ||
                    objectInfo.contains("jdk.internal.misc.Unsafe")) {
                    metadata.put("high_risk_access", "true");
                    metadata.put("risk_level", "HIGH");
                }
                
                // Check for reflection API modifications
                if (objectInfo.contains("java.lang.reflect")) {
                    metadata.put("reflection_api_access", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "MEDIUM"));
                }
                
                if (throwable != null) {
                    metadata.put("error", throwable.getClass().getSimpleName());
                    metadata.put("error_message", throwable.getMessage());
                }
                
                // Analyze call stack
                String callContext = ReflectionUtils.analyzeCallStack();
                metadata.put("call_context", callContext);
                
                logProxy.logMessage(NAMESPACE + ".set_accessible", "INFO", metadata);
            }
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}