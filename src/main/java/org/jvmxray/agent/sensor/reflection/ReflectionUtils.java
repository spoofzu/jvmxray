package org.jvmxray.agent.sensor.reflection;

/**
 * Utility methods shared by reflection interceptors.
 * 
 * @author Milton Smith
 */
public class ReflectionUtils {
    
    // Suspicious classes that should be monitored when accessed via reflection
    public static final String[] SUSPICIOUS_CLASSES = {
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
    public static final String[] SUSPICIOUS_METHODS = {
        "exec", "getRuntime", "load", "loadLibrary", "exit", "setSecurityManager",
        "getProperty", "setProperty", "getenv", "defineClass", "newInstance",
        "invoke", "get", "set", "setAccessible", "forName"
    };

    /**
     * Extracts method name from a Method object using reflection.
     */
    public static String extractMethodName(Object method) {
        try {
            Object name = method.getClass().getMethod("getName").invoke(method);
            return name != null ? name.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extracts declaring class name from a Method object using reflection.
     */
    public static String extractDeclaringClassName(Object method) {
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

    /**
     * Extracts class name from a Constructor object using reflection.
     */
    public static String extractConstructorClassName(Object constructor) {
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

    /**
     * Extracts field name from a Field object using reflection.
     */
    public static String extractFieldName(Object field) {
        try {
            Object name = field.getClass().getMethod("getName").invoke(field);
            return name != null ? name.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extracts declaring class name from a Field object using reflection.
     */
    public static String extractFieldClassName(Object field) {
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

    /**
     * Extracts information about an AccessibleObject.
     */
    public static String extractAccessibleObjectInfo(Object accessibleObject) {
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

    /**
     * Checks if a method name and class combination is suspicious.
     */
    public static boolean isSuspiciousMethod(String methodName, String className) {
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

    /**
     * Checks if a field is considered sensitive.
     */
    public static boolean isSensitiveField(String fieldName, String className) {
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

    /**
     * Analyzes the call stack to determine context.
     */
    public static String analyzeCallStack() {
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