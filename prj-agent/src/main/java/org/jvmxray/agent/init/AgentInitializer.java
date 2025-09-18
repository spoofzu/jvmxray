package org.jvmxray.agent.init;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.util.GUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Agent-specific initialization class for managing JVMXRay agent
 * initialization, configuration, and resource management.
 * <p>
 * This class is self-contained and does not depend on external initialization frameworks
 * to ensure the agent JAR can run independently. It handles directory setup, resource copying,
 * property management, and logging configuration specifically for the agent component.
 * </p>
 * <p>
 * The initialization follows the JVMXRay home directory standards:
 * - Test mode (jvmxray.test.home): Uses {jvmxray.test.home}/agent/
 * - Production mode (jvmxray.home): Uses {jvmxray.home}/jvmxray/agent/
 * - Auto-detect: Uses project/.jvmxray/agent/
 * </p>
 * <p>
 * <b>Important:</b> This class does not perform logging to avoid circular dependencies
 * in the agent bootloader context. Critical errors are reported via System.err only.
 * </p>
 * 
 * @since 0.0.1
 * @author JVMXRay Agent Team
 */
public class AgentInitializer {

    /**
     * Singleton instance.
     */
    private static volatile AgentInitializer instance = null;

    /**
     * Flag to track initialization status.
     */
    private static volatile boolean isInitialized = false;

    /**
     * JVMXRay home directory (parent directory for all components).
     */
    private Path jvmxrayHome = null;

    /**
     * Agent-specific project directory.
     */
    private File agentProject = null;

    /**
     * Agent-specific properties instance.
     */
    private AgentProperties agentProperties = null;

    /**
     * Isolated LoggerContext for agent-specific logging.
     */
    private LoggerContext loggerContext = null;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private AgentInitializer() {
        // Prevent external instantiation
    }

    /**
     * Returns the singleton instance of AgentInitializer.
     * 
     * @return The singleton instance
     * @throws Exception if initialization fails
     */
    public static synchronized AgentInitializer getInstance() throws Exception {
        if (instance == null) {
            instance = new AgentInitializer();
        }
        if (!isInitialized) {
            instance.initialize();
            isInitialized = true;
        }
        return instance;
    }

    /**
     * Initializes the agent by setting up directories, copying resources,
     * initializing properties, and configuring logging.
     * 
     * @throws Exception if initialization fails
     */
    private void initialize() throws Exception {
        try {
            setupDirectories();
            copyResources();
            initializeProperties();
            initializeLogging();
        } catch (Exception e) {
            System.err.println("AgentInitializer: Failed to initialize agent: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets up the JVMXRay home directory structure following project standards.
     * 
     * @throws IOException if directory creation fails
     */
    private void setupDirectories() throws IOException {
        // Determine if we're in a test scenario or production scenario
        boolean isMavenTest = isMavenTestMode();
        
        // Resolve home directory using simplified precedence logic
        String specifiedHomeParameter = specifiedJvmxrayHome();

        // Validate home path
        if (specifiedHomeParameter.isEmpty()) {
            throw new IOException("Unable to determine a working directory. Set -Djvmxray.home=/path/to/home or -Djvmxray.test.home=/path/to/test/home");
        }

        // Determine finalized project directory path.
        //   For testing we use, {jvmxray.test.home}/
        //   For production we use, {jvmxray.home}/jvmxray/
        Path adjustedJvmrayHome = null;
        if (!isMavenTest) {
            // Production override: add /jvmxray subdirectory under jvmxray.home
            adjustedJvmrayHome = Paths.get(specifiedHomeParameter, "jvmxray");
            ensureDirectory(adjustedJvmrayHome);
        } else {
            // Test scenario or auto-detected: use home directly
            adjustedJvmrayHome = Paths.get(specifiedHomeParameter);
        }

        // Set the jvmxrayHome and agentProject fields
        jvmxrayHome = adjustedJvmrayHome;
        
        // Create agent project structure
        Path agentHomePath = jvmxrayHome.resolve("agent");
        Path agentConfigPath = agentHomePath.resolve("config");
        Path agentLogPath = agentHomePath.resolve("logs");
        ensureDirectory(agentHomePath);
        ensureDirectory(agentConfigPath);
        ensureDirectory(agentLogPath);

        agentProject = agentHomePath.toFile();

        // Set agent-specific system properties
        if (System.getProperty("jvmxray.agent.logs") == null) {
            System.setProperty("jvmxray.agent.logs", agentLogPath.toString());
        }
        if (System.getProperty("jvmxray.agent.config") == null) {
            System.setProperty("jvmxray.agent.config", agentConfigPath.toString());
        }
    }

    /**
     * Copies required resources from classpath to agent directories.
     * Only copies resources that don't already exist to preserve user customizations.
     * 
     * @throws IOException if resource copying fails
     */
    private void copyResources() throws IOException {
        Path configPath = jvmxrayHome.resolve("agent").resolve("config");
        File targetFile = configPath.resolve("logback.xml").toFile();

        // Determine which logback config to copy based on test/production mode
        String resourceName = isMavenTestMode() ? "agent-logback-shaded.xml2" : "agent-logback-shaded.xml2";

        // In test mode, always refresh to avoid stale config from previous runs
        if (isMavenTestMode() && targetFile.exists()) {
            targetFile.delete();
        }

        // Try to copy the preferred resource, fallback to production if test resource not available
        try {
            copyResourceToTarget(resourceName, targetFile);
        } catch (IOException e) {
            if (isMavenTestMode() && resourceName.equals("agent-logback-test.xml2")) {
                copyResourceToTarget("agent-logback-production.xml2", targetFile);
            } else {
                throw e;
            }
        }
    }

    /**
     * Initializes agent properties, creating default configuration if needed.
     * 
     * @throws Exception if property initialization fails
     */
    private void initializeProperties() throws Exception {
        Path agentHomePath = jvmxrayHome.resolve("agent");
        agentProperties = new AgentProperties(agentHomePath);
        agentProperties.init();

        // Set default properties if file is new or empty
        if (agentProperties.isPropertyFileNewOrEmpty()) {
            setDefaultProperties();
            agentProperties.saveProperties("JVMXRay Agent Properties");
        }
    }

    /**
     * Sets default properties for the agent.
     */
    private void setDefaultProperties() {
        agentProperties.setProperty("AID", GUID.generate());
        agentProperties.setProperty("CID", "unit-test");
        agentProperties.setProperty("log.message.encoding", "true");
        agentProperties.setProperty("jvmxray.sensor.http", "org.jvmxray.agent.sensor.http.HttpSensor");
        agentProperties.setProperty("jvmxray.sensor.fileio", "org.jvmxray.agent.sensor.io.FileIOSensor");
        agentProperties.setProperty("jvmxray.agent.sensor.fileio.captures", "CUD");
        agentProperties.setProperty("jvmxray.sensor.monitor", "org.jvmxray.agent.sensor.monitor.MonitorSensor");
        agentProperties.setProperty("jvmxray.sensor.appinit", "org.jvmxray.agent.sensor.system.AppInitSensor");
        agentProperties.setProperty("jvmxray.sensor.lib", "org.jvmxray.agent.sensor.system.LibSensor");
        agentProperties.setProperty("jvmxray.sensor.sql", "org.jvmxray.agent.sensor.sql.SQLSensor");
        agentProperties.setProperty("jvmxray.sensor.socket", "org.jvmxray.agent.sensor.net.SocketSensor");
        agentProperties.setProperty("jvmxray.sensor.uncaughtexception", "org.jvmxray.agent.sensor.uncaughtexception.UncaughtExceptionSensor");
        agentProperties.setProperty("jvmxray.sensor.crypto", "org.jvmxray.agent.sensor.crypto.CryptoSensor");
        agentProperties.setProperty("jvmxray.sensor.process", "org.jvmxray.agent.sensor.system.ProcessSensor");
        agentProperties.setProperty("jvmxray.sensor.serialization", "org.jvmxray.agent.sensor.serialization.SerializationSensor");
        agentProperties.setProperty("#jvmxray.sensor.reflection", "org.jvmxray.agent.sensor.reflection.ReflectionSensor");
        agentProperties.setProperty("jvmxray.sensor.configuration", "org.jvmxray.agent.sensor.configuration.ConfigurationSensor");
        agentProperties.setProperty("jvmxray.sensor.memory", "org.jvmxray.agent.sensor.memory.MemorySensor");
        agentProperties.setProperty("jvmxray.sensor.auth", "org.jvmxray.agent.sensor.auth.AuthenticationSensor");
        agentProperties.setProperty("jvmxray.sensor.script", "org.jvmxray.agent.sensor.script.ScriptEngineSensor");
        agentProperties.setProperty("jvmxray.sensor.data", "org.jvmxray.agent.sensor.data.DataTransferSensor");
        agentProperties.setProperty("jvmxray.sensor.api", "org.jvmxray.agent.sensor.api.APICallSensor");
        agentProperties.setProperty("jvmxray.sensor.thread", "org.jvmxray.agent.sensor.thread.ThreadSensor");
        agentProperties.setIntProperty("monitor.interval", 60000);
        // LogProxy buffer configuration
        agentProperties.setIntProperty("org.jvmxray.agent.logproxy.buffer.size", 10000);
        agentProperties.setIntProperty("org.jvmxray.agent.logproxy.buffer.flush.interval", 10);
        agentProperties.setProperty("org.jvmxray.agent.logproxy.buffer.overflow.strategy", "wait");
    }

    /**
     * Initializes logging configuration for the agent.
     * 
     * @throws Exception if logging initialization fails
     */
    private void initializeLogging() throws Exception {
        try {
            // Create an isolated LoggerContext specifically for the agent (uses shaded logback in the agent JAR)
            loggerContext = new LoggerContext();
            loggerContext.setName("JVMXRayAgent-Isolated");

            // Initialize the MDC adapter for the isolated context
            loggerContext.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());

            // Determine paths and ensure properties are set for config
            String agentLogsPath = System.getProperty("jvmxray.agent.logs");
            if (agentLogsPath == null) {
                agentLogsPath = new File(agentProject, "logs").getAbsolutePath();
            }
            String agentConfigPath = System.getProperty("jvmxray.agent.config");
            if (agentConfigPath == null) {
                agentConfigPath = new File(agentProject, "config").getAbsolutePath();
            }

            // Check for system property override first
            String logbackConfigPath = System.getProperty("logback.agent.configurationFile");
            File logbackConfig;
            if (logbackConfigPath != null) {
                logbackConfig = new File(logbackConfigPath);
            } else {
                logbackConfig = new File(agentConfigPath, "logback.xml");
            }
            
            if (logbackConfig.exists()) {
                loggerContext.reset();
                loggerContext.putProperty("jvmxray.agent.logs", agentLogsPath);
                loggerContext.putProperty("jvmxray.agent.config", agentConfigPath);

                // Load the shaded logback configuration
                String xml = new String(java.nio.file.Files.readAllBytes(logbackConfig.toPath()), java.nio.charset.StandardCharsets.UTF_8);

                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    configurator.doConfigure(in);
                }
                loggerContext.start();
            }

        } catch (NoClassDefFoundError e) {
            // Logback not available in current context - acceptable fallback
            loggerContext = null;
        } catch (Exception e) {
            loggerContext = null;
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
    private void copyResourceToTarget(String resource, File target) throws IOException {
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
    private void ensureDirectory(Path path) {
        path.toFile().mkdirs();
    }

    /**
     * Checks if running in Maven test mode.
     */
    private boolean isMavenTestMode() {
        return System.getProperty("jvmxray.test.home") != null;
    }

    /**
     * Determines the specified JVMXRay home directory.
     */
    private String specifiedJvmxrayHome() {
        String testHome = System.getProperty("jvmxray.test.home");
        String prodHome = System.getProperty("jvmxray.home");

        if (testHome != null && prodHome != null) {
            throw new IllegalStateException("Both jvmxray.test.home and jvmxray.home are set. Only one should be specified.");
        }

        if (testHome != null) {
            return testHome;
        } else if (prodHome != null) {
            return prodHome;
        } else {
            // Auto-detect: prefer Maven project.basedir when available, otherwise user.dir
            String baseDir = System.getProperty("project.basedir");
            if (baseDir == null || baseDir.trim().isEmpty()) {
                baseDir = System.getProperty("user.dir");
            }
            return baseDir + File.separator + ".jvmxray";
        }
    }

    // ================================================================================================
    // PUBLIC API METHODS
    // ================================================================================================

    /**
     * Returns the agent properties instance.
     * 
     * @return The agent properties
     * @throws Exception if initialization has not been performed
     */
    public AgentProperties getProperties() throws Exception {
        if (agentProperties == null || jvmxrayHome == null || agentProject == null) {
            throw new IllegalStateException("AgentInitializer not initialized - call initialize() first");
        }
        return agentProperties;
    }

    /**
     * Returns the agent's LoggerContext for logging configuration.
     * 
     * @return The LoggerContext, or null if logging is not available
     */
    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    /**
     * Returns a logger for the specified name.
     * 
     * @param name Logger name
     * @return The logger instance
     */
    public Logger getLogger(String name) {
        if (loggerContext != null) {
            return loggerContext.getLogger(name);
        }
        return null;
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
     * Returns the agent's project directory.
     * 
     * @return The agent project directory
     */
    public File getAgentProject() {
        return agentProject;
    }

    /**
     * Checks if the agent has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isAgentInitialized() {
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
