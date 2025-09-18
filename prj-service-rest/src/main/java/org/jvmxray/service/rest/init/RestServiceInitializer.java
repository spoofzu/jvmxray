package org.jvmxray.service.rest.init;

import org.jvmxray.platform.shared.init.ComponentInitializer;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.platform.shared.property.ComponentProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializer for the REST Service component.
 * Handles setup of directories, configuration files, and logging for the REST service.
 *
 * @author Milton Smith
 */
public class RestServiceInitializer extends ComponentInitializer {

    private static final Logger logger = Logger.getLogger(RestServiceInitializer.class.getName());
    private static RestServiceInitializer instance;

    // Component name used for directory structure
    private static final String COMPONENT_NAME = "restservice";

    // Configuration file names
    private static final String LOGBACK_TEMPLATE = "/restservice-logback-production.xml2";
    private static final String LOGBACK_CONFIG = "logback.xml";
    private static final String PROPERTIES_FILE = "restservice.properties";

    // Default property values
    private static final String DEFAULT_PORT = "8080";
    private static final String DEFAULT_MAX_PAGE_SIZE = "1000";
    private static final String DEFAULT_DEFAULT_PAGE_SIZE = "100";
    private static final String DEFAULT_MAX_RESULT_SIZE = "100000";

    /**
     * Private constructor for singleton pattern.
     */
    private RestServiceInitializer() {
        super();
    }

    /**
     * Get the singleton instance of RestServiceInitializer.
     *
     * @return The singleton instance
     */
    public static synchronized RestServiceInitializer getInstance() {
        if (instance == null) {
            instance = new RestServiceInitializer();
        }
        return instance;
    }

    @Override
    protected String getComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    protected Map<String, String> getResourcesToCopy() {
        Map<String, String> resources = new HashMap<>();
        resources.put(LOGBACK_TEMPLATE, LOGBACK_CONFIG);
        return resources;
    }

    @Override
    protected Properties getDefaultProperties() {
        Properties props = new Properties();

        // Set default values
        props.setProperty("rest.service.port", DEFAULT_PORT);
        props.setProperty("rest.service.max.page.size", DEFAULT_MAX_PAGE_SIZE);
        props.setProperty("rest.service.default.page.size", DEFAULT_DEFAULT_PAGE_SIZE);
        props.setProperty("rest.service.max.result.size", DEFAULT_MAX_RESULT_SIZE);

        // Add database connection URL
        // Use the same database as the common module
        String jvmxrayHome = getJvmxrayHome() != null ? getJvmxrayHome().toString() : ".jvmxray";
        String dbUrl = "jdbc:sqlite:" + jvmxrayHome + "/common/data/jvmxray-test.db";
        props.setProperty("rest.service.database.url", dbUrl);

        return props;
    }

    @Override
    protected PropertyBase createProperties(Path componentHome) {
        return new ComponentProperties(componentHome);
    }

    /**
     * Get the REST service port from configuration.
     *
     * @return The configured port number
     */
    public int getPort() {
        String port = System.getProperty("rest.service.port", DEFAULT_PORT);
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            logger.warning("Invalid port number: " + port + ", using default: " + DEFAULT_PORT);
            return Integer.parseInt(DEFAULT_PORT);
        }
    }

    /**
     * Get the database connection URL.
     *
     * @return The database connection URL
     */
    public String getDatabaseUrl() {
        return System.getProperty("rest.service.database.url");
    }
}