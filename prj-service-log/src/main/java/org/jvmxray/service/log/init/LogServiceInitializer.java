package org.jvmxray.service.log.init;

import org.jvmxray.platform.shared.init.ComponentInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.property.ComponentProperties;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * LogService-specific implementation of ComponentInitializer for managing JVMXRay 
 * LogService initialization, configuration, and resource management.
 * <p>
 * This class extends ComponentInitializer to provide LogService-specific resource mappings
 * and configurations. It's designed to set up the LogService component directory structure
 * and logging configuration for centralized agent event processing.
 * </p>
 * 
 * @since 0.0.1
 * @author JVMXRay LogService Team
 */
public class LogServiceInitializer extends ComponentInitializer {

    /**
     * Singleton instance.
     */
    private static volatile LogServiceInitializer instance = null;

    /**
     * Flag to track initialization status.
     */
    private static volatile boolean isInitialized = false;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private LogServiceInitializer() {
        // Prevent external instantiation
    }

    /**
     * Returns the singleton instance of LogServiceInitializer.
     * 
     * @return The singleton instance
     * @throws Exception if initialization fails
     */
    public static synchronized LogServiceInitializer getInstance() throws Exception {
        if (instance == null) {
            instance = new LogServiceInitializer();
        }
        if (!isInitialized) {
            instance.initialize();
            isInitialized = true;
        }
        return instance;
    }

    @Override
    protected String getComponentName() {
        return "logservice";
    }

    protected Map<String, String> getResourcesToCopy() {
        Map<String, String> resources = new HashMap<>();
        
        // LogService logback template for agent event processing
        resources.put("logservice-logback-production.xml2", "logback.xml");
        
        return resources;
    }

    @Override
    protected Properties getDefaultProperties() {
        Properties defaults = new Properties();
        
        // Core LogService properties
        defaults.setProperty("AID", "logservice-" + System.currentTimeMillis());
        defaults.setProperty("CID", "logservice");
        defaults.setProperty("log.message.encoding", "true");

        // LogService configuration
        defaults.setProperty("logservice.agent.port", "9876");
        defaults.setProperty("logservice.agent.host", "localhost");

        // Buffer configuration for high-volume agent events
        defaults.setProperty("org.jvmxray.logservice.buffer.size", "10000");
        defaults.setProperty("org.jvmxray.logservice.buffer.flush.interval", "1");
        defaults.setProperty("org.jvmxray.logservice.buffer.overflow.strategy", "wait");

        return defaults;
    }

    @Override
    protected PropertyBase createProperties(Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Checks if the LogService initializer has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isLogServiceInitialized() {
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