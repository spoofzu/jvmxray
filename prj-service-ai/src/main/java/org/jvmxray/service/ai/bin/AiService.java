package org.jvmxray.service.ai.bin;

import org.apache.commons.cli.*;
import java.sql.DriverManager;
import org.jvmxray.platform.shared.property.PropertyBase;
import org.jvmxray.service.ai.init.AiServiceInitializer;
import org.jvmxray.service.ai.processor.Stage0Processor;
import org.jvmxray.service.ai.processor.Stage1Processor;
import org.jvmxray.service.ai.processor.Stage2Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main entry point for the JVMXRay AI Service.
 * Provides intelligence enrichment and analysis for security events.
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --init}: Initialize configuration and exit (does not start service).</li>
 *   <li>{@code --start}: Start the AI service (default action).</li>
 *   <li>{@code --stop}: Stop a running AI service instance.</li>
 *   <li>{@code --restart}: Restart the AI service (stop if running, then start).</li>
 *   <li>{@code --help}: Display usage information and exit.</li>
 *   <li>{@code -b, --batch-size <size>}: Batch size for event processing (default: 1000).</li>
 *   <li>{@code -i, --interval <seconds>}: Processing interval in seconds (default: 60).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.service.ai.bin.AiService --init
 * java org.jvmxray.service.ai.bin.AiService --start
 * java org.jvmxray.service.ai.bin.AiService --start --batch-size 500
 * java org.jvmxray.service.ai.bin.AiService --stop
 * java org.jvmxray.service.ai.bin.AiService --restart
 * java org.jvmxray.service.ai.bin.AiService --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    // Service action constants
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_INIT = "init";
    private static final String ACTION_HELP = "help";

    // Command-line option constants
    private static final String OPT_BATCH_SIZE_SHORT = "b";
    private static final String OPT_BATCH_SIZE_LONG = "batch-size";
    private static final String OPT_INTERVAL_SHORT = "i";
    private static final String OPT_INTERVAL_LONG = "interval";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_INTERVAL = 60;

    // PID file management
    private static final String PID_FILE_NAME = "aiservice.pid";
    private static final String DEFAULT_PID_DIR = System.getProperty("java.io.tmpdir");
    private static final Path PID_FILE_PATH = Paths.get(DEFAULT_PID_DIR, PID_FILE_NAME);

    // Service configuration
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int intervalSeconds = DEFAULT_INTERVAL;
    private String action = ACTION_START;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private PropertyBase properties;
    private Stage0Processor stage0Processor;
    private Stage1Processor stage1Processor;
    private Stage2Processor stage2Processor;

    /**
     * Main method to start the AI service.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create and run the AI service instance
            new AiService().run(args);
        } catch (Exception e) {
            System.err.println("Failed to initialize AiService: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the AI service application, parsing command-line arguments and executing the requested action.
     *
     * @param args Command-line arguments.
     */
    private void run(String[] args) {
        try {
            // Parse command-line arguments
            parseCommandLine(args);

            // Execute the requested action
            switch (action) {
                case ACTION_START:
                    startService();
                    break;
                case ACTION_STOP:
                    stopService();
                    break;
                case ACTION_RESTART:
                    restartService();
                    break;
                case ACTION_INIT:
                    initService();
                    break;
                case ACTION_HELP:
                    displayHelp();
                    break;
                default:
                    System.err.println("Unknown action: " + action);
                    displayHelp();
                    System.exit(1);
            }
        } catch (Exception e) {
            // Log fatal errors and exit with error code
            logger.error("Fatal error occurred", e);
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to determine action and configure service options.
     *
     * @param args Command-line arguments.
     * @throws ParseException If the arguments are invalid or cannot be parsed.
     */
    private void parseCommandLine(String[] args) throws ParseException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Determine action - check for action flags
        if (cmd.hasOption(ACTION_HELP)) {
            action = ACTION_HELP;
            return; // Don't need to parse other options for help
        } else if (cmd.hasOption(ACTION_STOP)) {
            action = ACTION_STOP;
            return; // Don't need to parse port options for stop
        } else if (cmd.hasOption(ACTION_INIT)) {
            action = ACTION_INIT;
            return; // Don't need to parse other options for init
        } else if (cmd.hasOption(ACTION_RESTART)) {
            action = ACTION_RESTART;
        } else if (cmd.hasOption(ACTION_START)) {
            action = ACTION_START;
        }
        // Default action is start if no action specified

        // Configure batch size from command-line or use default
        if (cmd.hasOption(OPT_BATCH_SIZE_SHORT)) {
            try {
                batchSize = Integer.parseInt(cmd.getOptionValue(OPT_BATCH_SIZE_SHORT));
            } catch (NumberFormatException e) {
                System.err.println("Invalid batch size provided. Using default: " + DEFAULT_BATCH_SIZE);
                batchSize = DEFAULT_BATCH_SIZE;
            }
        }

        // Configure interval from command-line or use default
        if (cmd.hasOption(OPT_INTERVAL_SHORT)) {
            try {
                intervalSeconds = Integer.parseInt(cmd.getOptionValue(OPT_INTERVAL_SHORT));
            } catch (NumberFormatException e) {
                System.err.println("Invalid interval provided. Using default: " + DEFAULT_INTERVAL);
                intervalSeconds = DEFAULT_INTERVAL;
            }
        }
    }

    /**
     * Create command line options.
     *
     * @return Command line options
     */
    private Options createOptions() {
        Options options = new Options();

        // Add service action options
        options.addOption(Option.builder()
                .longOpt(ACTION_START)
                .desc("Start the AI service (default action)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_STOP)
                .desc("Stop a running AI service instance")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_RESTART)
                .desc("Restart the AI service (stop if running, then start)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_INIT)
                .desc("Initialize AI service configuration and exit (does not start service)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_HELP)
                .desc("Display this help message and exit")
                .build());

        Option batchSize = Option.builder(OPT_BATCH_SIZE_SHORT)
                .longOpt(OPT_BATCH_SIZE_LONG)
                .hasArg()
                .argName("SIZE")
                .desc("Batch size for event processing (default: " + DEFAULT_BATCH_SIZE + ")")
                .build();
        options.addOption(batchSize);

        Option interval = Option.builder(OPT_INTERVAL_SHORT)
                .longOpt(OPT_INTERVAL_LONG)
                .hasArg()
                .argName("SECONDS")
                .desc("Processing interval in seconds (default: " + DEFAULT_INTERVAL + ")")
                .build();
        options.addOption(interval);

        return options;
    }

    /**
     * Displays help information and usage examples.
     */
    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("aiservice",
            "\nJVMXRay AI Service - Intelligence enrichment and analysis for security events\n\n" +
            "Service Management:\n" +
            "  aiservice --init          Initialize configuration (first-time setup)\n" +
            "  aiservice --start         Start the service (default)\n" +
            "  aiservice --stop          Stop the service\n" +
            "  aiservice --restart       Restart the service\n" +
            "  aiservice --help          Show this help\n\n" +
            "Configuration Options:",
            createOptions(),
            "\nExamples:\n" +
            "  aiservice --init                    # Initialize configuration\n" +
            "  aiservice --start\n" +
            "  aiservice --start -b 500 -i 30\n" +
            "  aiservice --restart\n" +
            "  aiservice --stop\n");
    }

    /**
     * Initializes the AI service configuration without starting the service.
     * Creates directory structure and configuration files, then exits.
     */
    private void initService() {
        try {
            System.out.println("Initializing AI Service configuration...");

            // Initialize the AI Service component
            AiServiceInitializer initializer = AiServiceInitializer.getInstance();

            // Get configuration paths
            String jvmxrayHome = initializer.getJvmxrayHome().toString();
            String configFile = jvmxrayHome + "/aiservice/config/aiservice.properties";

            System.out.println("\n=================================================");
            System.out.println("AI Service configuration initialized successfully");
            System.out.println("=================================================");
            System.out.println("Configuration directory: " + jvmxrayHome + "/aiservice");
            System.out.println("Configuration file: " + configFile);
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Failed to initialize AI Service configuration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Starts the AI service.
     */
    private void startService() {
        try {
            // Check if service is already running
            if (isServiceRunning()) {
                System.out.println("AI Service is already running (PID: " + getRunningPid() + ")");
                return;
            }

            System.out.println("Starting AI Service...");
            System.out.println("Initializing components...");

            // Initialize the AI Service component
            AiServiceInitializer initializer = AiServiceInitializer.getInstance();
            properties = initializer.getProperties();

            // Set logback configuration file system property
            String logbackConfig = initializer.getJvmxrayHome() + "/aiservice/config/logback.xml";
            System.setProperty("logback.configurationFile", logbackConfig);
            logger.info("Using logback configuration: " + logbackConfig);

            // Set database URL system property from initializer
            String jvmxrayHome = initializer.getJvmxrayHome().toString();
            String dbUrl = "jdbc:sqlite:" + jvmxrayHome + "/common/data/jvmxray-test.db";
            System.setProperty("aiservice.database.url", dbUrl);
            logger.info("Using database URL: " + dbUrl);

            // Set batch size and interval in properties
            properties.setProperty("aiservice.batch.size", String.valueOf(batchSize));
            properties.setProperty("aiservice.processing.interval.seconds", String.valueOf(intervalSeconds));
            logger.info("Using batch size: " + batchSize);
            logger.info("Using interval: " + intervalSeconds + " seconds");

            System.out.println("Initializing AI processors...");
            // Initialize all stage processors
            stage0Processor = new Stage0Processor(properties);
            stage0Processor.initialize(properties);

            stage1Processor = new Stage1Processor(properties);
            stage1Processor.initialize(properties);

            stage2Processor = new Stage2Processor(properties);
            stage2Processor.initialize(properties);

            // Write PID file
            writePidFile();

            // Start the service
            logger.info("Starting JVMXRay AI Service...");
            running.set(true);

            System.out.println("\n=================================================");
            System.out.println("AI Service started successfully");
            System.out.println("=================================================");
            System.out.println("Database: " + dbUrl);
            System.out.println("Batch size: " + batchSize + " events per cycle");
            System.out.println("Processing interval: " + intervalSeconds + " seconds");
            System.out.println("PID: " + ProcessHandle.current().pid());
            System.out.println("PID file: " + PID_FILE_PATH);
            System.out.println("=================================================");
            System.out.println("Multi-stage data pipeline processing:");
            System.out.println("  Stage0: STAGE0_EVENT → STAGE1_EVENT (parsing)");
            System.out.println("  Stage1: STAGE1_EVENT → STAGE2_LIBRARY (basic enrichment)");
            System.out.println("  Stage2: STAGE2_LIBRARY enrichment (CVE analysis)");
            System.out.println("=================================================");
            System.out.println("\n✓ AI Service ready - monitoring for events");
            System.out.println("  Processing every " + intervalSeconds + " seconds\n");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down AI Service...");
                running.set(false);
                cleanupPidFile();
            }));

            // Run the processing loop
            runProcessingLoop();

        } catch (Exception e) {
            logger.error("Failed to start AI Service", e);
            System.err.println("Failed to start AI Service: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the main processing loop for all stage processors.
     */
    private void runProcessingLoop() {
        int cycleCount = 0;
        while (running.get()) {
            try {
                cycleCount++;
                logger.debug("Starting processing cycle #{}", cycleCount);

                // Process all stages in sequence
                int stage0Count = 0;
                int stage1Count = 0;
                int stage2Count = 0;

                try (Connection connection = getConnection()) {
                    // Stage 0: STAGE0 → STAGE1 (parsing)
                    if (stage0Processor.isEnabled()) {
                        stage0Count = stage0Processor.processBatch(connection, batchSize);
                    }

                    // Stage 1: STAGE1 → STAGE2 (basic enrichment)
                    if (stage1Processor.isEnabled()) {
                        stage1Count = stage1Processor.processBatch(connection, batchSize);
                    }

                    // Stage 2: STAGE2 enrichment (CVE analysis)
                    if (stage2Processor.isEnabled()) {
                        stage2Count = stage2Processor.processBatch(connection, batchSize);
                    }
                }

                // Report processing results
                int totalProcessed = stage0Count + stage1Count + stage2Count;
                if (totalProcessed > 0) {
                    String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    System.out.println("[" + timestamp + "] Processed: " +
                        stage0Count + " stage0→stage1, " +
                        stage1Count + " stage1→stage2, " +
                        stage2Count + " stage2 enriched");
                    logger.info("Processing cycle #{}: {} stage0, {} stage1, {} stage2",
                               cycleCount, stage0Count, stage1Count, stage2Count);
                } else {
                    logger.debug("No events to process in cycle #{}", cycleCount);
                }

                // Wait for next cycle
                logger.debug("Waiting {} seconds until next processing cycle...", intervalSeconds);
                Thread.sleep(intervalSeconds * 1000L);

            } catch (InterruptedException e) {
                System.out.println("\nService interrupted, shutting down...");
                logger.info("Service interrupted, shutting down...");
                break;
            } catch (Exception e) {
                System.err.println("[ERROR] Processing cycle failed: " + e.getMessage());
                logger.error("Error in processing cycle: {}", e.getMessage(), e);
                // Continue running despite errors
            }
        }

        System.out.println("\n=================================================");
        System.out.println("AI Service stopped");
        System.out.println("=================================================");
        logger.info("AI Service stopped");
    }

    /**
     * Gets a database connection for processing.
     */
    private Connection getConnection() throws SQLException {
        String dbUrl = properties.getProperty("aiservice.database.url",
                      "jdbc:sqlite:.jvmxray/common/data/jvmxray-test.db");
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Stops the AI service using modern ProcessHandle API.
     */
    private void stopService() {
        try {
            if (!isServiceRunning()) {
                System.out.println("AI Service is not running");
                return;
            }

            long pid = getRunningPid();
            System.out.println("Stopping AI Service (PID: " + pid + ")...");

            Optional<ProcessHandle> processHandle = getServiceProcessHandle();
            if (processHandle.isPresent()) {
                ProcessHandle handle = processHandle.get();

                // Request graceful termination
                boolean terminated = handle.destroy();

                if (terminated) {
                    // Wait up to 10 seconds for graceful shutdown
                    try {
                        boolean exited = handle.onExit().get(10, java.util.concurrent.TimeUnit.SECONDS) != null;
                        if (exited) {
                            System.out.println("\n=================================================");
                            System.out.println("AI Service stopped gracefully");
                            System.out.println("=================================================");
                            cleanupPidFile();
                            return;
                        }
                    } catch (java.util.concurrent.TimeoutException e) {
                        System.out.println("Graceful shutdown timeout, forcing termination...");
                    }

                    // Force kill if still running
                    if (handle.isAlive()) {
                        boolean forceKilled = handle.destroyForcibly();
                        if (forceKilled) {
                            System.out.println("\n=================================================");
                            System.out.println("AI Service force stopped");
                            System.out.println("=================================================");
                            cleanupPidFile();
                        } else {
                            System.err.println("Failed to stop AI Service");
                            System.exit(1);
                        }
                    }
                } else {
                    System.err.println("Failed to initiate AI Service shutdown");
                    System.exit(1);
                }
            } else {
                System.err.println("Cannot obtain process handle for PID: " + pid);
                System.out.println("Cleaning up stale PID file...");
                // Clean up stale PID file
                cleanupPidFile();
            }

        } catch (Exception e) {
            logger.error("Failed to stop AI Service", e);
            System.err.println("Failed to stop AI Service: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Restarts the AI service.
     */
    private void restartService() {
        System.out.println("Restarting AI Service...");

        // Stop if running
        if (isServiceRunning()) {
            stopService();
        }

        // Small delay to ensure cleanup
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start the service
        startService();
    }

    /**
     * Checks if the service is currently running using modern ProcessHandle API.
     */
    private boolean isServiceRunning() {
        try {
            if (!Files.exists(PID_FILE_PATH)) {
                return false;
            }

            long pid = getRunningPid();
            if (pid <= 0) {
                return false;
            }

            // Use ProcessHandle API for cross-platform process checking (Java 9+)
            return ProcessHandle.of(pid).isPresent() && ProcessHandle.of(pid).get().isAlive();
        } catch (Exception e) {
            logger.debug("Error checking if service is running", e);
            return false;
        }
    }

    /**
     * Gets the PID of the running service.
     */
    private long getRunningPid() {
        try {
            if (!Files.exists(PID_FILE_PATH)) {
                return -1;
            }

            List<String> lines = Files.readAllLines(PID_FILE_PATH);
            if (lines.isEmpty()) {
                return -1;
            }

            return Long.parseLong(lines.get(0).trim());
        } catch (Exception e) {
            logger.debug("Error reading PID file", e);
            return -1;
        }
    }

    /**
     * Gets the ProcessHandle for the running service if available.
     */
    private Optional<ProcessHandle> getServiceProcessHandle() {
        long pid = getRunningPid();
        if (pid > 0) {
            return ProcessHandle.of(pid);
        }
        return Optional.empty();
    }

    /**
     * Writes the current process PID to the PID file.
     */
    private void writePidFile() throws IOException {
        long pid = ProcessHandle.current().pid();
        Files.write(PID_FILE_PATH, String.valueOf(pid).getBytes());
    }

    /**
     * Removes the PID file.
     */
    private void cleanupPidFile() {
        try {
            Files.deleteIfExists(PID_FILE_PATH);
        } catch (IOException e) {
            logger.warn("Failed to cleanup PID file: {}", e.getMessage());
        }
    }
}