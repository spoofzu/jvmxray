package org.jvmxray.service.log.init;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.jvmxray.service.log.init.LogServiceInitializer;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Unit tests for LogServiceInitializer to ensure proper component initialization.
 * 
 * This test class validates that LogServiceInitializer correctly:
 * - Creates the required directory structure (.jvmxray/logservice/config/ and .jvmxray/logservice/logs/)
 * - Copies logback.xml template from resources
 * - Creates logservice.properties with default values
 * - Sets appropriate system properties
 * - Follows the singleton pattern correctly
 * 
 * <p>This test also ensures that the LogService component directories are created
 * during the Maven build process, maintaining consistency with other JVMXRay components.</p>
 * 
 * @author JVMXRay LogService Team
 */
public class LogServiceInitializerTest {

    private static Path testHome;
    private static Path logServiceHome;
    private static Path configDir;
    private static Path logsDir;
    
    @BeforeClass
    public static void setUpTestEnvironment() throws Exception {
        System.out.println("Initializing LogService Test Environment...");
        
        // Set project.basedir to ensure directory is created at project root, not module root
        String baseDir = System.getProperty("user.dir");
        if (baseDir.endsWith("/prj-service-log")) {
            // We're running from the module directory, set basedir to parent
            baseDir = Paths.get(baseDir).getParent().toString();
            System.setProperty("project.basedir", baseDir);
        }
        
        // Initialize the LogService component directories and configuration
        LogServiceInitializer.getInstance();
        
        // Get the test directories for validation
        String testHomeProperty = System.getProperty("jvmxray.test.home");
        if (testHomeProperty != null) {
            testHome = Paths.get(testHomeProperty);
        } else {
            String projectBaseDir = System.getProperty("project.basedir", System.getProperty("user.dir"));
            testHome = Paths.get(projectBaseDir, ".jvmxray");
        }
        
        logServiceHome = testHome.resolve("logservice");
        configDir = logServiceHome.resolve("config");
        logsDir = logServiceHome.resolve("logs");
        
        System.out.println("✓ LogService test environment initialized");
        System.out.println("  - Test home: " + testHome);
        System.out.println("  - Project base: " + System.getProperty("project.basedir"));
        System.out.println("  - LogService home: " + logServiceHome);
        System.out.println("  - Config dir: " + configDir);
        System.out.println("  - Logs dir: " + logsDir);
    }
    
    @Before
    public void setUp() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("LogService Initialization Test");
        System.out.println("=".repeat(60));
    }
    
    @After
    public void tearDown() throws Exception {
        System.out.println("LogService initialization test completed.");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Tests that LogServiceInitializer creates the required directory structure.
     */
    @Test
    public void testDirectoryStructureCreated() throws Exception {
        System.out.println("Testing directory structure creation...");
        
        // Verify that all required directories exist
        assertTrue("LogService home directory should exist", Files.exists(logServiceHome));
        assertTrue("Config directory should exist", Files.exists(configDir));
        assertTrue("Logs directory should exist", Files.exists(logsDir));
        
        assertTrue("LogService home should be a directory", Files.isDirectory(logServiceHome));
        assertTrue("Config should be a directory", Files.isDirectory(configDir));
        assertTrue("Logs should be a directory", Files.isDirectory(logsDir));
        
        System.out.println("✓ Directory structure validated");
    }
    
    /**
     * Tests that the logback.xml configuration is copied from the template.
     */
    @Test
    public void testLogbackConfigurationCopied() throws Exception {
        System.out.println("Testing logback configuration copy...");
        
        Path logbackConfig = configDir.resolve("logback.xml");
        assertTrue("logback.xml should exist", Files.exists(logbackConfig));
        assertTrue("logback.xml should be a file", Files.isRegularFile(logbackConfig));
        
        // Verify the content contains expected LogService configuration
        String content = Files.readString(logbackConfig);
        assertTrue("logback.xml should contain LogService configuration", 
                   content.contains("JVMXRay LogService"));
        assertTrue("logback.xml should reference logservice logs property", 
                   content.contains("${jvmxray.logservice.logs}"));
        
        System.out.println("✓ Logback configuration validated");
    }
    
    /**
     * Tests that the logservice.properties file is created with default values.
     */
    @Test
    public void testPropertiesFileCreated() throws Exception {
        System.out.println("Testing properties file creation...");
        
        Path propertiesFile = configDir.resolve("logservice.properties");
        assertTrue("logservice.properties should exist", Files.exists(propertiesFile));
        assertTrue("logservice.properties should be a file", Files.isRegularFile(propertiesFile));
        
        // Load and validate properties
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(propertiesFile)) {
            props.load(is);
        }
        
        // Verify key properties exist
        assertNotNull("AID property should exist", props.getProperty("AID"));
        assertEquals("CID property should be logservice", "logservice", props.getProperty("CID"));
        assertEquals("Default agent port should be 9876", "9876", props.getProperty("logservice.agent.port"));
        assertEquals("Default agent host should be localhost", "localhost", props.getProperty("logservice.agent.host"));
        
        System.out.println("✓ Properties file validated");
    }
    
    /**
     * Tests that system properties are set correctly.
     */
    @Test
    public void testSystemPropertiesSet() throws Exception {
        System.out.println("Testing system properties...");
        
        String logsProperty = System.getProperty("jvmxray.logservice.logs");
        String configProperty = System.getProperty("jvmxray.logservice.config");
        
        assertNotNull("jvmxray.logservice.logs should be set", logsProperty);
        assertNotNull("jvmxray.logservice.config should be set", configProperty);
        
        assertTrue("Logs property should point to logs directory", 
                   logsProperty.endsWith("logservice/logs") || logsProperty.endsWith("logservice\\logs"));
        assertTrue("Config property should point to config directory", 
                   configProperty.endsWith("logservice/config") || configProperty.endsWith("logservice\\config"));
        
        System.out.println("✓ System properties validated");
    }
    
    /**
     * Tests the singleton pattern implementation.
     */
    @Test
    public void testSingletonPattern() throws Exception {
        System.out.println("Testing singleton pattern...");
        
        LogServiceInitializer instance1 = LogServiceInitializer.getInstance();
        LogServiceInitializer instance2 = LogServiceInitializer.getInstance();
        
        assertNotNull("First instance should not be null", instance1);
        assertNotNull("Second instance should not be null", instance2);
        assertSame("Both instances should be the same", instance1, instance2);
        
        assertTrue("Initializer should report as initialized", LogServiceInitializer.isLogServiceInitialized());
        
        System.out.println("✓ Singleton pattern validated");
    }
    
    /**
     * Tests that initialization can be performed multiple times without errors.
     */
    @Test
    public void testMultipleInitializations() throws Exception {
        System.out.println("Testing multiple initializations...");
        
        // Call getInstance multiple times - should not throw exceptions
        LogServiceInitializer init1 = LogServiceInitializer.getInstance();
        LogServiceInitializer init2 = LogServiceInitializer.getInstance();
        LogServiceInitializer init3 = LogServiceInitializer.getInstance();
        
        assertSame("All instances should be identical", init1, init2);
        assertSame("All instances should be identical", init2, init3);
        
        // Directory structure should still be intact
        assertTrue("Directory structure should remain intact", Files.exists(logServiceHome));
        assertTrue("Config directory should remain intact", Files.exists(configDir));
        assertTrue("Logs directory should remain intact", Files.exists(logsDir));
        
        System.out.println("✓ Multiple initializations handled correctly");
    }
}