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
    public static final String STAGE1_EVENT_TABLE = "STAGE1_EVENT";
    public static final String STAGE1_EVENT_KEYPAIR_TABLE = "STAGE1_EVENT_KEYPAIR";
    public static final String API_KEY_TABLE = "API_KEY";
    public static final String STAGE2_LIBRARY_TABLE = "STAGE2_LIBRARY";
    public static final String STAGE2_LIBRARY_CVE_TABLE = "STAGE2_LIBRARY_CVE";
    
    // Common Column Names
    public static final String COL_EVENT_ID = "EVENT_ID";
    public static final String COL_CONFIG_FILE = "CONFIG_FILE";
    public static final String COL_TIMESTAMP = "TIMESTAMP";
    public static final String COL_CURRENT_THREAD_ID = "CURRENT_THREAD_ID";
    public static final String COL_PRIORITY = "PRIORITY";
    public static final String COL_NAMESPACE = "NAMESPACE";
    public static final String COL_AID = "AID";
    public static final String COL_CID = "CID";
    public static final String COL_IS_STABLE = "IS_STABLE";
    public static final String COL_KEYPAIRS = "KEYPAIRS";
    
    // STAGE1_EVENT_KEYPAIR Column Names
    public static final String COL_KEY = "KEY";
    public static final String COL_VALUE = "VALUE";
    
    // API_KEY table column names
    public static final String COL_API_KEY = "API_KEY";
    public static final String COL_APP_NAME = "APP_NAME";
    public static final String COL_IS_SUSPENDED = "IS_SUSPENDED";
    public static final String COL_CREATED_AT = "CREATED_AT";
    public static final String COL_LAST_USED = "LAST_USED";

    // STAGE2_LIBRARY table column names
    public static final String COL_LIBRARY_ID = "LIBRARY_ID";
    public static final String COL_JARPATH = "JARPATH";
    public static final String COL_LIBRARY_NAME = "LIBRARY_NAME";
    public static final String COL_SHA256_HASH = "SHA256_HASH";
    public static final String COL_METHOD = "METHOD";
    public static final String COL_GROUP_ID = "GROUP_ID";
    public static final String COL_ARTIFACT_ID = "ARTIFACT_ID";
    public static final String COL_VERSION = "VERSION";
    public static final String COL_IMPL_TITLE = "IMPL_TITLE";
    public static final String COL_IMPL_VENDOR = "IMPL_VENDOR";
    public static final String COL_PACKAGE_NAMES = "PACKAGE_NAMES";
    public static final String COL_FIRST_SEEN = "FIRST_SEEN";
    public static final String COL_LAST_SEEN = "LAST_SEEN";
    public static final String COL_REMOVED_ON = "REMOVED_ON";
    public static final String COL_IS_ACTIVE = "IS_ACTIVE";

    // STAGE2_LIBRARY_CVE table column names
    public static final String COL_CVE_ID = "CVE_ID";
    public static final String COL_CVE_NAME = "CVE_NAME";
    public static final String COL_CVSS_SEVERITY = "CVSS_SEVERITY";
    public static final String COL_CVSS_V3 = "CVSS_V3";
    public static final String COL_DESCRIPTION = "DESCRIPTION";
    public static final String COL_PUBLISHED_DATE = "PUBLISHED_DATE";
    public static final String COL_LAST_MODIFIED = "LAST_MODIFIED";
    public static final String COL_AFFECTED_LIBRARIES = "AFFECTED_LIBRARIES";
    public static final String COL_FIXED_VERSIONS = "FIXED_VERSIONS";
    public static final String COL_REFERENCE_URLS = "REFERENCE_URLS";
    public static final String COL_CWE_IDS = "CWE_IDS";

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
            public static final String DECIMAL_3_1 = "DECIMAL";
        }
        
        // MySQL-specific types
        public static final class MySQL {
            public static final String VARCHAR_255 = "VARCHAR(255)";
            public static final String TEXT = "TEXT";
            public static final String BIGINT = "BIGINT";
            public static final String BOOLEAN = "BOOLEAN";
            public static final String DECIMAL_3_1 = "DECIMAL(3,1)";
        }
        
        // SQLite-specific types
        public static final class SQLite {
            public static final String TEXT = "TEXT";
            public static final String INTEGER = "INTEGER";
            public static final String BOOLEAN = "INTEGER"; // SQLite uses INTEGER for boolean
            public static final String DECIMAL_3_1 = "REAL"; // SQLite uses REAL for decimals
        }
    }
    
    // SQL Query Templates
    public static final class SQLTemplates {
        
        // STAGE0_EVENT table creation templates (raw events with KEYPAIRS column)
        public static final String CREATE_STAGE0_EVENT_MYSQL = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_TIMESTAMP + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_PRIORITY + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_NAMESPACE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_AID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_CID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_KEYPAIRS + " " + DataTypes.MySQL.TEXT +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.SQLite.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_AID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_CID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_KEYPAIRS + " " + DataTypes.SQLite.TEXT +
            ")";
            
        public static final String CREATE_STAGE0_EVENT_CASSANDRA = 
            "CREATE TABLE IF NOT EXISTS %s." + STAGE0_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_AID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_KEYPAIRS + " " + DataTypes.Cassandra.TEXT +
            ")";
        
        // STAGE1_EVENT table creation templates (processed events with IS_STABLE)
        public static final String CREATE_STAGE1_EVENT_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + STAGE1_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_TIMESTAMP + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_PRIORITY + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_NAMESPACE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_AID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_CID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_KEYPAIRS + " " + DataTypes.MySQL.TEXT + ", " +
            COL_IS_STABLE + " " + DataTypes.MySQL.BOOLEAN + " DEFAULT TRUE" +
            ")";
            
        public static final String CREATE_STAGE1_EVENT_SQLITE =
            "CREATE TABLE IF NOT EXISTS " + STAGE1_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.SQLite.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_AID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_CID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_KEYPAIRS + " " + DataTypes.SQLite.TEXT + ", " +
            COL_IS_STABLE + " " + DataTypes.SQLite.BOOLEAN + " DEFAULT 1" +
            ")";
            
        public static final String CREATE_STAGE1_EVENT_CASSANDRA =
            "CREATE TABLE IF NOT EXISTS %s." + STAGE1_EVENT_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_CONFIG_FILE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_TIMESTAMP + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_CURRENT_THREAD_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_PRIORITY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_NAMESPACE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_AID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_KEYPAIRS + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_IS_STABLE + " " + DataTypes.Cassandra.BOOLEAN +
            ")";
        
        // STAGE1_EVENT_KEYPAIR table creation templates (normalized keypairs)
        public static final String CREATE_STAGE1_EVENT_KEYPAIR_MYSQL = 
            "CREATE TABLE IF NOT EXISTS " + STAGE1_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_KEY + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_VALUE + " " + DataTypes.MySQL.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
            
        public static final String CREATE_STAGE1_EVENT_KEYPAIR_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + STAGE1_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_KEY + " " + DataTypes.SQLite.TEXT + ", " +
            COL_VALUE + " " + DataTypes.SQLite.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
            
        public static final String CREATE_STAGE1_EVENT_KEYPAIR_CASSANDRA = 
            "CREATE TABLE IF NOT EXISTS %s." + STAGE1_EVENT_KEYPAIR_TABLE + " (" +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_KEY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_VALUE + " " + DataTypes.Cassandra.TEXT + ", " +
            "PRIMARY KEY (" + COL_EVENT_ID + ", " + COL_KEY + ")" +
            ")";
        // API_KEY table creation templates
        public static final String CREATE_API_KEY_MYSQL = 
            "CREATE TABLE IF NOT EXISTS " + API_KEY_TABLE + " (" +
            COL_API_KEY + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_APP_NAME + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_IS_SUSPENDED + " " + DataTypes.MySQL.BOOLEAN + " DEFAULT FALSE, " +
            COL_CREATED_AT + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_LAST_USED + " " + DataTypes.MySQL.BIGINT +
            ")";
            
        public static final String CREATE_API_KEY_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + API_KEY_TABLE + " (" +
            COL_API_KEY + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_APP_NAME + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_IS_SUSPENDED + " " + DataTypes.SQLite.BOOLEAN + " DEFAULT 0, " +
            COL_CREATED_AT + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_LAST_USED + " " + DataTypes.SQLite.INTEGER +
            ")";
            
        public static final String CREATE_API_KEY_CASSANDRA =
            "CREATE TABLE IF NOT EXISTS %s." + API_KEY_TABLE + " (" +
            COL_API_KEY + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_APP_NAME + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_IS_SUSPENDED + " " + DataTypes.Cassandra.BOOLEAN + ", " +
            COL_CREATED_AT + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_LAST_USED + " " + DataTypes.Cassandra.BIGINT +
            ")";

        // STAGE2_LIBRARY table creation templates
        public static final String CREATE_STAGE2_LIBRARY_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + STAGE2_LIBRARY_TABLE + " (" +
            COL_LIBRARY_ID + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_EVENT_ID + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_AID + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_CID + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_JARPATH + " " + DataTypes.MySQL.TEXT + " NOT NULL, " +
            COL_LIBRARY_NAME + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_SHA256_HASH + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_METHOD + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_GROUP_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_ARTIFACT_ID + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_VERSION + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_IMPL_TITLE + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_IMPL_VENDOR + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            COL_PACKAGE_NAMES + " " + DataTypes.MySQL.TEXT + ", " +
            COL_FIRST_SEEN + " " + DataTypes.MySQL.BIGINT + " NOT NULL, " +
            COL_LAST_SEEN + " " + DataTypes.MySQL.BIGINT + " NOT NULL, " +
            COL_REMOVED_ON + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_IS_ACTIVE + " " + DataTypes.MySQL.BOOLEAN + " DEFAULT TRUE, " +
            "INDEX idx_aid_active (" + COL_AID + ", " + COL_IS_ACTIVE + "), " +
            "INDEX idx_event_id (" + COL_EVENT_ID + "), " +
            "INDEX idx_sha256_hash (" + COL_SHA256_HASH + "), " +
            "INDEX idx_maven_coords (" + COL_GROUP_ID + ", " + COL_ARTIFACT_ID + ", " + COL_VERSION + "), " +
            "INDEX idx_first_seen (" + COL_FIRST_SEEN + "), " +
            "INDEX idx_jarpath (" + COL_JARPATH + "(255))" +
            ")";

        public static final String CREATE_STAGE2_LIBRARY_SQLITE =
            "CREATE TABLE IF NOT EXISTS " + STAGE2_LIBRARY_TABLE + " (" +
            COL_LIBRARY_ID + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_EVENT_ID + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_AID + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_CID + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_JARPATH + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_LIBRARY_NAME + " " + DataTypes.SQLite.TEXT + ", " +
            COL_SHA256_HASH + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_METHOD + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_GROUP_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_ARTIFACT_ID + " " + DataTypes.SQLite.TEXT + ", " +
            COL_VERSION + " " + DataTypes.SQLite.TEXT + ", " +
            COL_IMPL_TITLE + " " + DataTypes.SQLite.TEXT + ", " +
            COL_IMPL_VENDOR + " " + DataTypes.SQLite.TEXT + ", " +
            COL_PACKAGE_NAMES + " " + DataTypes.SQLite.TEXT + ", " +
            COL_FIRST_SEEN + " " + DataTypes.SQLite.INTEGER + " NOT NULL, " +
            COL_LAST_SEEN + " " + DataTypes.SQLite.INTEGER + " NOT NULL, " +
            COL_REMOVED_ON + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_IS_ACTIVE + " " + DataTypes.SQLite.BOOLEAN + " DEFAULT 1" +
            ")";

        public static final String CREATE_STAGE2_LIBRARY_CASSANDRA =
            "CREATE TABLE IF NOT EXISTS %s." + STAGE2_LIBRARY_TABLE + " (" +
            COL_LIBRARY_ID + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_EVENT_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_AID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_JARPATH + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_LIBRARY_NAME + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_SHA256_HASH + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_METHOD + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_GROUP_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_ARTIFACT_ID + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_VERSION + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_IMPL_TITLE + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_IMPL_VENDOR + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_PACKAGE_NAMES + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_FIRST_SEEN + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_LAST_SEEN + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_REMOVED_ON + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_IS_ACTIVE + " " + DataTypes.Cassandra.BOOLEAN +
            ")";

        // STAGE2_LIBRARY_CVE table creation templates
        public static final String CREATE_STAGE2_LIBRARY_CVE_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + STAGE2_LIBRARY_CVE_TABLE + " (" +
            COL_CVE_ID + " " + DataTypes.MySQL.VARCHAR_255 + " PRIMARY KEY, " +
            COL_CVE_NAME + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_CVSS_SEVERITY + " " + DataTypes.MySQL.VARCHAR_255 + " NOT NULL, " +
            COL_CVSS_V3 + " " + DataTypes.MySQL.DECIMAL_3_1 + " NOT NULL, " +
            COL_DESCRIPTION + " " + DataTypes.MySQL.TEXT + " NOT NULL, " +
            COL_PUBLISHED_DATE + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_LAST_MODIFIED + " " + DataTypes.MySQL.BIGINT + ", " +
            COL_AFFECTED_LIBRARIES + " " + DataTypes.MySQL.TEXT + ", " +
            COL_FIXED_VERSIONS + " " + DataTypes.MySQL.TEXT + ", " +
            COL_REFERENCE_URLS + " " + DataTypes.MySQL.TEXT + ", " +
            COL_CWE_IDS + " " + DataTypes.MySQL.VARCHAR_255 + ", " +
            "INDEX idx_cvss_severity (" + COL_CVSS_SEVERITY + "), " +
            "INDEX idx_cvss_v3 (" + COL_CVSS_V3 + "), " +
            "INDEX idx_published_date (" + COL_PUBLISHED_DATE + ")" +
            ")";

        public static final String CREATE_STAGE2_LIBRARY_CVE_SQLITE =
            "CREATE TABLE IF NOT EXISTS " + STAGE2_LIBRARY_CVE_TABLE + " (" +
            COL_CVE_ID + " " + DataTypes.SQLite.TEXT + " PRIMARY KEY, " +
            COL_CVE_NAME + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_CVSS_SEVERITY + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_CVSS_V3 + " " + DataTypes.SQLite.DECIMAL_3_1 + " NOT NULL, " +
            COL_DESCRIPTION + " " + DataTypes.SQLite.TEXT + " NOT NULL, " +
            COL_PUBLISHED_DATE + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_LAST_MODIFIED + " " + DataTypes.SQLite.INTEGER + ", " +
            COL_AFFECTED_LIBRARIES + " " + DataTypes.SQLite.TEXT + ", " +
            COL_FIXED_VERSIONS + " " + DataTypes.SQLite.TEXT + ", " +
            COL_REFERENCE_URLS + " " + DataTypes.SQLite.TEXT + ", " +
            COL_CWE_IDS + " " + DataTypes.SQLite.TEXT +
            ")";

        public static final String CREATE_STAGE2_LIBRARY_CVE_CASSANDRA =
            "CREATE TABLE IF NOT EXISTS %s." + STAGE2_LIBRARY_CVE_TABLE + " (" +
            COL_CVE_ID + " " + DataTypes.Cassandra.TEXT + " PRIMARY KEY, " +
            COL_CVE_NAME + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CVSS_SEVERITY + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CVSS_V3 + " " + DataTypes.Cassandra.DECIMAL_3_1 + ", " +
            COL_DESCRIPTION + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_PUBLISHED_DATE + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_LAST_MODIFIED + " " + DataTypes.Cassandra.BIGINT + ", " +
            COL_AFFECTED_LIBRARIES + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_FIXED_VERSIONS + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_REFERENCE_URLS + " " + DataTypes.Cassandra.TEXT + ", " +
            COL_CWE_IDS + " " + DataTypes.Cassandra.TEXT +
            ")";

        // Drop table statements
        public static final String DROP_STAGE0_EVENT = "DROP TABLE IF EXISTS " + STAGE0_EVENT_TABLE;
        public static final String DROP_STAGE1_EVENT = "DROP TABLE IF EXISTS " + STAGE1_EVENT_TABLE;
        public static final String DROP_STAGE1_EVENT_KEYPAIR = "DROP TABLE IF EXISTS " + STAGE1_EVENT_KEYPAIR_TABLE;
        public static final String DROP_STAGE2_LIBRARY = "DROP TABLE IF EXISTS " + STAGE2_LIBRARY_TABLE;
        public static final String DROP_STAGE2_LIBRARY_CVE = "DROP TABLE IF EXISTS " + STAGE2_LIBRARY_CVE_TABLE;
        public static final String DROP_STAGE0_EVENT_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE0_EVENT_TABLE;
        public static final String DROP_STAGE1_EVENT_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE1_EVENT_TABLE;
        public static final String DROP_STAGE1_EVENT_KEYPAIR_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE1_EVENT_KEYPAIR_TABLE;
        public static final String DROP_STAGE2_LIBRARY_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE2_LIBRARY_TABLE;
        public static final String DROP_STAGE2_LIBRARY_CVE_CASSANDRA = "DROP TABLE IF EXISTS %s." + STAGE2_LIBRARY_CVE_TABLE;
        public static final String DROP_API_KEY = "DROP TABLE IF EXISTS " + API_KEY_TABLE;
        public static final String DROP_API_KEY_CASSANDRA = "DROP TABLE IF EXISTS %s." + API_KEY_TABLE;
        
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