package org.jvmxray.service.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating SHA-256 hashes of library files.
 * Provides methods for both file content hashing and path-based hashing
 * for integrity verification and library identification.
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class LibraryHashCalculator {

    private static final Logger logger = LoggerFactory.getLogger(LibraryHashCalculator.class);

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Calculates SHA-256 hash of the file content at the given path.
     *
     * @param filePath Path to the file to hash
     * @return SHA-256 hash as hexadecimal string
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if file path is invalid
     */
    public String calculateHash(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File does not exist: " + filePath);
        }

        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + filePath);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return calculateFileHash(file, digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculates SHA-256 hash of the file path string.
     * This is used as a fallback when the actual file cannot be accessed
     * but we still need a consistent identifier for the library.
     *
     * @param filePath Path to hash
     * @return SHA-256 hash of the path string as hexadecimal
     */
    public String calculatePathHash(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(filePath.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculates hash of file content using streaming to handle large files.
     */
    private String calculateFileHash(File file, MessageDigest digest) throws IOException {
        logger.debug("Calculating SHA-256 hash for file: {}", file.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            String hash = bytesToHex(hashBytes);

            logger.debug("Calculated hash for {}: {}", file.getName(),
                       hash.substring(0, 8) + "...");

            return hash;
        }
    }

    /**
     * Converts byte array to hexadecimal string representation.
     *
     * @param bytes Byte array to convert
     * @return Hexadecimal string (lowercase)
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Validates if a given string is a valid SHA-256 hash.
     *
     * @param hash String to validate
     * @return true if valid SHA-256 hash format, false otherwise
     */
    public boolean isValidHash(String hash) {
        if (hash == null) {
            return false;
        }

        // SHA-256 produces 64 character hexadecimal string
        if (hash.length() != 64) {
            return false;
        }

        // Check if all characters are valid hexadecimal
        return hash.matches("^[a-fA-F0-9]+$");
    }

    /**
     * Compares two file hashes for equality.
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return true if hashes are equal (case-insensitive), false otherwise
     */
    public boolean hashesEqual(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return false;
        }

        return hash1.equalsIgnoreCase(hash2);
    }

    /**
     * Calculates hash for multiple files and returns results.
     * Useful for batch processing of library files.
     *
     * @param filePaths Array of file paths to process
     * @return Array of corresponding hashes (null entries for failed calculations)
     */
    public String[] calculateBatchHashes(String[] filePaths) {
        if (filePaths == null) {
            return new String[0];
        }

        String[] hashes = new String[filePaths.length];

        for (int i = 0; i < filePaths.length; i++) {
            try {
                hashes[i] = calculateHash(filePaths[i]);
            } catch (Exception e) {
                logger.warn("Failed to calculate hash for {}: {}",
                          filePaths[i], e.getMessage());
                // Try path-based hash as fallback
                try {
                    hashes[i] = calculatePathHash(filePaths[i]);
                } catch (Exception fallbackError) {
                    logger.error("Failed to calculate path hash for {}: {}",
                               filePaths[i], fallbackError.getMessage());
                    hashes[i] = null;
                }
            }
        }

        return hashes;
    }
}