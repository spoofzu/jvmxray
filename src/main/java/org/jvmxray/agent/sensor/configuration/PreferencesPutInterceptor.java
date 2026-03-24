package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Preferences.put() to monitor preferences modification.
 * 
 * @author Milton Smith
 */
public class PreferencesPutInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Preferences.put() to monitor preferences modification.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
}