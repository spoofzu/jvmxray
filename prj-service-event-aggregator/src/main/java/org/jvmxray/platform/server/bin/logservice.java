package org.jvmxray.platform.server.bin;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import org.apache.commons.cli.*;
import org.jvmxray.platform.server.JvmxraySocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A Java application that runs a Logback-based logging service to aggregate logging events
 * from JVMXRay agents and persist them to centralized storage for processing and reporting.
 * Includes an embedded HTTP REST server to handle client requests at the {@code /api} endpoint.
 *
 * <p>Key functionalities:</p>
 * <ul>
 *   <li>Starts a Logback {@link SimpleSocketServer} to receive agent logging events.</li>
 *   <li>Initializes an HTTP REST server to process client requests.</li>
 *   <li>Parses command-line arguments to configure ports for the agent listener and REST server.</li>
 *   <li>Registers a shutdown hook for graceful termination.</li>
 * </ul>
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --start}: Start the logservice (default action).</li>
 *   <li>{@code --stop}: Stop a running logservice instance.</li>
 *   <li>{@code --restart}: Restart the logservice (stop if running, then start).</li>
 *   <li>{@code --help}: Display usage information and exit.</li>
 *   <li>{@code -p, --port <port>}: Port for the agent event listener (default: 9876).</li>
 *   <li>{@code -r, --restport <port>}: Port for the REST server (default: 8080).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.platform.server.bin.logservice --start -p 9876 -r 8080
 * java org.jvmxray.platform.server.bin.logservice --stop
 * java org.jvmxray.platform.server.bin.logservice --restart
 * java org.jvmxray.platform.server.bin.logservice --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class logservice {

    // SLF4J logger for the logservice application
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.server.bin.logservice");

    // Service action constants
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_HELP = "help";

    // Command-line option constants for agent port
    private static final String OPT_PORT_SHORT = "p";
    private static final String OPT_PORT_LONG = "port";
    private static final int DEFAULT_AGENT_PORT = 9876;

    // Command-line option constants for REST server port
    private static final String OPT_REST_PORT_SHORT = "r";
    private static final String OPT_REST_PORT_LONG = "restport";
    private static final int DEFAULT_REST_PORT = 8080;

    // PID file management
    private static final String PID_FILE_NAME = "logservice.pid";
    private static final String DEFAULT_PID_DIR = System.getProperty("java.io.tmpdir");
    private static final Path PID_FILE_PATH = Paths.get(DEFAULT_PID_DIR, PID_FILE_NAME);

    // Port for the Logback agent event listener
    private int agentPort = DEFAULT_AGENT_PORT;
    // Port for the REST server
    private int restPort = DEFAULT_REST_PORT;
    // Service action to perform
    private String action = ACTION_START;

    // Logback server instance for receiving agent events
    private JvmxraySocketServer logbackServer;
    // HTTP server instance for REST API
    private HttpServer restServer;

    /**
     * Main entry point for the logservice application.
     *
     * @param args Command-line arguments specifying action and configuration.
     */
    public static void main(String[] args) {
        // Create and run the logservice instance
        new logservice().run(args);
    }

    /**
     * Runs the logservice application, parsing command-line arguments and executing the requested action.
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
            logger.error("Fatal error occurred: {}", e.getMessage(), e);
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
        // Define command-line options
        Options options = new Options();

        // Add service action options
        options.addOption(Option.builder()
                .longOpt(ACTION_START)
                .desc("Start the logservice (default action)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_STOP)
                .desc("Stop a running logservice instance")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_RESTART)
                .desc("Restart the logservice (stop if running, then start)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_HELP)
                .desc("Display this help message and exit")
                .build());

        // Add agent port option
        options.addOption(Option.builder(OPT_PORT_SHORT)
                .longOpt(OPT_PORT_LONG)
                .desc("Port number for the Agent event listener (default: " + DEFAULT_AGENT_PORT + ")")
                .hasArg()
                .argName("AGENT_PORT")
                .build());

        // Add REST server port option
        options.addOption(Option.builder(OPT_REST_PORT_SHORT)
                .longOpt(OPT_REST_PORT_LONG)
                .desc("Port number for the REST server (default: " + DEFAULT_REST_PORT + ")")
                .hasArg()
                .argName("REST_PORT")
                .build());

        // Parse command-line arguments
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

        // Configure agent port from command-line or use default
        if (cmd.hasOption(OPT_PORT_SHORT)) {
            try {
                agentPort = Integer.parseInt(cmd.getOptionValue(OPT_PORT_SHORT));
            } catch (NumberFormatException e) {
                // Log warning and revert to default if invalid
                System.err.println("Invalid agent port number provided. Using default port " + DEFAULT_AGENT_PORT);
                agentPort = DEFAULT_AGENT_PORT;
            }
        }

        // Configure REST server port from command-line or use default
        if (cmd.hasOption(OPT_REST_PORT_SHORT)) {
            try {
                restPort = Integer.parseInt(cmd.getOptionValue(OPT_REST_PORT_SHORT));
            } catch (NumberFormatException e) {
                // Log warning and revert to default if invalid
                System.err.println("Invalid REST port number provided. Using default port " + DEFAULT_REST_PORT);
                restPort = DEFAULT_REST_PORT;
            }
        }
    }

    /**
     * Displays help information and usage examples.
     */
    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("logservice", 
            "\nJVMXRay LogService - Centralized logging service for JVMXRay agents\n\n" +
            "Service Management:\n" +
            "  logservice --start    Start the service (default)\n" +
            "  logservice --stop     Stop the service\n" +
            "  logservice --restart  Restart the service\n" +
            "  logservice --help     Show this help\n\n" +
            "Configuration Options:", 
            createOptions(), 
            "\nExamples:\n" +
            "  logservice --start -p 9876 -r 8080\n" +
            "  logservice --restart\n" +
            "  logservice --stop\n");
    }

    /**
     * Creates the command-line options for help display.
     */
    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt(ACTION_START)
                .desc("Start the logservice (default action)")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_STOP)
                .desc("Stop a running logservice instance")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_RESTART)
                .desc("Restart the logservice")
                .build());

        options.addOption(Option.builder()
                .longOpt(ACTION_HELP)
                .desc("Display this help message")
                .build());

        options.addOption(Option.builder(OPT_PORT_SHORT)
                .longOpt(OPT_PORT_LONG)
                .desc("Agent event listener port (default: " + DEFAULT_AGENT_PORT + ")")
                .hasArg()
                .argName("PORT")
                .build());

        options.addOption(Option.builder(OPT_REST_PORT_SHORT)
                .longOpt(OPT_REST_PORT_LONG)
                .desc("REST server port (default: " + DEFAULT_REST_PORT + ")")
                .hasArg()
                .argName("PORT")
                .build());

        return options;
    }

    /**
     * Starts the logservice.
     */
    private void startService() {
        try {
            // Check if service is already running
            if (isServiceRunning()) {
                System.out.println("LogService is already running (PID: " + getRunningPid() + ")");
                return;
            }

            System.out.println("Starting LogService...");
            logger.info("Agent server port set to {}", agentPort);
            logger.info("REST server port set to {}", restPort);

            // Initialize servers
            initServers();

            // Write PID file
            writePidFile();

            System.out.println("LogService started successfully");
            System.out.println("Agent listener: localhost:" + agentPort);
            System.out.println("REST server: http://localhost:" + restPort + "/api");
            System.out.println("PID file: " + PID_FILE_PATH);

            // Wait for shutdown signal
            waitForShutdown();

        } catch (Exception e) {
            logger.error("Failed to start LogService: {}", e.getMessage(), e);
            System.err.println("Failed to start LogService: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Stops the logservice.
     */
    private void stopService() {
        try {
            if (!isServiceRunning()) {
                System.out.println("LogService is not running");
                return;
            }

            long pid = getRunningPid();
            System.out.println("Stopping LogService (PID: " + pid + ")...");

            // Kill the process
            ProcessBuilder pb = new ProcessBuilder("kill", String.valueOf(pid));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Wait a moment for the process to terminate
                Thread.sleep(2000);
                
                if (!isServiceRunning()) {
                    System.out.println("LogService stopped successfully");
                    cleanupPidFile();
                } else {
                    // Force kill if still running
                    System.out.println("Force stopping LogService...");
                    pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                    process = pb.start();
                    process.waitFor();
                    
                    Thread.sleep(1000);
                    if (!isServiceRunning()) {
                        System.out.println("LogService force stopped");
                        cleanupPidFile();
                    } else {
                        System.err.println("Failed to stop LogService");
                        System.exit(1);
                    }
                }
            } else {
                System.err.println("Failed to stop LogService (exit code: " + exitCode + ")");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Failed to stop LogService: {}", e.getMessage(), e);
            System.err.println("Failed to stop LogService: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Restarts the logservice.
     */
    private void restartService() {
        System.out.println("Restarting LogService...");
        
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
     * Checks if the service is currently running.
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

            // Check if process is actually running
            ProcessBuilder pb = new ProcessBuilder("kill", "-0", String.valueOf(pid));
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
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
            return -1;
        }
    }

    /**
     * Writes the current process PID to the PID file.
     */
    private void writePidFile() throws IOException {
        long pid = ProcessHandle.current().pid();
        Files.write(PID_FILE_PATH, String.valueOf(pid).getBytes());
        
        // Add shutdown hook to clean up PID file
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupPidFile));
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

    /**
     * Initializes the Logback server for agent events and the REST server for client requests.
     *
     * @throws IOException If server initialization fails (e.g., port binding issues).
     */
    private void initServers() throws IOException {
        // Log initialization start
        logger.info("Initializing logservice.");

        // Register shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        // Initialize Logback server for agent events
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        logbackServer = new JvmxraySocketServer(loggerContext, agentPort);
        logbackServer.start();
        logger.info("JvmxraySocketServer started on port {}", agentPort);

        // Initialize REST server for client requests
        restServer = HttpServer.create(new InetSocketAddress(restPort), 0);
        restServer.createContext("/api", new RestHandler());
        restServer.setExecutor(null); // Use default executor
        restServer.start();
        logger.info("REST server started on port {}", restPort);

        // Log successful initialization
        logger.info("logservice is running.");
    }

    /**
     * Shuts down the Logback and REST servers gracefully.
     */
    private void shutdown() {
        // Log shutdown start
        logger.info("Shutting down logservice.");
        if (restServer != null) {
            // Stop the REST server
            restServer.stop(0);
            logger.info("REST server stopped.");
        }
        if (logbackServer != null) {
            // Log active connections before shutdown
            int activeConnections = logbackServer.getActiveConnectionCount();
            if (activeConnections > 0) {
                logger.info("Shutting down JvmxraySocketServer with {} active agent connections", activeConnections);
            }
            // Stop the Logback server
            logbackServer.close();
            logger.info("JvmxraySocketServer stopped.");
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
     * Handles HTTP requests to the {@code /api} endpoint, returning a simple text response.
     */
    private static class RestHandler implements HttpHandler {
        /**
         * Processes HTTP requests, sending a plain text response.
         *
         * @param exchange The {@code HttpExchange} object representing the request and response.
         * @throws IOException If an I/O error occurs while sending the response.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Define a simple response
            String response = "This is the response";
            // Set response headers
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            // Send response headers with status 200
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            // Write response body
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
}