package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Cipher operations to detect weak algorithms and configurations.
 * 
 * @author Milton Smith
 */
public class CipherInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Cipher.getInstance() to detect weak cipher algorithms.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void cipherGetInstance(@Advice.Argument(0) String transformation,
                                       @Advice.Return Object result,
                                       @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "cipher_getInstance");
            metadata.put("transformation", transformation != null ? transformation : "unknown");
            
            if (transformation != null) {
                String[] parts = transformation.split("/");
                String algorithm = parts[0].toUpperCase();
                metadata.put("algorithm", algorithm);
                
                // Check for weak algorithms
                for (String weakCipher : CryptoUtils.WEAK_CIPHERS) {
                    if (algorithm.contains(weakCipher)) {
                        metadata.put("weak_algorithm", "true");
                        metadata.put("risk_level", "HIGH");
                        metadata.put("weakness_type", "deprecated_algorithm");
                        break;
                    }
                }
                
                // Check for missing padding or mode
                if (parts.length < 3) {
                    metadata.put("incomplete_transformation", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "MEDIUM"));
                }
            }
            
            if (result != null) {
                metadata.put("cipher_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".cipher", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently to avoid disrupting crypto operations
        }
    }
}