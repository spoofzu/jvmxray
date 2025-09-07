package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for System.getenv() calls (no parameters) to monitor bulk environment variable access.
 * 
 * @author Milton Smith
 */
public class SystemGetEnvAllInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts System.getenv() calls (no parameters) to monitor bulk environment variable access.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void systemGetEnvAll(@Advice.Return Map<String, String> result,
                                      @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_getenv_all");
            metadata.put("env_map_retrieved", result != null ? "true" : "false");
            metadata.put("bulk_env_access", "true");
            metadata.put("risk_level", "MEDIUM");
            
            if (result != null) {
                metadata.put("env_var_count", String.valueOf(result.size()));
                
                // Check if any sensitive environment variables are present
                boolean hasSensitive = false;
                boolean hasCredentials = false;
                for (String key : result.keySet()) {
                    if (ConfigurationUtils.isSensitiveEnvironmentVariable(key)) {
                        hasSensitive = true;
                    }
                    
                    String upperKey = key.toUpperCase();
                    if (upperKey.contains("PASSWORD") || 
                        upperKey.contains("SECRET") ||
                        upperKey.contains("TOKEN") ||
                        upperKey.contains("KEY")) {
                        hasCredentials = true;
                    }
                }
                
                if (hasSensitive) {
                    metadata.put("sensitive_env_access", "true");
                    metadata.put("risk_level", "HIGH");
                }
                
                if (hasCredentials) {
                    metadata.put("credential_env_access", "true");
                    metadata.put("risk_level", "HIGH");
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".environment", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}