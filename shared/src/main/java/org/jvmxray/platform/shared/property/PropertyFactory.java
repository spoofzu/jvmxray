package org.jvmxray.platform.shared.property;

import org.jvmxray.platform.shared.util.GUID;

import java.io.File;
import java.io.IOException;

/**
 * Singleton factory for initializing and managing JVMXRay configuration properties
 * and directory structure. Sets up base, home, and log directories based on system
 * properties or defaults, and provides access to an {@link AgentProperties} instance
 * for agent-specific configurations.
 *
 * <p>In unit testing, it prioritizes loading configurations from the classpath
 * (e.g., <code>logback-test.xml2</code>). In production, it uses the JVMXRay home
 * directory (e.g., <code>jvmxray.home/logback.xml</code>), copying defaults from the
 * classpath if needed. System properties like <code>jvmxray.base</code>,
 * <code>jvmxray.home</code>, and <code>jvmxray.logs</code> can override default
 * directory locations.</p>
 *
 * @author Milton Smith
 * @see AgentProperties
 * @see PropertyBase
 */
public class PropertyFactory {

    // Singleton instance of the factory
    private static final PropertyFactory instance = new PropertyFactory();
    // System file separator
    private static final String systemSlash = System.getProperty("file.separator");
    // Base directory for JVMXRay (e.g., user.home or jvmxray.base)
    private static File jvmxrayBase = null;
    // Home directory for JVMXRay (e.g., jvmxrayBase/jvmxray)
    private static File jvmxrayHome = null;
    // Log directory for JVMXRay (e.g., jvmxrayHome/logs)
    private static File jvmxrayLogHome = null;
    // Agent properties instance
    private static AgentProperties agentProperties = null;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private PropertyFactory() {
        // Prevent external instantiation
    }

    /**
     * Retrieves the singleton instance of the {@code PropertyFactory}.
     *
     * @return The singleton {@code PropertyFactory} instance.
     */
    public static final PropertyFactory getInstance() {
        // Return the singleton instance
        return instance;
    }

    /**
     * Initializes the JVMXRay property factory by setting up the base, home, and log
     * directories based on system properties or defaults. Updates system properties
     * with resolved directory paths.
     *
     * @throws Exception If directory creation fails or required properties are invalid.
     */
    public synchronized void init() throws Exception {
        // Resolve base directory from jvmxray.base or user.home
        String basePath = getOrDefault("jvmxray.base", System.getProperty("user.home"));
        // Validate base path
        if (basePath == null || basePath.isEmpty()) {
            throw new IOException("Unable to compute jvmxray.base. Set -Djvmxray.base=/path/to/base");
        }
        // Ensure base directory exists
        jvmxrayBase = ensureDirectory(basePath);
        // Update system property
        System.setProperty("jvmxray.base", jvmxrayBase.getPath());

        // Resolve home directory from jvmxray.home or jvmxrayBase/jvmxray
        String homePath = getOrDefault("jvmxray.home", jvmxrayBase.getPath() + File.separator + "jvmxray");
        // Ensure home directory exists
        jvmxrayHome = ensureDirectory(homePath);
        // Update system property
        System.setProperty("jvmxray.home", jvmxrayHome.getPath());

        // Resolve log directory from jvmxray.logs or jvmxrayHome/logs
        String logsPath = getOrDefault("jvmxray.logs", jvmxrayHome.getPath() + File.separator + "logs");
        // Ensure log directory exists
        jvmxrayLogHome = ensureDirectory(logsPath);
        // Update system property
        System.setProperty("jvmxray.logs", jvmxrayLogHome.getPath());
    }

    /**
     * Retrieves a system property, returning a default value if null or empty.
     *
     * @param propertyKey The system property key.
     * @param defaultValue The default value if the property is null or empty.
     * @return The property value or the default value.
     */
    private String getOrDefault(String propertyKey, String defaultValue) {
        // Get system property
        String value = System.getProperty(propertyKey);
        // Return value or default if null or empty
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * Ensures a directory exists, creating it if necessary.
     *
     * @param path The directory path.
     * @return The {@link File} object representing the directory.
     */
    private File ensureDirectory(String path) {
        // Create directory if it does not exist
        File dir = new File(path);
        dir.mkdirs();
        return dir;
    }

    /**
     * Retrieves the JVMXRay base directory.
     *
     * @return The base directory {@link File}.
     * @throws RuntimeException If the factory is not initialized.
     */
    public File getJvmxrayBase() {
        // Check if initialized
        if (jvmxrayBase == null) {
            throw new RuntimeException("PropertyFactory must be initialized prior to use.");
        }
        return jvmxrayBase;
    }

    /**
     * Retrieves the JVMXRay home directory.
     *
     * @return The home directory {@link File}.
     * @throws RuntimeException If the factory is not initialized.
     */
    public File getJvmxrayHome() {
        // Check if initialized
        if (jvmxrayHome == null) {
            throw new RuntimeException("PropertyFactory must be initialized prior to use.");
        }
        return jvmxrayHome;
    }

    /**
     * Retrieves the JVMXRay log directory.
     *
     * @return The log directory {@link File}.
     * @throws RuntimeException If the factory is not initialized.
     */
    public File getJvmxrayLogHome() {
        // Check if initialized
        if (jvmxrayLogHome == null) {
            throw new RuntimeException("PropertyFactory must be initialized prior to use.");
        }
        return jvmxrayLogHome;
    }

    /**
     * Retrieves the {@link AgentProperties} instance, initializing it if necessary
     * with default properties if modified or new.
     *
     * @return The {@link AgentProperties} instance.
     * @throws Exception If initialization fails due to uninitialized directories or
     *                   property loading errors.
     */
    public synchronized AgentProperties getAgentProperties() throws Exception {
        //todo: need to redesign.  knowledge of property config should be stored in PropertyBase implementation (e.g., AgentProperties).
        if (agentProperties == null) {
            // Validate directory initialization
            if (jvmxrayBase == null || jvmxrayHome == null || jvmxrayLogHome == null) {
                throw new Exception("PropertyFactory must be initialized prior to use.");
            }
            // Create and initialize agent properties
            agentProperties = new AgentProperties(jvmxrayHome.toPath());
            agentProperties.init();
            // Always set default properties for new or empty files
            if (agentProperties.isPropertyFileNewOrEmpty()) {
                agentProperties.setProperty("AID", GUID.getID());
                agentProperties.setProperty("CAT", "unit-test");
                agentProperties.setProperty("log.message.encoding", "true");
                agentProperties.setProperty("jvmxray.sensor.http","org.jvmxray.agent.sensor.http.HttpSensor");
                agentProperties.setProperty("jvmxray.sensor.fileio","org.jvmxray.agent.sensor.io.FileIOSensor");
                agentProperties.setProperty("jvmxray.sensor.monitor","org.jvmxray.agent.sensor.monitor.MonitorSensor");
                agentProperties.setProperty("jvmxray.sensor.appinit","org.jvmxray.agent.sensor.system.AppInitSensor");
                agentProperties.setProperty("jvmxray.sensor.lib","org.jvmxray.agent.sensor.system.LibSensor");
                agentProperties.setProperty("jvmxray.sensor.sql","org.jvmxray.agent.sensor.sql.SQLSensor");
                agentProperties.setIntProperty("monitor.interval", 60000);
                // Save properties file with header
                agentProperties.saveProperties("JVMXRay Agent Properties");
            }
        }
        return agentProperties;
    }

}