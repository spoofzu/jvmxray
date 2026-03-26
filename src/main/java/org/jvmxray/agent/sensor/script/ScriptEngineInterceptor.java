package org.jvmxray.agent.sensor.script;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interceptor for monitoring script engine operations including script evaluation
 * and engine lookups. Extracts rich security metadata such as engine details,
 * script hashing, and suspicious pattern detection.
 *
 * @author Milton Smith
 */
public class ScriptEngineInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.script";
    public static final LogProxy logProxy = LogProxy.getInstance();

    private static final String[] SUSPICIOUS_PATTERNS = {
        "Runtime.exec", "ProcessBuilder", "Class.forName",
        "java.net", "URLClassLoader", "java.io.File",
        "getRuntime", "loadClass"
    };

    /**
     * Computes a SHA-256 hash of the script content, returning the first 16 hex characters.
     */
    private static String computeScriptHash(String script) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // first 8 bytes = 16 hex chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Detects suspicious patterns in script content that may indicate malicious activity.
     */
    private static String detectSuspiciousPatterns(String script) {
        List<String> found = new ArrayList<>();
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (script.contains(pattern)) {
                found.add(pattern);
            }
        }
        return found.isEmpty() ? null : String.join(",", found);
    }

    /**
     * Checks if the intercepted object is a ScriptEngine (vs ScriptEngineManager)
     * using reflection to avoid bootloader class issues.
     */
    private static boolean isScriptEngine(Object instance) {
        if (instance == null) return false;
        try {
            Class<?> scriptEngineClass = Class.forName("javax.script.ScriptEngine");
            return scriptEngineClass.isAssignableFrom(instance.getClass());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the intercepted object is a ScriptEngineManager.
     */
    private static boolean isScriptEngineManager(Object instance) {
        if (instance == null) return false;
        try {
            Class<?> managerClass = Class.forName("javax.script.ScriptEngineManager");
            return managerClass.isAssignableFrom(instance.getClass());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the argument is a Reader instance.
     */
    private static boolean isReader(Object arg) {
        if (arg == null) return false;
        try {
            Class<?> readerClass = Class.forName("java.io.Reader");
            return readerClass.isAssignableFrom(arg.getClass());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts engine name from a ScriptEngine instance via reflection.
     */
    private static String getEngineName(Object scriptEngine) {
        try {
            Object factory = scriptEngine.getClass().getMethod("getFactory").invoke(scriptEngine);
            return (String) factory.getClass().getMethod("getEngineName").invoke(factory);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extracts language name from a ScriptEngine instance via reflection.
     */
    private static String getLanguageName(Object scriptEngine) {
        try {
            Object factory = scriptEngine.getClass().getMethod("getFactory").invoke(scriptEngine);
            return (String) factory.getClass().getMethod("getLanguageName").invoke(factory);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Enter method records start time and enters MCC correlation scope.
     */
    @Advice.OnMethodEnter
    public static long enter() {
        MCCScope.enter("Script");
        return System.nanoTime();
    }

    /**
     * Exit method extracts rich metadata from the script engine operation.
     *
     * @param instance The ScriptEngine or ScriptEngineManager instance.
     * @param arg0 The first argument (script text, Reader, or engine name/extension).
     * @param startTime The start time from enter.
     * @param throwable Any thrown exception.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This Object instance,
                            @Advice.Argument(0) Object arg0,
                            @Advice.Enter long startTime,
                            @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();

            // Calculate duration
            double durationMs = (System.nanoTime() - startTime) / 1_000_000.0;
            metadata.put("duration_ms", String.format("%.2f", durationMs));

            // Determine operation type and extract metadata based on instance type
            if (isScriptEngine(instance)) {
                metadata.put("operation", "script_eval");

                // Extract engine details via reflection
                metadata.put("engine_name", getEngineName(instance));
                metadata.put("script_language", getLanguageName(instance));

                // Analyze the script argument
                if (arg0 instanceof String) {
                    String scriptText = (String) arg0;
                    metadata.put("script_length", String.valueOf(scriptText.length()));
                    metadata.put("script_hash", computeScriptHash(scriptText));
                    metadata.put("script_snippet", scriptText.length() > 200
                            ? scriptText.substring(0, 200) : scriptText);

                    String suspicious = detectSuspiciousPatterns(scriptText);
                    if (suspicious != null) {
                        metadata.put("suspicious_patterns", suspicious);
                        metadata.put("risk_level", "HIGH");
                    } else {
                        metadata.put("risk_level", "LOW");
                    }
                } else if (isReader(arg0)) {
                    metadata.put("script_source", "reader");
                }

            } else if (isScriptEngineManager(instance)) {
                metadata.put("operation", "engine_lookup");

                if (arg0 instanceof String) {
                    metadata.put("engine_lookup", (String) arg0);
                }
            }

            // Handle errors
            if (throwable != null) {
                metadata.put("status", "error");
                metadata.put("error_class", throwable.getClass().getName());
                metadata.put("error_message", throwable.getMessage());
                logProxy.logMessage(NAMESPACE + ".execution", "ERROR", metadata);
            } else {
                metadata.put("status", "success");
                logProxy.logMessage(NAMESPACE + ".execution", "INFO", metadata);
            }

        } catch (Exception e) {
            // Fail silently - sensor must not disrupt application
        } finally {
            MCCScope.exit("Script");
        }
    }
}
