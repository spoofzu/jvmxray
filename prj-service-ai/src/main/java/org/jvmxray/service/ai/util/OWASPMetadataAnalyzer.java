package org.jvmxray.service.ai.util;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvd.ecosystem.Ecosystem;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side OWASP Dependency Check analyzer that uses agent-provided
 * JAR metadata to identify vulnerabilities without requiring access to
 * the actual JAR files.
 *
 * <p>This analyzer builds ODC Evidence from Maven coordinates, manifest
 * attributes, and package names, then leverages ODC's CPE matching engine
 * to query the NVD database for known vulnerabilities.
 *
 * @author JVMXRay AI Service Team
 * @since 0.0.1
 */
public class OWASPMetadataAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(OWASPMetadataAnalyzer.class);

    private Engine engine;
    private Settings settings;
    private boolean initialized = false;

    /**
     * Container for JAR metadata received from agent.
     */
    public static class JarMetadata {
        public String groupId;
        public String artifactId;
        public String version;
        public String implementationTitle;
        public String implementationVersion;
        public String implementationVendor;
        public String[] packageNames;

        public JarMetadata() {}

        public JarMetadata(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public boolean hasMavenCoordinates() {
            return groupId != null && !groupId.isEmpty()
                && artifactId != null && !artifactId.isEmpty()
                && version != null && !version.isEmpty();
        }
    }

    /**
     * Container for CVE vulnerability information.
     */
    public static class CVEInfo {
        public String cveId;
        public String name;
        public String severity;
        public double cvssScore;
        public String description;
        public String cweIds;
        public String source;

        public CVEInfo(String cveId, String name, String severity, double cvssScore,
                      String description, String cweIds, String source) {
            this.cveId = cveId;
            this.name = name;
            this.severity = severity;
            this.cvssScore = cvssScore;
            this.description = description;
            this.cweIds = cweIds;
            this.source = source;
        }

        @Override
        public String toString() {
            return String.format("CVE{%s, severity=%s, score=%.1f}", cveId, severity, cvssScore);
        }
    }

    /**
     * Initializes the OWASP Dependency Check engine.
     *
     * @param dataDirectory Directory for ODC data storage
     * @param nvdApiKey Optional NVD API key for faster updates (can be null)
     * @throws Exception if initialization fails
     */
    public void initialize(String dataDirectory, String nvdApiKey) throws Exception {
        if (initialized) {
            logger.warn("OWASPMetadataAnalyzer already initialized");
            return;
        }

        logger.info("Initializing OWASP Dependency Check engine...");

        try {
            // Configure data directory path
            String dataDir = dataDirectory != null && !dataDirectory.isEmpty() ?
                dataDirectory : ".jvmxray/aiservice/data/odc";

            // Create data directory if it doesn't exist
            File dataDirFile = new File(dataDir);
            if (!dataDirFile.exists()) {
                dataDirFile.mkdirs();
            }

            // Initialize engine (ODC will log its own progress)
            initializeEngine(dataDir, nvdApiKey);

            initialized = true;
            logger.info("OWASP Dependency Check engine initialized successfully");
            logger.warn("ODC integration is experimental - using default configuration");

        } catch (Exception e) {
            logger.error("Failed to initialize OWASP Dependency Check engine", e);
            throw e;
        }
    }

    /**
     * Core engine initialization logic.
     */
    private void initializeEngine(String dataDir, String nvdApiKey) throws Exception {
        // Create minimal Settings instance
        settings = new Settings();

        // Configure data directory for NVD database storage
        if (dataDir != null && !dataDir.isEmpty()) {
            settings.setString(Settings.KEYS.DATA_DIRECTORY, dataDir);
            logger.info("ODC data directory configured: {}", dataDir);
        }

        // Configure NVD API key if provided
        if (nvdApiKey != null && !nvdApiKey.trim().isEmpty()) {
            settings.setString(Settings.KEYS.NVD_API_KEY, nvdApiKey.trim());
            logger.info("NVD API key configured for faster database updates");
        }

        // Create engine instance with settings
        engine = new Engine(settings);

        // Trigger database update synchronously
        // This ensures NVD database is downloaded/updated during initialization
        // so progress monitoring can track the download
        engine.doUpdates();
    }

    /**
     * Analyzes JAR metadata to identify vulnerabilities.
     *
     * @param metadata JAR metadata from agent
     * @return List of identified vulnerabilities
     * @throws Exception if analysis fails
     */
    public List<CVEInfo> analyzeFromMetadata(JarMetadata metadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("OWASPMetadataAnalyzer not initialized");
        }

        if (metadata == null) {
            return new ArrayList<>();
        }

        logger.debug("Analyzing metadata: groupId={}, artifactId={}, version={}",
            metadata.groupId, metadata.artifactId, metadata.version);

        try {
            // Create virtual dependency from metadata
            Dependency dependency = createDependencyFromMetadata(metadata);

            // Build evidence buckets
            addVendorEvidence(dependency, metadata);
            addProductEvidence(dependency, metadata);
            addVersionEvidence(dependency, metadata);

            // Add dependency to engine for analysis
            engine.addDependency(dependency);

            // Run analysis
            engine.analyzeDependencies();

            // Extract vulnerabilities
            List<CVEInfo> cves = extractVulnerabilities(dependency);

            // Only log at INFO level if vulnerabilities were found (reduces noise)
            if (!cves.isEmpty()) {
                String libraryId = metadata.hasMavenCoordinates()
                    ? metadata.groupId + ":" + metadata.artifactId + ":" + metadata.version
                    : (metadata.implementationTitle != null ? metadata.implementationTitle : "unknown");
                logger.info("Found {} vulnerabilities for {}", cves.size(), libraryId);
            } else {
                String libraryId = metadata.hasMavenCoordinates()
                    ? metadata.groupId + ":" + metadata.artifactId + ":" + metadata.version
                    : (metadata.implementationTitle != null ? metadata.implementationTitle : "unknown");
                logger.debug("No vulnerabilities found for {}", libraryId);
            }

            return cves;

        } catch (Exception e) {
            logger.error("Failed to analyze metadata", e);
            throw e;
        }
    }

    /**
     * Creates a virtual Dependency object from agent-provided metadata.
     */
    private Dependency createDependencyFromMetadata(JarMetadata metadata) {
        // Create a virtual file reference (not actually accessed)
        String virtualPath = metadata.hasMavenCoordinates()
            ? String.format("%s-%s-%s.jar", metadata.groupId, metadata.artifactId, metadata.version)
            : (metadata.implementationTitle != null ? metadata.implementationTitle + ".jar" : "unknown.jar");

        File virtualFile = new File(virtualPath);
        Dependency dependency = new Dependency(virtualFile, false);

        // Set ecosystem to Java
        dependency.setEcosystem(Ecosystem.JAVA);

        // Add Maven coordinates if available
        if (metadata.hasMavenCoordinates()) {
            dependency.addProjectReference(
                String.format("%s:%s:%s", metadata.groupId, metadata.artifactId, metadata.version)
            );
            logger.debug("Added Maven coordinates: {}:{}:{}",
                metadata.groupId, metadata.artifactId, metadata.version);
        }

        return dependency;
    }

    /**
     * Adds vendor evidence from metadata.
     */
    private void addVendorEvidence(Dependency dependency, JarMetadata metadata) {
        if (metadata.groupId != null && !metadata.groupId.isEmpty()) {
            dependency.addEvidence(EvidenceType.VENDOR, "maven", "groupId",
                metadata.groupId, Confidence.HIGHEST);
        }

        if (metadata.implementationVendor != null && !metadata.implementationVendor.isEmpty()) {
            dependency.addEvidence(EvidenceType.VENDOR, "manifest", "Implementation-Vendor",
                metadata.implementationVendor, Confidence.HIGH);
        }

        // Extract vendor from package names
        if (metadata.packageNames != null && metadata.packageNames.length > 0) {
            for (String packageName : metadata.packageNames) {
                String vendor = extractVendorFromPackage(packageName);
                if (vendor != null) {
                    dependency.addEvidence(EvidenceType.VENDOR, "package", "namespace",
                        vendor, Confidence.MEDIUM);
                }
            }
        }
    }

    /**
     * Adds product evidence from metadata.
     */
    private void addProductEvidence(Dependency dependency, JarMetadata metadata) {
        if (metadata.artifactId != null && !metadata.artifactId.isEmpty()) {
            dependency.addEvidence(EvidenceType.PRODUCT, "maven", "artifactId",
                metadata.artifactId, Confidence.HIGHEST);
        }

        if (metadata.implementationTitle != null && !metadata.implementationTitle.isEmpty()) {
            dependency.addEvidence(EvidenceType.PRODUCT, "manifest", "Implementation-Title",
                metadata.implementationTitle, Confidence.HIGH);
        }
    }

    /**
     * Adds version evidence from metadata.
     */
    private void addVersionEvidence(Dependency dependency, JarMetadata metadata) {
        if (metadata.version != null && !metadata.version.isEmpty()) {
            dependency.addEvidence(EvidenceType.VERSION, "maven", "version",
                metadata.version, Confidence.HIGHEST);
        }

        if (metadata.implementationVersion != null && !metadata.implementationVersion.isEmpty()) {
            dependency.addEvidence(EvidenceType.VERSION, "manifest", "Implementation-Version",
                metadata.implementationVersion, Confidence.HIGH);
        }
    }

    /**
     * Extracts vendor information from package name.
     * Example: "org.apache" from "org.apache.commons"
     */
    private String extractVendorFromPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }

        String[] parts = packageName.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return null;
    }

    /**
     * Extracts CVE information from analyzed dependency.
     */
    private List<CVEInfo> extractVulnerabilities(Dependency dependency) {
        List<CVEInfo> cves = new ArrayList<>();

        for (Vulnerability vuln : dependency.getVulnerabilities()) {
            String cveId = vuln.getName();
            String description = vuln.getDescription();

            // Extract CWE IDs from vulnerability
            StringBuilder cweBuilder = new StringBuilder();
            if (vuln.getCwes() != null) {
                String cweStr = vuln.getCwes().toString();
                if (cweStr != null && !cweStr.isEmpty()) {
                    cweBuilder.append(cweStr);
                }
            }
            String cweIds = cweBuilder.toString();

            String source = vuln.getSource() != null ? vuln.getSource().name() : "NVD";

            // Get CVSS score and severity
            double cvssScore = 0.0;
            String severity = "UNKNOWN";

            // Try CVSS v3 first - use the score directly from vuln object
            if (vuln.getCvssV3() != null) {
                try {
                    // Access score via toString() and parse or use unscoredSeverity
                    String scoreStr = vuln.getCvssV3().toString();
                    if (scoreStr != null && scoreStr.contains("baseScore=")) {
                        int start = scoreStr.indexOf("baseScore=") + 10;
                        int end = scoreStr.indexOf(",", start);
                        if (end > start) {
                            cvssScore = Double.parseDouble(scoreStr.substring(start, end).trim());
                        }
                    }
                    severity = vuln.getUnscoredSeverity() != null ? vuln.getUnscoredSeverity() : calculateSeverityFromScore(cvssScore);
                } catch (Exception e) {
                    logger.warn("Failed to parse CVSS v3 score: {}", e.getMessage());
                }
            }
            // Fallback to CVSS v2
            else if (vuln.getCvssV2() != null) {
                try {
                    String scoreStr = vuln.getCvssV2().toString();
                    if (scoreStr != null && scoreStr.contains("score=")) {
                        int start = scoreStr.indexOf("score=") + 6;
                        int end = scoreStr.indexOf(",", start);
                        if (end > start) {
                            cvssScore = Double.parseDouble(scoreStr.substring(start, end).trim());
                        }
                    }
                    severity = calculateSeverityFromScore(cvssScore);
                } catch (Exception e) {
                    logger.warn("Failed to parse CVSS v2 score: {}", e.getMessage());
                }
            }

            CVEInfo cveInfo = new CVEInfo(
                cveId,
                vuln.getName(),
                severity,
                cvssScore,
                description,
                cweIds,
                source
            );

            cves.add(cveInfo);
            logger.debug("Extracted CVE: {}", cveInfo);
        }

        return cves;
    }

    /**
     * Calculates severity level from CVSS score.
     */
    private String calculateSeverityFromScore(double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        if (score > 0.0) return "LOW";
        return "UNKNOWN";
    }

    /**
     * Shuts down the ODC engine and releases resources.
     */
    public void shutdown() {
        if (engine != null) {
            logger.info("Shutting down OWASP Dependency Check engine");
            engine.close();
            initialized = false;
        }
    }

    /**
     * Checks if the analyzer is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}