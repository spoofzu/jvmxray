package org.jvmxray.platform.server.bin;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

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
 *   <li>{@code -p, --port <port>}: Port for the agent event listener (default: 9876).</li>
 *   <li>{@code -r, --restport <port>}: Port for the REST server (default: 8080).</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * java org.jvmxray.platform.server.bin.logservice -p 9876 -r 8080
 * }</pre>
 *
 * @author Milton Smith
 */
public class logservice {

    // SLF4J logger for the logservice application
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.server.bin.logservice");

    // Command-line option constants for agent port
    private static final String OPT_PORT_SHORT = "p";
    private static final String OPT_PORT_LONG = "port";
    private static final int DEFAULT_AGENT_PORT = 9876;

    // Command-line option constants for REST server port
    private static final String OPT_REST_PORT_SHORT = "r";
    private static final String OPT_REST_PORT_LONG = "restport";
    private static final int DEFAULT_REST_PORT = 8080;

    // Port for the Logback agent event listener
    private int agentPort = DEFAULT_AGENT_PORT;
    // Port for the REST server
    private int restPort = DEFAULT_REST_PORT;

    // Logback server instance for receiving agent events
    private SimpleSocketServer logbackServer;
    // HTTP server instance for REST API
    private HttpServer restServer;

    /**
     * Main entry point for the logservice application.
     *
     * @param args Command-line arguments specifying ports (e.g., {@code -p 9876 -r 8080}).
     */
    public static void main(String[] args) {
        // Create and run the logservice instance
        new logservice().run(args);
    }

    /**
     * Runs the logservice application, parsing command-line arguments, initializing servers,
     * and waiting for shutdown.
     *
     * @param args Command-line arguments.
     */
    private void run(String[] args) {
        try {
            // Parse command-line arguments to configure ports
            parseCommandLine(args);
            // Initialize Logback and REST servers
            initServers();
            // Wait for shutdown signal
            waitForShutdown();
        } catch (Exception e) {
            // Log fatal errors and exit with error code
            logger.error("Fatal error occurred: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to configure the agent listener and REST server ports.
     *
     * @param args Command-line arguments.
     * @throws ParseException If the arguments are invalid or cannot be parsed.
     */
    private void parseCommandLine(String[] args) throws ParseException {
        // Define command-line options
        Options options = new Options();

        // Add agent port option
        options.addOption(Option.builder(OPT_PORT_SHORT)
                .longOpt(OPT_PORT_LONG)
                .desc("Port number for the Agent event listener.")
                .hasArg()
                .argName("AGENT_PORT")
                .build());

        // Add REST server port option
        options.addOption(Option.builder(OPT_REST_PORT_SHORT)
                .longOpt(OPT_REST_PORT_LONG)
                .desc("Port number for the REST server.")
                .hasArg()
                .argName("REST_PORT")
                .build());

        // Parse command-line arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Configure agent port from command-line or use default
        if (cmd.hasOption(OPT_PORT_SHORT)) {
            try {
                agentPort = Integer.parseInt(cmd.getOptionValue(OPT_PORT_SHORT));
            } catch (NumberFormatException e) {
                // Log warning and revert to default if invalid
                logger.warn("Invalid agent port number provided. Using default port {}", DEFAULT_AGENT_PORT);
                agentPort = DEFAULT_AGENT_PORT;
            }
        }

        // Configure REST server port from command-line or use default
        if (cmd.hasOption(OPT_REST_PORT_SHORT)) {
            try {
                restPort = Integer.parseInt(cmd.getOptionValue(OPT_REST_PORT_SHORT));
            } catch (NumberFormatException e) {
                // Log warning and revert to default if invalid
                logger.warn("Invalid REST port number provided. Using default port {}", DEFAULT_REST_PORT);
                restPort = DEFAULT_REST_PORT;
            }
        }

        // Log configured ports
        logger.info("Agent server port set to {}", agentPort);
        logger.info("REST server port set to {}", restPort);
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
        logbackServer = new SimpleSocketServer(loggerContext, agentPort);
        logbackServer.start();
        logger.info("Logback server started on port {}", agentPort);

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
            // Stop the Logback server
            logbackServer.close();
            logger.info("Logback server stopped.");
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