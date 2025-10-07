package org.jvmxray.agent.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility class for extracting metadata from JAR files without requiring
 * full OWASP Dependency Check scanning. Extracts Maven coordinates, manifest
 * attributes, and package names needed for server-side vulnerability analysis.
 *
 * <p>This lightweight extractor reads only metadata entries from JAR files,
 * avoiding the overhead of full ODC analysis on agent-side deployments.
 *
 * @author JVMXRay Agent Team
 * @since 0.0.1
 */
public class JarMetadataExtractor {

    private static final int MAX_PACKAGES = 5;
    private static final String POM_PROPERTIES_PATH = "META-INF/maven/";

    /**
     * Container for JAR metadata extracted for OWASP Dependency Check analysis.
     */
    public static class JarMetadata {
        // Maven coordinates from META-INF/maven/*/pom.properties
        public String groupId;
        public String artifactId;
        public String version;

        // Manifest evidence fields
        public String implementationTitle;
        public String implementationVersion;
        public String implementationVendor;
        public String bundleName;
        public String bundleVersion;
        public String bundleDescription;

        // Package evidence (top packages for CPE matching)
        public List<String> packageNames = new ArrayList<>();

        /**
         * Checks if metadata contains valid Maven coordinates.
         *
         * @return true if groupId, artifactId, and version are present
         */
        public boolean hasMavenCoordinates() {
            return groupId != null && !groupId.isEmpty()
                && artifactId != null && !artifactId.isEmpty()
                && version != null && !version.isEmpty();
        }

        /**
         * Checks if any manifest evidence is present.
         *
         * @return true if at least one manifest field is populated
         */
        public boolean hasManifestEvidence() {
            return (implementationTitle != null && !implementationTitle.isEmpty())
                || (implementationVersion != null && !implementationVersion.isEmpty())
                || (implementationVendor != null && !implementationVendor.isEmpty())
                || (bundleName != null && !bundleName.isEmpty())
                || (bundleVersion != null && !bundleVersion.isEmpty());
        }

        @Override
        public String toString() {
            return String.format("JarMetadata{groupId=%s, artifactId=%s, version=%s, packages=%d}",
                groupId, artifactId, version, packageNames.size());
        }
    }

    /**
     * Extracts metadata from a JAR file for OWASP Dependency Check analysis.
     * This method reads Maven coordinates, manifest attributes, and package names
     * without requiring full JAR scanning.
     *
     * @param jarPath Path to the JAR file
     * @return Extracted metadata, or null if extraction fails
     */
    public static JarMetadata extractMetadata(String jarPath) {
        if (jarPath == null || jarPath.isEmpty()) {
            return null;
        }

        try (JarFile jarFile = new JarFile(jarPath)) {
            JarMetadata metadata = new JarMetadata();

            // Extract Maven coordinates from pom.properties
            extractPomProperties(jarFile, metadata);

            // Extract manifest attributes
            extractManifestData(jarFile.getManifest(), metadata);

            // Extract top-level package names
            extractPackageNames(jarFile, metadata);

            return metadata;

        } catch (IOException e) {
            // Return null on failure - caller handles gracefully
            return null;
        }
    }

    /**
     * Extracts Maven coordinates from META-INF/maven/&#42;&#47;&#42;/pom.properties.
     */
    private static void extractPomProperties(JarFile jarFile, JarMetadata metadata) {
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Look for pom.properties in META-INF/maven/{groupId}/{artifactId}/
                if (name.startsWith(POM_PROPERTIES_PATH) && name.endsWith("/pom.properties")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        Properties props = new Properties();
                        props.load(is);

                        metadata.groupId = props.getProperty("groupId");
                        metadata.artifactId = props.getProperty("artifactId");
                        metadata.version = props.getProperty("version");

                        // Found pom.properties, no need to continue searching
                        return;
                    }
                }
            }
        } catch (IOException e) {
            // Silently fail - not all JARs have pom.properties
        }
    }

    /**
     * Extracts evidence from JAR manifest attributes.
     */
    private static void extractManifestData(Manifest manifest, JarMetadata metadata) {
        if (manifest == null || manifest.getMainAttributes() == null) {
            return;
        }

        var attributes = manifest.getMainAttributes();

        // Implementation attributes (common in Maven-built JARs)
        metadata.implementationTitle = attributes.getValue("Implementation-Title");
        metadata.implementationVersion = attributes.getValue("Implementation-Version");
        metadata.implementationVendor = attributes.getValue("Implementation-Vendor");

        // OSGi Bundle attributes (common in OSGi bundles)
        metadata.bundleName = attributes.getValue("Bundle-Name");
        metadata.bundleVersion = attributes.getValue("Bundle-Version");
        metadata.bundleDescription = attributes.getValue("Bundle-Description");

        // Fallback: If no Implementation-Version, try Specification-Version
        if (metadata.implementationVersion == null || metadata.implementationVersion.isEmpty()) {
            metadata.implementationVersion = attributes.getValue("Specification-Version");
        }

        // Fallback: Use Bundle-Version if Implementation-Version not found
        if (metadata.implementationVersion == null || metadata.implementationVersion.isEmpty()) {
            metadata.implementationVersion = metadata.bundleVersion;
        }
    }

    /**
     * Extracts top-level package names from JAR entries.
     * Package names are used as evidence for CPE matching.
     */
    private static void extractPackageNames(JarFile jarFile, JarMetadata metadata) {
        Set<String> packages = new HashSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            // Look for .class files (package structure)
            if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                // Extract package name from class path
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash > 0) {
                    String packagePath = name.substring(0, lastSlash);
                    // Convert path to package name (replace / with .)
                    String packageName = packagePath.replace('/', '.');

                    // Add top-level package (e.g., "org.apache" from "org.apache.commons.lang")
                    String topPackage = extractTopLevelPackage(packageName);
                    if (topPackage != null) {
                        packages.add(topPackage);
                    }
                }
            }

            // Limit to top MAX_PACKAGES to avoid excessive data
            if (packages.size() >= MAX_PACKAGES) {
                break;
            }
        }

        metadata.packageNames.addAll(packages);
    }

    /**
     * Extracts the top-level package name from a fully-qualified package.
     * Examples:
     * - "org.apache.commons.lang" -> "org.apache"
     * - "com.fasterxml.jackson.databind" -> "com.fasterxml"
     * - "java.util" -> null (filter standard library packages)
     */
    private static String extractTopLevelPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }

        // Filter out standard Java library packages
        if (packageName.startsWith("java.") || packageName.startsWith("javax.")
            || packageName.startsWith("sun.") || packageName.startsWith("com.sun.")) {
            return null;
        }

        // Extract first two segments for meaningful package identification
        String[] parts = packageName.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        } else if (parts.length == 1) {
            return parts[0];
        }

        return null;
    }

    /**
     * Creates a compact string representation of metadata for event transmission.
     *
     * @param metadata The metadata to serialize
     * @return Compact string suitable for logback keypairs
     */
    public static String serializeForTransmission(JarMetadata metadata) {
        if (metadata == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (metadata.groupId != null) {
            sb.append("groupId=").append(metadata.groupId).append(";");
        }
        if (metadata.artifactId != null) {
            sb.append("artifactId=").append(metadata.artifactId).append(";");
        }
        if (metadata.version != null) {
            sb.append("version=").append(metadata.version).append(";");
        }
        if (metadata.implementationTitle != null) {
            sb.append("implTitle=").append(metadata.implementationTitle).append(";");
        }
        if (metadata.implementationVendor != null) {
            sb.append("implVendor=").append(metadata.implementationVendor).append(";");
        }
        if (!metadata.packageNames.isEmpty()) {
            sb.append("packages=").append(String.join(",", metadata.packageNames));
        }

        return sb.toString();
    }
}