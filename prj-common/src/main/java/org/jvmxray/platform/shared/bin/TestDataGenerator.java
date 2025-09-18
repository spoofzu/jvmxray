package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.init.CommonInitializer;
import org.jvmxray.platform.shared.test.TurtleTestExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test Data Generator - Generates realistic JVMXRay sensor events for testing and development.
 * 
 * This tool uses the TurtleIntegrationTest with the JVMXRay agent to generate authentic
 * sensor events and store them in the SQLite test database. It provides a command-line
 * interface for configuring test data volume, intensity, and database location.
 * 
 * <p>Key functionalities:</p>
 * <ul>
 *   <li>On-demand generation of realistic agent sensor events</li>
 *   <li>Configurable test duration and intensity levels</li>
 *   <li>Custom database location and logback configuration</li>
 *   <li>Integration with existing component initialization system</li>
 * </ul>
 * 
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code -d, --duration <seconds>}: Test execution duration (default: 30 seconds)</li>
 *   <li>{@code -i, --intensity <level>}: Test intensity level - low/medium/high (default: medium)</li>
 *   <li>{@code -db, --database <path>}: Custom database file path</li>
 *   <li>{@code -lc, --logback-config <path>}: Custom logback configuration file</li>
 *   <li>{@code -v, --verbose}: Enable verbose output</li>
 *   <li>{@code -h, --help}: Display help information</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * # Basic usage - generate 30 seconds of medium intensity test data
 * java org.jvmxray.platform.shared.bin.TestDataGenerator
 * 
 * # Extended generation for development
 * java org.jvmxray.platform.shared.bin.TestDataGenerator -d 120 -i high -v
 * 
 * # Custom database and logback configuration
 * java org.jvmxray.platform.shared.bin.TestDataGenerator -db /path/to/test.db -lc /path/to/logback-db.xml
 * }</pre>
 * 
 * @author JVMXRay Development Team
 */
public class TestDataGenerator {

    // Command-line option constants
    private static final String OPT_DURATION_SHORT = "d";
    private static final String OPT_DURATION_LONG = "duration";
    private static final int DEFAULT_DURATION = 30;

    private static final String OPT_INTENSITY_SHORT = "i";
    private static final String OPT_INTENSITY_LONG = "intensity";
    private static final String DEFAULT_INTENSITY = "medium";

    private static final String OPT_DATABASE_SHORT = "db";
    private static final String OPT_DATABASE_LONG = "database";

    private static final String OPT_LOGBACK_CONFIG_SHORT = "lc";
    private static final String OPT_LOGBACK_CONFIG_LONG = "logback-config";

    private static final String OPT_VERBOSE_SHORT = "v";
    private static final String OPT_VERBOSE_LONG = "verbose";

    private static final String OPT_HELP_SHORT = "h";
    private static final String OPT_HELP_LONG = "help";

    private static final String OPT_NO_PROGRESS_SHORT = "np";
    private static final String OPT_NO_PROGRESS_LONG = "no-progress";

    // Configuration fields
    private int duration = DEFAULT_DURATION;
    private String intensity = DEFAULT_INTENSITY;
    private String databasePath = null;
    private String logbackConfigPath = null;
    private boolean verbose = false;
    private boolean enableProgress = true;

    /**
     * Main entry point for the Test Data Generator application.
     *
     * @param args Command-line arguments specifying generation configuration
     */
    public static void main(String[] args) {
        new TestDataGenerator().run(args);
    }

    /**
     * Runs the test data generation process.
     *
     * @param args Command-line arguments
     */
    private void run(String[] args) {
        try {
            // Parse command-line arguments
            parseCommandLineOptions(args);
            
            // Initialize environment
            initializeEnvironment();
            
            // Validate configuration
            validateConfiguration();
            
            // Execute test data generation
            generateTestData();
            
            System.out.println("✓ Test data generation completed successfully");
            
        } catch (Exception e) {
            System.err.println("ERROR: Test data generation failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to configure generation parameters.
     *
     * @param args Command-line arguments
     * @throws ParseException If arguments are invalid or cannot be parsed
     */
    private void parseCommandLineOptions(String[] args) throws ParseException {
        Options options = createCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            throw e;
        }

        // Display help if requested
        if (cmd.hasOption(OPT_HELP_SHORT)) {
            printUsage(options);
            System.exit(0);
        }

        // Parse duration
        if (cmd.hasOption(OPT_DURATION_SHORT)) {
            try {
                duration = Integer.parseInt(cmd.getOptionValue(OPT_DURATION_SHORT));
                if (duration <= 0) {
                    System.err.println("Duration must be positive. Using default: " + DEFAULT_DURATION);
                    duration = DEFAULT_DURATION;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid duration format. Using default: " + DEFAULT_DURATION);
                duration = DEFAULT_DURATION;
            }
        }

        // Parse intensity
        if (cmd.hasOption(OPT_INTENSITY_SHORT)) {
            String intensityValue = cmd.getOptionValue(OPT_INTENSITY_SHORT);
            if (isValidIntensity(intensityValue)) {
                intensity = intensityValue.toLowerCase();
            } else {
                System.err.println("Invalid intensity level. Valid values: low, medium, high. Using default: " + DEFAULT_INTENSITY);
                intensity = DEFAULT_INTENSITY;
            }
        }

        // Parse database path
        if (cmd.hasOption(OPT_DATABASE_SHORT)) {
            databasePath = cmd.getOptionValue(OPT_DATABASE_SHORT);
        }

        // Parse logback configuration path
        if (cmd.hasOption(OPT_LOGBACK_CONFIG_SHORT)) {
            logbackConfigPath = cmd.getOptionValue(OPT_LOGBACK_CONFIG_SHORT);
        }

        // Parse verbose flag
        verbose = cmd.hasOption(OPT_VERBOSE_SHORT);

        // Parse no-progress flag
        enableProgress = !cmd.hasOption(OPT_NO_PROGRESS_SHORT);

        // Log parsed configuration
        if (verbose) {
            System.out.println("Configuration:");
            System.out.println("  Duration: " + duration + " seconds");
            System.out.println("  Intensity: " + intensity);
            System.out.println("  Database: " + (databasePath != null ? databasePath : "default"));
            System.out.println("  Logback Config: " + (logbackConfigPath != null ? logbackConfigPath : "default"));
            System.out.println("  Verbose: " + verbose);
            System.out.println("  Progress: " + enableProgress);
        }
    }

    /**
     * Creates command-line options definition.
     *
     * @return Options object with all defined command-line options
     */
    private Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(Option.builder(OPT_DURATION_SHORT)
                .longOpt(OPT_DURATION_LONG)
                .desc("Test execution duration in seconds (default: " + DEFAULT_DURATION + ")")
                .hasArg()
                .argName("SECONDS")
                .build());

        options.addOption(Option.builder(OPT_INTENSITY_SHORT)
                .longOpt(OPT_INTENSITY_LONG)
                .desc("Test intensity level: low/medium/high (default: " + DEFAULT_INTENSITY + ")")
                .hasArg()
                .argName("LEVEL")
                .build());

        options.addOption(Option.builder(OPT_DATABASE_SHORT)
                .longOpt(OPT_DATABASE_LONG)
                .desc("Custom database file path")
                .hasArg()
                .argName("PATH")
                .build());

        options.addOption(Option.builder(OPT_LOGBACK_CONFIG_SHORT)
                .longOpt(OPT_LOGBACK_CONFIG_LONG)
                .desc("Custom logback configuration file path")
                .hasArg()
                .argName("PATH")
                .build());

        options.addOption(Option.builder(OPT_VERBOSE_SHORT)
                .longOpt(OPT_VERBOSE_LONG)
                .desc("Enable verbose output")
                .build());

        options.addOption(Option.builder(OPT_NO_PROGRESS_SHORT)
                .longOpt(OPT_NO_PROGRESS_LONG)
                .desc("Disable progress reporting")
                .build());

        options.addOption(Option.builder(OPT_HELP_SHORT)
                .longOpt(OPT_HELP_LONG)
                .desc("Display this help message")
                .build());

        return options;
    }

    /**
     * Validates intensity level value.
     *
     * @param intensity Intensity level to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidIntensity(String intensity) {
        if (intensity == null) return false;
        String lower = intensity.toLowerCase();
        return "low".equals(lower) || "medium".equals(lower) || "high".equals(lower);
    }

    /**
     * Prints usage information for the command-line options.
     *
     * @param options The command-line options
     */
    private void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java org.jvmxray.platform.shared.bin.TestDataGenerator [options]",
                           "JVMXRay Test Data Generator - Generates realistic sensor events for testing and development",
                           options,
                           "Examples:\\n" +
                           "  java org.jvmxray.platform.shared.bin.TestDataGenerator\\n" +
                           "  java org.jvmxray.platform.shared.bin.TestDataGenerator -d 120 -i high -v\\n" +
                           "  java org.jvmxray.platform.shared.bin.TestDataGenerator -db /path/to/test.db -lc /path/to/logback-db.xml\\n" +
                           "  java org.jvmxray.platform.shared.bin.TestDataGenerator -d 60 -np");
    }

    /**
     * Initializes the test environment and component directories.
     *
     * @throws Exception If environment initialization fails
     */
    private void initializeEnvironment() throws Exception {
        if (verbose) {
            System.out.println("Initializing JVMXRay test environment...");
        }

        // Resolve default paths if not specified BEFORE initializing CommonInitializer
        // This ensures that system properties are checked before logging is configured
        if (databasePath == null) {
            String testHome = System.getProperty("jvmxray.test.home");
            if (testHome != null) {
                databasePath = testHome + "/common/data/jvmxray-test.db";
            } else {
                throw new IllegalStateException("Cannot resolve default database path - jvmxray.test.home not set");
            }
        }

        if (logbackConfigPath == null) {
            // First check for the component-specific system property
            String systemLogbackConfig = System.getProperty("logback.common.configurationFile");
            if (systemLogbackConfig != null && !systemLogbackConfig.trim().isEmpty()) {
                logbackConfigPath = systemLogbackConfig;
            } else {
                // Try to find script/config/logback/common.xml first, fall back to default
                String projectRoot = determineProjectRoot();
                Path scriptLogbackPath = Paths.get(projectRoot, "script", "config", "logback", "common.xml");

                if (Files.exists(scriptLogbackPath)) {
                    logbackConfigPath = scriptLogbackPath.toString();
                } else {
                    // Fall back to default common config
                    String testHome = System.getProperty("jvmxray.test.home");
                    logbackConfigPath = testHome + "/common/config/logback.xml";
                    System.out.println("WARNING: script/config/logback/common.xml not found, using default logback configuration");
                    System.out.println("         Events will not be stored in database unless custom configuration is provided");
                }
            }
        }

        // Initialize CommonInitializer to set up directories and configuration
        // This is done AFTER resolving logback config so the system property is respected
        CommonInitializer.getInstance();

        if (verbose) {
            System.out.println("✓ Environment initialized");
            System.out.println("  Test Home: " + System.getProperty("jvmxray.test.home"));
            System.out.println("  Database Path: " + databasePath);
            System.out.println("  Logback Config: " + logbackConfigPath);
        }
    }

    /**
     * Attempts to determine the project root directory.
     *
     * @return Project root directory path
     */
    private String determineProjectRoot() {
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome != null) {
            // Test home is typically {project}/.jvmxray, so parent is project root
            return Paths.get(testHome).getParent().toString();
        }

        // Fall back to current working directory
        return System.getProperty("user.dir");
    }

    /**
     * Validates configuration and prerequisites.
     *
     * @throws Exception If validation fails
     */
    private void validateConfiguration() throws Exception {
        // Validate Maven build prerequisites
        validateBuildArtifacts();
        
        // Validate logback configuration file exists
        if (logbackConfigPath != null && !Files.exists(Paths.get(logbackConfigPath))) {
            throw new IllegalArgumentException("Logback configuration file not found: " + logbackConfigPath);
        }

        // Create database directory if it doesn't exist
        if (databasePath != null) {
            Path dbPath = Paths.get(databasePath);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                if (verbose) {
                    System.out.println("Created database directory: " + parentDir);
                }
            }
        }

        // Validate agent JAR exists (this will throw if not found)
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome != null) {
            Path agentJar = Paths.get(testHome).getParent()
                .resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar");
            if (!Files.exists(agentJar)) {
                throw new IllegalStateException("JVMXRay agent JAR not found: " + agentJar + 
                    "\\nPlease run 'mvn clean install' to build the project first");
            }
            if (verbose) {
                System.out.println("✓ Agent JAR found: " + agentJar);
            }
        }
        
        // Validate essential dependencies are accessible
        validateDependencies();
    }
    
    /**
     * Validates that required Maven build artifacts exist.
     *
     * @throws IllegalStateException If required JARs are not found
     */
    private void validateBuildArtifacts() throws IllegalStateException {
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome == null) {
            throw new IllegalStateException("System property 'jvmxray.test.home' not set");
        }
        
        Path projectRoot = Paths.get(testHome).getParent();
        
        // Check for common JAR
        Path commonJar = projectRoot.resolve("prj-common/target/prj-common-0.0.1.jar");
        if (!Files.exists(commonJar)) {
            throw new IllegalStateException("Common JAR not found: " + commonJar + 
                "\\nPlease run 'mvn clean install' to build the project first");
        }
        
        // Check for agent JAR
        Path agentJar = projectRoot.resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar");
        if (!Files.exists(agentJar)) {
            throw new IllegalStateException("Agent JAR not found: " + agentJar + 
                "\\nPlease run 'mvn clean install' to build the project first");
        }
        
        if (verbose) {
            System.out.println("✓ Build artifacts validated");
        }
    }
    
    /**
     * Validates that essential dependencies are accessible in Maven repository.
     *
     * @throws IllegalStateException If critical dependencies are missing
     */
    private void validateDependencies() throws IllegalStateException {
        String mavenRepo = System.getProperty("user.home") + "/.m2/repository";
        
        // List of critical dependencies that must be present
        String[] criticalDeps = {
            "ch/qos/logback/logback-classic/1.5.6/logback-classic-1.5.6.jar",
            "org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar",
            "commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar",
            "org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar"
        };
        
        boolean missingDeps = false;
        StringBuilder missingList = new StringBuilder();
        
        for (String dep : criticalDeps) {
            Path depPath = Paths.get(mavenRepo, dep);
            if (!Files.exists(depPath)) {
                missingDeps = true;
                if (missingList.length() > 0) {
                    missingList.append("\\n  ");
                }
                missingList.append(dep);
            }
        }
        
        if (missingDeps) {
            throw new IllegalStateException("Critical Maven dependencies not found:\\n  " + 
                missingList.toString() + "\\n\\nPlease run 'mvn clean install' to download dependencies");
        }
        
        if (verbose) {
            System.out.println("✓ Maven dependencies validated");
        }
    }

    /**
     * Executes the test data generation process.
     *
     * @throws Exception If test data generation fails
     */
    private void generateTestData() throws Exception {
        System.out.println("Starting test data generation...");
        System.out.println("Duration: " + duration + " seconds");
        System.out.println("Intensity: " + intensity);
        System.out.println("Database: " + databasePath);
        System.out.println();

        // Set system property for database path if custom path specified
        if (databasePath != null) {
            System.setProperty("jvmxray.test.database", databasePath);
        }

        // Set system property for progress reporting
        System.setProperty("jvmxray.test.progress", String.valueOf(enableProgress));

        // Create execution configuration
        TurtleTestExecutor.ExecutionConfig config = 
            TurtleTestExecutor.createDatabaseConfig(duration, intensity, logbackConfigPath, verbose);

        // Execute test data generation
        long startTime = System.currentTimeMillis();
        TurtleTestExecutor.ExecutionResult result = TurtleTestExecutor.executeTurtleTest(config);
        long endTime = System.currentTimeMillis();

        // Report results
        if (result.isSuccess()) {
            System.out.println();
            System.out.println("✓ Test data generation completed successfully");
            System.out.println("  Duration: " + (endTime - startTime) / 1000.0 + " seconds");
            System.out.println("  Database: " + databasePath);
            
            // Validate database was created/updated
            if (Files.exists(Paths.get(databasePath))) {
                long dbSize = Files.size(Paths.get(databasePath));
                System.out.println("  Database size: " + formatFileSize(dbSize));
            }
        } else {
            throw new RuntimeException("Test data generation failed: " + result.getMessage() + 
                " (exit code: " + result.getExitCode() + ")");
        }
    }

    /**
     * Formats file size in human-readable format.
     *
     * @param bytes File size in bytes
     * @return Formatted file size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}