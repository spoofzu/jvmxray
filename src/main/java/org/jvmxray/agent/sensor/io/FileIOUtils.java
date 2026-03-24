package org.jvmxray.agent.sensor.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for extracting file metadata for security monitoring.
 * Provides path resolution, file attributes, and POSIX permissions.
 *
 * @author Milton Smith
 */
public class FileIOUtils {

    // Configuration flags (can be set via properties)
    private static volatile boolean collectPathResolution = true;
    private static volatile boolean collectFileMetadata = true;
    private static volatile boolean collectPosixAttributes = true;

    /**
     * Sets the configuration for metadata collection.
     */
    public static void configure(boolean pathResolution, boolean fileMetadata, boolean posixAttrs) {
        collectPathResolution = pathResolution;
        collectFileMetadata = fileMetadata;
        collectPosixAttributes = posixAttrs;
    }

    /**
     * Collects path resolution metadata for a file path.
     *
     * @param originalPath The path as provided by the application
     * @return Map of path resolution fields
     */
    public static Map<String, String> getPathResolutionMetadata(String originalPath) {
        Map<String, String> metadata = new HashMap<>();

        if (!collectPathResolution || originalPath == null) {
            return metadata;
        }

        try {
            metadata.put("original_path", originalPath);

            File file = new File(originalPath);
            String absolutePath = file.getAbsolutePath();
            metadata.put("absolute_path", absolutePath);

            // Get canonical path (resolves symlinks and ../ sequences)
            try {
                String canonicalPath = file.getCanonicalPath();
                metadata.put("canonical_path", canonicalPath);

                // If paths differ significantly, it might indicate path traversal
                if (!absolutePath.equals(canonicalPath)) {
                    metadata.put("path_normalized", "true");
                }
            } catch (IOException e) {
                metadata.put("canonical_path_error", e.getClass().getSimpleName());
            }

            // Check if it's a symbolic link
            Path path = file.toPath();
            boolean isSymlink = Files.isSymbolicLink(path);
            metadata.put("is_symlink", String.valueOf(isSymlink));

            // Get symlink target if it is a symlink
            if (isSymlink) {
                try {
                    Path target = Files.readSymbolicLink(path);
                    metadata.put("symlink_target", target.toString());
                } catch (IOException e) {
                    metadata.put("symlink_target_error", e.getClass().getSimpleName());
                }
            }

            // Extract file name and extension
            String fileName = file.getName();
            metadata.put("file_name", fileName);
            metadata.put("parent_directory", file.getParent() != null ? file.getParent() : "");

            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) {
                metadata.put("file_extension", fileName.substring(dotIdx + 1).toLowerCase());
            } else {
                metadata.put("file_extension", "");
            }

        } catch (Exception e) {
            metadata.put("path_resolution_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Collects file metadata (existence, size, permissions, etc.).
     *
     * @param filePath The file path to inspect
     * @return Map of file metadata fields
     */
    public static Map<String, String> getFileMetadata(String filePath) {
        Map<String, String> metadata = new HashMap<>();

        if (!collectFileMetadata || filePath == null) {
            return metadata;
        }

        try {
            Path path = new File(filePath).toPath();

            boolean exists = Files.exists(path);
            metadata.put("file_exists", String.valueOf(exists));

            if (exists) {
                // Basic attributes
                metadata.put("is_directory", String.valueOf(Files.isDirectory(path)));
                metadata.put("is_regular_file", String.valueOf(Files.isRegularFile(path)));
                metadata.put("is_readable", String.valueOf(Files.isReadable(path)));
                metadata.put("is_writable", String.valueOf(Files.isWritable(path)));
                metadata.put("is_executable", String.valueOf(Files.isExecutable(path)));
                metadata.put("is_hidden", String.valueOf(Files.isHidden(path)));

                // File size
                try {
                    if (Files.isRegularFile(path)) {
                        long size = Files.size(path);
                        metadata.put("file_size_bytes", String.valueOf(size));
                    }
                } catch (IOException e) {
                    // Size not available
                }

                // Timestamps
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    metadata.put("last_modified_time", attrs.lastModifiedTime().toString());
                    metadata.put("creation_time", attrs.creationTime().toString());
                    metadata.put("last_access_time", attrs.lastAccessTime().toString());
                } catch (IOException e) {
                    // Attributes not available
                }

                // POSIX-specific attributes (Linux/Mac)
                if (collectPosixAttributes) {
                    try {
                        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                        metadata.put("posix_permissions", PosixFilePermissions.toString(perms));

                        // Check for world-writable (security concern)
                        if (perms.contains(PosixFilePermission.OTHERS_WRITE)) {
                            metadata.put("world_writable", "true");
                        }
                    } catch (UnsupportedOperationException e) {
                        // Windows - POSIX not supported
                    } catch (IOException e) {
                        // Permissions not accessible
                    }

                    try {
                        metadata.put("file_owner", Files.getOwner(path).getName());
                    } catch (UnsupportedOperationException e) {
                        // Owner lookup not supported
                    } catch (IOException e) {
                        // Owner not accessible
                    }
                }
            }
        } catch (Exception e) {
            metadata.put("file_metadata_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Collects all available metadata for a file operation.
     *
     * @param filePath The file path
     * @return Combined map of all metadata
     */
    public static Map<String, String> getAllMetadata(String filePath) {
        Map<String, String> metadata = new HashMap<>();
        metadata.putAll(getPathResolutionMetadata(filePath));
        metadata.putAll(getFileMetadata(filePath));
        return metadata;
    }

    /**
     * Collects metadata for a rename/move operation.
     *
     * @param sourcePath The original file path
     * @param targetPath The destination file path
     * @return Map of rename operation metadata
     */
    public static Map<String, String> getRenameMetadata(String sourcePath, String targetPath) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("source_path", sourcePath);
        metadata.put("target_path", targetPath);

        // Add resolved paths
        try {
            File sourceFile = new File(sourcePath);
            File targetFile = new File(targetPath);

            metadata.put("source_canonical_path", sourceFile.getCanonicalPath());
            metadata.put("target_canonical_path", targetFile.getCanonicalPath());

            // Extract file names
            metadata.put("source_file_name", sourceFile.getName());
            metadata.put("target_file_name", targetFile.getName());

            // Check if extension changed
            String sourceExt = getExtension(sourceFile.getName());
            String targetExt = getExtension(targetFile.getName());
            if (!sourceExt.equals(targetExt)) {
                metadata.put("extension_changed", "true");
                metadata.put("source_extension", sourceExt);
                metadata.put("target_extension", targetExt);
            }

            // Check if moved to different directory
            String sourceParent = sourceFile.getParent();
            String targetParent = targetFile.getParent();
            if (sourceParent != null && targetParent != null && !sourceParent.equals(targetParent)) {
                metadata.put("directory_changed", "true");
                metadata.put("source_directory", sourceParent);
                metadata.put("target_directory", targetParent);
            }

        } catch (IOException e) {
            metadata.put("rename_metadata_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Extracts the file extension from a filename.
     */
    private static String getExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(dotIdx + 1).toLowerCase() : "";
    }
}
