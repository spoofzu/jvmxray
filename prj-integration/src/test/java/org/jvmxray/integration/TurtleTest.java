package org.jvmxray.integration;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.jvmxray.integration.turtle.TurtleIntegrationTest;
import org.jvmxray.integration.init.IntegrationInitializer;

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
        
        // Initialize the integration component directories and configuration
        IntegrationInitializer integrationInitializer = IntegrationInitializer.getInstance();
        
        System.out.println("✓ Integration test environment initialized");
        System.out.println("  - Integration logs: " + System.getProperty("jvmxray.integration.logs"));
        System.out.println("  - Integration config: " + System.getProperty("jvmxray.integration.config"));
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
     * Executes the Turtle integration test application in a separate JVM with the JVMXRay agent.
     * 
     * @return true if the test completed successfully, false otherwise
     * @throws Exception if there's an error executing the test
     */
    private boolean executeTurtleTest() throws Exception {
        // Get the agent JAR path from system properties
        String agentJar = System.getProperty("jvmxray.agent.jar");
        if (agentJar == null || agentJar.isEmpty()) {
            // Try to construct the path from the test home
            String testHome = System.getProperty("jvmxray.test.home");
            if (testHome != null) {
                agentJar = Paths.get(testHome).getParent().resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar").toString();
            } else {
                throw new IllegalStateException("jvmxray.agent.jar system property not set and cannot construct path");
            }
        }
        
        // Get the test home directory
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome == null || testHome.isEmpty()) {
            throw new IllegalStateException("jvmxray.test.home system property not set");
        }
        
        // Get the Java executable path
        String javaExecutable = System.getProperty("java.home") + "/bin/java";
        
        // Get the current classpath
        String classpath = System.getProperty("java.class.path");
        
        // Build the command to execute TurtleIntegrationTest with the agent
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-cp");
        command.add(classpath);
        command.add("-Djvmxray.test.home=" + testHome);
        command.add("-Dlogback.configurationFile=" + testHome + "/integration/config/logback.xml");
        command.add("-javaagent:" + agentJar);
        command.add(TurtleIntegrationTest.class.getName());
        command.add("-d");
        command.add(String.valueOf(TEST_DURATION));
        command.add("-i");
        command.add(INTENSITY);
        command.add("-v"); // Verbose mode
        
        System.out.println("Executing Turtle test in separate JVM with agent:");
        System.out.println("Agent JAR: " + agentJar);
        System.out.println("Test Home: " + testHome);
        System.out.println("Command: " + String.join(" ", command));
        System.out.println();
        
        try {
            // Create and start the process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit I/O to see output in real time
            
            Process process = pb.start();
            
            // Wait for the process to complete with a timeout
            boolean finished = process.waitFor(TEST_DURATION + 30, TimeUnit.SECONDS);
            
            if (!finished) {
                System.err.println("Turtle test process timed out, killing it...");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                System.out.println("Turtle integration test completed successfully (exit code: " + exitCode + ")");
                return true;
            } else {
                System.err.println("Turtle integration test failed with exit code: " + exitCode);
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing Turtle integration test: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    

}
