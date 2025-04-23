package org.jvmxray.platform.shared.log;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.jvmxray.platform.shared.property.PropertyFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Singleton factory for creating and managing an isolated SLF4J {@link Logger}
 * context in the JVMXRay framework. Initializes a Logback {@link LoggerContext}
 * with a configuration file and provides loggers tied to this context for logging
 * system metrics and events.
 *
 * <p>The factory must be initialized with a JVMXRay home directory containing
 * the Logback configuration file (<code>logback.xml</code>). It verifies that the
 * directory and file are readable and writable. If the configuration file is missing,
 * it copies a default template from the classpath. If no template is available,
 * it falls back to a basic in-memory configuration.</p>
 *
 * @author Milton Smith
 */
public class JVMXRayLogFactory {

    // Isolated Logback context for JVMXRay logging
    private LoggerContext context = null;
    // Singleton instance of the factory
    private static final JVMXRayLogFactory instance = new JVMXRayLogFactory();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private JVMXRayLogFactory() {
        // Empty constructor for singleton initialization
    }

    /**
     * Retrieves the singleton instance of the {@code JVMXRayLogFactory}.
     *
     * @return The singleton {@code JVMXRayLogFactory} instance.
     */
    public static final JVMXRayLogFactory getInstance() {
        // Return the singleton instance
        return instance;
    }

    /**
     * Initializes the Logback {@link LoggerContext} with a configuration file from
     * the specified JVMXRay home directory. Verifies that the directory and configuration
     * file are readable and writable. Attempts to load <code>logback-test.xml2</code>
     * from the classpath for unit testing. If unavailable, uses <code>logback.xml</code>
     * from the home directory, copying <code>logback.xml2</code> from the classpath
     * if it does not exist. Falls back to a basic in-memory configuration if no
     * configuration file is available.
     *
     * @param jvmxrayHome The JVMXRay home directory containing the Logback configuration.
     * @throws IllegalArgumentException If <code>jvmxrayHome</code> is null.
     * @throws IOException If directory or file permissions are insufficient, configuration
     *                     parsing fails, or I/O errors occur during file operations.
     */
    public final void init(File jvmxrayHome) throws IOException {
        // Validate input directory
        if (jvmxrayHome == null) {
            throw new IllegalArgumentException("JVMXRay home directory cannot be null");
        }

        // Verify directory permissions
        if (!jvmxrayHome.canRead() || !jvmxrayHome.canWrite()) {
            throw new IOException("JVMXRay home directory must be readable and writable: " + jvmxrayHome.getAbsolutePath());
        }

        // Initialize an isolated LoggerContext
        context = new LoggerContext();
        // Set a unique name for the context
        context.setName("JvmXrayAgentContext");

        // Set MDC adapter to avoid null pointer exceptions
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());

        // Define the test and production Logback configuration file names
        String testLogConfig = "logback-test.xml2";
        // Define the production Logback configuration template
        String prodLogConfigTemplate = "logback.xml2";
        // Define the target Logback configuration file in the JVMXRay home directory
        File logbackTarget = new File(jvmxrayHome, "logback.xml");

        // Load test configuration from classpath
        InputStream in = PropertyFactory.class.getClassLoader().getResourceAsStream(testLogConfig);

        // Check if test configuration is unavailable
        if (in == null) {
            // Check if production configuration exists
            if (logbackTarget.exists()) {
                // Verify file is readable
                if (!logbackTarget.canRead()) {
                    throw new IOException("Logback configuration file is not readable: " + logbackTarget.getAbsolutePath());
                }
            } else {
                // Verify directory is writable for copying template
                if (!jvmxrayHome.canWrite()) {
                    throw new IOException("Cannot write to JVMXRay home directory for logback configuration: " + jvmxrayHome.getAbsolutePath());
                }
                // Load production template from classpath
                try (InputStream prodIn = PropertyFactory.class.getClassLoader().getResourceAsStream(prodLogConfigTemplate)) {
                    if (prodIn == null) {
                        // Fall back to basic in-memory configuration
                        JoranConfigurator configurator = new JoranConfigurator();
                        configurator.setContext(context);
                        try {
                            new BasicConfigurator().configure(context);
                        } catch (Exception e) {
                            throw new IOException("Failed to configure basic in-memory logging", e);
                        }
                        return;
                    }
                    // Copy template to target
                    Files.copy(prodIn, logbackTarget.toPath());
                }
            }
            // Use the target file as the configuration source
            if (!logbackTarget.canRead()) {
                throw new IOException("Logback configuration file is not readable after creation: " + logbackTarget.getAbsolutePath());
            }
            in = new FileInputStream(logbackTarget);
        }

        // Configure Logback context with the configuration file
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        // Configure using the input stream with try-with-resources
        try (InputStream configIn = in) {
            if (configIn != null) {
                try {
                    configurator.doConfigure(configIn);
                } catch (ch.qos.logback.core.joran.spi.JoranException e) {
                    throw new IOException("Failed to parse Logback configuration from input stream", e);
                }
            } else {
                // Configure using the target file as fallback
                if (!logbackTarget.canRead()) {
                    throw new IOException("Logback configuration file is not readable: " + logbackTarget.getAbsolutePath());
                }
                try {
                    configurator.doConfigure(logbackTarget.getAbsoluteFile());
                } catch (ch.qos.logback.core.joran.spi.JoranException e) {
                    throw new IOException("Failed to parse Logback configuration from file: " + logbackTarget.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * Retrieves a logger from the isolated {@link LoggerContext} for the specified namespace.
     *
     * @param namespace The logger namespace (e.g., class or package name).
     * @return A {@link Logger} instance tied to the isolated context.
     * @throws IllegalStateException If the context is not initialized.
     */
    public Logger getLogger(String namespace) {
        // Check if the context is initialized
        if (context == null) {
            throw new IllegalStateException("JVMXRayLogFactory must be initialized before getting loggers.");
        }
        // Return a logger for the specified namespace
        return context.getLogger(namespace);
    }
}