package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Cipher.init() to monitor key usage and modes.
 * 
 * @author Milton Smith
 */
public class CipherInitInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Cipher.init() to monitor key usage and modes.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void cipherInit(@Advice.This Object cipher,
                                @Advice.Argument(0) int opmode,
                                @Advice.Argument(1) Object key,
                                @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "cipher_init");
            metadata.put("cipher_class", cipher.getClass().getName());
            
            // Map operation mode
            String mode;
            switch (opmode) {
                case 1:
                    mode = "ENCRYPT_MODE";
                    break;
                case 2:
                    mode = "DECRYPT_MODE";
                    break;
                case 3:
                    mode = "WRAP_MODE";
                    break;
                case 4:
                    mode = "UNWRAP_MODE";
                    break;
                default:
                    mode = "UNKNOWN_MODE";
                    break;
            }
            metadata.put("operation_mode", mode);
            
            if (key != null) {
                metadata.put("key_class", key.getClass().getSimpleName());
                metadata.put("key_algorithm", CryptoUtils.extractKeyAlgorithm(key));
                
                // Check key strength
                int keyLength = CryptoUtils.extractKeyLength(key);
                if (keyLength > 0) {
                    metadata.put("key_length", String.valueOf(keyLength));
                    
                    // Flag weak key lengths
                    if (keyLength < 128) {
                        metadata.put("weak_key_length", "true");
                        metadata.put("risk_level", "HIGH");
                    } else if (keyLength < 256) {
                        metadata.put("moderate_key_length", "true");
                        metadata.put("risk_level", "MEDIUM");
                    }
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".cipher_init", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}