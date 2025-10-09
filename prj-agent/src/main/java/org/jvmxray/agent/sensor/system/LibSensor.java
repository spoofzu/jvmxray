package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.agent.util.JarMetadataExtractor;
import org.jvmxray.agent.util.JarMetadataExtractor.JarMetadata;
import org.jvmxray.agent.util.StatsRegistry;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sensor implementation for detecting and logging JAR files loaded in the JVM.
 * Monitors both static classpath JARs at startup and dynamically loaded JARs during runtime
 * using the Instrumentation API. Runs a background thread to periodically check for new JARs
 * and logs events via {@link LogProxy}. Integrates with the JVMXRay agent framework for system monitoring.
 *
 * @author Milton Smith
 */
public class LibSensor extends AbstractSensor implements Sensor {
    // Namespace for logging JAR detection events
    private static final String NAMESPACE = "org.jvmxray.events.system.lib";
    // Singleton instance of LogProxy for logging
    private static final LogProxy logProxy = LogProxy.getInstance();
    // Background thread for dynamic JAR detection
    private Thread libDetectionThread;
    // Cache of known JARs to avoid duplicate logging (bounded to 10,000 entries)
    private final Map<String, Boolean> knownJars = new LinkedHashMap<String, Boolean>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 10000;
        }
    };
    // Interval for checking dynamic JARs (in seconds)
    private int checkIntervalSeconds = 60; // Default to 60 seconds
    // Instrumentation instance for accessing loaded classes
    private Instrumentation instrumentation;

    // Metrics tracking
    private final AtomicInteger staticJarsLoaded = new AtomicInteger(0);
    private final AtomicInteger dynamicJarsLoaded = new AtomicInteger(0);
    private final AtomicInteger totalPackages = new AtomicInteger(0);

    // Static sensor identity.
    private static final String SENSOR_GUID = "D2F1DE24-EA0F-44F8-8D0E-E23398ECB537"; // Generated via uuidgen

    public LibSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }


    /**
     * Initializes the sensor by setting up the instrumentation instance, parsing agent arguments,
     * checking the static classpath, and starting a background thread for dynamic JAR detection.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent, may include "lib.interval" for check interval.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        this.instrumentation = inst;

        // Parse agent arguments for custom check interval
        if (agentArgs != null && agentArgs.contains("lib.interval=")) {
            try {
                String[] args = agentArgs.split(",");
                for (String arg : args) {
                    if (arg.startsWith("lib.interval=")) {
                        checkIntervalSeconds = Integer.parseInt(arg.split("=")[1]);
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                // Log warning if interval parsing fails
                logProxy.logMessage(NAMESPACE, "WARN", Map.of(
                        "message", "Invalid lib.interval in agentArgs, using default: " + checkIntervalSeconds
                ));
            }
        }

        // Check static classpath JARs at startup
        checkStaticClasspath();

        // Start background thread for dynamic JAR detection
        libDetectionThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    detectDynamicJars();
                    Thread.yield(); // Reduce CPU contention
                    Thread.sleep(checkIntervalSeconds * 1000L); // Sleep for configured interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    break;
                } catch (Exception e) {
                    // Log errors during dynamic detection
                    logProxy.logMessage(NAMESPACE, "ERROR", Map.of(
                            "message", "Dynamic JAR detection failed: " + e.getMessage()
                    ));
                }
            }
        }, "jvmxray.libsensor-1");
        libDetectionThread.setDaemon(true);
        libDetectionThread.start();
    }

    /**
     * Shuts down the sensor by interrupting the background detection thread.
     */
    @Override
    public void shutdown() {
        if (libDetectionThread != null) {
            // Interrupt the detection thread to stop it
            libDetectionThread.interrupt();
        }
    }

    /**
     * Calculates SHA-256 hash of a file.
     *
     * @param filePath Path to the file to hash
     * @return SHA-256 hash as hexadecimal string, or null if calculation fails
     */
    private String calculateSHA256(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            // Log error but don't fail the entire event
            logProxy.logMessage(NAMESPACE, "WARN", Map.of(
                    "message", "Failed to calculate SHA-256 for " + filePath + ": " + e.getMessage()
            ));
            return null;
        }
    }

    /**
     * Checks the static classpath for JAR files and logs their presence.
     */
    private void checkStaticClasspath() {
        // Retrieve classpath entries
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(System.getProperty("path.separator"));
        for (String entry : entries) {
            if (entry.endsWith(".jar") && !knownJars.containsKey(entry)) {
                // Calculate SHA-256 hash for the JAR file
                String sha256Hash = calculateSHA256(entry);

                // Extract JAR metadata for OWASP Dependency Check analysis
                JarMetadata metadata = JarMetadataExtractor.extractMetadata(entry);

                // Log new JAR file with metadata
                Map<String, String> eventData = new HashMap<>();
                eventData.put("method", "static");
                eventData.put("jarPath", entry);
                if (sha256Hash != null) {
                    eventData.put("sha256", sha256Hash);
                }

                // Add Maven coordinates if available
                if (metadata != null && metadata.hasMavenCoordinates()) {
                    eventData.put("groupId", metadata.groupId);
                    eventData.put("artifactId", metadata.artifactId);
                    eventData.put("version", metadata.version);
                }

                // Add manifest evidence if available
                if (metadata != null && metadata.hasManifestEvidence()) {
                    if (metadata.implementationTitle != null) {
                        eventData.put("implTitle", metadata.implementationTitle);
                    }
                    if (metadata.implementationVersion != null) {
                        eventData.put("implVersion", metadata.implementationVersion);
                    }
                    if (metadata.implementationVendor != null) {
                        eventData.put("implVendor", metadata.implementationVendor);
                    }
                }

                // Add package names if available
                if (metadata != null && !metadata.packageNames.isEmpty()) {
                    eventData.put("packages", String.join(",", metadata.packageNames));
                }

                logProxy.logMessage(NAMESPACE, "INFO", eventData);
                // Cache JAR to avoid duplicate logging
                knownJars.put(entry, true);

                // Update metrics
                staticJarsLoaded.incrementAndGet();
                if (metadata != null && !metadata.packageNames.isEmpty()) {
                    totalPackages.addAndGet(metadata.packageNames.size());
                }
                updateStats();
            }
        }
    }

    /**
     * Detects dynamically loaded JAR files by inspecting loaded classes' code sources.
     */
    private void detectDynamicJars() {
        // Retrieve all loaded classes
        Class<?>[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : loadedClasses) {
            try {
                if (clazz.getProtectionDomain() != null && clazz.getProtectionDomain().getCodeSource() != null) {
                    // Get JAR path from class's code source
                    String jarPath = clazz.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .getPath();
                    if (jarPath.endsWith(".jar") && !knownJars.containsKey(jarPath)) {
                        // Calculate SHA-256 hash for the JAR file
                        String sha256Hash = calculateSHA256(jarPath);

                        // Extract JAR metadata for OWASP Dependency Check analysis
                        JarMetadata metadata = JarMetadataExtractor.extractMetadata(jarPath);

                        // Log new dynamically loaded JAR with metadata
                        Map<String, String> eventData = new HashMap<>();
                        eventData.put("method", "dynamic");
                        eventData.put("jarPath", jarPath);
                        if (sha256Hash != null) {
                            eventData.put("sha256", sha256Hash);
                        }

                        // Add Maven coordinates if available
                        if (metadata != null && metadata.hasMavenCoordinates()) {
                            eventData.put("groupId", metadata.groupId);
                            eventData.put("artifactId", metadata.artifactId);
                            eventData.put("version", metadata.version);
                        }

                        // Add manifest evidence if available
                        if (metadata != null && metadata.hasManifestEvidence()) {
                            if (metadata.implementationTitle != null) {
                                eventData.put("implTitle", metadata.implementationTitle);
                            }
                            if (metadata.implementationVersion != null) {
                                eventData.put("implVersion", metadata.implementationVersion);
                            }
                            if (metadata.implementationVendor != null) {
                                eventData.put("implVendor", metadata.implementationVendor);
                            }
                        }

                        // Add package names if available
                        if (metadata != null && !metadata.packageNames.isEmpty()) {
                            eventData.put("packages", String.join(",", metadata.packageNames));
                        }

                        logProxy.logMessage(NAMESPACE, "INFO", eventData);
                        // Cache JAR to avoid duplicate logging
                        knownJars.put(jarPath, true);

                        // Update metrics
                        dynamicJarsLoaded.incrementAndGet();
                        if (metadata != null && !metadata.packageNames.isEmpty()) {
                            totalPackages.addAndGet(metadata.packageNames.size());
                        }
                        updateStats();
                    }
                }
            } catch (Exception e) {
                // Skip classes with invalid code sources
            }
        }
    }

    /**
     * Updates statistics in the StatsRegistry for monitoring.
     */
    private void updateStats() {
        synchronized (knownJars) {  // Brief lock for HashMap.size()
            StatsRegistry.register("lib_static_loaded", String.valueOf(staticJarsLoaded.get()));
            StatsRegistry.register("lib_dynamic_loaded", String.valueOf(dynamicJarsLoaded.get()));
            StatsRegistry.register("lib_total_packages", String.valueOf(totalPackages.get()));
            StatsRegistry.register("lib_cache_size", String.valueOf(knownJars.size()));
        }
    }
}