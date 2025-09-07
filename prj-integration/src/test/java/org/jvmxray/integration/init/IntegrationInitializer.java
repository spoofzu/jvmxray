package org.jvmxray.integration.init;

import org.jvmxray.platform.shared.init.ComponentInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.property.ComponentProperties;
import org.jvmxray.platform.shared.util.GUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Integration test-specific implementation of ComponentInitializer for managing JVMXRay 
 * integration test initialization, configuration, and resource management.
 * <p>
 * This class extends ComponentInitializer to provide integration test-specific resource mappings
 * and configurations. It's designed to be used only during integration testing to set up
 * the integration component directory structure and logging configuration.
 * </p>
 * <p>
 * <b>Important:</b> This class is for testing purposes only and should not be used in production.
 * </p>
 * 
 * @since 0.0.1
 * @author JVMXRay Integration Team
 */
public class IntegrationInitializer extends ComponentInitializer {

    /**
     * Singleton instance.
     */
    private static volatile IntegrationInitializer instance = null;

    /**
     * Flag to track initialization status.
     */
    private static volatile boolean isInitialized = false;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private IntegrationInitializer() {
        // Prevent external instantiation
    }

    /**
     * Returns the singleton instance of IntegrationInitializer.
     * 
     * @return The singleton instance
     * @throws Exception if initialization fails
     */
    public static synchronized IntegrationInitializer getInstance() throws Exception {
        if (instance == null) {
            instance = new IntegrationInitializer();
        }
        if (!isInitialized) {
            instance.initialize();
            isInitialized = true;
        }
        return instance;
    }

    @Override
    protected String getComponentName() {
        return "integration";
    }

    @Override
    protected Map<String, String> getResourcesToCopy() {
        Map<String, String> resources = new HashMap<>();
        
        // Integration test logback template
        resources.put("integration-logback-test.xml2", "logback.xml");
        
        return resources;
    }

    @Override
    protected Properties getDefaultProperties() {
        Properties defaults = new Properties();
        
        // Core integration test properties
        defaults.setProperty("AID", GUID.getID());
        defaults.setProperty("CID", "integration-test");
        defaults.setProperty("log.message.encoding", "true");

        // Integration test configuration
        defaults.setProperty("integration.test.duration", "10");
        defaults.setProperty("integration.test.intensity", "medium");

        // LogProxy buffer configuration for integration tests
        defaults.setProperty("org.jvmxray.integration.logproxy.buffer.size", "5000");
        defaults.setProperty("org.jvmxray.integration.logproxy.buffer.flush.interval", "5");
        defaults.setProperty("org.jvmxray.integration.logproxy.buffer.overflow.strategy", "wait");

        return defaults;
    }

    @Override
    protected PropertyBase createProperties(java.nio.file.Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Checks if the integration initializer has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isIntegrationInitialized() {
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