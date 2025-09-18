package org.jvmxray.service.rest.bin;

import org.apache.commons.cli.*;
import org.jvmxray.service.rest.init.RestServiceInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the JVMXRay REST Service.
 * Provides RESTful API access to security event data.
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --start}: Start the REST service (default action).</li>
 *   <li>{@code --stop}: Stop a running REST service instance.</li>
 *   <li>{@code --restart}: Restart the REST service (stop if running, then start).</li>
 *   <li>{@code --help}: Display usage information and exit.</li>
 *   <li>{@code -p, --port <port>}: Port for the REST service (default: 8080).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.service.rest.bin.RestService --start -p 8080
 * java org.jvmxray.service.rest.bin.RestService --stop
 * java org.jvmxray.service.rest.bin.RestService --restart
 * java org.jvmxray.service.rest.bin.RestService --help
 * }</pre>
 *
 * @author Milton Smith
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.jvmxray.service.rest")
public class RestService {

    private static final Logger logger = Logger.getLogger(RestService.class.getName());
    private static ConfigurableApplicationContext context;

    // Service action constants
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_HELP = "help";

    // Command-line option constants
    private static final String OPT_PORT_SHORT = "p";
    private static final String OPT_PORT_LONG = "port";
    private static final int DEFAULT_PORT = 8080;

    // PID file management
    private static final String PID_FILE_NAME = "restservice.pid";
    private static final String DEFAULT_PID_DIR = System.getProperty("java.io.tmpdir");
    private static final Path PID_FILE_PATH = Paths.get(DEFAULT_PID_DIR, PID_FILE_NAME);

    // Service configuration
    private int port = DEFAULT_PORT;
    private String action = ACTION_START;

    /**
     * Main method to start the REST service.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create and run the REST service instance
            new RestService().run(args);
        } catch (Exception e) {
            System.err.println("Failed to initialize RestService: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the REST service application, parsing command-line arguments and executing the requested action.
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
            logger.log(Level.SEVERE, "Fatal error occurred", e);
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
        } else if (cmd.hasOption(ACTION_RESTART)) {
            action = ACTION_RESTART;
        } else if (cmd.hasOption(ACTION_START)) {
            action = ACTION_START;
        }
        // Default action is start if no action specified

        // Configure port from command-line or use default
        if (cmd.hasOption(OPT_PORT_SHORT)) {
            try {
                port = Integer.parseInt(cmd.getOptionValue(OPT_PORT_SHORT));
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
                port = DEFAULT_PORT;
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
                .desc("Start the REST service (default action)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_STOP)
                .desc("Stop a running REST service instance")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_RESTART)
                .desc("Restart the REST service (stop if running, then start)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_HELP)
                .desc("Display this help message and exit")
                .build());

        Option port = Option.builder(OPT_PORT_SHORT)
                .longOpt(OPT_PORT_LONG)
                .hasArg()
                .argName("PORT")
                .desc("Port to run the REST service on (default: " + DEFAULT_PORT + ")")
                .build();
        options.addOption(port);

        return options;
    }

    /**
     * Displays help information and usage examples.
     */
    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("restservice",
            "\nJVMXRay REST Service - RESTful API for querying security events\n\n" +
            "Service Management:\n" +
            "  restservice --start       Start the service (default)\n" +
            "  restservice --stop        Stop the service\n" +
            "  restservice --restart     Restart the service\n" +
            "  restservice --help        Show this help\n\n" +
            "Note: For API key management, use the generate-api-key script.\n\n" +
            "Configuration Options:",
            createOptions(),
            "\nExamples:\n" +
            "  restservice --start -p 8080\n" +
            "  restservice --restart\n" +
            "  restservice --stop\n");
    }

    /**
     * Starts the REST service.
     */
    private void startService() {
        try {
            // Check if service is already running
            if (isServiceRunning()) {
                System.out.println("REST Service is already running (PID: " + getRunningPid() + ")");
                return;
            }

            System.out.println("Starting REST Service...");

            // Initialize the REST Service component
            RestServiceInitializer initializer = RestServiceInitializer.getInstance();
            initializer.initialize();

            // Set logback configuration file system property
            String logbackConfig = initializer.getJvmxrayHome() + "/restservice/config/logback.xml";
            System.setProperty("logback.configurationFile", logbackConfig);
            logger.info("Using logback configuration: " + logbackConfig);

            // Set database URL system property from initializer (BEFORE Spring Boot starts)
            String jvmxrayHome = initializer.getJvmxrayHome().toString();
            String dbUrl = "jdbc:sqlite:" + jvmxrayHome + "/common/data/jvmxray-test.db";
            System.setProperty("rest.service.database.url", dbUrl);
            logger.info("Using database URL: " + dbUrl);

            // Set port system property
            System.setProperty("server.port", String.valueOf(port));
            logger.info("Using port: " + port);

            // Set additional Spring Boot properties
            System.setProperty("spring.main.banner-mode", "off");
            System.setProperty("logging.level.root", "WARN");
            System.setProperty("logging.level.org.jvmxray", "INFO");

            // Write PID file
            writePidFile();

            // Start Spring Boot application
            logger.info("Starting JVMXRay REST Service...");
            context = SpringApplication.run(RestService.class, new String[0]);

            logger.info("JVMXRay REST Service started successfully on port " + port);
            System.out.println("REST Service started successfully");
            System.out.println("API endpoint: http://localhost:" + port + "/api/v1/");
            System.out.println("PID file: " + PID_FILE_PATH);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down REST Service...");
                if (context != null) {
                    context.close();
                }
                cleanupPidFile();
            }));

            // Keep the main thread alive
            waitForShutdown();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start REST Service", e);
            System.err.println("Failed to start REST Service: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Stops the REST service using modern ProcessHandle API.
     */
    private void stopService() {
        try {
            if (!isServiceRunning()) {
                System.out.println("REST Service is not running");
                return;
            }

            long pid = getRunningPid();
            System.out.println("Stopping REST Service (PID: " + pid + ")...");

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
                            System.out.println("REST Service stopped gracefully");
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
                            System.out.println("REST Service force stopped");
                            cleanupPidFile();
                        } else {
                            System.err.println("Failed to stop REST Service");
                            System.exit(1);
                        }
                    }
                } else {
                    System.err.println("Failed to initiate REST Service shutdown");
                    System.exit(1);
                }
            } else {
                System.err.println("Cannot obtain process handle for PID: " + pid);
                // Clean up stale PID file
                cleanupPidFile();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to stop REST Service", e);
            System.err.println("Failed to stop REST Service: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Restarts the REST service.
     */
    private void restartService() {
        System.out.println("Restarting REST Service...");

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
            logger.log(Level.FINE, "Error checking if service is running", e);
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
            logger.log(Level.FINE, "Error reading PID file", e);
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
            logger.warning("Failed to cleanup PID file: " + e.getMessage());
        }
    }

    /**
     * Keeps the main thread alive, waiting for a shutdown signal.
     *
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    private void waitForShutdown() throws InterruptedException {
        // Keep the main thread alive to prevent application exit
        synchronized (this) {
            this.wait();
        }
    }

    /**
     * Stop the REST service programmatically.
     */
    public static void stop() {
        if (context != null) {
            logger.info("Stopping REST Service...");
            SpringApplication.exit(context, () -> 0);
            context = null;
        }
    }

    /**
     * Check if the REST service is running.
     *
     * @return true if running, false otherwise
     */
    public static boolean isRunning() {
        return context != null && context.isActive();
    }
}