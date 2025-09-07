package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for KeyStore.load() to monitor keystore operations.
 * 
 * @author Milton Smith
 */
public class KeyStoreInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts KeyStore.load() to monitor keystore operations.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void keyStoreLoad(@Advice.This Object keyStore,
                                  @Advice.Argument(0) Object inputStream,
                                  @Advice.Argument(1) char[] password,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "keyStore_load");
            metadata.put("keystore_class", keyStore.getClass().getName());
            metadata.put("has_inputstream", inputStream != null ? "true" : "false");
            metadata.put("has_password", password != null ? "true" : "false");
            
            if (password != null && password.length < 8) {
                metadata.put("weak_password", "true");
                metadata.put("risk_level", "HIGH");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".keystore", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}