package org.jvmxray.platform.shared.init;

import org.jvmxray.platform.shared.property.ComponentProperties;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.util.GUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Common module-specific implementation of ComponentInitializer for managing JVMXRay common
 * initialization, configuration, and resource management.
 * <p>
 * This class extends ComponentInitializer to provide common module-specific resource mappings
 * and default properties. It handles the copying of common configuration templates
 * and sets up shared infrastructure configurations.
 * </p>
 * <p>
 * <b>Important:</b> This class does not perform logging to avoid circular dependencies
 * in the common module context. Critical errors are reported via System.err only.
 * </p>
 * 
 * @since 0.0.1
 * @author JVMXRay Common Team
 */
public class CommonInitializer extends ComponentInitializer {

    /**
     * Singleton instance.
     */
    private static volatile CommonInitializer instance = null;

    /**
     * Flag to track initialization status.
     */
    private static volatile boolean isInitialized = false;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private CommonInitializer() {
        // Prevent external instantiation
    }

    /**
     * Returns the singleton instance of CommonInitializer.
     * 
     * @return The singleton instance
     * @throws Exception if initialization fails
     */
    public static synchronized CommonInitializer getInstance() throws Exception {
        if (instance == null) {
            instance = new CommonInitializer();
        }
        if (!isInitialized) {
            instance.initialize();
            isInitialized = true;
        }
        return instance;
    }

    @Override
    protected String getComponentName() {
        return "common";
    }

    @Override
    protected Map<String, String> getResourcesToCopy() {
        Map<String, String> resources = new HashMap<>();
        
        // Select logback template based on test/production mode
        if (System.getProperty("jvmxray.test.home") != null) {
            resources.put("common-logback-test.xml2", "logback.xml");
        } else {
            resources.put("common-logback-production.xml2", "logback.xml");
        }
        
        return resources;
    }

    @Override
    protected Properties getDefaultProperties() {
        Properties defaults = new Properties();
        
        // Core common properties
        defaults.setProperty("AID", GUID.getID());
        defaults.setProperty("CID", "common");
        defaults.setProperty("log.message.encoding", "true");

        // Common module configuration
        defaults.setProperty("jvmxray.common.database.type", "sqlite");
        defaults.setProperty("jvmxray.common.database.pool.size", "5");
        defaults.setProperty("jvmxray.common.log.retention.days", "30");

        // Schema management configuration
        defaults.setProperty("jvmxray.common.schema.auto.create", "true");
        defaults.setProperty("jvmxray.common.schema.validation.enabled", "true");

        return defaults;
    }

    @Override
    protected PropertyBase createProperties(java.nio.file.Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Checks if the common module has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isCommonInitialized() {
        return isInitialized;
    }

    /**
     * Resets the singleton instance for testing purposes.
     * <b>Warning:</b> This method should only be used in test contexts.
     */
    public static synchronized void resetForTesting() {
        instance = null;
        isInitialized = false;
    }
}