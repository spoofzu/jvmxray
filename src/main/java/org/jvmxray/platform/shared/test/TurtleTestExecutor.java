package org.jvmxray.platform.shared.test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared execution logic for running TurtleIntegrationTest with JVMXRay agent.
 * 
 * This class abstracts the common functionality needed to execute the Turtle
 * integration test in a separate JVM with the JVMXRay agent attached. It can
 * be used by both the test framework (TurtleTest) and the test data generator.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Agent JAR path resolution and validation</li>
 *   <li>JVM process configuration and execution</li>
 *   <li>Environment setup and property management</li>
 *   <li>Process lifecycle management and timeout handling</li>
 * </ul>
 * 
 * @author JVMXRay Development Team
 */
public class TurtleTestExecutor {

    /**
     * Configuration parameters for Turtle test execution
     */
    public static class ExecutionConfig {
        private final int duration;
        private final String intensity;
        private final String logbackConfig;
        private final String agentLogbackConfig;
        private final boolean verbose;
        private final int timeoutSeconds;

        public ExecutionConfig(int duration, String intensity, String logbackConfig, boolean verbose) {
            this.duration = duration;
            this.intensity = intensity;
            this.logbackConfig = logbackConfig;
            this.agentLogbackConfig = null;
            this.verbose = verbose;
            this.timeoutSeconds = duration + 30; // Add 30 second buffer for timeout
        }

        public ExecutionConfig(int duration, String intensity, String logbackConfig, boolean verbose, int timeoutSeconds) {
            this.duration = duration;
            this.intensity = intensity;
            this.logbackConfig = logbackConfig;
            this.agentLogbackConfig = null;
            this.verbose = verbose;
            this.timeoutSeconds = timeoutSeconds;
        }

        public ExecutionConfig(int duration, String intensity, String logbackConfig, String agentLogbackConfig, boolean verbose) {
            this.duration = duration;
            this.intensity = intensity;
            this.logbackConfig = logbackConfig;
            this.agentLogbackConfig = agentLogbackConfig;
            this.verbose = verbose;
            this.timeoutSeconds = duration + 30; // Add 30 second buffer for timeout
        }

        public int getDuration() { return duration; }
        public String getIntensity() { return intensity; }
        public String getLogbackConfig() { return logbackConfig; }
        public String getAgentLogbackConfig() { return agentLogbackConfig; }
        public boolean isVerbose() { return verbose; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
    }

    /**
     * Result of Turtle test execution
     */
    public static class ExecutionResult {
        private final boolean success;
        private final int exitCode;
        private final String message;

        public ExecutionResult(boolean success, int exitCode, String message) {
            this.success = success;
            this.exitCode = exitCode;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getMessage() { return message; }
    }

    /**
     * Executes the Turtle integration test with the specified configuration.
     *
     * @param config The execution configuration parameters
     * @return ExecutionResult containing success status, exit code, and message
     * @throws Exception If environment validation or process setup fails
     */
    public static ExecutionResult executeTurtleTest(ExecutionConfig config) throws Exception {
        // Resolve agent JAR path
        String agentJar = resolveAgentJarPath();
        
        // Validate test home
        String testHome = validateTestHome();
        
        // Build JVM command
        List<String> command = buildJvmCommand(agentJar, testHome, config);
        
        // Log execution details
        logExecutionDetails(agentJar, testHome, command);
        
        // Execute the process
        return executeProcess(command, config.getTimeoutSeconds());
    }

    /**
 * Resolves the path to the JVMXRay agent JAR file.
 *
 * @return Path to the agent JAR file
 * @throws IllegalStateException If agent JAR cannot be located
 */
private static String resolveAgentJarPath() throws IllegalStateException {
    String agentJar = System.getProperty("jvmxray.agent.jar");
    if (agentJar == null || agentJar.isEmpty()) {
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome != null) {
            agentJar = Paths.get(testHome).getParent()
                .resolve("prj-agent/target/prj-agent-0.0.1-shaded.jar")
                .toString();
        } else {
            throw new IllegalStateException(
                "jvmxray.agent.jar system property not set and cannot construct path");
        }
    }
    
    // Check if the agent JAR file actually exists
    if (!Paths.get(agentJar).toFile().exists()) {
        throw new IllegalStateException(
            "Agent JAR file does not exist: " + agentJar + 
            ". Please ensure 'mvn install' has been run on prj-agent module first.");
    }
    
    return agentJar;
}

    /**
     * Validates that the test home directory is properly configured.
     *
     * @return The test home directory path
     * @throws IllegalStateException If test home is not configured
     */
    private static String validateTestHome() throws IllegalStateException {
        String testHome = System.getProperty("jvmxray.test.home");
        if (testHome == null || testHome.isEmpty()) {
            throw new IllegalStateException("jvmxray.test.home system property not set");
        }
        return testHome;
    }

    /**
     * Builds the JVM command list for executing the Turtle integration test.
     *
     * @param agentJar Path to the agent JAR
     * @param testHome Path to the test home directory
     * @param config Execution configuration
     * @return List of command arguments
     */
    private static List<String> buildJvmCommand(String agentJar, String testHome, ExecutionConfig config) {
        String javaExecutable = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-cp");
        command.add(classpath);
        command.add("-Djvmxray.test.home=" + testHome);

        // Add progress reporting configuration
        String progressSetting = System.getProperty("jvmxray.test.progress", "true");
        command.add("-Djvmxray.test.progress=" + progressSetting);

        // Add logback configuration if specified
        if (config.getLogbackConfig() != null && !config.getLogbackConfig().isEmpty()) {
            command.add("-Dlogback.common.configurationFile=" + config.getLogbackConfig());
        }
        
        // Add agent logback configuration if specified
        if (config.getAgentLogbackConfig() != null && !config.getAgentLogbackConfig().isEmpty()) {
            command.add("-Dlogback.agent.configurationFile=" + config.getAgentLogbackConfig());
        }
        
        command.add("-javaagent:" + agentJar);
        command.add("org.jvmxray.shared.integration.turtle.TurtleIntegrationTest");
        command.add("-d");
        command.add(String.valueOf(config.getDuration()));
        command.add("-i");
        command.add(config.getIntensity());
        
        if (config.isVerbose()) {
            command.add("-v");
        }

        return command;
    }

    /**
     * Logs execution details for debugging and monitoring.
     *
     * @param agentJar Path to the agent JAR
     * @param testHome Path to the test home directory
     * @param command Full JVM command
     */
    private static void logExecutionDetails(String agentJar, String testHome, List<String> command) {
        System.out.println("Executing Turtle test in separate JVM with agent:");
        System.out.println("Agent JAR: " + agentJar);
        System.out.println("Test Home: " + testHome);
        System.out.println("Command: " + String.join(" ", command));
        System.out.println();
    }

    /**
     * Executes the JVM process and waits for completion.
     *
     * @param command JVM command arguments
     * @param timeoutSeconds Maximum time to wait for process completion
     * @return ExecutionResult with success status and details
     */
    private static ExecutionResult executeProcess(List<String> command, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                System.err.println("Turtle test process timed out, killing it...");
                process.destroyForcibly();
                return new ExecutionResult(false, -1, "Process timed out after " + timeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                String message = "Turtle integration test completed successfully (exit code: " + exitCode + ")";
                System.out.println(message);
                return new ExecutionResult(true, exitCode, message);
            } else {
                String message = "Turtle integration test failed with exit code: " + exitCode;
                System.err.println(message);
                return new ExecutionResult(false, exitCode, message);
            }
            
        } catch (IOException | InterruptedException e) {
            String message = "Error executing Turtle integration test: " + e.getMessage();
            System.err.println(message);
            e.printStackTrace();
            return new ExecutionResult(false, -1, message);
        }
    }

    /**
     * Creates a default execution configuration suitable for integration testing.
     *
     * @param duration Test duration in seconds
     * @param intensity Test intensity level (low, medium, high)
     * @return ExecutionConfig with default settings
     */
    public static ExecutionConfig createDefaultConfig(int duration, String intensity) {
        String testHome = System.getProperty("jvmxray.test.home");
        String defaultLogbackConfig = testHome + "/common/config/logback.xml";
        return new ExecutionConfig(duration, intensity, defaultLogbackConfig, true);
    }

    /**
     * Creates an execution configuration for database test data generation.
     *
     * @param duration Test duration in seconds
     * @param intensity Test intensity level (low, medium, high)
     * @param databaseLogbackConfig Path to database-specific logback configuration
     * @param verbose Enable verbose output
     * @return ExecutionConfig configured for database logging
     */
    public static ExecutionConfig createDatabaseConfig(int duration, String intensity, 
                                                      String databaseLogbackConfig, boolean verbose) {
        // Resolve the agent logback database configuration path
        String testHome = System.getProperty("jvmxray.test.home");
        String agentLogbackDbConfig = null;
        if (testHome != null) {
            agentLogbackDbConfig = Paths.get(testHome).getParent()
                .resolve("script/config/logback/agent-db.xml")
                .toString();
        }
        
        return new ExecutionConfig(duration, intensity, databaseLogbackConfig, agentLogbackDbConfig, verbose);
    }
}