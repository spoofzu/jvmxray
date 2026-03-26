package org.jvmxray.agent.sensor.crypto;

/**
 * Utility methods shared by crypto interceptors.
 * Enhanced with compliance checking (FIPS, PCI-DSS, NIST) and recommendation metadata.
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

    // FIPS 140-2/3 approved algorithms
    private static final String[] FIPS_APPROVED_CIPHERS = {
        "AES", "AES-128", "AES-192", "AES-256",
        "Triple-DES", "3DES", "DESede"  // Note: 3DES approved but deprecated
    };

    private static final String[] FIPS_APPROVED_HASHES = {
        "SHA-224", "SHA-256", "SHA-384", "SHA-512",
        "SHA-512/224", "SHA-512/256", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"
    };

    // NIST recommendation status
    private static final java.util.Map<String, String> NIST_STATUS = new java.util.HashMap<>();
    static {
        // Prohibited
        NIST_STATUS.put("DES", "prohibited");
        NIST_STATUS.put("RC4", "prohibited");
        NIST_STATUS.put("MD4", "prohibited");
        NIST_STATUS.put("MD5", "prohibited");
        NIST_STATUS.put("SHA1", "deprecated");
        NIST_STATUS.put("SHA-1", "deprecated");

        // Deprecated (acceptable through specific date)
        NIST_STATUS.put("3DES", "deprecated_2023");
        NIST_STATUS.put("DESede", "deprecated_2023");
        NIST_STATUS.put("Triple-DES", "deprecated_2023");

        // Current (recommended)
        NIST_STATUS.put("AES", "current");
        NIST_STATUS.put("AES-128", "current");
        NIST_STATUS.put("AES-192", "current");
        NIST_STATUS.put("AES-256", "current");
        NIST_STATUS.put("SHA-256", "current");
        NIST_STATUS.put("SHA-384", "current");
        NIST_STATUS.put("SHA-512", "current");
        NIST_STATUS.put("SHA3-256", "current");
        NIST_STATUS.put("SHA3-384", "current");
        NIST_STATUS.put("SHA3-512", "current");
    }

    // Algorithm replacement suggestions
    private static final java.util.Map<String, String> REPLACEMENTS = new java.util.HashMap<>();
    static {
        REPLACEMENTS.put("DES", "AES-256");
        REPLACEMENTS.put("3DES", "AES-256");
        REPLACEMENTS.put("DESede", "AES-256");
        REPLACEMENTS.put("RC4", "AES-256-GCM");
        REPLACEMENTS.put("RC2", "AES-256");
        REPLACEMENTS.put("MD5", "SHA-256");
        REPLACEMENTS.put("MD4", "SHA-256");
        REPLACEMENTS.put("SHA1", "SHA-256");
        REPLACEMENTS.put("SHA-1", "SHA-256");
        REPLACEMENTS.put("ARCFOUR", "AES-256-GCM");
    }

    // Algorithm deprecation years
    private static final java.util.Map<String, String> DEPRECATION_YEARS = new java.util.HashMap<>();
    static {
        DEPRECATION_YEARS.put("DES", "1999");
        DEPRECATION_YEARS.put("MD5", "2008");
        DEPRECATION_YEARS.put("SHA1", "2011");
        DEPRECATION_YEARS.put("SHA-1", "2011");
        DEPRECATION_YEARS.put("RC4", "2015");
        DEPRECATION_YEARS.put("3DES", "2023");
        DEPRECATION_YEARS.put("DESede", "2023");
    }

    // Minimum key lengths for PCI-DSS compliance
    private static final int PCI_DSS_MIN_RSA_KEY_BITS = 2048;
    private static final int PCI_DSS_MIN_AES_KEY_BITS = 128;

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

    /**
     * Checks if an algorithm is FIPS 140-2/3 compliant.
     */
    public static boolean isFIPSCompliant(String algorithm) {
        if (algorithm == null) return false;
        String upper = algorithm.toUpperCase();

        // Check approved ciphers
        for (String approved : FIPS_APPROVED_CIPHERS) {
            if (upper.contains(approved.toUpperCase())) {
                return true;
            }
        }

        // Check approved hashes
        for (String approved : FIPS_APPROVED_HASHES) {
            if (upper.contains(approved.toUpperCase().replace("-", ""))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an algorithm with given key length is PCI-DSS compliant.
     */
    public static boolean isPCIDSSCompliant(String algorithm, int keyLengthBits) {
        if (algorithm == null) return false;
        String upper = algorithm.toUpperCase();

        // Check for prohibited algorithms
        for (String weak : WEAK_CIPHERS) {
            if (upper.contains(weak)) {
                return false;
            }
        }

        // Check key lengths
        if (upper.contains("RSA")) {
            return keyLengthBits >= PCI_DSS_MIN_RSA_KEY_BITS;
        }

        if (upper.contains("AES")) {
            return keyLengthBits >= PCI_DSS_MIN_AES_KEY_BITS;
        }

        // Default to compliant if not a known weak algorithm
        return true;
    }

    /**
     * Gets the NIST recommendation status for an algorithm.
     */
    public static String getNISTRecommendation(String algorithm) {
        if (algorithm == null) return "unknown";
        String upper = algorithm.toUpperCase();

        for (java.util.Map.Entry<String, String> entry : NIST_STATUS.entrySet()) {
            if (upper.contains(entry.getKey().toUpperCase())) {
                return entry.getValue();
            }
        }

        return "unclassified";
    }

    /**
     * Gets the deprecation year for an algorithm.
     */
    public static String getDeprecationYear(String algorithm) {
        if (algorithm == null) return null;
        String upper = algorithm.toUpperCase();

        for (java.util.Map.Entry<String, String> entry : DEPRECATION_YEARS.entrySet()) {
            if (upper.contains(entry.getKey().toUpperCase())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets a suggested replacement for a weak algorithm.
     */
    public static String getSuggestedReplacement(String algorithm) {
        if (algorithm == null) return null;
        String upper = algorithm.toUpperCase();

        for (java.util.Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            if (upper.contains(entry.getKey().toUpperCase())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets comprehensive compliance metadata for an algorithm.
     */
    public static java.util.Map<String, String> getComplianceMetadata(String algorithm, int keyLengthBits) {
        java.util.Map<String, String> metadata = new java.util.HashMap<>();

        if (algorithm == null) {
            return metadata;
        }

        // Add key length if available
        if (keyLengthBits > 0) {
            metadata.put("key_length_bits", String.valueOf(keyLengthBits));
        }

        // FIPS compliance
        metadata.put("fips_compliant", String.valueOf(isFIPSCompliant(algorithm)));

        // PCI-DSS compliance
        metadata.put("pci_dss_compliant", String.valueOf(isPCIDSSCompliant(algorithm, keyLengthBits)));

        // NIST recommendation
        String nistStatus = getNISTRecommendation(algorithm);
        metadata.put("nist_recommendation", nistStatus);

        // Deprecation year
        String deprecationYear = getDeprecationYear(algorithm);
        if (deprecationYear != null) {
            metadata.put("algorithm_end_of_life", deprecationYear);
        }

        // Suggested replacement
        String replacement = getSuggestedReplacement(algorithm);
        if (replacement != null) {
            metadata.put("suggested_replacement", replacement);
        }

        return metadata;
    }

    /**
     * Checks if an algorithm is considered weak.
     */
    public static boolean isWeakAlgorithm(String algorithm) {
        if (algorithm == null) return false;
        String upper = algorithm.toUpperCase();

        for (String weak : WEAK_CIPHERS) {
            if (upper.contains(weak)) {
                return true;
            }
        }
        return false;
    }
}