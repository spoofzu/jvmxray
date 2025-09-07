package org.jvmxray.integration.turtle;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Turtle Integration Test Application
 * 
 * A simplified integration test application that exercises the JVMXRay File I/O sensor
 * through controlled test operations while demonstrating real-time event collection,
 * aggregation, and persistence.
 * 
 * <p>Key functionalities:</p>
 * <ul>
 *   <li>Exercises JVMXRay File I/O sensor through comprehensive file operations</li>
 *   <li>Configurable test duration and intensity levels</li>
 *   <li>Real-time event transmission to LogService</li>
 *   <li>Command-line interface for test configuration</li>
 * </ul>
 * 
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code -d, --duration <seconds>}: Test execution duration (default: 30 seconds)</li>
 *   <li>{@code -i, --intensity <level>}: Test intensity level - low/medium/high (default: medium)</li>
 *   <li>{@code -v, --verbose}: Enable verbose output</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * java -javaagent:prj-agent-0.0.1-shaded.jar TurtleIntegrationTest -d 60 -i high -v
 * }</pre>
 * 
 * @author JVMXRay Development Team
 */
public class TurtleIntegrationTest {

    // SLF4J logger for the Turtle test application
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.integration.turtle.TurtleIntegrationTest");

    // Command-line option constants for duration
    private static final String OPT_DURATION_SHORT = "d";
    private static final String OPT_DURATION_LONG = "duration";
    private static final int DEFAULT_DURATION = 30;

    // Command-line option constants for intensity
    private static final String OPT_INTENSITY_SHORT = "i";
    private static final String OPT_INTENSITY_LONG = "intensity";
    private static final String DEFAULT_INTENSITY = "medium";

    // Command-line option constants for verbose mode
    private static final String OPT_VERBOSE_SHORT = "v";
    private static final String OPT_VERBOSE_LONG = "verbose";

    // Test configuration fields
    private int duration = DEFAULT_DURATION;
    private IntensityLevel intensity = IntensityLevel.MEDIUM;
    private boolean verbose = false;

    // Test execution control
    private volatile boolean testRunning = false;
    private long testStartTime;

    /**
     * Enumeration for test intensity levels
     */
    public enum IntensityLevel {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");

        private final String value;

        IntensityLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static IntensityLevel fromString(String value) {
            for (IntensityLevel level : IntensityLevel.values()) {
                if (level.value.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Invalid intensity level: " + value + ". Valid values are: low, medium, high");
        }
    }

    /**
     * Context class for tracking file test resources and enabling proper cleanup.
     */
    private static class FileTestContext {
        private final Path tempDirectory;
        private final List<Path> createdFiles;
        private final List<Path> createdDirectories;
        private final Random random;

        public FileTestContext() throws IOException {
            this.tempDirectory = Files.createTempDirectory("turtle-fileio-");
            this.createdFiles = new ArrayList<>();
            this.createdDirectories = new ArrayList<>();
            this.random = new Random();
        }

        public Path getTempDirectory() {
            return tempDirectory;
        }

        public void addCreatedFile(Path file) {
            createdFiles.add(file);
        }

        public void addCreatedDirectory(Path directory) {
            createdDirectories.add(directory);
        }

        public List<Path> getCreatedFiles() {
            return Collections.unmodifiableList(createdFiles);
        }

        public List<Path> getCreatedDirectories() {
            return Collections.unmodifiableList(createdDirectories);
        }

        public String generateRandomContent(int size) {
            StringBuilder content = new StringBuilder(size);
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n\t ";
            for (int i = 0; i < size; i++) {
                content.append(characters.charAt(random.nextInt(characters.length())));
            }
            return content.toString();
        }

        public byte[] generateRandomBytes(int size) {
            byte[] bytes = new byte[size];
            random.nextBytes(bytes);
            return bytes;
        }

        public void cleanup() {
            try {
                // Delete files first
                for (Path file : createdFiles) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        // Log but don't fail cleanup for individual files
                        logger.debug("Failed to delete file {}: {}", file, e.getMessage());
                    }
                }

                // Delete directories (in reverse order for nested directories)
                Collections.reverse(createdDirectories);
                for (Path directory : createdDirectories) {
                    try {
                        Files.deleteIfExists(directory);
                    } catch (IOException e) {
                        logger.debug("Failed to delete directory {}: {}", directory, e.getMessage());
                    }
                }

                // Finally, delete the temp directory and any remaining contents
                if (Files.exists(tempDirectory)) {
                    Files.walkFileTree(tempDirectory, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } catch (IOException e) {
                logger.warn("Error during file test context cleanup: {}", e.getMessage());
            }
        }
    }

    /**
     * Main entry point for the Turtle integration test application.
     *
     * @param args Command-line arguments specifying test configuration
     */
    public static void main(String[] args) {
        // Create and run the Turtle test instance
        new TurtleIntegrationTest().run(args);
    }

    /**
     * Runs the Turtle integration test application, parsing command-line arguments,
     * initializing test environment, and executing test operations.
     *
     * @param args Command-line arguments
     */
    private void run(String[] args) {
        try {
            // Parse command-line arguments to configure test parameters
            parseCommandLineOptions(args);
            // Initialize test environment
            initializeTest();
            // Execute test operations
            executeTestOperations();
            // Clean up test environment
            cleanupTest();
            
            logger.info("Turtle integration test completed successfully");
        } catch (Exception e) {
            // Log fatal errors and exit with error code
            logger.error("Fatal error occurred during test execution: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Parses command-line arguments to configure test parameters.
     *
     * @param args Command-line arguments
     * @throws ParseException If the arguments are invalid or cannot be parsed
     */
    private void parseCommandLineOptions(String[] args) throws ParseException {
        // Define command-line options
        Options options = new Options();

        // Add duration option
        options.addOption(Option.builder(OPT_DURATION_SHORT)
                .longOpt(OPT_DURATION_LONG)
                .desc("Test execution duration in seconds (default: " + DEFAULT_DURATION + ")")
                .hasArg()
                .argName("SECONDS")
                .build());

        // Add intensity option
        options.addOption(Option.builder(OPT_INTENSITY_SHORT)
                .longOpt(OPT_INTENSITY_LONG)
                .desc("Test intensity level: low/medium/high (default: " + DEFAULT_INTENSITY + ")")
                .hasArg()
                .argName("LEVEL")
                .build());

        // Add verbose option
        options.addOption(Option.builder(OPT_VERBOSE_SHORT)
                .longOpt(OPT_VERBOSE_LONG)
                .desc("Enable verbose output")
                .build());

        // Add help option
        options.addOption(Option.builder("help")
                .desc("Display this help message")
                .build());

        // Parse command-line arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            throw e;
        }

        // Display help if requested
        if (cmd.hasOption("help")) {
            printUsage(options);
            System.exit(0);
        }

        // Configure duration from command-line or use default
        if (cmd.hasOption(OPT_DURATION_SHORT)) {
            try {
                duration = Integer.parseInt(cmd.getOptionValue(OPT_DURATION_SHORT));
                if (duration <= 0) {
                    logger.warn("Invalid duration provided. Duration must be positive. Using default duration {}", DEFAULT_DURATION);
                    duration = DEFAULT_DURATION;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid duration number provided. Using default duration {}", DEFAULT_DURATION);
                duration = DEFAULT_DURATION;
            }
        }

        // Configure intensity from command-line or use default
        if (cmd.hasOption(OPT_INTENSITY_SHORT)) {
            try {
                intensity = IntensityLevel.fromString(cmd.getOptionValue(OPT_INTENSITY_SHORT));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid intensity level provided. Using default intensity {}", DEFAULT_INTENSITY);
                intensity = IntensityLevel.MEDIUM;
            }
        }

        // Configure verbose mode
        verbose = cmd.hasOption(OPT_VERBOSE_SHORT);

        // Log configured parameters
        logger.info("Test duration set to {} seconds", duration);
        logger.info("Test intensity set to {}", intensity.getValue());
        logger.info("Verbose mode {}", verbose ? "enabled" : "disabled");
    }

    /**
     * Prints usage information for the command-line options.
     *
     * @param options The command-line options
     */
    private void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -javaagent:prj-agent-0.0.1-shaded.jar TurtleIntegrationTest [options]", 
                           "Turtle Integration Test Application - Exercises JVMXRay File I/O sensor", 
                           options, 
                           "Example: java -javaagent:prj-agent-0.0.1-shaded.jar TurtleIntegrationTest -d 60 -i high -v");
    }

    /**
     * Initializes the test environment.
     */
    private void initializeTest() {
        logger.info("Initializing Turtle integration test environment");
        
        // Set test start time and running flag
        testStartTime = System.currentTimeMillis();
        testRunning = true;
        
        if (verbose) {
            logger.info("Test environment initialized successfully");
            logger.info("Test will run for {} seconds with {} intensity", duration, intensity.getValue());
        }
    }

    /**
     * Executes file I/O test operations in sequence based on duration and intensity.
     */
    private void executeTestOperations() {
        logger.info("Starting file I/O test operations execution");
        
        long endTime = testStartTime + (duration * 1000L);
        
        while (testRunning && System.currentTimeMillis() < endTime) {
            try {
                // Execute file operations
                executeFileOperations();
                
                // Execute new sensor operations (basic validation)
                executeNewSensorOperations();
                
                // Brief pause between cycles
                Thread.sleep(1000);
                
            } catch (InterruptedException e) {
                logger.info("Test execution interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error during test operations: {}", e.getMessage(), e);
                // Continue with next cycle
            }
        }
        
        testRunning = false;
        logger.info("File I/O test operations execution completed");
    }

    /**
     * Executes file I/O operations to exercise FileIOSensor.
     * Operations include: create temporary files, write data, read data, delete files, directory operations.
     */
    public void executeFileOperations() {
        if (verbose) {
            logger.info("Executing file I/O operations (intensity: {})", intensity.getValue());
        }
        
        FileTestContext context = null;
        try {
            // Create test context and temporary directory
            context = new FileTestContext();
            if (verbose) {
                logger.info("Created temporary directory: {}", context.getTempDirectory());
            }
            
            // Determine number of operations based on intensity
            int operationCount = getFileOperationCount(intensity);
            
            // Execute file write operations
            writeFileOperations(context, operationCount);
            
            // Execute file read operations
            readFileOperations(context, operationCount);
            
            // Execute directory operations
            directoryOperations(context, operationCount);
            
            // Execute file deletion operations
            deleteFileOperations(context);
            
            if (verbose) {
                logger.info("Completed file I/O operations. Created {} files, {} directories", 
                           context.getCreatedFiles().size(), context.getCreatedDirectories().size());
            }
            
        } catch (Exception e) {
            logger.error("Error during file I/O operations: {}", e.getMessage(), e);
        } finally {
            // Ensure cleanup happens even if exceptions occur
            if (context != null) {
                context.cleanup();
                if (verbose) {
                    logger.info("Cleaned up file test resources");
                }
            }
        }
    }

    /**
     * Determines the number of file operations to perform based on intensity level.
     * 
     * @param intensity The test intensity level
     * @return Number of operations to perform
     */
    private int getFileOperationCount(IntensityLevel intensity) {
        switch (intensity) {
            case LOW:
                return 2 + new Random().nextInt(2); // 2-3 operations
            case MEDIUM:
                return 5 + new Random().nextInt(4); // 5-8 operations  
            case HIGH:
                return 10 + new Random().nextInt(6); // 10-15 operations
            default:
                return 5; // Default to medium
        }
    }

    /**
     * Executes file write operations using different Java I/O mechanisms.
     * 
     * @param context The file test context
     * @param operationCount Number of write operations to perform
     * @throws IOException If file write operations fail
     */
    private void writeFileOperations(FileTestContext context, int operationCount) throws IOException {
        if (verbose) {
            logger.info("Executing {} file write operations", operationCount);
        }
        
        for (int i = 0; i < operationCount; i++) {
            Path filePath = context.getTempDirectory().resolve("testfile_" + i + ".txt");
            context.addCreatedFile(filePath);
            
            // Alternate between different write mechanisms
            if (i % 2 == 0) {
                // Use FileOutputStream for byte-based writing
                writeFileWithOutputStream(filePath, context.generateRandomBytes(256 + i * 100));
            } else {
                // Use BufferedWriter for character-based writing  
                writeFileWithBufferedWriter(filePath, context.generateRandomContent(200 + i * 50));
            }
            
            if (verbose) {
                logger.debug("Created and wrote to file: {}", filePath);
            }
        }
    }

    /**
     * Writes data to a file using FileOutputStream (triggers WriteInterceptor).
     * 
     * @param filePath The path to write to
     * @param data The byte data to write
     * @throws IOException If write operation fails
     */
    private void writeFileWithOutputStream(Path filePath, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            // Write bytes one by one to trigger WriteInterceptor for each byte
            for (byte b : data) {
                fos.write(b); // This triggers WriteInterceptor.exit()
            }
            fos.flush();
        }
    }

    /**
     * Writes data to a file using BufferedWriter.
     * 
     * @param filePath The path to write to  
     * @param content The string content to write
     * @throws IOException If write operation fails
     */
    private void writeFileWithBufferedWriter(Path filePath, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }

    /**
     * Executes file read operations using different Java I/O mechanisms.
     * 
     * @param context The file test context
     * @param operationCount Number of read operations to perform
     * @throws IOException If file read operations fail
     */
    private void readFileOperations(FileTestContext context, int operationCount) throws IOException {
        if (verbose) {
            logger.info("Executing file read operations on {} files", context.getCreatedFiles().size());
        }
        
        List<Path> filesToRead = context.getCreatedFiles();
        if (filesToRead.isEmpty()) {
            logger.warn("No files available for read operations");
            return;
        }
        
        // Read from created files using different mechanisms
        for (int i = 0; i < Math.min(operationCount, filesToRead.size()); i++) {
            Path filePath = filesToRead.get(i);
            
            if (i % 2 == 0) {
                // Use FileInputStream for byte-based reading
                readFileWithInputStream(filePath);
            } else {
                // Use BufferedReader for character-based reading
                readFileWithBufferedReader(filePath);
            }
            
            if (verbose) {
                logger.debug("Read from file: {}", filePath);
            }
        }
    }

    /**
     * Reads data from a file using FileInputStream (triggers ReadInterceptor).
     * 
     * @param filePath The path to read from
     * @throws IOException If read operation fails
     */
    private void readFileWithInputStream(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            int bytesRead = 0;
            int data;
            // Read bytes one by one to trigger ReadInterceptor for each read
            while ((data = fis.read()) != -1) { // This triggers ReadInterceptor.exit()
                bytesRead++;
                // Limit reading to avoid excessive logging
                if (bytesRead > 100) break;
            }
        }
    }

    /**
     * Reads data from a file using BufferedReader.
     * 
     * @param filePath The path to read from
     * @throws IOException If read operation fails  
     */
    private void readFileWithBufferedReader(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null) {
                linesRead++;
                // Limit reading to avoid excessive processing
                if (linesRead > 10) break;
            }
        }
    }

    /**
     * Executes directory operations such as creating subdirectories and listing contents.
     * 
     * @param context The file test context
     * @param operationCount Number of directory operations to perform
     * @throws IOException If directory operations fail
     */
    private void directoryOperations(FileTestContext context, int operationCount) throws IOException {
        if (verbose) {
            logger.info("Executing {} directory operations", operationCount);
        }
        
        // Create nested subdirectory structure
        int directoriesToCreate = Math.max(1, operationCount / 3);
        for (int i = 0; i < directoriesToCreate; i++) {
            Path subDir = context.getTempDirectory().resolve("subdir_" + i);
            Files.createDirectory(subDir);
            context.addCreatedDirectory(subDir);
            
            // Create nested subdirectory
            if (i % 2 == 0) {
                Path nestedDir = subDir.resolve("nested_" + i);
                Files.createDirectory(nestedDir);
                context.addCreatedDirectory(nestedDir);
                
                // Create marker file in nested directory
                Path markerFile = nestedDir.resolve("marker.txt");
                Files.write(markerFile, "marker content".getBytes(StandardCharsets.UTF_8));
                context.addCreatedFile(markerFile);
            }
            
            if (verbose) {
                logger.debug("Created directory: {}", subDir);
            }
        }
        
        // List directory contents using different APIs
        listDirectoryContents(context.getTempDirectory());
        
        // Traverse directory tree
        traverseDirectoryTree(context.getTempDirectory());
    }

    /**
     * Lists directory contents using File.list() and DirectoryStream APIs.
     * 
     * @param directory The directory to list
     * @throws IOException If directory listing fails
     */
    private void listDirectoryContents(Path directory) throws IOException {
        // List using File.list()
        File dir = directory.toFile();
        String[] fileNames = dir.list();
        if (fileNames != null && verbose) {
            logger.debug("Directory {} contains {} items", directory, fileNames.length);
        }
        
        // List using DirectoryStream
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            int count = 0;
            for (Path entry : stream) {
                count++;
                if (verbose) {
                    logger.debug("Found entry: {}", entry.getFileName());
                }
            }
        }
    }

    /**
     * Traverses directory tree using Files.walkFileTree().
     * 
     * @param rootDirectory The root directory to traverse
     * @throws IOException If directory traversal fails
     */
    private void traverseDirectoryTree(Path rootDirectory) throws IOException {
        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (verbose) {
                    logger.debug("Visited file: {}", file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (verbose) {
                    logger.debug("Entered directory: {}", dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Executes file deletion operations (triggers DeleteInterceptor).
     * 
     * @param context The file test context
     */
    private void deleteFileOperations(FileTestContext context) {
        if (verbose) {
            logger.info("Executing file deletion operations");
        }
        
        // Delete some of the created files using File.delete()
        List<Path> filesToDelete = new ArrayList<>(context.getCreatedFiles());
        Collections.shuffle(filesToDelete);
        
        // Delete about half of the files to trigger DeleteInterceptor
        int filesToDeleteCount = Math.max(1, filesToDelete.size() / 2);
        for (int i = 0; i < filesToDeleteCount && i < filesToDelete.size(); i++) {
            Path filePath = filesToDelete.get(i);
            File file = filePath.toFile();
            boolean deleted = file.delete(); // This triggers DeleteInterceptor.exit()
            
            if (verbose) {
                logger.debug("Deleted file {}: {}", filePath, deleted ? "success" : "failed");
            }
        }
    }

    /**
     * Executes operations to trigger new sensors for basic validation.
     */
    public void executeNewSensorOperations() {
        if (verbose) {
            logger.info("Executing new sensor validation operations");
        }
        
        try {
            // Trigger CryptoSensor
            executeCryptoOperations();
            
            // Trigger ProcessSensor (simple, safe command)
            executeProcessOperations();
            
            // Trigger SerializationSensor
            executeSerializationOperations();
            
            // Trigger ReflectionSensor
            executeReflectionOperations();
            
            // Trigger Phase 2 sensors
            executePhase2SensorOperations();
            
        } catch (Exception e) {
            logger.error("Error during new sensor operations: {}", e.getMessage(), e);
        }
    }

    /**
     * Executes basic cryptographic operations to trigger CryptoSensor.
     */
    private void executeCryptoOperations() {
        try {
            // Basic MessageDigest operations
            java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
            java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
            
            byte[] data = "test data for crypto sensor".getBytes();
            md5.digest(data);
            sha256.digest(data);
            
            if (verbose) {
                logger.debug("Executed crypto operations (MessageDigest)");
            }
        } catch (Exception e) {
            logger.debug("Error in crypto operations: {}", e.getMessage());
        }
    }

    /**
     * Executes basic process operations to trigger ProcessSensor.
     */
    private void executeProcessOperations() {
        try {
            // Simple, safe command that works on most systems
            ProcessBuilder pb = new ProcessBuilder();
            
            // Use different commands based on OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd", "/c", "echo", "JVMXRay Process Test");
            } else {
                pb.command("echo", "JVMXRay Process Test");
            }
            
            Process process = pb.start();
            process.waitFor();
            
            if (verbose) {
                logger.debug("Executed process operations (ProcessBuilder)");
            }
        } catch (Exception e) {
            logger.debug("Error in process operations: {}", e.getMessage());
        }
    }

    /**
     * Executes basic serialization operations to trigger SerializationSensor.
     */
    private void executeSerializationOperations() {
        try {
            // Basic Java serialization
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            
            // Serialize a simple, safe object
            java.util.HashMap<String, String> testMap = new java.util.HashMap<>();
            testMap.put("test", "serialization");
            oos.writeObject(testMap);
            oos.close();
            
            // Deserialize it back
            byte[] serializedData = baos.toByteArray();
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(serializedData);
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
            Object deserialized = ois.readObject();
            ois.close();
            
            if (verbose) {
                logger.debug("Executed serialization operations (ObjectOutputStream/ObjectInputStream)");
            }
        } catch (Exception e) {
            logger.debug("Error in serialization operations: {}", e.getMessage());
        }
    }

    /**
     * Executes basic reflection operations to trigger ReflectionSensor.
     */
    private void executeReflectionOperations() {
        try {
            // Basic reflection operations
            Class<?> stringClass = Class.forName("java.lang.String");
            
            java.lang.reflect.Method lengthMethod = stringClass.getMethod("length");
            String testString = "JVMXRay Reflection Test";
            Object result = lengthMethod.invoke(testString);
            
            // Access a field via reflection
            java.lang.reflect.Field[] fields = stringClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getName().equals("value") && field.getType() == byte[].class) {
                    // Don't actually access it, just test setAccessible
                    field.setAccessible(true);
                    break;
                }
            }
            
            if (verbose) {
                logger.debug("Executed reflection operations (Class.forName, Method.invoke, setAccessible)");
            }
        } catch (Exception e) {
            logger.debug("Error in reflection operations: {}", e.getMessage());
        }
    }

    /**
     * Executes operations to trigger Phase 2 sensors for basic validation.
     */
    public void executePhase2SensorOperations() {
        if (verbose) {
            logger.info("Executing Phase 2 sensor validation operations");
        }
        
        try {
            // Trigger ConfigurationSensor
            String osName = System.getProperty("os.name");
            System.getenv("PATH");
            
            // Trigger MemorySensor
            Runtime.getRuntime().freeMemory();
            Runtime.getRuntime().totalMemory();
            
            // Trigger AuthenticationSensor (simple Principal getName if available)
            try {
                Class.forName("java.security.Principal");
            } catch (ClassNotFoundException e) {
                // Principal not available, skip
            }
            
            // Trigger ThreadSensor
            Thread currentThread = Thread.currentThread();
            currentThread.getName();
            
            // Basic memory allocation for DataTransferSensor
            byte[] testData = new byte[1024];
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(testData);
            bais.read(testData);
            bais.close();
            
            if (verbose) {
                logger.debug("Executed Phase 2 sensor operations");
            }
        } catch (Exception e) {
            logger.debug("Error in Phase 2 sensor operations: {}", e.getMessage());
        }
    }

    /**
     * Cleans up test environment and resources.
     */
    private void cleanupTest() {
        logger.info("Cleaning up test environment");
        
        testRunning = false;
        
        if (verbose) {
            long testDuration = System.currentTimeMillis() - testStartTime;
            logger.info("Test cleanup completed. Total test duration: {} ms", testDuration);
        }
    }
}