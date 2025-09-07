package org.jvmxray.agent.sensor.reflection;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Java reflection operations to detect potential security
 * vulnerabilities including privilege escalation, code injection, and
 * unauthorized access to private members.
 * 
 * @author Milton Smith
 */
public class ReflectionInterceptor {
    
    // Namespace for logging reflection events
    public static final String NAMESPACE = "org.jvmxray.events.reflection";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Suspicious classes that should be monitored when accessed via reflection
    private static final String[] SUSPICIOUS_CLASSES = {
        "java.lang.Runtime",
        "java.lang.ProcessBuilder",
        "java.lang.System",
        "java.io.File",
        "java.nio.file.Files",
        "java.net.URL",
        "java.net.URLClassLoader",
        "java.security.AccessController",
        "sun.misc.Unsafe",
        "jdk.internal.misc.Unsafe",
        "java.lang.reflect.Method",
        "java.lang.reflect.Constructor",
        "java.lang.reflect.Field",
        "javax.script.ScriptEngine",
        "org.springframework.context.ApplicationContext"
    };

    // Suspicious methods that could indicate malicious activity
    private static final String[] SUSPICIOUS_METHODS = {
        "exec", "getRuntime", "load", "loadLibrary", "exit", "setSecurityManager",
        "getProperty", "setProperty", "getenv", "defineClass", "newInstance",
        "invoke", "get", "set", "setAccessible", "forName"
    };

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
                for (String suspiciousClass : SUSPICIOUS_CLASSES) {
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
            String callContext = analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".class_forname", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently to avoid disrupting reflection operations
        }
    }

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
            
            String methodName = extractMethodName(method);
            String className = extractDeclaringClassName(method);
            
            metadata.put("method_name", methodName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            metadata.put("arg_count", args != null ? String.valueOf(args.length) : "0");
            
            // Check for suspicious method invocations
            if (isSuspiciousMethod(methodName, className)) {
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
            String callContext = analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".method_invoke", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Constructor.newInstance() calls to detect suspicious object creation.
     */
    @Advice.OnMethodExit
    public static void constructorNewInstance(@Advice.This Object constructor,
                                            @Advice.Argument(0) Object[] args,
                                            @Advice.Return Object result,
                                            @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "constructor_newInstance");
            
            String className = extractConstructorClassName(constructor);
            metadata.put("class_name", className);
            metadata.put("arg_count", args != null ? String.valueOf(args.length) : "0");
            
            // Check for suspicious class instantiation
            for (String suspiciousClass : SUSPICIOUS_CLASSES) {
                if (className.contains(suspiciousClass)) {
                    metadata.put("suspicious_instantiation", "true");
                    metadata.put("risk_level", "HIGH");
                    break;
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            if (result != null) {
                metadata.put("instance_created", "true");
            }
            
            String callContext = analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".constructor_invoke", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Field.get() calls to detect unauthorized field access.
     */
    @Advice.OnMethodExit
    public static void fieldGet(@Advice.This Object field,
                              @Advice.Argument(0) Object instance,
                              @Advice.Return Object result,
                              @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "field_get");
            
            String fieldName = extractFieldName(field);
            String className = extractFieldClassName(field);
            
            metadata.put("field_name", fieldName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            
            // Check for access to sensitive fields
            if (isSensitiveField(fieldName, className)) {
                metadata.put("sensitive_field_access", "true");
                metadata.put("risk_level", "MEDIUM");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            if (result != null) {
                metadata.put("result_type", result.getClass().getName());
            }
            
            logProxy.logMessage(NAMESPACE + ".field_get", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Field.set() calls to detect unauthorized field modification.
     */
    @Advice.OnMethodExit
    public static void fieldSet(@Advice.This Object field,
                              @Advice.Argument(0) Object instance,
                              @Advice.Argument(1) Object value,
                              @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "field_set");
            
            String fieldName = extractFieldName(field);
            String className = extractFieldClassName(field);
            
            metadata.put("field_name", fieldName);
            metadata.put("declaring_class", className);
            metadata.put("instance_provided", instance != null ? "true" : "false");
            metadata.put("value_provided", value != null ? "true" : "false");
            
            if (value != null) {
                metadata.put("value_type", value.getClass().getName());
            }
            
            // Check for modification of sensitive fields
            if (isSensitiveField(fieldName, className)) {
                metadata.put("sensitive_field_modification", "true");
                metadata.put("risk_level", "HIGH");
                metadata.put("threat_type", "privilege_escalation");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".field_set", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts setAccessible() calls to detect bypassing of access controls.
     */
    @Advice.OnMethodExit
    public static void setAccessible(@Advice.This Object accessibleObject,
                                   @Advice.Argument(0) boolean flag,
                                   @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "setAccessible");
            metadata.put("accessible_flag", String.valueOf(flag));
            metadata.put("object_type", accessibleObject.getClass().getSimpleName());
            
            if (flag) {
                metadata.put("access_control_bypass", "true");
                metadata.put("risk_level", "MEDIUM");
                metadata.put("threat_type", "privilege_escalation");
                
                // Get details about what's being made accessible
                String targetInfo = extractAccessibleObjectInfo(accessibleObject);
                metadata.put("target_info", targetInfo);
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            String callContext = analyzeCallStack();
            metadata.put("call_context", callContext);
            
            logProxy.logMessage(NAMESPACE + ".setAccessible", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    // Helper methods
    private static String extractMethodName(Object method) {
        try {
            Object name = method.getClass().getMethod("getName").invoke(method);
            return name != null ? name.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String extractDeclaringClassName(Object method) {
        try {
            Object declaringClass = method.getClass().getMethod("getDeclaringClass").invoke(method);
            if (declaringClass != null) {
                Object name = declaringClass.getClass().getMethod("getName").invoke(declaringClass);
                return name != null ? name.toString() : "unknown";
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    private static String extractConstructorClassName(Object constructor) {
        try {
            Object declaringClass = constructor.getClass().getMethod("getDeclaringClass").invoke(constructor);
            if (declaringClass != null) {
                Object name = declaringClass.getClass().getMethod("getName").invoke(declaringClass);
                return name != null ? name.toString() : "unknown";
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    private static String extractFieldName(Object field) {
        try {
            Object name = field.getClass().getMethod("getName").invoke(field);
            return name != null ? name.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String extractFieldClassName(Object field) {
        try {
            Object declaringClass = field.getClass().getMethod("getDeclaringClass").invoke(field);
            if (declaringClass != null) {
                Object name = declaringClass.getClass().getMethod("getName").invoke(declaringClass);
                return name != null ? name.toString() : "unknown";
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    private static String extractAccessibleObjectInfo(Object accessibleObject) {
        try {
            if (accessibleObject.getClass().getSimpleName().equals("Method")) {
                return "Method: " + extractMethodName(accessibleObject) + 
                       " in " + extractDeclaringClassName(accessibleObject);
            } else if (accessibleObject.getClass().getSimpleName().equals("Field")) {
                return "Field: " + extractFieldName(accessibleObject) + 
                       " in " + extractFieldClassName(accessibleObject);
            } else if (accessibleObject.getClass().getSimpleName().equals("Constructor")) {
                return "Constructor: " + extractConstructorClassName(accessibleObject);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    private static boolean isSuspiciousMethod(String methodName, String className) {
        // Check method name
        for (String suspicious : SUSPICIOUS_METHODS) {
            if (methodName.equals(suspicious)) {
                return true;
            }
        }
        
        // Check class-method combinations
        if (className.contains("Runtime") && "exec".equals(methodName)) {
            return true;
        }
        if (className.contains("ProcessBuilder") && "start".equals(methodName)) {
            return true;
        }
        if (className.contains("System") && ("exit".equals(methodName) || "setSecurityManager".equals(methodName))) {
            return true;
        }
        
        return false;
    }

    private static boolean isSensitiveField(String fieldName, String className) {
        // Security-related fields
        if (fieldName.contains("password") || fieldName.contains("secret") || fieldName.contains("key")) {
            return true;
        }
        
        // System property fields
        if (className.contains("System") && fieldName.contains("props")) {
            return true;
        }
        
        // SecurityManager fields
        if (className.contains("System") && fieldName.contains("security")) {
            return true;
        }
        
        return false;
    }

    private static String analyzeCallStack() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            StringBuilder context = new StringBuilder();
            
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("org.jvmxray") && 
                    !className.startsWith("java.lang.reflect") &&
                    !className.startsWith("sun.") &&
                    !className.startsWith("net.bytebuddy") &&
                    context.length() < 200) {
                    context.append(className).append(".").append(element.getMethodName()).append(";");
                }
            }
            
            return context.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}