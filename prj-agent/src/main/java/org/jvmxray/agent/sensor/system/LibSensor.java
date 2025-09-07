package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

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
    // Cache of known JARs to avoid duplicate logging
    private final Map<String, Boolean> knownJars = new HashMap<>();
    // Interval for checking dynamic JARs (in seconds)
    private int checkIntervalSeconds = 60; // Default to 60 seconds
    // Instrumentation instance for accessing loaded classes
    private Instrumentation instrumentation;

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
     * Checks the static classpath for JAR files and logs their presence.
     */
    private void checkStaticClasspath() {
        // Retrieve classpath entries
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(System.getProperty("path.separator"));
        for (String entry : entries) {
            if (entry.endsWith(".jar") && !knownJars.containsKey(entry)) {
                // Log new JAR file
                Map<String, String> eventData = new HashMap<>();
                eventData.put("method", "static");
                eventData.put("jarPath", entry);
                logProxy.logMessage(NAMESPACE, "INFO", eventData);
                // Cache JAR to avoid duplicate logging
                knownJars.put(entry, true);
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
                        // Log new dynamically loaded JAR
                        Map<String, String> eventData = new HashMap<>();
                        eventData.put("method", "dynamic");
                        eventData.put("jarPath", jarPath);
                        logProxy.logMessage(NAMESPACE, "INFO", eventData);
                        // Cache JAR to avoid duplicate logging
                        knownJars.put(jarPath, true);
                    }
                }
            } catch (Exception e) {
                // Skip classes with invalid code sources
            }
        }
    }
}