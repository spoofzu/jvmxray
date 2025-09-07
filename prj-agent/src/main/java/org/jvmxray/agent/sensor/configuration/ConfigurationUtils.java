package org.jvmxray.agent.sensor.configuration;

/**
 * Utility methods shared by configuration interceptors.
 * 
 * @author Milton Smith
 */
public class ConfigurationUtils {
    
    // Sensitive system properties that should be monitored
    public static final String[] SENSITIVE_PROPERTIES = {
        "java.security.policy",
        "java.security.manager",
        "java.security.auth.login.config",
        "java.library.path",
        "java.class.path",
        "java.ext.dirs",
        "java.endorsed.dirs",
        "user.dir",
        "user.home",
        "java.io.tmpdir",
        "file.separator",
        "path.separator",
        "line.separator",
        "os.name",
        "os.arch",
        "os.version",
        "java.version",
        "java.vendor",
        "java.home"
    };

    // Sensitive environment variables
    public static final String[] SENSITIVE_ENV_VARS = {
        "PATH",
        "JAVA_HOME",
        "CLASSPATH",
        "LD_LIBRARY_PATH",
        "DYLD_LIBRARY_PATH",
        "HOME",
        "USER",
        "USERNAME",
        "TEMP",
        "TMP",
        "PASSWORD",
        "SECRET",
        "TOKEN",
        "KEY",
        "API_KEY"
    };

    /**
     * Checks if a system property is considered sensitive.
     */
    public static boolean isSensitiveProperty(String key) {
        if (key == null) return false;
        
        for (String sensitive : SENSITIVE_PROPERTIES) {
            if (key.equals(sensitive)) {
                return true;
            }
        }
        
        return key.contains("password") || 
               key.contains("secret") || 
               key.contains("key") ||
               key.contains("token");
    }

    /**
     * Checks if an environment variable is considered sensitive.
     */
    public static boolean isSensitiveEnvironmentVariable(String name) {
        if (name == null) return false;
        
        String upper = name.toUpperCase();
        for (String sensitive : SENSITIVE_ENV_VARS) {
            if (upper.equals(sensitive) || upper.contains(sensitive)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Truncates a value for logging purposes.
     */
    public static String truncateValue(String value) {
        if (value == null) return null;
        if (value.length() <= 100) return value;
        return value.substring(0, 100) + "...[truncated]";
    }

    /**
     * Gets the property count from a Properties object.
     */
    public static int getPropertyCount(Object properties) {
        try {
            Object result = properties.getClass().getMethod("size").invoke(properties);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Analyzes the call stack to determine context.
     */
    public static String analyzeCallContext() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("org.jvmxray") && 
                    !className.startsWith("java.lang.System") &&
                    !className.startsWith("java.util.Properties") &&
                    !className.startsWith("java.util.prefs") &&
                    !className.startsWith("sun.") &&
                    !className.startsWith("net.bytebuddy")) {
                    return className + "." + element.getMethodName();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }
}