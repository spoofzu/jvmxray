package org.jvmxray.agent.sensor.crypto;

/**
 * Utility methods shared by crypto interceptors.
 * 
 * @author Milton Smith
 */
public class CryptoUtils {
    
    // Known weak algorithms and configurations
    public static final String[] WEAK_CIPHERS = {
        "DES", "RC4", "MD5", "SHA1", "3DES", "DESede", "RC2", "ARCFOUR"
    };
    
    public static final String[] WEAK_SSL_CIPHERS = {
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "SSL_DH_anon_WITH_DES_CBC_SHA"
    };

    /**
     * Extracts the algorithm name from a key object.
     */
    public static String extractKeyAlgorithm(Object key) {
        try {
            Object result = key.getClass().getMethod("getAlgorithm").invoke(key);
            return result != null ? result.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extracts the key length from a key object.
     */
    public static int extractKeyLength(Object key) {
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