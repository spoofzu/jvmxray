package org.jvmxray.service.ai.processor;

import org.jvmxray.platform.shared.property.PropertyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for stage processors providing common functionality.
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public abstract class AbstractStageProcessor implements StageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractStageProcessor.class);

    protected PropertyBase properties;
    protected boolean enabled = true;

    /**
     * Constructs a new AbstractStageProcessor.
     *
     * @param properties Component properties for configuration
     */
    public AbstractStageProcessor(PropertyBase properties) {
        this.properties = properties;
    }

    @Override
    public void initialize(PropertyBase properties) throws Exception {
        this.properties = properties;

        // Read enabled flag from properties
        String enabledProperty = getEnabledPropertyKey();
        if (enabledProperty != null) {
            String enabledValue = properties.getProperty(enabledProperty, "true");
            this.enabled = Boolean.parseBoolean(enabledValue);

            if (!enabled) {
                logger.info("{} is disabled in configuration", getProcessorName());
            }
        }

        // Allow subclasses to perform additional initialization
        onInitialize();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the property key for the enabled flag.
     * Subclasses should override to specify their enabled property key.
     *
     * @return Property key (e.g., "aiservice.stage0.enabled"), or null if no property
     */
    protected abstract String getEnabledPropertyKey();

    /**
     * Called during initialization after base initialization is complete.
     * Subclasses can override to perform additional initialization.
     *
     * @throws Exception if initialization fails
     */
    protected void onInitialize() throws Exception {
        // Default: no additional initialization
    }

    /**
     * Gets an integer property value with a default.
     *
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value as integer
     */
    protected int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer property {}: {}, using default: {}",
                       key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    /**
     * Gets a boolean property value with a default.
     *
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value as boolean
     */
    protected boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Gets a string property value with a default.
     *
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value as string
     */
    protected String getStringProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }
}
