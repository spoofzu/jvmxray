package org.jvmxray.platform.shared.init;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.property.ComponentProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract base class for component initialization in JVMXRay.
 * <p>
 * This class provides a common initialization framework for all JVMXRay components,
 * handling directory setup, resource copying, property management, and logging configuration.
 * Each component extends this class to provide component-specific configuration.
 * </p>
 * <p>
 * The initialization follows the JVMXRay home directory standards:
 * - Test mode (jvmxray.test.home): Uses {jvmxray.test.home}/component/
 * - Production mode (jvmxray.home): Uses {jvmxray.home}/jvmxray/component/
 * - Auto-detect: Uses project/.jvmxray/component/
 * </p>
 * <p>
 * <b>Important:</b> This class does not perform logging to avoid circular dependencies.
 * Critical errors are reported via System.err only.
 * </p>
 * 
 * @since 0.0.1
 * @author JVMXRay Platform
 */
public abstract class ComponentInitializer {

    /**
     * JVMXRay home directory (parent directory for all components).
     */
    protected Path jvmxrayHome = null;

    /**
     * Component-specific project directory.
     */
    protected File componentProject = null;

    /**
     * Component-specific properties instance.
     */
    protected PropertyBase properties = null;


    /**
     * Flag to track initialization status.
     */
    protected boolean isInitialized = false;

    /**
     * Returns the name of the component (e.g., "agent", "common", "integration").
     * This name is used for directory structure and resource resolution.
     * 
     * @return The component name
     */
    protected abstract String getComponentName();

    /**
     * Returns a map of resources to copy from classpath to component directories.
     * The key is the source resource name, the value is the target file path
     * relative to the component's config directory.
     * 
     * @return Map of resource names to target paths
     */
    protected abstract Map<String, String> getResourcesToCopy();

    /**
     * Returns default properties for the component.
     * These will be used to initialize the properties file if it doesn't exist
     * or is empty.
     * 
     * @return Default properties for the component
     */
    protected abstract Properties getDefaultProperties();

    /**
     * Creates a component-specific PropertyBase instance.
     * Subclasses can override to provide specialized property handling.
     * 
     * @param componentHome The component's home directory
     * @return PropertyBase instance for the component
     */
    protected PropertyBase createProperties(Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Checks if running in Maven test mode.
     */
    private boolean isMavenTestMode() {
        return System.getProperty("jvmxray.test.home") != null;
    }

    /**
     * Initializes the component by setting up directories, copying resources,
     * initializing properties, and configuring logging.
     * 
     * @throws Exception if initialization fails
     */
    public synchronized void initialize() throws Exception {
        if (isInitialized) {
            return;
        }

        try {
            setupDirectories();
            copyResources();
            initializeProperties();
            initializeLogging();
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("ComponentInitializer: Failed to initialize " + getComponentName() + " component: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets up the JVMXRay home directory structure following project standards.
     * 
     * Directory structure logic:
     * - If jvmxray.test.home is set (Maven tests): use directly (e.g., .jvmxray/component)
     * - If jvmxray.home is set (production): add /jvmxray subdirectory (e.g., /custom/jvmxray/component)
     * - If auto-detected: use project/.jvmxray directly (e.g., .jvmxray/component)
     * 
     * @throws IOException if directory creation fails
     */
    protected void setupDirectories() throws IOException {
        // Determine JVMXRay home directory
        String testHome = System.getProperty("jvmxray.test.home");
        String prodHome = System.getProperty("jvmxray.home");

        if (testHome != null && prodHome != null) {
            throw new IllegalStateException("Both jvmxray.test.home and jvmxray.home are set. Only one should be specified.");
        }

        if (testHome != null) {
            // Test mode: use test home directly
            jvmxrayHome = Path.of(testHome);
        } else if (prodHome != null) {
            // Production mode: add jvmxray subdirectory
            jvmxrayHome = Path.of(prodHome, "jvmxray");
        } else {
            // Auto-detect: use current project/.jvmxray
            String currentDir = System.getProperty("user.dir");
            jvmxrayHome = Path.of(currentDir, ".jvmxray");
        }

        // Create component directory structure
        Path componentHomePath = jvmxrayHome.resolve(getComponentName());
        Path componentConfigPath = componentHomePath.resolve("config");
        Path componentLogPath = componentHomePath.resolve("logs");

        ensureDirectory(componentHomePath);
        ensureDirectory(componentConfigPath);
        ensureDirectory(componentLogPath);

        componentProject = componentHomePath.toFile();

        // Set component-specific system properties
        String componentName = getComponentName();
        if (System.getProperty("jvmxray." + componentName + ".logs") == null) {
            System.setProperty("jvmxray." + componentName + ".logs", componentLogPath.toString());
        }
        if (System.getProperty("jvmxray." + componentName + ".config") == null) {
            System.setProperty("jvmxray." + componentName + ".config", componentConfigPath.toString());
        }
    }

    /**
     * Copies required resources from classpath to component directories.
     * Only copies resources that don't already exist to preserve user customizations.
     * 
     * @throws IOException if resource copying fails
     */
    protected void copyResources() throws IOException {
        Map<String, String> resources = getResourcesToCopy();
        Path configPath = jvmxrayHome.resolve(getComponentName()).resolve("config");

        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String resourceName = entry.getKey();
            String targetFileName = entry.getValue();
            File targetFile = configPath.resolve(targetFileName).toFile();

            copyResourceToTarget(resourceName, targetFile);
        }
    }

    /**
     * Initializes component properties, creating default configuration if needed.
     * 
     * @throws Exception if property initialization fails
     */
    protected void initializeProperties() throws Exception {
        Path componentHomePath = jvmxrayHome.resolve(getComponentName());
        properties = createProperties(componentHomePath);
        properties.init();

        // Set default properties if file is new or empty
        if (properties.isPropertyFileNewOrEmpty()) {
            Properties defaults = getDefaultProperties();
            for (String key : defaults.stringPropertyNames()) {
                properties.setProperty(key, defaults.getProperty(key));
            }
            properties.saveProperties("JVMXRay " + getComponentName() + " Properties");
        }
    }

    /**
     * Initializes logging configuration for the component.
     * Sets the logback configuration file system property for standard SLF4J integration.
     * 
     * @throws Exception if logging initialization fails
     */
    protected void initializeLogging() throws Exception {
        String componentName = getComponentName();
        
        // Skip agent - it's a special case that uses LogProxy instead of Logback
        if ("agent".equals(componentName)) {
            return;
        }
        
        // Set logback configuration file path for standard SLF4J integration
        String componentConfigPath = System.getProperty("jvmxray." + componentName + ".config");
        if (componentConfigPath == null) {
            componentConfigPath = new File(componentProject, "config").getAbsolutePath();
        }
        
        File logbackConfig = new File(componentConfigPath, "logback.xml");
        if (logbackConfig.exists()) {
            System.setProperty("logback.configurationFile", logbackConfig.getAbsolutePath());
        }
    }

    /**
     * Copies a resource from the classpath to a target file.
     * Only copies if the target file doesn't exist to preserve user customizations.
     * 
     * @param resource Name of resource in classpath (at root level)
     * @param target Target file to copy to
     * @throws IOException if resource copying fails
     */
    protected void copyResourceToTarget(String resource, File target) throws IOException {
        if (!target.exists()) {
            // Load resources from root level of classpath
            String resourcePath = "/" + resource;
            try (InputStream source = getClass().getResourceAsStream(resourcePath)) {
                if (source != null) {
                    Files.copy(source, target.toPath());
                } else {
                    throw new IOException("Resource not found in classpath: " + resourcePath);
                }
            }
        }
    }

    /**
     * Ensures a directory exists, creating it if necessary.
     * 
     * @param path The directory path to ensure
     */
    protected void ensureDirectory(Path path) {
        path.toFile().mkdirs();
    }

    // ================================================================================================
    // PUBLIC API METHODS
    // ================================================================================================

    /**
     * Returns the component properties instance.
     * 
     * @return The component properties
     * @throws Exception if initialization has not been performed
     */
    public PropertyBase getProperties() throws Exception {
        if (properties == null || jvmxrayHome == null || componentProject == null) {
            throw new IllegalStateException("ComponentInitializer not initialized - call initialize() first");
        }
        return properties;
    }


    /**
     * Returns the JVMXRay home directory.
     * 
     * @return The JVMXRay home path
     */
    public Path getJvmxrayHome() {
        return jvmxrayHome;
    }

    /**
     * Returns the component's project directory.
     * 
     * @return The component project directory
     */
    public File getComponentProject() {
        return componentProject;
    }

    /**
     * Checks if the component has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}