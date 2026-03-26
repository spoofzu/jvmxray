package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for MessageDigest.getInstance() to detect weak hash algorithms.
 * 
 * @author Milton Smith
 */
public class MessageDigestInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Enter advice for MessageDigest.getInstance() — establishes MCCScope.
     */
    @Advice.OnMethodEnter
    public static long enter() {
        MCCScope.enter("Crypto");
        return System.nanoTime();
    }

    /**
     * Intercepts MessageDigest.getInstance() to detect weak hash algorithms.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void messageDigestGetInstance(@Advice.Enter long startTime,
                                              @Advice.Argument(0) String algorithm,
                                              @Advice.Return Object result,
                                              @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "messageDigest_getInstance");
            metadata.put("algorithm", algorithm != null ? algorithm.toUpperCase() : "unknown");

            if (algorithm != null) {
                String upperAlgo = algorithm.toUpperCase();

                // Check for weak hash algorithms
                if (upperAlgo.contains("MD5")) {
                    metadata.put("weak_algorithm", "true");
                    metadata.put("risk_level", "CRITICAL");
                    metadata.put("weakness_type", "collision_vulnerable");
                } else if (upperAlgo.contains("SHA1") || upperAlgo.equals("SHA")) {
                    metadata.put("weak_algorithm", "true");
                    metadata.put("risk_level", "HIGH");
                    metadata.put("weakness_type", "deprecated_hash");
                }
            }

            if (result != null) {
                metadata.put("digest_class", result.getClass().getName());
            }

            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }

            logProxy.logMessage(NAMESPACE + ".digest", "INFO", metadata);

        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Crypto");
        }
    }
}