package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Method.invoke() calls to detect suspicious method invocations.
 * 
 * @author Milton Smith
 */
public class MethodInvokeInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Method.invoke() calls to detect suspicious method invocations.
     */
    @Advice.OnMethodExit
    public static void methodInvoke(@Advice.This Object method,
                                  @Advice.Argument(0) Object instance,
                                  @Advice.Argument(1) Object[] args,
                                  @Advice.Return Object result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "method_invoke");
            
            String methodName = ReflectionUtils.extractMethodName(method);
            String className = ReflectionUtils.extractDeclaringClassName(method);
            
            metadata.put("method_name", methodName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            metadata.put("arg_count", args != null ? String.valueOf(args.length) : "0");
            
            // Check for suspicious method invocations
            if (ReflectionUtils.isSuspiciousMethod(methodName, className)) {
                metadata.put("suspicious_method", "true");
                metadata.put("risk_level", "HIGH");
                metadata.put("threat_type", "code_injection");
            }
            
            // Special handling for specific dangerous methods
            if ("exec".equals(methodName) && className.contains("Runtime")) {
                metadata.put("command_execution", "true");
                metadata.put("risk_level", "CRITICAL");
                if (args != null && args.length > 0 && args[0] != null) {
                    metadata.put("command", args[0].toString());
                }
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
            
            logProxy.logMessage(NAMESPACE + ".method_invoke", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}