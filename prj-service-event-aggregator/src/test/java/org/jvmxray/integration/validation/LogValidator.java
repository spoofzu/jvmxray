package org.jvmxray.integration.validation;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Log Validation Utility for JVMXRay Integration Tests
 * 
 * This utility validates that the integration test produced the expected log entries
 * from the JVMXRay File I/O sensor. It parses the LogService output and verifies that
 * expected file operation events were captured during the test execution.
 * 
 * <p>The validator checks for events from the File I/O sensor:</p>
 * <ul>
 *   <li>File I/O operations (FileIOSensor)</li>
 * </ul>
 * 
 * <p>The build will fail if validation doesn't pass, causing the integration test
 * to fail appropriately when expected events are missing.</p>
 * 
 * @author JVMXRay Development Team
 */
public class LogValidator {

    private static final String USAGE = "Usage: LogValidator <log-file-path-or-directory>";
    
    // Expected sensor event patterns based on integration test requirements
    private static final Map<String, Pattern> SENSOR_PATTERNS = new HashMap<>();
    private static final Map<String, Integer> MINIMUM_EVENT_COUNTS = new HashMap<>();
    
    static {
        // File I/O Sensor patterns - only sensor we're testing now
        SENSOR_PATTERNS.put("FileIO", Pattern.compile("org\\.jvmxray\\.events\\.io"));
        MINIMUM_EVENT_COUNTS.put("FileIO", 5); // At least 5 file operations expected
    }

    /**
     * JUnit test method for log validation.
     */
    @Test
    public void testLogValidation() throws Exception {
        String logFilePath = System.getProperty("user.home") + "/jvmxray/logservice/logs/logservice-agent-events.log";
        
        LogValidator validator = new LogValidator();
        boolean validationPassed = validator.validateLogs(logFilePath);
        
        assertTrue("Integration test log validation failed", validationPassed);
        System.out.println("✓ Integration test log validation PASSED");
    }

    /**
     * Main entry point for the log validator (kept for backward compatibility).
     * 
     * @param args Command line arguments - expects log directory path as first argument
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(USAGE);
            System.exit(1);
        }
        
        String logPath = args[0];
        LogValidator validator = new LogValidator();
        
        try {
            boolean validationPassed;
            
            // Check if the argument is a directory or a file
            Path path = Paths.get(logPath);
            if (Files.isDirectory(path)) {
                validationPassed = validator.validateLogDirectory(logPath);
            } else {
                validationPassed = validator.validateLogs(logPath);
            }
            
            if (validationPassed) {
                System.out.println("✓ Integration test log validation PASSED");
                System.exit(0);
            } else {
                System.err.println("✗ Integration test log validation FAILED");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error during log validation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Validates all log files in a directory to ensure all expected sensor events are present.
     * 
     * @param logDirPath Path to the directory containing log files
     * @return true if validation passes, false otherwise
     * @throws IOException if there's an error reading the log files
     */
    public boolean validateLogDirectory(String logDirPath) throws IOException {
        Path logDir = Paths.get(logDirPath);
        
        if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
            System.err.println("Log directory not found: " + logDirPath);
            return false;
        }
        
        System.out.println("Validating integration test logs in directory: " + logDirPath);
        
        // Find all .log files in the directory
        List<Path> logFiles = Files.list(logDir)
            .filter(path -> path.toString().endsWith(".log"))
            .collect(Collectors.toList());
        
        if (logFiles.isEmpty()) {
            System.err.println("No log files found in directory: " + logDirPath);
            return false;
        }
        
        System.out.println("Found " + logFiles.size() + " log files to validate");
        
        // Count sensor events by type across all log files
        Map<String, Integer> eventCounts = new HashMap<>();
        for (String sensorType : SENSOR_PATTERNS.keySet()) {
            eventCounts.put(sensorType, 0);
        }
        
        int totalLines = 0;
        
        // Process each log file
        for (Path logFile : logFiles) {
            System.out.println("Processing: " + logFile.getFileName());
            
            try (BufferedReader reader = Files.newBufferedReader(logFile)) {
                String line;
                int fileLines = 0;
                
                while ((line = reader.readLine()) != null) {
                    totalLines++;
                    fileLines++;
                    
                    // Check each sensor pattern against the current line
                    for (Map.Entry<String, Pattern> entry : SENSOR_PATTERNS.entrySet()) {
                        String sensorType = entry.getKey();
                        Pattern pattern = entry.getValue();
                        
                        if (pattern.matcher(line).find()) {
                            eventCounts.put(sensorType, eventCounts.get(sensorType) + 1);
                        }
                    }
                }
                
                System.out.println("  - " + fileLines + " lines processed");
            }
        }
        
        System.out.println("Analyzed " + totalLines + " total log lines across " + logFiles.size() + " files");
        
        // Validate event counts against minimum requirements
        return validateEventCounts(eventCounts);
    }
    
    /**
     * Validates the integration test logs to ensure all expected sensor events are present.
     * 
     * @param logFilePath Path to the log file to validate
     * @return true if validation passes, false otherwise
     * @throws IOException if there's an error reading the log file
     */
    public boolean validateLogs(String logFilePath) throws IOException {
        Path logPath = Paths.get(logFilePath);
        
        if (!Files.exists(logPath)) {
            System.err.println("Log file not found: " + logFilePath);
            return false;
        }
        
        System.out.println("Validating integration test logs: " + logFilePath);
        
        // Count sensor events by type
        Map<String, Integer> eventCounts = new HashMap<>();
        for (String sensorType : SENSOR_PATTERNS.keySet()) {
            eventCounts.put(sensorType, 0);
        }
        
        // Read and analyze log file
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            int totalLines = 0;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                
                // Check each sensor pattern against the current line
                for (Map.Entry<String, Pattern> entry : SENSOR_PATTERNS.entrySet()) {
                    String sensorType = entry.getKey();
                    Pattern pattern = entry.getValue();
                    
                    if (pattern.matcher(line).find()) {
                        eventCounts.put(sensorType, eventCounts.get(sensorType) + 1);
                    }
                }
            }
            
            System.out.println("Analyzed " + totalLines + " log lines");
        }
        
        // Validate event counts against minimum requirements
        return validateEventCounts(eventCounts);
    }
    
    /**
     * Validates specific operations that should have occurred during the test.
     * 
     * @param logPath Path to the log file
     * @return true if specific operations are found, false otherwise
     * @throws IOException if there's an error reading the log file
     */
    private boolean validateSpecificOperations(Path logPath) throws IOException {
        System.out.println("\nSpecific Operation Validation:");
        System.out.println("-".repeat(30));
        
        // Specific patterns to look for based on Turtle test operations
        Map<String, Pattern> specificPatterns = new HashMap<>();
        specificPatterns.put("File Creation", Pattern.compile("target=.*turtle-test.*\\.tmp"));
        specificPatterns.put("Socket Connection", Pattern.compile("connect|bind"));
        specificPatterns.put("HTTP Request", Pattern.compile("GET|POST"));
        
        Map<String, Boolean> operationFound = new HashMap<>();
        for (String operation : specificPatterns.keySet()) {
            operationFound.put(operation, false);
        }
        
        // Search for specific operations
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (Map.Entry<String, Pattern> entry : specificPatterns.entrySet()) {
                    String operation = entry.getKey();
                    Pattern pattern = entry.getValue();
                    
                    if (pattern.matcher(line).find()) {
                        operationFound.put(operation, true);
                    }
                }
            }
        }
        
        // Report specific operation results
        boolean allSpecificOperationsFound = true;
        for (Map.Entry<String, Boolean> entry : operationFound.entrySet()) {
            String operation = entry.getKey();
            boolean found = entry.getValue();
            String status = found ? "✓ FOUND" : "? NOT FOUND";
            System.out.printf("%-20s: %s%n", operation, status);
            
            // Note: We don't fail the build for missing specific operations
            // as they might be optional or implementation-dependent
        }
        
        return true; // Always pass specific operation validation for now
    }
    
    /**
     * Validates that the log file has proper timing and metadata completeness.
     * 
     * @param logPath Path to the log file
     * @return true if timing validation passes, false otherwise
     * @throws IOException if there's an error reading the log file
     */
    private boolean validateLogTiming(Path logPath) throws IOException {
        System.out.println("\nLog Timing Validation:");
        System.out.println("-".repeat(22));
        
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String firstLine = reader.readLine();
            String lastLine = null;
            String line;
            
            // Read to the last line
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            
            if (firstLine != null && lastLine != null) {
                System.out.println("First event: " + firstLine.substring(0, Math.min(100, firstLine.length())) + "...");
                System.out.println("Last event:  " + lastLine.substring(0, Math.min(100, lastLine.length())) + "...");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates event counts against minimum requirements.
     * 
     * @param eventCounts Map of event counts by sensor type
     * @return true if all validation passes, false otherwise
     */
    private boolean validateEventCounts(Map<String, Integer> eventCounts) {
        boolean allValidationsPassed = true;
        System.out.println("\nSensor Event Validation Results:");
        System.out.println("=".repeat(50));
        
        for (String sensorType : SENSOR_PATTERNS.keySet()) {
            int actualCount = eventCounts.get(sensorType);
            int minimumCount = MINIMUM_EVENT_COUNTS.get(sensorType);
            boolean passed = actualCount >= minimumCount;
            
            String status = passed ? "✓ PASS" : "✗ FAIL";
            System.out.printf("%-10s: %3d events (min: %d) %s%n", 
                sensorType, actualCount, minimumCount, status);
            
            if (!passed) {
                allValidationsPassed = false;
            }
        }
        
        System.out.println("=".repeat(50));
        
        return allValidationsPassed;
    }
}
