package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for cryptographic operations to detect weak algorithms,
 * improper key management, and potential cryptographic attacks.
 * 
 * @author Milton Smith
 */
public class CryptoInterceptor {
    
    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Known weak algorithms and configurations
    private static final String[] WEAK_CIPHERS = {
        "DES", "RC4", "MD5", "SHA1", "3DES", "DESede", "RC2", "ARCFOUR"
    };
    
    private static final String[] WEAK_SSL_CIPHERS = {
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "SSL_DH_anon_WITH_DES_CBC_SHA"
    };

    /**
     * Intercepts Cipher.getInstance() to detect weak cipher algorithms.
     * Enhanced with compliance checking (FIPS, PCI-DSS, NIST) and replacement suggestions.
     */
    @Advice.OnMethodExit
    public static void cipherGetInstance(@Advice.Argument(0) String transformation,
                                       @Advice.Return Object result,
                                       @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Crypto");
        try {
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sensor_class", "CryptoInterceptor");
                metadata.put("sensor_method", "cipherGetInstance");
                metadata.put("operation", "cipher_getInstance");
                metadata.put("transformation", transformation != null ? transformation : "unknown");

                if (transformation != null) {
                    String[] parts = transformation.split("/");
                    String algorithm = parts[0].toUpperCase();
                    metadata.put("algorithm", algorithm);

                    // Check for weak algorithms
                    boolean isWeak = CryptoUtils.isWeakAlgorithm(algorithm);
                    if (isWeak) {
                        metadata.put("weak_algorithm", "true");
                        metadata.put("risk_level", "HIGH");
                        metadata.put("weakness_type", "deprecated_algorithm");
                    }

                    // Add compliance metadata
                    Map<String, String> compliance = CryptoUtils.getComplianceMetadata(algorithm, -1);
                    metadata.putAll(compliance);

                    // Check for missing padding or mode
                    if (parts.length < 3) {
                        metadata.put("incomplete_transformation", "true");
                        if (!metadata.containsKey("risk_level")) {
                            metadata.put("risk_level", "MEDIUM");
                        }
                    }

                    // Extract mode and padding if present
                    if (parts.length >= 2) {
                        metadata.put("mode", parts[1]);
                    }
                    if (parts.length >= 3) {
                        metadata.put("padding", parts[2]);
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
        } finally {
            MCCScope.exit("Crypto");
        }
    }

    /**
     * Intercepts Cipher.init() to monitor key usage and modes.
     * Enhanced with key strength analysis and compliance checking.
     */
    @Advice.OnMethodExit
    public static void cipherInit(@Advice.This Object cipher,
                                @Advice.Argument(0) int opmode,
                                @Advice.Argument(1) Object key,
                                @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Crypto");
        try {
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sensor_class", "CryptoInterceptor");
                metadata.put("sensor_method", "cipherInit");
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

                    String keyAlgorithm = CryptoUtils.extractKeyAlgorithm(key);
                    metadata.put("key_algorithm", keyAlgorithm);

                    // Check key strength
                    int keyLength = CryptoUtils.extractKeyLength(key);
                    if (keyLength > 0) {
                        metadata.put("key_length_bits", String.valueOf(keyLength));

                        // Flag weak key lengths
                        if (keyLength < 128) {
                            metadata.put("weak_key_length", "true");
                            metadata.put("risk_level", "HIGH");
                        } else if (keyLength < 256) {
                            metadata.put("moderate_key_length", "true");
                            if (!metadata.containsKey("risk_level")) {
                                metadata.put("risk_level", "MEDIUM");
                            }
                        }

                        // Add compliance metadata with key length
                        Map<String, String> compliance = CryptoUtils.getComplianceMetadata(keyAlgorithm, keyLength);
                        metadata.putAll(compliance);
                    } else {
                        // Add compliance metadata without key length
                        Map<String, String> compliance = CryptoUtils.getComplianceMetadata(keyAlgorithm, -1);
                        metadata.putAll(compliance);
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
        } finally {
            MCCScope.exit("Crypto");
        }
    }

    /**
     * Intercepts MessageDigest.getInstance() to detect weak hash algorithms.
     * Enhanced with compliance checking and replacement suggestions.
     */
    @Advice.OnMethodExit
    public static void messageDigestGetInstance(@Advice.Argument(0) String algorithm,
                                              @Advice.Return Object result,
                                              @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Crypto");
        try {
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sensor_class", "CryptoInterceptor");
                metadata.put("sensor_method", "messageDigestGetInstance");
                metadata.put("operation", "messageDigest_getInstance");
                metadata.put("algorithm", algorithm != null ? algorithm.toUpperCase() : "unknown");

                if (algorithm != null) {
                    String upperAlgo = algorithm.toUpperCase();

                    // Check for weak hash algorithms
                    if (upperAlgo.contains("MD5") || upperAlgo.contains("MD4")) {
                        metadata.put("weak_algorithm", "true");
                        metadata.put("risk_level", "CRITICAL");
                        metadata.put("weakness_type", "collision_vulnerable");
                    } else if (upperAlgo.contains("SHA1") || upperAlgo.equals("SHA")) {
                        metadata.put("weak_algorithm", "true");
                        metadata.put("risk_level", "HIGH");
                        metadata.put("weakness_type", "deprecated_hash");
                    }

                    // Add compliance metadata
                    Map<String, String> compliance = CryptoUtils.getComplianceMetadata(algorithm, -1);
                    metadata.putAll(compliance);
                }

                if (result != null) {
                    metadata.put("digest_class", result.getClass().getName());

                    // Get digest length if available
                    try {
                        java.lang.reflect.Method getLengthMethod = result.getClass().getMethod("getDigestLength");
                        int digestLength = (Integer) getLengthMethod.invoke(result);
                        metadata.put("digest_length_bytes", String.valueOf(digestLength));
                        metadata.put("digest_length_bits", String.valueOf(digestLength * 8));
                    } catch (Exception e) {
                        // Digest length not available
                    }
                }

                if (throwable != null) {
                    metadata.put("error", throwable.getClass().getSimpleName());
                }

                logProxy.logMessage(NAMESPACE + ".digest", "INFO", metadata);

            } catch (Exception e) {
                // Fail silently
            }
        } finally {
            MCCScope.exit("Crypto");
        }
    }

    /**
     * Intercepts KeyStore.load() to monitor keystore operations.
     */
    @Advice.OnMethodExit
    public static void keyStoreLoad(@Advice.This Object keyStore,
                                  @Advice.Argument(0) Object inputStream,
                                  @Advice.Argument(1) char[] password,
                                  @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Crypto");
        try {
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sensor_class", "CryptoInterceptor");
                metadata.put("sensor_method", "keyStoreLoad");
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
        } finally {
            MCCScope.exit("Crypto");
        }
    }

    /**
     * Intercepts SSLSocket.setEnabledCipherSuites() to detect weak SSL configurations.
     */
    @Advice.OnMethodExit
    public static void sslSetCipherSuites(@Advice.This Object sslSocket,
                                        @Advice.Argument(0) String[] cipherSuites,
                                        @Advice.Thrown Throwable throwable) {
        MCCScope.enter("Crypto");
        try {
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sensor_class", "CryptoInterceptor");
                metadata.put("sensor_method", "sslSetCipherSuites");
                metadata.put("operation", "ssl_setCipherSuites");
                metadata.put("socket_class", sslSocket.getClass().getName());

                if (cipherSuites != null) {
                    metadata.put("cipher_count", String.valueOf(cipherSuites.length));

                    // Check for weak SSL cipher suites
                    int weakCount = 0;
                    StringBuilder weakCiphers = new StringBuilder();

                    for (String cipher : cipherSuites) {
                        for (String weakCipher : WEAK_SSL_CIPHERS) {
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
        } finally {
            MCCScope.exit("Crypto");
        }
    }

    // Helper methods
    private static String extractKeyAlgorithm(Object key) {
        try {
            Object result = key.getClass().getMethod("getAlgorithm").invoke(key);
            return result != null ? result.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static int extractKeyLength(Object key) {
        try {
            // Try to get encoded key and calculate length
            Object encoded = key.getClass().getMethod("getEncoded").invoke(key);
            if (encoded instanceof byte[]) {
                return ((byte[]) encoded).length * 8; // Convert to bits
            }
        } catch (Exception e) {
            // Try alternative methods or return -1 if unavailable
        }
        return -1;
    }
}