package org.jvmxray.agent.sensor.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for HTTP security analysis.
 * Provides detection of missing security headers, suspicious patterns,
 * and potential attack signatures in HTTP requests.
 *
 * @author Milton Smith
 */
public class HttpSecurityUtils {

    // Security headers that should be present in responses
    private static final String[] SECURITY_HEADERS = {
            "Content-Security-Policy",
            "Strict-Transport-Security",
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection",
            "Referrer-Policy",
            "Permissions-Policy"
    };

    // Known attack tool user agents
    private static final Pattern SUSPICIOUS_USER_AGENT_PATTERN = Pattern.compile(
            "(?i)(sqlmap|nikto|nessus|nmap|burp|dirbuster|gobuster|wfuzz|" +
            "havij|acunetix|appscan|webscarab|paros|w3af|arachni|skipfish|" +
            "zaproxy|zap|metasploit|hydra|medusa|openvas)",
            Pattern.CASE_INSENSITIVE
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|%2e%2e%5c|%252e%252e%252f|" +
            "\\.\\.%2f|%2e\\.\\.%2f|\\.%2e/|%2e\\./)",
            Pattern.CASE_INSENSITIVE
    );

    // SQL injection patterns (common keywords and syntax)
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)('|\"|--)|(\\b(union|select|insert|update|delete|drop|alter|create|" +
            "truncate|exec|execute|xp_|sp_|having|group by|order by|or 1=1|or '1'='1'|" +
            "waitfor|benchmark|sleep|pg_sleep|load_file|into outfile|into dumpfile)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script|javascript:|onerror=|onload=|onclick=|onmouseover=|" +
            "onfocus=|onblur=|<img[^>]+onerror|<svg[^>]+onload|<iframe|" +
            "expression\\(|eval\\(|alert\\(|document\\.cookie|" +
            "document\\.domain|document\\.write)",
            Pattern.CASE_INSENSITIVE
    );

    // Command injection patterns
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
            "(;|\\||`|\\$\\(|\\$\\{|&&|\\|\\||>[>&]|<[<&])",
            Pattern.CASE_INSENSITIVE
    );

    // LDAP injection patterns
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
            "(\\(|\\)|\\*|\\\\[0-9a-fA-F]{2}|\\x00|\\x0a|\\x0d)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Analyzes response headers for missing security headers.
     *
     * @param headers Map of response headers
     * @return Map containing security header analysis
     */
    public static Map<String, String> analyzeSecurityHeaders(Map<String, String> headers) {
        Map<String, String> analysis = new HashMap<>();
        List<String> missingHeaders = new ArrayList<>();

        // Check each security header
        for (String header : SECURITY_HEADERS) {
            boolean present = headers.containsKey(header) ||
                    headers.containsKey(header.toLowerCase());

            switch (header) {
                case "Content-Security-Policy":
                    analysis.put("csp_present", String.valueOf(present));
                    break;
                case "Strict-Transport-Security":
                    analysis.put("hsts_present", String.valueOf(present));
                    break;
                case "X-Content-Type-Options":
                    String ctOptions = getHeaderValue(headers, header);
                    analysis.put("content_type_options", ctOptions != null ? ctOptions : "missing");
                    break;
                case "X-Frame-Options":
                    String frameOptions = getHeaderValue(headers, header);
                    analysis.put("frame_options", frameOptions != null ? frameOptions : "missing");
                    break;
                case "X-XSS-Protection":
                    analysis.put("xss_protection_present", String.valueOf(present));
                    break;
            }

            if (!present) {
                missingHeaders.add(header);
            }
        }

        if (!missingHeaders.isEmpty()) {
            analysis.put("security_headers_missing", String.join(",", missingHeaders));
            analysis.put("security_headers_missing_count", String.valueOf(missingHeaders.size()));
        }

        return analysis;
    }

    /**
     * Analyzes request for suspicious patterns.
     *
     * @param uri The request URI
     * @param queryString The query string (if any)
     * @param userAgent The User-Agent header
     * @return Map containing security pattern analysis
     */
    public static Map<String, String> analyzeRequestPatterns(String uri, String queryString, String userAgent) {
        Map<String, String> analysis = new HashMap<>();
        List<String> detectedPatterns = new ArrayList<>();

        // Combine URI and query string for analysis
        String fullPath = uri != null ? uri : "";
        if (queryString != null && !queryString.isEmpty()) {
            fullPath += "?" + queryString;
        }

        // Check for suspicious user agent
        if (userAgent != null && SUSPICIOUS_USER_AGENT_PATTERN.matcher(userAgent).find()) {
            analysis.put("suspicious_user_agent", "true");
            detectedPatterns.add("suspicious_user_agent");
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(fullPath).find()) {
            analysis.put("path_traversal_attempt", "true");
            detectedPatterns.add("path_traversal");
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(fullPath).find()) {
            analysis.put("sql_injection_pattern", "true");
            detectedPatterns.add("sql_injection");
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(fullPath).find()) {
            analysis.put("xss_pattern", "true");
            detectedPatterns.add("xss");
        }

        // Check for command injection patterns
        if (COMMAND_INJECTION_PATTERN.matcher(fullPath).find()) {
            analysis.put("command_injection_pattern", "true");
            detectedPatterns.add("command_injection");
        }

        // Add summary
        if (!detectedPatterns.isEmpty()) {
            analysis.put("attack_patterns_detected", String.join(",", detectedPatterns));
            analysis.put("risk_indicators_count", String.valueOf(detectedPatterns.size()));
        }

        return analysis;
    }

    /**
     * Computes a hash of content for correlation purposes.
     *
     * @param content The content to hash
     * @return SHA-256 hash prefix (first 16 chars)
     */
    public static String computeContentHash(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hexString.append(String.format("%02x", hash[i]));
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a header value case-insensitively.
     */
    private static String getHeaderValue(Map<String, String> headers, String headerName) {
        String value = headers.get(headerName);
        if (value == null) {
            value = headers.get(headerName.toLowerCase());
        }
        return value;
    }

    /**
     * Classifies an HTTP status code.
     *
     * @param status HTTP status code
     * @return Classification string
     */
    public static String classifyStatus(int status) {
        if (status >= 200 && status < 300) return "success";
        if (status >= 300 && status < 400) return "redirect";
        if (status >= 400 && status < 500) return "client_error";
        if (status >= 500) return "server_error";
        return "unknown";
    }

    /**
     * Checks if a content type indicates sensitive data.
     *
     * @param contentType The Content-Type header value
     * @return true if potentially sensitive
     */
    public static boolean isSensitiveContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("application/json") ||
               ct.contains("application/xml") ||
               ct.contains("text/xml") ||
               ct.contains("application/x-www-form-urlencoded") ||
               ct.contains("multipart/form-data");
    }
}
