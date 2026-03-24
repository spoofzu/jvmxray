package org.jvmxray.agent.sensor.configuration;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Preferences.get() to monitor preferences access.
 * 
 * @author Milton Smith
 */
public class PreferencesGetInterceptor {
    
    // Namespace for logging configuration events
    public static final String NAMESPACE = "org.jvmxray.events.config";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Preferences.get() to monitor preferences access.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
}