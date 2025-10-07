package org.jvmxray.service.ai.init;

import org.jvmxray.platform.shared.init.ComponentInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.property.ComponentProperties;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * AiService-specific implementation of ComponentInitializer for managing JVMXRay
 * AI Service initialization, configuration, and resource management.
 * <p>
 * This class extends ComponentInitializer to provide AI Service-specific resource mappings
 * and configurations. It's designed to set up the AI Service component directory structure
 * and logging configuration for intelligent event enrichment processing.
 * </p>
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class AiServiceInitializer extends ComponentInitializer {

    /**
     * Singleton instance.
     */
    private static volatile AiServiceInitializer instance = null;

    /**
     * Flag to track initialization status.
     */
    private static volatile boolean isInitialized = false;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private AiServiceInitializer() {
        // Prevent external instantiation
    }

    /**
     * Returns the singleton instance of AiServiceInitializer.
     *
     * @return The singleton instance
     * @throws Exception if initialization fails
     */
    public static synchronized AiServiceInitializer getInstance() throws Exception {
        if (instance == null) {
            instance = new AiServiceInitializer();
        }
        if (!isInitialized) {
            instance.initialize();
            isInitialized = true;
        }
        return instance;
    }

    @Override
    protected String getComponentName() {
        return "aiservice";
    }

    protected Map<String, String> getResourcesToCopy() {
        Map<String, String> resources = new HashMap<>();

        // AI Service logback template for event enrichment processing
        resources.put("aiservice-logback-production.xml2", "logback.xml");

        return resources;
    }

    @Override
    protected Properties getDefaultProperties() {
        Properties defaults = new Properties();

        // Core AI Service properties
        defaults.setProperty("AID", "aiservice-" + System.currentTimeMillis());
        defaults.setProperty("CID", "aiservice");
        defaults.setProperty("log.message.encoding", "true");

        // AI Service processing configuration
        defaults.setProperty("aiservice.batch.size", "1000");
        defaults.setProperty("aiservice.processing.interval.seconds", "60");

        // Library enrichment configuration
        defaults.setProperty("aiservice.library.hash.algorithm", "SHA-256");
        defaults.setProperty("aiservice.library.tracking.enabled", "true");

        // CVE database configuration
        defaults.setProperty("aiservice.cve.database.enabled", "true");
        defaults.setProperty("aiservice.cve.update.interval.hours", "24");

        // Database configuration
        defaults.setProperty("aiservice.database.connection.pool.size", "10");
        defaults.setProperty("aiservice.database.batch.size", "100");

        return defaults;
    }

    @Override
    protected PropertyBase createProperties(Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Checks if the AI Service initializer has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isAiServiceInitialized() {
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