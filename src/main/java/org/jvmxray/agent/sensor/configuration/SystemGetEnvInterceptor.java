package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for System.getenv(String name) calls to monitor specific environment variable access.
 * 
 * @author Milton Smith
 */
public class SystemGetEnvInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts System.getenv(String name) calls to monitor specific environment variable access.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void systemGetEnv(@Advice.Argument(0) String name,
                                  @Advice.Return String result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "system_getenv");
            
            metadata.put("env_var_name", name);
            metadata.put("value_retrieved", result != null ? "true" : "false");
            
            if (ConfigurationUtils.isSensitiveEnvironmentVariable(name)) {
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
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".environment", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}