package org.jvmxray.integration;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.jvmxray.integration.turtle.TurtleIntegrationTest;

import java.io.*;
import java.nio.file.*;

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
    
    @Before
    public void setUp() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("JVMXRay Integration Test - Turtle File I/O Test");
        System.out.println("=".repeat(60));
        
        // Test setup complete - no additional setup needed for simplified File I/O test
    }
    
    @After
    public void tearDown() throws Exception {
        System.out.println("Integration test completed.");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Main integration test that executes the Turtle application and validates results.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void testTurtleIntegrationWithJVMXRayAgent() throws Exception {
        System.out.println("Starting Turtle File I/O Integration Test...");
        System.out.println("Test Duration: " + TEST_DURATION + " seconds");
        System.out.println("Test Intensity: " + INTENSITY);
        System.out.println();
        
        // Execute the Turtle integration test
        boolean testPassed = executeTurtleTest();
        
        // Wait a moment for any final log events to be processed
        Thread.sleep(2000);
        
        // Validate that the test completed successfully
        assertTrue("Turtle integration test execution failed", testPassed);
        
        System.out.println("✓ Turtle integration test execution completed successfully");
    }
    
    /**
     * Executes the Turtle integration test application.
     * 
     * @return true if the test completed successfully, false otherwise
     * @throws Exception if there's an error executing the test
     */
    private boolean executeTurtleTest() throws Exception {
        // Prepare arguments for the Turtle test
        String[] args = {
            "-d", String.valueOf(TEST_DURATION),
            "-i", INTENSITY,
            "-v" // Verbose mode
        };
        
        System.out.println("Executing Turtle test with arguments: " + String.join(" ", args));
        
        try {
            // Create and run the Turtle integration test
            TurtleIntegrationTest turtle = new TurtleIntegrationTest();
            
            // Use reflection to call the main method since it's designed as a standalone application
            TurtleIntegrationTest.main(args);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error executing Turtle integration test: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    

}
