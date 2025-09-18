package org.jvmxray.shared.integration;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.jvmxray.platform.shared.init.ComponentInitializer;
import org.jvmxray.platform.shared.test.TurtleTestExecutor;
import org.jvmxray.shared.integration.turtle.TurtleIntegrationTest;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Integration Test wrapper for the Turtle Integration Test Application.
 * 
 * This class serves as a JUnit wrapper around the TurtleIntegrationTest main class
 * to enable execution via Maven Failsafe plugin. It handles test setup, execution,
 * and validation of the File I/O integration test results.
 * 
 * <p>The test performs the following steps:</p>
 * <ul>
 *   <li>Executes the Turtle File I/O integration test application</li>
 *   <li>Waits for test completion and validates results</li>
 *   <li>Performs log-based validation of File I/O sensor events</li>
 * </ul>
 * 
 * <p>This test is designed to run as part of the Maven integration-test profile
 * with the JVMXRay agent attached.</p>
 * 
 * @author JVMXRay Development Team
 */
public class TurtleTest {

    private static final int TEST_DURATION = 1; // seconds
    private static final String INTENSITY = "medium";
    
    @BeforeClass
    public static void setUpIntegrationEnvironment() throws Exception {
        System.out.println("Initializing JVMXRay Integration Test Environment...");
        
        // Initialize the common component directories and configuration
        org.jvmxray.platform.shared.init.CommonInitializer.getInstance();
        
        System.out.println("✓ Integration test environment initialized");
        System.out.println("  - Test home: " + System.getProperty("jvmxray.test.home"));
        System.out.println("  - Project base: " + System.getProperty("project.basedir"));
    }
    
    @Before
    public void setUp() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("JVMXRay Integration Test - Turtle File I/O Test");
        System.out.println("=".repeat(60));
        
        // Test setup complete - integration environment already initialized
    }
    
    @After
    public void tearDown() throws Exception {
        System.out.println("Integration test completed.");
        System.out.println("=".repeat(60));
    }
    
    /**
 * Main integration test that executes the Turtle application and validates results.
 */
@Test
public void testTurtleIntegrationWithJVMXRayAgent() throws Exception {
    System.out.println("Starting Turtle File I/O Integration Test...");
    System.out.println("Test Duration: " + TEST_DURATION + " seconds");
    System.out.println("Test Intensity: " + INTENSITY);
    System.out.println();

    try {
        boolean testPassed = executeTurtleTest();
        Thread.sleep(1500);
        assertTrue("Turtle integration test execution failed", testPassed);
        System.out.println("✓ Turtle integration test execution completed successfully");
    } catch (IllegalStateException e) {
        if (e.getMessage().contains("Agent JAR file does not exist")) {
            System.out.println("⚠ Skipping Turtle integration test - Agent JAR not yet built: " + e.getMessage());
            System.out.println("  This is expected during the initial build. Run 'mvn install' again after prj-agent is built.");
            // Test passes - this is expected during first build
            return;
        } else {
            // Re-throw other IllegalStateExceptions
            throw e;
        }
    }
}

    /**
     * Executes the Turtle integration test application in a separate JVM with the JVMXRay agent.
     */
    private boolean executeTurtleTest() throws Exception {
        // Create configuration for integration test execution
        TurtleTestExecutor.ExecutionConfig config = 
            TurtleTestExecutor.createDefaultConfig(TEST_DURATION, INTENSITY);
        
        // Execute using shared executor
        TurtleTestExecutor.ExecutionResult result = TurtleTestExecutor.executeTurtleTest(config);
        
        return result.isSuccess();
    }
}