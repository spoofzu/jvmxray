package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for configuration operations to detect unauthorized system
 * property modifications, environment variable access, and configuration
 * file tampering.
 * 
 * @author Milton Smith
 */
public class ConfigurationInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Sensitive system properties that should be monitored
    private static final String[] SENSITIVE_PROPERTIES = {
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
    private static final String[] SENSITIVE_ENV_VARS = {
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
     * Intercepts System.getProperty() calls to monitor property access.
     */
    @Advice.OnMethodExit
    public static void systemGetProperty(@Advice.Argument(0) String key,
                                       @Advice.Return String result,
                                       @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_getProperty");
            metadata.put("property_key", key != null ? key : "unknown");
            metadata.put("value_retrieved", result != null ? "true" : "false");
            
            if (key != null && isSensitiveProperty(key)) {
                metadata.put("sensitive_property", "true");
                metadata.put("risk_level", "MEDIUM");
            }
            
            // Flag access to security-related properties
            if (key != null && (key.contains("security") || key.contains("policy"))) {
                metadata.put("security_property_access", "true");
                metadata.put("risk_level", "HIGH");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            // Don't log the actual value for sensitive properties
            if (result != null && !isSensitiveProperty(key)) {
                metadata.put("property_value", truncateValue(result));
            }
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".property", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts System.setProperty() calls to monitor property modifications.
     */
    @Advice.OnMethodExit
    public static void systemSetProperty(@Advice.Argument(0) String key,
                                       @Advice.Argument(1) String value,
                                       @Advice.Return String result,
                                       @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_setProperty");
            metadata.put("property_key", key != null ? key : "unknown");
            metadata.put("modification_success", throwable == null ? "true" : "false");
            
            if (key != null) {
                if (isSensitiveProperty(key)) {
                    metadata.put("sensitive_property_modification", "true");
                    metadata.put("risk_level", "HIGH");
                    metadata.put("threat_type", "privilege_escalation");
                }
                
                // Critical security property modifications
                if (key.equals("java.security.manager") || key.equals("java.security.policy")) {
                    metadata.put("critical_security_modification", "true");
                    metadata.put("risk_level", "CRITICAL");
                }
                
                // Path manipulation attempts
                if (key.contains("path") || key.contains("dir")) {
                    metadata.put("path_modification", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "MEDIUM"));
                }
            }
            
            if (result != null) {
                metadata.put("previous_value_existed", "true");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Don't log actual values for sensitive properties
            if (value != null && !isSensitiveProperty(key)) {
                metadata.put("new_value", truncateValue(value));
            }
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".property", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts System.getenv() calls to monitor environment variable access.
     */
    @Advice.OnMethodExit
    public static void systemGetEnv(@Advice.Argument(0) String name,
                                  @Advice.Return String result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_getenv");
            
            if (name != null) {
                metadata.put("env_var_name", name);
                metadata.put("value_retrieved", result != null ? "true" : "false");
                
                if (isSensitiveEnvironmentVariable(name)) {
                    metadata.put("sensitive_env_access", "true");
                    metadata.put("risk_level", "MEDIUM");
                }
                
                // Flag access to authentication/security variables
                if (name.toUpperCase().contains("PASSWORD") || 
                    name.toUpperCase().contains("SECRET") ||
                    name.toUpperCase().contains("TOKEN") ||
                    name.toUpperCase().contains("KEY")) {
                    metadata.put("credential_env_access", "true");
                    metadata.put("risk_level", "HIGH");
                }
            } else {
                metadata.put("operation", "system_getenv_all");
                metadata.put("env_map_retrieved", result != null ? "true" : "false");
                metadata.put("bulk_env_access", "true");
                metadata.put("risk_level", "MEDIUM");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".environment", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Properties.load() to monitor configuration file loading.
     */
    @Advice.OnMethodExit
    public static void propertiesLoad(@Advice.This Object properties,
                                    @Advice.Argument(0) Object inputStream,
                                    @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "properties_load");
            metadata.put("input_stream_class", inputStream != null ? inputStream.getClass().getName() : "null");
            metadata.put("load_success", throwable == null ? "true" : "false");
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Try to get property count after loading
            if (throwable == null) {
                int propertyCount = getPropertyCount(properties);
                if (propertyCount > 0) {
                    metadata.put("property_count", String.valueOf(propertyCount));
                }
            }
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".file", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Properties.store() to monitor configuration file writing.
     */
    @Advice.OnMethodExit
    public static void propertiesStore(@Advice.This Object properties,
                                     @Advice.Argument(0) Object outputStream,
                                     @Advice.Argument(1) String comments,
                                     @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "properties_store");
            metadata.put("output_stream_class", outputStream != null ? outputStream.getClass().getName() : "null");
            metadata.put("store_success", throwable == null ? "true" : "false");
            metadata.put("has_comments", comments != null ? "true" : "false");
            
            // Get property count being stored
            int propertyCount = getPropertyCount(properties);
            if (propertyCount > 0) {
                metadata.put("property_count", String.valueOf(propertyCount));
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            // Configuration modification is potentially risky
            metadata.put("config_modification", "true");
            metadata.put("risk_level", "MEDIUM");
            
            logProxy.logMessage(NAMESPACE + ".file", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Preferences.get() to monitor preferences access.
     */
    @Advice.OnMethodExit
    public static void preferencesGet(@Advice.This Object preferences,
                                    @Advice.Argument(0) String key,
                                    @Advice.Argument(1) String defaultValue,
                                    @Advice.Return String result,
                                    @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "preferences_get");
            metadata.put("preference_key", key != null ? key : "unknown");
            metadata.put("has_default", defaultValue != null ? "true" : "false");
            metadata.put("value_retrieved", result != null ? "true" : "false");
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".preferences", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Preferences.put() to monitor preferences modification.
     */
    @Advice.OnMethodExit
    public static void preferencesPut(@Advice.This Object preferences,
                                    @Advice.Argument(0) String key,
                                    @Advice.Argument(1) String value,
                                    @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "preferences_put");
            metadata.put("preference_key", key != null ? key : "unknown");
            metadata.put("modification_success", throwable == null ? "true" : "false");
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            // Preferences modification
            metadata.put("preference_modification", "true");
            metadata.put("risk_level", "LOW");
            
            logProxy.logMessage(NAMESPACE + ".preferences", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    // Helper methods
    private static boolean isSensitiveProperty(String key) {
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

    private static boolean isSensitiveEnvironmentVariable(String name) {
        if (name == null) return false;
        
        String upper = name.toUpperCase();
        for (String sensitive : SENSITIVE_ENV_VARS) {
            if (upper.equals(sensitive) || upper.contains(sensitive)) {
                return true;
            }
        }
        
        return false;
    }

    private static String truncateValue(String value) {
        if (value == null) return null;
        if (value.length() <= 100) return value;
        return value.substring(0, 100) + "...[truncated]";
    }

    private static int getPropertyCount(Object properties) {
        try {
            Object result = properties.getClass().getMethod("size").invoke(properties);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String analyzeCallContext() {
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