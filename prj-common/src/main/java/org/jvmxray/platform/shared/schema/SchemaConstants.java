package org.jvmxray.platform.shared.schema;

/**
 * Constants for JVMXRay database schema definitions.
 * Contains table names, column names, and default values used across
 * different database implementations (Cassandra, MySQL, SQLite).
 * 
 * @author Milton Smith
 */
public final class SchemaConstants {
    
    private SchemaConstants() {
        // Utility class - prevent instantiation
    }
    
    // Database and Keyspace Names
    public static final String DEFAULT_KEYSPACE = "JVMXRAY";
    public static final String DEFAULT_DATABASE = "jvmxray";
    public static final int DEFAULT_REPLICATION_FACTOR = 1;
    
    // Table Names
    public static final String STAGE0_EVENT_TABLE = "STAGE0_EVENT";
    public static final String STAGE0_EVENT_KEYPAIR_TABLE = "STAGE0_EVENT_KEYPAIR";
    
    // STAGE0_EVENT Column Names
    public static final String COL_EVENT_ID = "EVENT_ID";
    public static final String COL_CONFIG_FILE = "CONFIG_FILE";
    public static final String COL_TIMESTAMP = "TIMESTAMP";
    public static final String COL_THREAD_ID = "THREAD_ID";
    public static final String COL_PRIORITY = "PRIORITY";
    public static final String COL_NAMESPACE = "NAMESPACE";
    public static final String COL_AID = "AID";
    public static final String COL_CID = "CID";
    public static final String COL_IS_STABLE = "IS_STABLE";
    
    // STAGE0_EVENT_KEYPAIR Column Names
    public static final String COL_KEY = "KEY";
    public static final String COL_VALUE = "VALUE";
    
    // Data Type Mappings
    public static final class DataTypes {
        // Common data types across databases
        public static final String VARCHAR_255 = "VARCHAR(255)";
        public static final String TEXT = "TEXT";
        public static final String BIGINT = "BIGINT";
        public static final String BOOLEAN = "BOOLEAN";
        public static final String INTEGER = "INTEGER";
        
        // Cassandra-specific types
        public static final class Cassandra {
            public static final String TEXT = "TEXT";
            public static final String BIGINT = "BIGINT";
            public static final String BOOLEAN = "BOOLEAN";
            public static final String VARCHAR = "VARCHAR";
        }
        
        // MySQL-specific types
        public static final class MySQL {
            public static final String VARCHAR_255 = "VARCHAR(255)";
            public static final String TEXT = "TEXT";
            public static final String BIGINT = "BIGINT";
            public static final String BOOLEAN = "BOOLEAN";
        }
        
        // SQLite-specific types
        public static final class SQLite {
            public static final String TEXT = "TEXT";
            public static final String INTEGER = "INTEGER";
            public static final String BOOLEAN = "INTEGER"; // SQLite uses INTEGER for boolean
        }
    }
    
    // SQL Query Templates
    public static final class SQLTemplates {
        
        // STAGE0_EVENT table creation templates
        public static final String CREATE_STAGE0_EVENT_MYSQL = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_TIMESTAMP + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_THREAD_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_PRIORITY + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_NAMESPACE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_AID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_CID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_IS_STABLE + " " + DataTypes.MySQL.BOOLEAN + " DEFAULT TRUE" +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_THREAD_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.SQLite.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_AID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_CID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_IS_STABLE + " " + DataTypes.SQLite.BOOLEAN + " DEFAULT 1" +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_CASSANDRA = 
            "CREATE TABLE IF NOT EXISTS %s." + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_THREAD_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_AID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_IS_STABLE + " " + DataTypes.Cassandra.BOOLEAN +
            ")";
        
        // STAGE0_EVENT_KEYPAIR table creation templates
        public static final String CREATE_STAGE0_EVENT_KEYPAIR_MYSQL = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_KEY + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_VALUE + " " + DataTypes.MySQL.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_KEYPAIR_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_KEY + " " + DataTypes.SQLite.TEXT + ", " +
            COL_VALUE + " " + DataTypes.SQLite.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_KEYPAIR_CASSANDRA = 
            "CREATE TABLE IF NOT EXISTS %s." + STAGE0_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_KEY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_VALUE + " " + DataTypes.Cassandra.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
            
        // Drop table statements
        public static final String DROP_STAGE0_EVENT = "DROP TABLE IF EXISTS " + STAGE0_EVENT_TABLE;
        public static final String DROP_STAGE0_EVENT_KEYPAIR = "DROP TABLE IF EXISTS " + STAGE0_EVENT_KEYPAIR_TABLE;
        public static final String DROP_STAGE0_EVENT_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE0_EVENT_TABLE;
        public static final String DROP_STAGE0_EVENT_KEYPAIR_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE0_EVENT_KEYPAIR_TABLE;
        
        // Table existence check queries
        public static final String CHECK_TABLE_EXISTS_MYSQL = 
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        public static final String CHECK_TABLE_EXISTS_SQLITE = 
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
    }
    
    // Configuration Properties
    public static final class Config {
        public static final String JVMXRAY_HOME_PROPERTY = "jvmxray.home";
        public static final String TEST_DB_RELATIVE_PATH = "/common/data/jvmxray-test.db";
        
        /**
         * Gets the default JVMXRay home directory using project detection logic.
         * 
         * <p>This method uses the following precedence order:</p>
         * <ol>
         *   <li>If {@code jvmxray.home} system property is set, use it directly</li>
         *   <li>Search upward from current working directory for project markers</li>
         *   <li>If project root found, return {@code projectRoot/.jvmxray}</li>
         *   <li>If no markers found, return {@code user.dir/.jvmxray}</li>
         *   <li>Final fallback: {@code user.home/.jvmxray}</li>
         * </ol>
         *
         * @return The resolved JVMXRay home directory path.
         */
        public static String getDefaultJvmxrayHome() {
            // Priority 1: Check for explicit jvmxray.home system property
            String explicitHome = System.getProperty(JVMXRAY_HOME_PROPERTY);
            if (explicitHome != null && !explicitHome.isEmpty()) {
                return explicitHome;
            }
            
            // Priority 2: Detect project root directory
            java.io.File projectRoot = detectProjectRoot();
            if (projectRoot != null) {
                return projectRoot.getAbsolutePath() + java.io.File.separator + ".jvmxray";
            }
            
            // Priority 3: Use current working directory
            String userDir = System.getProperty("user.dir");
            if (userDir != null && !userDir.isEmpty()) {
                return userDir + java.io.File.separator + ".jvmxray";
            }
            
            // Final fallback: Use user home directory
            return System.getProperty("user.home") + java.io.File.separator + ".jvmxray";
        }
        
        /**
         * Detects the project root directory by searching upward for project markers.
         * Enhanced to handle Maven multi-module projects by finding the root pom.xml.
         * 
         * @return The project root directory, or null if no project markers are found.
         */
        private static java.io.File detectProjectRoot() {
            // Start from current working directory
            String startPath = System.getProperty("user.dir");
            if (startPath == null || startPath.isEmpty()) {
                return null;
            }
            
            java.io.File currentDir = new java.io.File(startPath);
            java.io.File lastPomDir = null;
            
            // Search upward for project markers
            while (currentDir != null && currentDir.exists()) {
                // Check for Maven project marker
                if (new java.io.File(currentDir, "pom.xml").exists()) {
                    lastPomDir = currentDir;
                    // For Maven multi-module projects, keep searching for parent pom
                    java.io.File parentDir = currentDir.getParentFile();
                    if (parentDir != null && new java.io.File(parentDir, "pom.xml").exists()) {
                        // Parent also has pom.xml, continue searching
                        currentDir = parentDir;
                        continue;
                    }
                    // No parent pom.xml, this is the root
                    return currentDir;
                }
                
                // Check for Git repository marker (fallback)
                if (new java.io.File(currentDir, ".git").exists()) {
                    return currentDir;
                }
                
                // Check for Gradle project markers
                if (new java.io.File(currentDir, "build.gradle").exists() || 
                    new java.io.File(currentDir, "build.gradle.kts").exists()) {
                    return currentDir;
                }
                
                // Move up to parent directory
                currentDir = currentDir.getParentFile();
            }
            
            // Return the last directory with pom.xml if found
            return lastPomDir;
        }
    }
    
    // Special key names that are extracted from keypairs and stored as columns
    public static final String KEYPAIR_AID = "AID";
    public static final String KEYPAIR_CID = "CID";
    public static final String KEYPAIR_CALLER = "caller";
}