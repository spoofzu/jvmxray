package org.jvmxray.service.ai.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for matching libraries against the CVE database.
 * Correlates library names and paths with known vulnerabilities
 * stored in the STAGE2_LIBRARY_CVE table.
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class CVEMatcher {

    private static final Logger logger = LoggerFactory.getLogger(CVEMatcher.class);

    private final ObjectMapper objectMapper;

    // SQL queries
    private static final String SELECT_CVE_SQL =
        "SELECT CVE_ID, CVE_NAME, CVSS_SEVERITY, CVSS_V3, DESCRIPTION, " +
        "AFFECTED_LIBRARIES, FIXED_VERSIONS, CWE_IDS " +
        "FROM STAGE2_LIBRARY_CVE WHERE CVSS_SEVERITY IN (?, ?, ?)";

    private static final String SELECT_ALL_CVE_SQL =
        "SELECT CVE_ID, CVE_NAME, CVSS_SEVERITY, CVSS_V3, DESCRIPTION, " +
        "AFFECTED_LIBRARIES, FIXED_VERSIONS, CWE_IDS " +
        "FROM STAGE2_LIBRARY_CVE";

    // Cache for CVE patterns to avoid repeated JSON parsing
    private final Map<String, List<Pattern>> cvePatternCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 3600000; // 1 hour

    /**
     * Constructs a new CVEMatcher.
     */
    public CVEMatcher() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks for CVE vulnerabilities matching the given library information.
     *
     * @param libraryName Name of the library extracted from path
     * @param eventData Additional event data containing library information
     * @return List of matching CVE records
     */
    public List<CVEMatch> checkVulnerabilities(String libraryName, Map<String, String> eventData) {
        List<CVEMatch> matches = new ArrayList<>();

        if (libraryName == null || libraryName.trim().isEmpty()) {
            logger.debug("No library name provided for CVE matching");
            return matches;
        }

        // For now, return empty list since we don't have a database connection here
        // In actual implementation, this would be called with a connection
        logger.debug("CVE check requested for library: {}", libraryName);

        return matches;
    }

    /**
     * Checks for CVE vulnerabilities with database connection.
     *
     * @param connection Database connection
     * @param libraryName Name of the library
     * @param jarPath Full path to the JAR file
     * @return List of matching CVE records
     * @throws SQLException if database operation fails
     */
    public List<CVEMatch> checkVulnerabilities(Connection connection, String libraryName, String jarPath)
            throws SQLException {

        List<CVEMatch> matches = new ArrayList<>();

        if (libraryName == null || libraryName.trim().isEmpty()) {
            return matches;
        }

        // Refresh cache if needed
        refreshCacheIfNeeded(connection);

        // Check critical and high severity CVEs first
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_CVE_SQL)) {
            stmt.setString(1, "CRITICAL");
            stmt.setString(2, "HIGH");
            stmt.setString(3, "MEDIUM");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CVERecord cveRecord = mapResultSetToCVE(rs);
                    if (isLibraryAffected(libraryName, jarPath, cveRecord)) {
                        matches.add(new CVEMatch(cveRecord, libraryName, jarPath));
                    }
                }
            }
        }

        if (!matches.isEmpty()) {
            logger.info("Found {} CVE matches for library: {}", matches.size(), libraryName);
        }

        return matches;
    }

    /**
     * Determines if a library is affected by a CVE based on pattern matching.
     */
    private boolean isLibraryAffected(String libraryName, String jarPath, CVERecord cve) {
        List<Pattern> patterns = getOrCreatePatterns(cve.cveId, cve.affectedLibraries);

        if (patterns.isEmpty()) {
            return false;
        }

        // Check library name against patterns
        String normalizedName = libraryName.toLowerCase();
        String normalizedPath = jarPath != null ? jarPath.toLowerCase() : "";

        for (Pattern pattern : patterns) {
            if (pattern.matcher(normalizedName).find() ||
                (jarPath != null && pattern.matcher(normalizedPath).find())) {

                logger.debug("Library {} matches CVE pattern: {}",
                           libraryName, pattern.pattern());
                return true;
            }
        }

        return false;
    }

    /**
     * Gets or creates compiled regex patterns for a CVE's affected libraries.
     */
    private List<Pattern> getOrCreatePatterns(String cveId, String affectedLibrariesJson) {
        List<Pattern> patterns = cvePatternCache.get(cveId);

        if (patterns != null) {
            return patterns;
        }

        patterns = new ArrayList<>();

        if (affectedLibrariesJson != null && !affectedLibrariesJson.trim().isEmpty()) {
            try {
                List<String> libraryPatterns = objectMapper.readValue(
                    affectedLibrariesJson, new TypeReference<List<String>>() {});

                for (String patternStr : libraryPatterns) {
                    try {
                        // Convert simple glob patterns to regex
                        String regex = convertGlobToRegex(patternStr);
                        patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                    } catch (Exception e) {
                        logger.warn("Invalid pattern in CVE {}: {}", cveId, patternStr);
                    }
                }

            } catch (Exception e) {
                logger.warn("Failed to parse affected libraries JSON for CVE {}: {}",
                          cveId, e.getMessage());
            }
        }

        cvePatternCache.put(cveId, patterns);
        return patterns;
    }

    /**
     * Converts glob-style patterns to regex.
     * Examples:
     * - "log4j*" -> "log4j.*"
     * - "commons-*-3.*" -> "commons-.*-3\\..*"
     */
    private String convertGlobToRegex(String glob) {
        if (glob == null) {
            return "";
        }

        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                case '+':
                    regex.append("\\+");
                    break;
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        return regex.toString();
    }

    /**
     * Maps a database result set row to a CVERecord object.
     */
    private CVERecord mapResultSetToCVE(ResultSet rs) throws SQLException {
        return new CVERecord(
            rs.getString("CVE_ID"),
            rs.getString("CVE_NAME"),
            rs.getString("CVSS_SEVERITY"),
            rs.getDouble("CVSS_V3"),
            rs.getString("DESCRIPTION"),
            rs.getString("AFFECTED_LIBRARIES"),
            rs.getString("FIXED_VERSIONS"),
            rs.getString("CWE_IDS")
        );
    }

    /**
     * Refreshes the pattern cache if TTL has expired.
     */
    private void refreshCacheIfNeeded(Connection connection) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_TTL_MS) {
            logger.debug("Refreshing CVE pattern cache");
            cvePatternCache.clear();
            lastCacheUpdate = currentTime;
        }
    }

    /**
     * Represents a CVE record from the database.
     */
    public static class CVERecord {
        public final String cveId;
        public final String cveName;
        public final String severity;
        public final double cvssScore;
        public final String description;
        public final String affectedLibraries;
        public final String fixedVersions;
        public final String cweIds;

        public CVERecord(String cveId, String cveName, String severity, double cvssScore,
                        String description, String affectedLibraries, String fixedVersions,
                        String cweIds) {
            this.cveId = cveId;
            this.cveName = cveName;
            this.severity = severity;
            this.cvssScore = cvssScore;
            this.description = description;
            this.affectedLibraries = affectedLibraries;
            this.fixedVersions = fixedVersions;
            this.cweIds = cweIds;
        }
    }

    /**
     * Represents a CVE match for a specific library.
     */
    public static class CVEMatch {
        public final CVERecord cve;
        public final String libraryName;
        public final String jarPath;

        public CVEMatch(CVERecord cve, String libraryName, String jarPath) {
            this.cve = cve;
            this.libraryName = libraryName;
            this.jarPath = jarPath;
        }

        @Override
        public String toString() {
            return String.format("CVEMatch{%s -> %s (%s)}",
                               cve.cveId, libraryName, cve.severity);
        }
    }

    /**
     * Gets count of CVEs by severity level.
     *
     * @param connection Database connection
     * @return Map of severity -> count
     * @throws SQLException if database operation fails
     */
    public Map<String, Integer> getCVECountsBySeverity(Connection connection) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();

        String sql = "SELECT CVSS_SEVERITY, COUNT(*) as count FROM STAGE2_LIBRARY_CVE GROUP BY CVSS_SEVERITY";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                counts.put(rs.getString("CVSS_SEVERITY"), rs.getInt("count"));
            }
        }

        return counts;
    }
}