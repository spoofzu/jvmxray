package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.schema.*;

import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Command-line tool for managing JVMXRay database schemas.
 * Supports schema creation, validation, and management across
 * Cassandra, MySQL, and SQLite databases.
 * 
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --create-schema}: Create database schema</li>
 *   <li>{@code --drop-schema}: Drop existing schema</li>
 *   <li>{@code --validate-schema}: Validate schema exists and is correct</li>
 *   <li>{@code --database-type <type>}: Database type (cassandra|mysql|sqlite)</li>
 *   <li>{@code --connection-url <url>}: JDBC connection URL</li>
 *   <li>{@code --host <host>}: Database host</li>
 *   <li>{@code --port <port>}: Database port</li>
 *   <li>{@code --username <user>}: Database username</li>
 *   <li>{@code --password <pass>}: Database password</li>
 *   <li>{@code --database-name <name>}: Database/keyspace name</li>
 *   <li>{@code --datacenter <dc>}: Cassandra datacenter</li>
 *   <li>{@code --replication <factor>}: Cassandra replication factor</li>
 *   <li>{@code --help}: Display usage information</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * # Create SQLite schema for testing
 * java SchemaManager --create-schema --database-type sqlite --connection-url jdbc:sqlite:/tmp/test.db
 * 
 * # Create MySQL schema
 * java SchemaManager --create-schema --database-type mysql --connection-url jdbc:mysql://localhost:3306/jvmxray --username root --password secret --database-name jvmxray
 * 
 * # Create Cassandra schema
 * java SchemaManager --create-schema --database-type cassandra --host localhost --port 9042 --username cassandra --password cassandra --database-name jvmxray --datacenter datacenter1
 * 
 * # Validate existing schema
 * java SchemaManager --validate-schema --database-type sqlite --connection-url jdbc:sqlite:/tmp/test.db
 * }</pre>
 * 
 * @author Milton Smith
 */
public class SchemaManager {
    
    private static final Logger logger = Logger.getLogger(SchemaManager.class.getName());
    
    // Command-line option constants
    private static final String OPT_CREATE_SCHEMA = "create-schema";
    private static final String OPT_DROP_SCHEMA = "drop-schema";
    private static final String OPT_VALIDATE_SCHEMA = "validate-schema";
    private static final String OPT_DATABASE_TYPE = "database-type";
    private static final String OPT_CONNECTION_URL = "connection-url";
    private static final String OPT_HOST = "host";
    private static final String OPT_PORT = "port";
    private static final String OPT_USERNAME = "username";
    private static final String OPT_PASSWORD = "password";
    private static final String OPT_DATABASE_NAME = "database-name";
    private static final String OPT_DATACENTER = "datacenter";
    private static final String OPT_REPLICATION = "replication";
    private static final String OPT_HELP = "help";
    
    /**
     * Main entry point for the SchemaManager application.
     * 
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            new SchemaManager().run(args);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SchemaManager failed: " + e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Main application logic.
     * 
     * @param args Command-line arguments
     * @throws Exception if execution fails
     */
    public void run(String[] args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        
        try {
            CommandLine cmd = parser.parse(options, args);
            
            if (cmd.hasOption(OPT_HELP) || args.length == 0) {
                printHelp(options);
                return;
            }
            
            // Parse configuration from command line
            DatabaseSchemaFactory.DatabaseConfig config = parseConfiguration(cmd);
            
            // Create schema implementation
            AbstractDatabaseSchema schema = DatabaseSchemaFactory.createSchema(config);
            
            // Execute requested operation
            if (cmd.hasOption(OPT_CREATE_SCHEMA)) {
                createSchema(schema);
            } else if (cmd.hasOption(OPT_DROP_SCHEMA)) {
                dropSchema(schema);
            } else if (cmd.hasOption(OPT_VALIDATE_SCHEMA)) {
                validateSchema(schema);
            } else {
                throw new IllegalArgumentException("No operation specified. Use --help for usage information.");
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(options);
            throw e;
        }
    }
    
    /**
     * Create command-line options.
     */
    private Options createOptions() {
        Options options = new Options();
        
        // Operation options (mutually exclusive)
        options.addOption(Option.builder()
            .longOpt(OPT_CREATE_SCHEMA)
            .desc("Create database schema")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_DROP_SCHEMA)
            .desc("Drop existing schema (WARNING: destroys all data)")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_VALIDATE_SCHEMA)
            .desc("Validate schema exists and is correct")
            .build());
        
        // Database connection options
        options.addOption(Option.builder()
            .longOpt(OPT_DATABASE_TYPE)
            .hasArg()
            .argName("type")
            .desc("Database type (cassandra|mysql|sqlite)")
            .required()
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_CONNECTION_URL)
            .hasArg()
            .argName("url")
            .desc("JDBC connection URL (for MySQL/SQLite)")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_HOST)
            .hasArg()
            .argName("host")
            .desc("Database host (for Cassandra)")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_PORT)
            .hasArg()
            .argName("port")
            .type(Integer.class)
            .desc("Database port")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_USERNAME)
            .hasArg()
            .argName("user")
            .desc("Database username")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_PASSWORD)
            .hasArg()
            .argName("password")
            .desc("Database password")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_DATABASE_NAME)
            .hasArg()
            .argName("name")
            .desc("Database name (MySQL/SQLite) or keyspace name (Cassandra)")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_DATACENTER)
            .hasArg()
            .argName("datacenter")
            .desc("Cassandra datacenter name")
            .build());
            
        options.addOption(Option.builder()
            .longOpt(OPT_REPLICATION)
            .hasArg()
            .argName("factor")
            .type(Integer.class)
            .desc("Cassandra replication factor (default: 1)")
            .build());
        
        // Help option
        options.addOption(Option.builder()
            .longOpt(OPT_HELP)
            .desc("Display this help message")
            .build());
        
        return options;
    }
    
    /**
     * Parse database configuration from command line arguments.
     */
    private DatabaseSchemaFactory.DatabaseConfig parseConfiguration(CommandLine cmd) 
            throws IllegalArgumentException {
        
        String databaseType = cmd.getOptionValue(OPT_DATABASE_TYPE);
        String connectionUrl = cmd.getOptionValue(OPT_CONNECTION_URL);
        String host = cmd.getOptionValue(OPT_HOST);
        Integer port = null;
        if (cmd.hasOption(OPT_PORT)) {
            try {
                port = Integer.parseInt(cmd.getOptionValue(OPT_PORT));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number: " + cmd.getOptionValue(OPT_PORT));
            }
        }
        String username = cmd.getOptionValue(OPT_USERNAME);
        String password = cmd.getOptionValue(OPT_PASSWORD);
        String databaseName = cmd.getOptionValue(OPT_DATABASE_NAME);
        String datacenter = cmd.getOptionValue(OPT_DATACENTER);
        Integer replicationFactor = null;
        if (cmd.hasOption(OPT_REPLICATION)) {
            try {
                replicationFactor = Integer.parseInt(cmd.getOptionValue(OPT_REPLICATION));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid replication factor: " + cmd.getOptionValue(OPT_REPLICATION));
            }
        }
        
        return DatabaseSchemaFactory.createConfigFromParameters(
            databaseType, connectionUrl, host, port, username, password,
            databaseName, datacenter, replicationFactor);
    }
    
    /**
     * Execute schema creation.
     */
    private void createSchema(AbstractDatabaseSchema schema) throws SQLException {
        System.out.println("Creating JVMXRay database schema...");
        logger.info("Starting schema creation for " + schema.getDatabaseType());
        
        try {
            schema.createSchema();
            System.out.println("✓ Database schema created successfully");
            logger.info("Schema creation completed successfully");
        } catch (SQLException e) {
            System.err.println("✗ Failed to create database schema: " + e.getMessage());
            logger.log(Level.SEVERE, "Schema creation failed", e);
            throw e;
        }
    }
    
    /**
     * Execute schema dropping.
     */
    private void dropSchema(AbstractDatabaseSchema schema) throws SQLException {
        System.out.println("WARNING: This will permanently delete all JVMXRay data!");
        System.out.println("Dropping JVMXRay database schema...");
        logger.info("Starting schema dropping for " + schema.getDatabaseType());
        
        try {
            schema.dropSchema();
            System.out.println("✓ Database schema dropped successfully");
            logger.info("Schema dropping completed successfully");
        } catch (SQLException e) {
            System.err.println("✗ Failed to drop database schema: " + e.getMessage());
            logger.log(Level.SEVERE, "Schema dropping failed", e);
            throw e;
        }
    }
    
    /**
     * Execute schema validation.
     */
    private void validateSchema(AbstractDatabaseSchema schema) throws SQLException {
        System.out.println("Validating JVMXRay database schema...");
        logger.info("Starting schema validation for " + schema.getDatabaseType());
        
        try {
            boolean isValid = schema.validateSchema();
            if (isValid) {
                System.out.println("✓ Database schema is valid");
                logger.info("Schema validation passed");
            } else {
                System.err.println("✗ Database schema validation failed");
                logger.warning("Schema validation failed");
                throw new SQLException("Schema validation failed");
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to validate database schema: " + e.getMessage());
            logger.log(Level.SEVERE, "Schema validation failed", e);
            throw e;
        }
    }
    
    /**
     * Print usage help.
     */
    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        
        System.out.println("JVMXRay Database Schema Manager");
        System.out.println("==============================");
        System.out.println();
        
        formatter.printHelp(
            "java " + SchemaManager.class.getName() + " [OPTIONS]",
            "\\nManage JVMXRay database schemas across Cassandra, MySQL, and SQLite.\\n\\n",
            options,
            "\\nExamples:\\n" +
            "  # Create SQLite test schema\\n" +
            "  java SchemaManager --create-schema --database-type sqlite --connection-url jdbc:sqlite:/tmp/test.db\\n\\n" +
            "  # Create MySQL schema\\n" +
            "  java SchemaManager --create-schema --database-type mysql --connection-url jdbc:mysql://localhost:3306/jvmxray --username root --password secret --database-name jvmxray\\n\\n" +
            "  # Create Cassandra schema\\n" +
            "  java SchemaManager --create-schema --database-type cassandra --host localhost --port 9042 --username cassandra --password cassandra --database-name jvmxray --datacenter datacenter1\\n\\n" +
            "  # Validate existing schema\\n" +
            "  java SchemaManager --validate-schema --database-type sqlite --connection-url jdbc:sqlite:/tmp/test.db\\n",
            true
        );
    }
    
    /**
     * Demonstrate schema operations with a sample event.
     * This method can be used for testing purposes.
     */
    public static void demonstrateSchema(AbstractDatabaseSchema schema) throws SQLException {
        System.out.println("\\nDemonstrating schema with sample data...");
        
        // Create sample event
        EventParser.ParsedEvent sampleEvent = EventParser.createSampleEvent();
        System.out.println("Sample event: " + sampleEvent.toString());
        
        // TODO: Add sample data insertion logic here when data access layer is implemented
        System.out.println("Schema demonstration completed");
    }
}