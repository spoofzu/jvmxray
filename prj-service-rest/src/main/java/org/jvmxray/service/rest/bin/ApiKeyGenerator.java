package org.jvmxray.service.rest.bin;

import org.apache.commons.cli.*;
import org.jvmxray.service.rest.init.RestServiceInitializer;
import org.jvmxray.service.rest.util.ApiKeyManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone CLI utility for managing JVMXRay API keys.
 * Provides secure command-line interface for creating, listing, revoking, and checking API keys.
 * Must be run locally on the server with database access for security.
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --help}: Display usage information and exit.</li>
 *   <li>{@code --list}: List all API keys in the database.</li>
 *   <li>{@code --revoke <api_key>}: Revoke (suspend) the specified API key.</li>
 *   <li>{@code --check <api_key>}: Check if the specified API key exists and is active.</li>
 *   <li>{@code --database <url>}: Database connection URL (optional, defaults to standard location).</li>
 *   <li>{@code <app_name>}: Generate a new API key for the specified application (default action).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.service.rest.bin.ApiKeyGenerator myapp
 * java org.jvmxray.service.rest.bin.ApiKeyGenerator --list
 * java org.jvmxray.service.rest.bin.ApiKeyGenerator --revoke jvmxray_abc123
 * java org.jvmxray.service.rest.bin.ApiKeyGenerator --check jvmxray_abc123
 * java org.jvmxray.service.rest.bin.ApiKeyGenerator --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class ApiKeyGenerator {

    private static final Logger logger = Logger.getLogger(ApiKeyGenerator.class.getName());

    // Command-line option constants
    private static final String OPT_HELP = "help";
    private static final String OPT_LIST = "list";
    private static final String OPT_REVOKE = "revoke";
    private static final String OPT_CHECK = "check";
    private static final String OPT_DATABASE = "database";

    private String databaseUrl;
    private String action;
    private String appName;
    private String apiKey;

    /**
     * Main method to run the API key generator.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            new ApiKeyGenerator().run(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.log(Level.SEVERE, "Fatal error occurred", e);
            System.exit(1);
        }
    }

    /**
     * Runs the API key generator, parsing command-line arguments and executing the requested action.
     *
     * @param args Command-line arguments.
     */
    private void run(String[] args) throws Exception {
        // Parse command-line arguments
        parseCommandLine(args);

        // Initialize database connection
        initializeDatabase();

        // Execute the requested action
        switch (action) {
            case "generate":
                generateApiKey();
                break;
            case OPT_LIST:
                listApiKeys();
                break;
            case OPT_REVOKE:
                revokeApiKey();
                break;
            case OPT_CHECK:
                checkApiKey();
                break;
            case OPT_HELP:
                displayHelp();
                break;
            default:
                System.err.println("Unknown action: " + action);
                displayHelp();
                System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to determine action and configure options.
     *
     * @param args Command-line arguments.
     * @throws ParseException If the arguments are invalid or cannot be parsed.
     */
    private void parseCommandLine(String[] args) throws ParseException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Check for help first
        if (cmd.hasOption(OPT_HELP)) {
            action = OPT_HELP;
            return;
        }

        // Configure database URL if provided
        if (cmd.hasOption(OPT_DATABASE)) {
            databaseUrl = cmd.getOptionValue(OPT_DATABASE);
        }

        // Determine action based on options
        if (cmd.hasOption(OPT_LIST)) {
            action = OPT_LIST;
        } else if (cmd.hasOption(OPT_REVOKE)) {
            action = OPT_REVOKE;
            apiKey = cmd.getOptionValue(OPT_REVOKE);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new ParseException("API key is required for revoke operation");
            }
        } else if (cmd.hasOption(OPT_CHECK)) {
            action = OPT_CHECK;
            apiKey = cmd.getOptionValue(OPT_CHECK);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new ParseException("API key is required for check operation");
            }
        } else {
            // Check if any arguments were provided
            String[] remainingArgs = cmd.getArgs();
            if (remainingArgs.length == 0) {
                // No arguments provided - show help instead of throwing error
                action = OPT_HELP;
            } else {
                // Arguments provided - attempt to generate API key
                action = "generate";
                if (remainingArgs.length > 1) {
                    throw new ParseException("Too many arguments provided. Expected: <app_name>");
                }
                appName = remainingArgs[0].trim();
                if (appName.isEmpty()) {
                    throw new ParseException("Application name cannot be empty");
                }
            }
        }
    }

    /**
     * Creates command line options.
     *
     * @return Command line options
     */
    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt(OPT_HELP)
                .desc("Display this help message and exit")
                .build());

        options.addOption(Option.builder()
                .longOpt(OPT_LIST)
                .desc("List all API keys in the database")
                .build());

        options.addOption(Option.builder()
                .longOpt(OPT_REVOKE)
                .hasArg()
                .argName("API_KEY")
                .desc("Revoke (suspend) the specified API key")
                .build());

        options.addOption(Option.builder()
                .longOpt(OPT_CHECK)
                .hasArg()
                .argName("API_KEY")
                .desc("Check if the specified API key exists and is active")
                .build());

        options.addOption(Option.builder()
                .longOpt(OPT_DATABASE)
                .hasArg()
                .argName("URL")
                .desc("Database connection URL (optional, defaults to standard location)")
                .build());

        return options;
    }

    /**
     * Displays help information and usage examples.
     */
    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("generate-api-key",
            "\nJVMXRay API Key Generator - Secure command-line API key management\n\n" +
            "SECURITY: This tool must be run locally on the server with database access.\n" +
            "API key generation requires administrator privileges for security.\n\n" +
            "Key Management:\n" +
            "  generate-api-key <app_name>        Generate new API key\n" +
            "  generate-api-key --list            List all API keys\n" +
            "  generate-api-key --revoke <key>    Revoke an API key\n" +
            "  generate-api-key --check <key>     Check API key status\n" +
            "  generate-api-key --help            Show this help\n\n" +
            "Options:",
            createOptions(),
            "\nExamples:\n" +
            "  generate-api-key myapp\n" +
            "  generate-api-key --list\n" +
            "  generate-api-key --revoke jvmxray_abc123def456\n" +
            "  generate-api-key --check jvmxray_abc123def456\n" +
            "  generate-api-key --database jdbc:sqlite:/custom/path/db.sqlite myapp\n");
    }

    /**
     * Initializes the database connection URL if not provided.
     */
    private void initializeDatabase() throws Exception {
        if (databaseUrl == null) {
            // Use the standard database location
            RestServiceInitializer initializer = RestServiceInitializer.getInstance();
            initializer.initialize();
            String jvmxrayHome = initializer.getJvmxrayHome().toString();
            databaseUrl = "jdbc:sqlite:" + jvmxrayHome + "/common/data/jvmxray-test.db";
        }
        logger.info("Using database: " + databaseUrl);
    }

    /**
     * Generates a new API key for the specified application.
     */
    private void generateApiKey() throws SQLException {
        String newApiKey = ApiKeyManager.generateApiKeyString();
        ApiKeyManager.insertApiKey(databaseUrl, newApiKey, appName);

        System.out.println("API Key generated successfully:");
        System.out.println("Application: " + appName);
        System.out.println("API Key: " + newApiKey);
        System.out.println();
        System.out.println("Use this API key in the X-API-Key header for REST API requests.");
        System.out.println("Store this key securely - it cannot be retrieved again.");
    }

    /**
     * Lists all API keys in the database.
     */
    private void listApiKeys() throws SQLException {
        List<ApiKeyManager.ApiKey> keys = ApiKeyManager.listApiKeys(databaseUrl);

        if (keys.isEmpty()) {
            System.out.println("No API keys found in database.");
            return;
        }

        System.out.println("API Keys (" + keys.size() + " total):");
        System.out.println();
        System.out.printf("%-30s %-20s %-10s %-20s %-20s%n",
                "API Key", "Application", "Status", "Created", "Last Used");
        System.out.println("-".repeat(100));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (ApiKeyManager.ApiKey key : keys) {
            String status = key.isSuspended() ? "REVOKED" : "ACTIVE";
            String created = sdf.format(key.getCreatedAt());
            String lastUsed = key.getLastUsed() != null ? sdf.format(key.getLastUsed()) : "Never";

            // Truncate long API keys for display
            String displayKey = key.getKey().length() > 28 ?
                    key.getKey().substring(0, 25) + "..." : key.getKey();

            System.out.printf("%-30s %-20s %-10s %-20s %-20s%n",
                    displayKey, key.getAppName(), status, created, lastUsed);
        }
    }

    /**
     * Revokes the specified API key.
     */
    private void revokeApiKey() throws SQLException {
        boolean revoked = ApiKeyManager.revokeApiKey(databaseUrl, apiKey);

        if (revoked) {
            System.out.println("API key revoked successfully: " + apiKey);
            System.out.println("This key can no longer be used for authentication.");
        } else {
            System.out.println("API key not found: " + apiKey);
            System.exit(1);
        }
    }

    /**
     * Checks the specified API key status.
     */
    private void checkApiKey() throws SQLException {
        ApiKeyManager.ApiKey key = ApiKeyManager.checkApiKey(databaseUrl, apiKey);

        if (key == null) {
            System.out.println("API key not found: " + apiKey);
            System.exit(1);
            return;
        }

        System.out.println("API Key Information:");
        System.out.println("API Key: " + key.getKey());
        System.out.println("Application: " + key.getAppName());
        System.out.println("Status: " + (key.isSuspended() ? "REVOKED" : "ACTIVE"));
        System.out.println("Created: " + key.getCreatedAt());
        System.out.println("Last Used: " + (key.getLastUsed() != null ? key.getLastUsed() : "Never"));

        if (key.isSuspended()) {
            System.out.println("\nWARNING: This API key has been revoked and cannot be used for authentication.");
            System.exit(1);
        } else {
            System.out.println("\nThis API key is active and can be used for authentication.");
        }
    }
}