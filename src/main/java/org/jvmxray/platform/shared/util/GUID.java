package org.jvmxray.platform.shared.util;

import java.util.UUID;

/**
 * Utility class for generating globally unique identifiers (GUIDs) in the JVMXRay
 * framework. Produces compact, Base36-encoded UUIDs that are globally unique while
 * being significantly shorter than standard UUID representations.
 *
 * <p>This class provides multiple GUID formats:
 * <ul>
 * <li><strong>Compact format (~25 chars)</strong>: Base36-encoded UUID for storage efficiency</li>
 * <li><strong>Standard format (36 chars)</strong>: Standard UUID with dashes for external compatibility</li>
 * <li><strong>Short format (8 chars)</strong>: First segment only for human-readable contexts</li>
 * </ul>
 *
 * <p>Base36 encoding uses [0-9a-z] characters only, making GUIDs uniform,
 * URL/filesystem safe, and reducing storage requirements by approximately 30%.</p>
 *
 * @author Milton Smith
 */
public class GUID {

    /**
     * Base36 alphabet for encoding UUIDs (0-9, a-z lower-case only).
     */
    private static final String BASE36_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private GUID() {
        // Prevent instantiation
    }

    /**
     * Generates a compact GUID using Base36 encoding of a UUID v4.
     * This is the primary method for generating GUIDs in the system.
     *
     * @return A ~25-character Base36-encoded GUID (uppercase)
     */
    public static String generate() {
        UUID uuid = UUID.randomUUID();
        return encodeBase36(uuid);
    }

    /**
     * Generates a standard UUID v4 format with dashes (RFC 4122).
     * Uses uppercase hexadecimal letters to match macOS uuidgen format.
     * Use this when interfacing with external systems that expect standard UUID format.
     *
     * @return A 36-character standard UUID string (uppercase with dashes)
     */
    public static String generateStandard() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Generates a short 8-character GUID for human-readable contexts.
     * Use this for display purposes, logging where full uniqueness isn't critical.
     *
     * @return An 8-character string derived from a UUID
     */
    public static String generateShort() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Validates whether a string is a valid GUID in any supported format.
     * Supports Base36 compact format, standard UUID format, and short format.
     *
     * @param guid The string to validate
     * @return true if the string is a valid GUID, false otherwise
     */
    public static boolean isValid(String guid) {
        if (guid == null || guid.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = guid.trim();
        
        // Check Base36 format (~25 characters, Base36 alphabet)
        if (trimmed.length() <= 27 && trimmed.length() >= 20) {
            return isValidBase36(trimmed);
        }
        
        // Check standard UUID format (36 characters with dashes)
        if (trimmed.length() == 36) {
            return isValidUUID(trimmed);
        }
        
        // Check short format (8 characters, hex)
        if (trimmed.length() == 8) {
            return trimmed.matches("^[0-9a-fA-F]{8}$");
        }
        
        return false;
    }

    /**
     * Converts a standard UUID string to Base36 compact format.
     *
     * @param uuid Standard UUID string with dashes
     * @return Base36-encoded compact GUID (uppercase)
     * @throws IllegalArgumentException if the input is not a valid UUID
     */
    public static String toCompact(String uuid) {
        if (!isValidUUID(uuid)) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuid);
        }
        return encodeBase36(UUID.fromString(uuid));
    }

    /**
     * Converts a Base36 compact GUID to standard UUID format.
     *
     * @param compactGuid Base36-encoded compact GUID (uppercase)
     * @return Standard UUID string with dashes
     * @throws IllegalArgumentException if the input is not a valid Base36 GUID
     */
    public static String toStandard(String compactGuid) {
        if (!isValidBase36(compactGuid)) {
            throw new IllegalArgumentException("Invalid Base36 GUID format: " + compactGuid);
        }
        return decodeBase36(compactGuid).toString();
    }

    /**
     * Encodes a UUID as a Base36 string (uppercase).
     *
     * @param uuid The UUID to encode
     * @return Base36-encoded string (uppercase)
     */
    private static String encodeBase36(UUID uuid) {
        // Get the 128-bit UUID as two 64-bit longs
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        
        // Combine into a single 128-bit big integer (using string representation)
        // Handle negative values by converting to unsigned representation
        java.math.BigInteger mostSig = java.math.BigInteger.valueOf(mostSigBits);
        if (mostSigBits < 0) {
            mostSig = mostSig.add(java.math.BigInteger.ONE.shiftLeft(64));
        }
        
        java.math.BigInteger leastSig = java.math.BigInteger.valueOf(leastSigBits);
        if (leastSigBits < 0) {
            leastSig = leastSig.add(java.math.BigInteger.ONE.shiftLeft(64));
        }
        
        java.math.BigInteger combined = mostSig.shiftLeft(64).add(leastSig);
        
        // Convert to Base36
        if (combined.equals(java.math.BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        java.math.BigInteger base = java.math.BigInteger.valueOf(36);

        while (combined.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger[] divMod = combined.divideAndRemainder(base);
            result.append(BASE36_ALPHABET.charAt(divMod[1].intValue()));
            combined = divMod[0];
        }
        
        return result.reverse().toString();
    }

    /**
     * Decodes a Base36 string back to a UUID.
     *
     * @param base36 The Base36 string to decode (uppercase)
     * @return The decoded UUID
     */
    private static UUID decodeBase36(String base36) {
        java.math.BigInteger result = java.math.BigInteger.ZERO;
        java.math.BigInteger base = java.math.BigInteger.valueOf(36);

        for (char c : base36.toCharArray()) {
            int digit = BASE36_ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base36 character: " + c);
            }
            result = result.multiply(base).add(java.math.BigInteger.valueOf(digit));
        }
        
        // Extract the two 64-bit longs
        java.math.BigInteger mask64 = java.math.BigInteger.ONE.shiftLeft(64).subtract(java.math.BigInteger.ONE);
        long leastSigBits = result.and(mask64).longValue();
        long mostSigBits = result.shiftRight(64).and(mask64).longValue();
        
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Validates if a string matches Base36 format.
     *
     * @param str The string to validate
     * @return true if valid Base36 format
     */
    private static boolean isValidBase36(String str) {
        if (str == null || str.length() > 27 || str.length() < 20) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (BASE36_ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates if a string matches standard UUID format.
     *
     * @param str The string to validate
     * @return true if valid UUID format
     */
    private static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}