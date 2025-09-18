package org.jvmxray.service.log.bin;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import org.apache.commons.cli.*;
import org.jvmxray.service.log.server.SocketServer;
import org.jvmxray.service.log.init.LogServiceInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * A Java application that runs a Logback-based logging service to aggregate logging events
 * from JVMXRay agents and persist them to centralized storage for processing and reporting.
 *
 * <p>Key functionalities:</p>
 * <ul>
 *   <li>Starts a Logback {@link SimpleSocketServer} to receive agent logging events.</li>
 *   <li>Parses command-line arguments to configure the port for the agent listener.</li>
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
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.service.log.bin.LogService --start -p 9876
 * java org.jvmxray.service.log.bin.LogService --stop
 * java org.jvmxray.service.log.bin.LogService --restart
 * java org.jvmxray.service.log.bin.LogService --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class LogService {

    // SLF4J logger for the logservice application - initialized after LogServiceInitializer
    private static Logger logger = null;

    // Service action constants
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_HELP = "help";

    // Command-line option constants for agent port
    private static final String OPT_PORT_SHORT = "p";
    private static final String OPT_PORT_LONG = "port";
    private static final int DEFAULT_AGENT_PORT = 9876;

    // Command-line option constants for daemon mode
    private static final String OPT_DAEMON_SHORT = "d";
    private static final String OPT_DAEMON_LONG = "daemon";


    // PID file management
    private static final String PID_FILE_NAME = "logservice.pid";
    private static final String DEFAULT_PID_DIR = System.getProperty("java.io.tmpdir");
    private static final Path PID_FILE_PATH = Paths.get(DEFAULT_PID_DIR, PID_FILE_NAME);

    // Port for the Logback agent event listener
    private int agentPort = DEFAULT_AGENT_PORT;
    // Service action to perform
    private String action = ACTION_START;
    // Daemon mode flag
    private boolean daemonMode = false;

    // Logback server instance for receiving agent events
    private SocketServer logbackServer;

    /**
     * Main entry point for the logservice application.
     *
     * @param args Command-line arguments specifying action and configuration.
     */
    public static void main(String[] args) {
        try {
            // CRITICAL: Initialize Common component first (needed for jvmxray.common.data property)
            org.jvmxray.platform.shared.init.CommonInitializer.getInstance();
            
            // Initialize LogService component BEFORE any logging
            LogServiceInitializer.getInstance();
            
            // Now safe to initialize logger
            logger = LoggerFactory.getLogger(LogService.class);
            
            // Validate dependencies are available
            validateDependencies();
            
            // Create and run the logservice instance
            new LogService().run(args);
        } catch (Exception e) {
            System.err.println("Failed to initialize LogService: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Validates that required dependencies are available on the classpath.
     */
    private static void validateDependencies() {
        String[] requiredClasses = {
            "ch.qos.logback.classic.LoggerContext",
            "org.slf4j.LoggerFactory",
            "org.apache.commons.cli.Options",
            "org.sqlite.SQLiteConfig",
            "com.zaxxer.hikari.HikariDataSource"
        };

        for (String className : requiredClasses) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.err.println("ERROR: Required dependency not found: " + className);
                System.err.println("Please ensure all dependencies are available on the classpath.");
                System.exit(1);
            }
        }
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

        // Add daemon mode option
        options.addOption(Option.builder(OPT_DAEMON_SHORT)
                .longOpt(OPT_DAEMON_LONG)
                .desc("Run in daemon mode (background process)")
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

        // Configure daemon mode
        daemonMode = cmd.hasOption(OPT_DAEMON_SHORT);

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
            "  logservice --start -p 9876\n" +
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

        options.addOption(Option.builder(OPT_DAEMON_SHORT)
                .longOpt(OPT_DAEMON_LONG)
                .desc("Run in daemon mode (background process)")
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

            // Initialize Event Aggregator component if available (optional)
            try {
                Class<?> initClass = Class.forName("org.jvmxray.platform.server.init.EventAggregatorInitializer");
                initClass.getMethod("getInstance").invoke(null);
                logger.info("Event Aggregator component initialized");
            } catch (ClassNotFoundException e) {
                logger.info("Event Aggregator not present; skipping initialization");
            } catch (Throwable t) {
                logger.warn("Event Aggregator initialization failed: {}", t.toString());
            }

            // Initialize servers
            initServers();

            // Write PID file
            writePidFile();

            System.out.println("LogService started successfully");
            System.out.println("Agent listener: localhost:" + agentPort);
            System.out.println("PID file: " + PID_FILE_PATH);

            // Handle daemon vs foreground mode
            if (daemonMode) {
                logger.info("Running in daemon mode - detaching from terminal");
                System.out.println("LogService running in daemon mode");
                
                // In daemon mode, redirect stdout/stderr to log files to prevent hanging
                redirectOutputToLogs();
                
                // Create a background thread to keep the service alive
                Thread daemonThread = new Thread(() -> {
                    try {
                        // Keep the daemon thread alive
                        synchronized (LogService.this) {
                            LogService.this.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Daemon thread interrupted - shutting down");
                    }
                });
                daemonThread.setDaemon(false); // Prevent JVM from exiting
                daemonThread.setName("LogService-Daemon");
                daemonThread.start();
                
            } else {
                // Foreground mode - wait for shutdown signal
                waitForShutdown();
            }

        } catch (Exception e) {
            logger.error("Failed to start LogService: {}", e.getMessage(), e);
            System.err.println("Failed to start LogService: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Stops the logservice using modern ProcessHandle API.
     */
    private void stopService() {
        try {
            if (!isServiceRunning()) {
                System.out.println("LogService is not running");
                return;
            }

            long pid = getRunningPid();
            System.out.println("Stopping LogService (PID: " + pid + ")...");

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
                            System.out.println("LogService stopped gracefully");
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
                            System.out.println("LogService force stopped");
                            cleanupPidFile();
                        } else {
                            System.err.println("Failed to stop LogService");
                            System.exit(1);
                        }
                    }
                } else {
                    System.err.println("Failed to initiate LogService shutdown");
                    System.exit(1);
                }
            } else {
                System.err.println("Cannot obtain process handle for PID: " + pid);
                // Clean up stale PID file
                cleanupPidFile();
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
            logger.debug("Error checking if service is running: {}", e.getMessage());
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
            logger.debug("Error reading PID file: {}", e.getMessage());
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
     * Initializes the Logback server for agent events.
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
        logbackServer = new SocketServer(loggerContext, agentPort);
        logbackServer.start();
        logger.info("SocketServer started on port {}", agentPort);


        // Log successful initialization
        logger.info("logservice is running.");
    }

    /**
     * Shuts down the Logback server gracefully.
     */
    private void shutdown() {
        // Log shutdown start
        logger.info("Shutting down logservice.");
        if (logbackServer != null) {
            // Log active connections before shutdown
            int activeConnections = logbackServer.getActiveConnectionCount();
            if (activeConnections > 0) {
                logger.info("Shutting down SocketServer with {} active agent connections", activeConnections);
            }
            // Stop the Logback server
            logbackServer.close();
            logger.info("SocketServer stopped.");
        }
    }

    /**
     * Redirects stdout and stderr to log files when running in daemon mode.
     * This prevents the daemon process from hanging when the parent terminal is closed.
     */
    private void redirectOutputToLogs() {
        try {
            String logDir = System.getProperty("jvmxray.logservice.logs", ".jvmxray/logservice/logs");
            Path logDirPath = Paths.get(logDir);
            
            // Ensure log directory exists
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }
            
            Path stdoutLog = logDirPath.resolve("logservice-stdout.log");
            Path stderrLog = logDirPath.resolve("logservice-stderr.log");
            
            // Redirect System.out and System.err to log files
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(stdoutLog.toFile(), true)));
            System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(stderrLog.toFile(), true)));
            
            logger.info("Redirected stdout to: {}", stdoutLog);
            logger.info("Redirected stderr to: {}", stderrLog);
            
        } catch (Exception e) {
            logger.warn("Failed to redirect output to log files: {}", e.getMessage());
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

}