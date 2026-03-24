package org.jvmxray.agent.sensor.configuration;

/**
 * Utility methods shared by configuration interceptors.
 * Enhanced with OWASP/CWE mappings, risk classification, and remediation guidance.
 *
 * @author Milton Smith
 */
public class ConfigurationUtils {

    // Sensitive system properties that should be monitored
    public static final String[] SENSITIVE_PROPERTIES = {
        "java.security.policy",
        "java.security.manager",
        "java.security.auth.login.config",
        "java.library.path",
        "java.class.path",
        "java.ext.dirs",
        "java.endorsed.dirs",
        "user.dir",
        "user.home",
        "java.io.tmpdir",
        "file.separator",
        "path.separator",
        "line.separator",
        "os.name",
        "os.arch",
        "os.version",
        "java.version",
        "java.vendor",
        "java.home"
    };

    // Sensitive environment variables
    public static final String[] SENSITIVE_ENV_VARS = {
        "PATH",
        "JAVA_HOME",
        "CLASSPATH",
        "LD_LIBRARY_PATH",
        "DYLD_LIBRARY_PATH",
        "HOME",
        "USER",
        "USERNAME",
        "TEMP",
        "TMP",
        "PASSWORD",
        "SECRET",
        "TOKEN",
        "KEY",
        "API_KEY"
    };

    // Security-critical properties (modification is high risk)
    private static final String[] SECURITY_CRITICAL_PROPERTIES = {
        "java.security.policy",
        "java.security.manager",
        "java.security.auth.login.config",
        "java.class.path",
        "java.library.path",
        "java.ext.dirs"
    };

    // OWASP category mappings for property access
    private static final java.util.Map<String, String> OWASP_MAPPINGS = new java.util.HashMap<>();
    static {
        // A05: Security Misconfiguration
        OWASP_MAPPINGS.put("java.security.policy", "A05");
        OWASP_MAPPINGS.put("java.security.manager", "A05");
        OWASP_MAPPINGS.put("java.security.auth.login.config", "A05");
        OWASP_MAPPINGS.put("java.security.debug", "A05");

        // A01: Broken Access Control (path manipulation)
        OWASP_MAPPINGS.put("java.class.path", "A01");
        OWASP_MAPPINGS.put("java.library.path", "A01");
        OWASP_MAPPINGS.put("java.ext.dirs", "A01");
        OWASP_MAPPINGS.put("user.dir", "A01");
        OWASP_MAPPINGS.put("java.io.tmpdir", "A01");

        // A09: Security Logging and Monitoring Failures
        OWASP_MAPPINGS.put("java.util.logging.config.file", "A09");
        OWASP_MAPPINGS.put("log4j.configuration", "A09");
    }

    // CWE ID mappings for property access
    private static final java.util.Map<String, String> CWE_MAPPINGS = new java.util.HashMap<>();
    static {
        CWE_MAPPINGS.put("java.security.policy", "CWE-1188"); // Insecure Default
        CWE_MAPPINGS.put("java.security.manager", "CWE-266"); // Incorrect Privilege Assignment
        CWE_MAPPINGS.put("java.class.path", "CWE-426"); // Untrusted Search Path
        CWE_MAPPINGS.put("java.library.path", "CWE-426"); // Untrusted Search Path
        CWE_MAPPINGS.put("java.ext.dirs", "CWE-426"); // Untrusted Search Path
        CWE_MAPPINGS.put("user.dir", "CWE-22"); // Path Traversal
        CWE_MAPPINGS.put("java.io.tmpdir", "CWE-379"); // Temp File Creation
    }

    // Remediation guidance
    private static final java.util.Map<String, String> REMEDIATION_GUIDANCE = new java.util.HashMap<>();
    static {
        REMEDIATION_GUIDANCE.put("java.security.policy", "Review security policy file permissions and content");
        REMEDIATION_GUIDANCE.put("java.security.manager", "Ensure SecurityManager is not being disabled");
        REMEDIATION_GUIDANCE.put("java.class.path", "Validate classpath entries for untrusted code");
        REMEDIATION_GUIDANCE.put("java.library.path", "Restrict native library loading paths");
        REMEDIATION_GUIDANCE.put("java.io.tmpdir", "Ensure temp directory has proper permissions");
    }

    /**
     * Checks if a system property is considered sensitive.
     */
    public static boolean isSensitiveProperty(String key) {
        if (key == null) return false;

        for (String sensitive : SENSITIVE_PROPERTIES) {
            if (key.equals(sensitive)) {
                return true;
            }
        }

        return key.contains("password") ||
               key.contains("secret") ||
               key.contains("key") ||
               key.contains("token");
    }

    /**
     * Checks if a property is security-critical.
     */
    public static boolean isSecurityCritical(String key) {
        if (key == null) return false;

        for (String critical : SECURITY_CRITICAL_PROPERTIES) {
            if (key.equals(critical)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets comprehensive security metadata for a property access.
     */
    public static java.util.Map<String, String> getSecurityMetadata(String propertyKey, boolean isModification) {
        java.util.Map<String, String> metadata = new java.util.HashMap<>();

        if (propertyKey == null) return metadata;

        // Check if sensitive
        boolean isSensitive = isSensitiveProperty(propertyKey);
        metadata.put("sensitive_property", String.valueOf(isSensitive));

        // Check if security critical
        boolean isCritical = isSecurityCritical(propertyKey);
        metadata.put("security_critical", String.valueOf(isCritical));

        // Determine risk level
        String riskLevel;
        if (isCritical && isModification) {
            riskLevel = "CRITICAL";
        } else if (isCritical) {
            riskLevel = "HIGH";
        } else if (isSensitive && isModification) {
            riskLevel = "HIGH";
        } else if (isSensitive) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        metadata.put("risk_level", riskLevel);

        // Add OWASP category if available
        String owaspCategory = OWASP_MAPPINGS.get(propertyKey);
        if (owaspCategory != null) {
            metadata.put("owasp_category", owaspCategory);
        }

        // Add CWE ID if available
        String cweId = CWE_MAPPINGS.get(propertyKey);
        if (cweId != null) {
            metadata.put("cwe_id", cweId);
        }

        // Add remediation guidance if available
        String guidance = REMEDIATION_GUIDANCE.get(propertyKey);
        if (guidance != null) {
            metadata.put("remediation_guidance", guidance);
        }

        // Add modification impact
        if (isModification) {
            metadata.put("modification_impact", isCritical ? "HIGH" : (isSensitive ? "MEDIUM" : "LOW"));
        }

        return metadata;
    }

    /**
     * Checks if an environment variable is considered sensitive.
     */
    public static boolean isSensitiveEnvironmentVariable(String name) {
        if (name == null) return false;

        String upper = name.toUpperCase();
        for (String sensitive : SENSITIVE_ENV_VARS) {
            if (upper.equals(sensitive) || upper.contains(sensitive)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Truncates a value for logging purposes.
     */
    public static String truncateValue(String value) {
        if (value == null) return null;
        if (value.length() <= 100) return value;
        return value.substring(0, 100) + "...[truncated]";
    }

    /**
     * Gets the property count from a Properties object.
     */
    public static int getPropertyCount(Object properties) {
        try {
            Object result = properties.getClass().getMethod("size").invoke(properties);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Analyzes the call stack to determine context.
     */
    public static String analyzeCallContext() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("org.jvmxray") &&
                    !className.startsWith("java.lang.System") &&
                    !className.startsWith("java.util.Properties") &&
                    !className.startsWith("java.util.prefs") &&
                    !className.startsWith("sun.") &&
                    !className.startsWith("net.bytebuddy")) {
                    return className + "." + element.getMethodName();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    /**
     * Determines if the access context is expected (framework code vs application code).
     */
    public static String categorizeAccessContext(String callContext) {
        if (callContext == null) return "unknown";

        if (callContext.contains("spring") || callContext.contains("springframework")) {
            return "framework_spring";
        } else if (callContext.contains("hibernate")) {
            return "framework_hibernate";
        } else if (callContext.contains("apache")) {
            return "framework_apache";
        } else if (callContext.startsWith("java.") || callContext.startsWith("javax.")) {
            return "jdk_internal";
        } else {
            return "application_code";
        }
    }
}