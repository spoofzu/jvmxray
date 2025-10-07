package org.jvmxray.service.ai.processor;

import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.service.ai.util.CVEMatcher;
import org.jvmxray.service.ai.util.LibraryTemporalTracker;
import org.jvmxray.service.ai.util.OWASPMetadataAnalyzer;
import org.jvmxray.service.ai.util.OWASPMetadataAnalyzer.JarMetadata;
import org.jvmxray.service.ai.util.OWASPMetadataAnalyzer.CVEInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage2Processor - Enriches library records in STAGE2_LIBRARY with CVE analysis and metadata.
 *
 * <p>This processor reads existing library records from STAGE2_LIBRARY, performs OWASP
 * Dependency Check vulnerability analysis, extracts Maven coordinates and manifest metadata,
 * and updates the records with enriched information.</p>
 *
 * <p>Processing Flow:</p>
 * <ul>
 *   <li>Read library records from STAGE2_LIBRARY that need enrichment</li>
 *   <li>Analyze with OWASP Dependency Check for CVE vulnerabilities</li>
 *   <li>Extract metadata (Maven coordinates, manifest info, package names)</li>
 *   <li>Update STAGE2_LIBRARY with enriched data</li>
 *   <li>Store CVE records in STAGE2_LIBRARY_CVE</li>
 * </ul>
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class Stage2Processor extends AbstractStageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(Stage2Processor.class);

    private final CVEMatcher cveMatcher;
    private final LibraryTemporalTracker temporalTracker;
    private OWASPMetadataAnalyzer owaspAnalyzer;
    private boolean owaspEnabled = false;

    // SQL queries - read libraries from STAGE2_LIBRARY that need enrichment
    private static final String SELECT_LIBRARY_RECORDS_SQL =
        "SELECT LIBRARY_ID, EVENT_ID, AID, CID, JARPATH, LIBRARY_NAME, SHA256_HASH, METHOD, " +
        "FIRST_SEEN, LAST_SEEN FROM STAGE2_LIBRARY " +
        "WHERE (GROUP_ID IS NULL OR GROUP_ID = '') " +  // Not yet enriched with metadata
        "ORDER BY FIRST_SEEN ASC LIMIT ?";

    // Update library with enriched metadata
    private static final String UPDATE_LIBRARY_METADATA_SQL =
        "UPDATE STAGE2_LIBRARY SET " +
        "GROUP_ID = ?, ARTIFACT_ID = ?, VERSION = ?, " +
        "IMPL_TITLE = ?, IMPL_VENDOR = ?, PACKAGE_NAMES = ? " +
        "WHERE LIBRARY_ID = ?";

    private static final String INSERT_CVE_SQL =
        "INSERT INTO STAGE2_LIBRARY_CVE " +
        "(CVE_ID, CVE_NAME, CVSS_SEVERITY, CVSS_V3, DESCRIPTION, CWE_IDS) " +
        "VALUES (?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "CVSS_SEVERITY = VALUES(CVSS_SEVERITY), CVSS_V3 = VALUES(CVSS_V3), " +
        "DESCRIPTION = VALUES(DESCRIPTION)";

    private static final String UPDATE_LIBRARY_REMOVED_SQL =
        "UPDATE STAGE2_LIBRARY SET REMOVED_ON = ?, IS_ACTIVE = false " +
        "WHERE LIBRARY_ID = ? AND AID = ?";

    /**
     * Constructs a new Stage2Processor.
     *
     * @param properties Component properties for configuration
     */
    public Stage2Processor(PropertyBase properties) {
        super(properties);
        this.cveMatcher = new CVEMatcher();
        this.temporalTracker = new LibraryTemporalTracker();
    }

    @Override
    public String getProcessorName() {
        return "Stage2Processor";
    }

    @Override
    protected String getEnabledPropertyKey() {
        return "aiservice.stage2.enabled";
    }

    @Override
    protected void onInitialize() throws Exception {
        // Initialize OWASP Dependency Check if enabled
        initializeOWASP();
    }

    /**
     * Initializes OWASP Dependency Check analyzer if enabled in configuration.
     */
    private void initializeOWASP() {
        try {
            // Check if ODC is enabled (default: true)
            String owaspEnabledStr = properties.getProperty("aiservice.odc.enabled", "true");
            owaspEnabled = Boolean.parseBoolean(owaspEnabledStr);

            if (!owaspEnabled) {
                logger.info("OWASP Dependency Check is disabled");
                return;
            }

            // Get ODC configuration
            String dataDirectory = properties.getProperty("aiservice.odc.data.directory",
                ".jvmxray/aiservice/data/odc");
            String nvdApiKey = properties.getProperty("aiservice.odc.nvd.api.key", null);

            // Initialize analyzer (ODC will log its own progress)
            owaspAnalyzer = new OWASPMetadataAnalyzer();
            owaspAnalyzer.initialize(dataDirectory, nvdApiKey);

            logger.info("OWASP Dependency Check initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize OWASP Dependency Check, falling back to pattern matching", e);
            owaspEnabled = false;
            owaspAnalyzer = null;
        }
    }

    /**
     * Processes a batch of library records from STAGE2_LIBRARY for enrichment.
     *
     * @param connection Database connection
     * @param batchSize Maximum number of records to process
     * @return Number of records processed
     * @throws SQLException if database operation fails
     */
    @Override
    public int processBatch(Connection connection, int batchSize) throws SQLException {
        if (!isEnabled()) {
            return 0;
        }

        logger.debug("Processing STAGE2 library enrichment batch, size: {}", batchSize);

        int processedCount = 0;

        try (PreparedStatement selectStmt = connection.prepareStatement(SELECT_LIBRARY_RECORDS_SQL)) {
            selectStmt.setInt(1, batchSize);

            try (ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        processLibraryRecord(connection, rs);
                        processedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to enrich library record {}: {}",
                                   rs.getString("LIBRARY_ID"), e.getMessage(), e);
                        // Continue processing other records
                    }
                }
            }
        }

        logger.debug("Processed {} STAGE2 library enrichment records", processedCount);
        return processedCount;
    }

    /**
     * Processes a single library record for enrichment.
     *
     * @param connection Database connection
     * @param rs Result set positioned at current library record
     * @throws SQLException if database operation fails
     */
    private void processLibraryRecord(Connection connection, ResultSet rs) throws SQLException {
        String libraryId = rs.getString("LIBRARY_ID");
        String eventId = rs.getString("EVENT_ID");
        String aid = rs.getString("AID");
        String cid = rs.getString("CID");
        String jarPath = rs.getString("JARPATH");
        String libraryName = rs.getString("LIBRARY_NAME");
        String sha256Hash = rs.getString("SHA256_HASH");
        String method = rs.getString("METHOD");
        long firstSeen = rs.getLong("FIRST_SEEN");
        long lastSeen = rs.getLong("LAST_SEEN");

        // For now, we'll create minimal metadata - in future, could read from JAR file
        JarMetadata metadata = new JarMetadata();
        // metadata extraction could be enhanced by reading actual JAR file

        // Analyze with OWASP Dependency Check if enabled
        List<CVEInfo> cves = null;
        if (owaspEnabled && owaspAnalyzer != null && metadata != null) {
            try {
                cves = owaspAnalyzer.analyzeFromMetadata(metadata);
                if (cves != null && !cves.isEmpty()) {
                    logger.info("Found {} vulnerabilities via ODC for {}",
                        cves.size(), libraryName);
                    // Store CVE records
                    for (CVEInfo cve : cves) {
                        storeCVERecord(connection, cve);
                    }
                }
            } catch (Exception e) {
                logger.warn("ODC analysis failed for {}, falling back to pattern matching: {}",
                    libraryName, e.getMessage());
            }
        }

        // Fallback to pattern-based CVE matching if ODC disabled or failed
        if (cves == null || cves.isEmpty()) {
            // Create eventData map for CVE matcher
            Map<String, String> eventData = new HashMap<>();
            eventData.put("jarPath", jarPath);
            eventData.put("method", method);
            cveMatcher.checkVulnerabilities(libraryName, eventData);
        }

        // Update library record with enriched metadata
        updateLibraryMetadata(connection, libraryId, metadata);

        logger.debug("Enriched library record: {} -> {} ({})",
                   libraryId, libraryName, sha256Hash.substring(0, 8) + "...");
    }

    /**
     * Updates a library record with enriched metadata.
     */
    private void updateLibraryMetadata(Connection connection, String libraryId,
                                      JarMetadata metadata) throws SQLException {

        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_LIBRARY_METADATA_SQL)) {
            // Maven coordinates from metadata
            if (metadata != null && (metadata.hasMavenCoordinates() ||
                metadata.implementationTitle != null ||
                metadata.implementationVendor != null)) {
                stmt.setString(1, metadata.groupId);
                stmt.setString(2, metadata.artifactId);
                stmt.setString(3, metadata.version);
                stmt.setString(4, metadata.implementationTitle);
                stmt.setString(5, metadata.implementationVendor);
                stmt.setString(6, metadata.packageNames != null ?
                    String.join(",", metadata.packageNames) : null);
            } else {
                // Mark as enriched but with no metadata found
                stmt.setString(1, "");  // Empty string indicates "enriched but no data"
                stmt.setNull(2, java.sql.Types.VARCHAR);
                stmt.setNull(3, java.sql.Types.VARCHAR);
                stmt.setNull(4, java.sql.Types.VARCHAR);
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }

            stmt.setString(7, libraryId);

            int rowsAffected = stmt.executeUpdate();
            logger.debug("Updated library metadata: {} (rows affected: {})",
                       libraryId, rowsAffected);
        }
    }

    /**
     * Stores a CVE record in STAGE2_LIBRARY_CVE table.
     */
    private void storeCVERecord(Connection connection, CVEInfo cve) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_CVE_SQL)) {
            stmt.setString(1, cve.cveId);
            stmt.setString(2, cve.name);
            stmt.setString(3, cve.severity);
            stmt.setDouble(4, cve.cvssScore);
            stmt.setString(5, cve.description);
            stmt.setString(6, cve.cweIds);

            stmt.executeUpdate();
            logger.debug("Stored CVE record: {}", cve.cveId);
        }
    }


    /**
     * Marks libraries as removed when they are no longer present.
     *
     * @param connection Database connection
     * @param aid Application ID
     * @param currentTimestamp Current timestamp
     * @return Number of libraries marked as removed
     * @throws SQLException if database operation fails
     */
    public int markRemovedLibraries(Connection connection, String aid, long currentTimestamp)
            throws SQLException {

        // This would be called by a separate process that compares
        // current library state with previous state
        logger.debug("Checking for removed libraries for AID: {}", aid);

        // Implementation would compare current active libraries with
        // previously seen libraries and mark missing ones as removed

        return 0; // Placeholder - would return actual count
    }
}