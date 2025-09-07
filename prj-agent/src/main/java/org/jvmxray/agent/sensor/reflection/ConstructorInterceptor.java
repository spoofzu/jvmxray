package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Constructor.newInstance() calls to detect suspicious constructor invocations.
 * 
 * @author Milton Smith
 */
public class ConstructorInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Constructor.newInstance() calls to detect suspicious constructor invocations.
     */
    @Advice.OnMethodExit
    public static void constructorNewInstance(@Advice.This Object constructor,
                                            @Advice.Argument(0) Object[] args,
                                            @Advice.Return Object result,
                                            @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "constructor_newInstance");
            
            String className = ReflectionUtils.extractConstructorClassName(constructor);
            metadata.put("declaring_class", className);
            metadata.put("arg_count", args != null ? String.valueOf(args.length) : "0");
            
            // Check for suspicious class instantiation
            for (String suspiciousClass : ReflectionUtils.SUSPICIOUS_CLASSES) {
                if (className.equals(suspiciousClass) || className.contains(suspiciousClass)) {
                    metadata.put("suspicious_class", "true");
                    metadata.put("risk_level", "HIGH");
                    metadata.put("threat_type", "privilege_escalation");
                    break;
                }
            }
            
            // Special handling for dangerous constructors
            if (className.contains("ProcessBuilder")) {
                metadata.put("process_creation", "true");
                metadata.put("risk_level", "HIGH");
                if (args != null && args.length > 0 && args[0] != null) {
                    metadata.put("command", args[0].toString());
                }
            }
            
            if (className.contains("URLClassLoader")) {
                metadata.put("classloader_creation", "true");
                metadata.put("risk_level", "MEDIUM");
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
            
            logProxy.logMessage(NAMESPACE + ".constructor_newInstance", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}