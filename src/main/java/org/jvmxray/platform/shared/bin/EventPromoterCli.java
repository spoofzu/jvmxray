package org.jvmxray.platform.shared.bin;

import org.jvmxray.platform.shared.schema.EventPromoter;
import org.jvmxray.platform.shared.schema.SchemaConstants;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI entry point for promoting STAGE0_EVENT data to STAGE1_EVENT and STAGE1_EVENT_KEYPAIR.
 *
 * <p>Auto-detects the SQLite database location using the standard JVMXRay home
 * directory resolution. Requires no parameters by default.</p>
 *
 * <p><b>Database Resolution Order:</b></p>
 * <ol>
 *   <li>Command-line argument: {@code jdbc:sqlite:/path/to/db}</li>
 *   <li>{@code jvmxray.home} system property: {@code {jvmxray.home}/agent/data/jvmxray-events.db}</li>
 *   <li>Project root detection: {@code {projectRoot}/.jvmxray/agent/data/jvmxray-events.db}</li>
 * </ol>
 *
 * @author Milton Smith
 */
public class EventPromoterCli {

    private static final Logger logger = Logger.getLogger(EventPromoterCli.class.getName());
    private static final String DEFAULT_DB_RELATIVE_PATH = "/agent/data/jvmxray-events.db";
    private static final String TEST_DB_RELATIVE_PATH = "/target/test-jvmxray/common/data/jvmxray-test.db";

    public static void main(String[] args) {
        try {
            String connectionUrl = resolveConnectionUrl(args);
            System.out.println("Database: " + connectionUrl);

            Class.forName("org.sqlite.JDBC");
            try (Connection connection = DriverManager.getConnection(connectionUrl)) {
                int promoted = EventPromoter.promoteEvents(connection);
                System.out.println("Promoted " + promoted + " events from STAGE0 to STAGE1");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Event promotion failed: " + e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String resolveConnectionUrl(String[] args) {
        // Priority 1: explicit connection URL argument
        if (args.length > 0) {
            String arg = args[0];
            if (arg.startsWith("jdbc:sqlite:")) {
                return arg;
            }
            // Treat bare path as a file path
            return "jdbc:sqlite:" + arg;
        }

        // Priority 2: production database via jvmxray home
        String jvmxrayHome = SchemaConstants.Config.getDefaultJvmxrayHome();
        String prodPath = jvmxrayHome + DEFAULT_DB_RELATIVE_PATH;
        if (new File(prodPath).exists()) {
            return "jdbc:sqlite:" + prodPath;
        }

        // Priority 3: test database under project root
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome != null && !testHome.isEmpty()) {
            String testPath = testHome + "/common/data/jvmxray-test.db";
            if (new File(testPath).exists()) {
                return "jdbc:sqlite:" + testPath;
            }
        }

        // Priority 4: detect project root and look for test db
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            String testPath = userDir + TEST_DB_RELATIVE_PATH;
            if (new File(testPath).exists()) {
                return "jdbc:sqlite:" + testPath;
            }
        }

        System.err.println("No database found. Searched:");
        System.err.println("  " + prodPath);
        if (userDir != null) {
            System.err.println("  " + userDir + TEST_DB_RELATIVE_PATH);
        }
        System.err.println("Usage: etl-stage0-to-stage1.sh [jdbc:sqlite:/path/to/db | /path/to/db]");
        System.exit(1);
        return null;
    }
}
