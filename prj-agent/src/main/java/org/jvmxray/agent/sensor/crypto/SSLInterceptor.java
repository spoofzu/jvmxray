package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for SSLSocket.setEnabledCipherSuites() to detect weak SSL configurations.
 * 
 * @author Milton Smith
 */
public class SSLInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts SSLSocket.setEnabledCipherSuites() to detect weak SSL configurations.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void sslSetCipherSuites(@Advice.This Object sslSocket,
                                        @Advice.Argument(0) String[] cipherSuites,
                                        @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "ssl_setCipherSuites");
            metadata.put("socket_class", sslSocket.getClass().getName());
            
            if (cipherSuites != null) {
                metadata.put("cipher_count", String.valueOf(cipherSuites.length));
                
                // Check for weak SSL cipher suites
                int weakCount = 0;
                StringBuilder weakCiphers = new StringBuilder();
                
                for (String cipher : cipherSuites) {
                    for (String weakCipher : CryptoUtils.WEAK_SSL_CIPHERS) {
                        if (cipher.contains(weakCipher)) {
                            weakCount++;
                            if (weakCiphers.length() > 0) weakCiphers.append(",");
                            weakCiphers.append(cipher);
                            break;
                        }
                    }
                }
                
                if (weakCount > 0) {
                    metadata.put("weak_cipher_count", String.valueOf(weakCount));
                    metadata.put("weak_ciphers", weakCiphers.toString());
                    metadata.put("risk_level", "HIGH");
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".ssl", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}