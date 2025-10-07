package org.jvmxray.agent.sensor.io;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.util.MCCScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileIOInterceptor {
    // Namespace for logging file I/O events
    public static final String NAMESPACE = "org.jvmxray.events.io.fileio";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();
    
    // Configuration for which operations to capture
    private static volatile String captureOperations = "CUD"; // Default: Create, Update, Delete
    private static volatile Set<Character> enabledOps = null;
    
    // Aggregate statistics tracking
    private static final java.util.concurrent.ConcurrentHashMap<Integer, FileStats> activeFiles = 
            new java.util.concurrent.ConcurrentHashMap<>();
    
    // Configuration for aggregate statistics
    private static volatile boolean configLoaded = false;
    private static volatile long thresholdBytesRead = 10 * 1024 * 1024; // 10MB default
    private static volatile long thresholdBytesWrite = 10 * 1024 * 1024; // 10MB default
    private static volatile java.util.regex.Pattern monitorPattern = null;
    private static volatile java.util.regex.Pattern ignorePattern = null;

    /**
     * Initialize configuration from agent properties.
     */
    private static void initConfiguration() {
        if (!configLoaded) {
            synchronized (FileIOInterceptor.class) {
                if (!configLoaded) {
                    try {
                        AgentProperties props = AgentInitializer.getInstance().getProperties();
                        
                        // Load capture operations
                        captureOperations = props.getProperty("jvmxray.agent.sensor.fileio.captures", "CUD");
                        enabledOps = captureOperations.chars()
                                .mapToObj(c -> (char) c)
                                .collect(java.util.stream.Collectors.toSet());
                        
                        // Load thresholds
                        String readThreshold = props.getProperty("jvmxray.io.threshold.bytes.read", "10485760");
                        String writeThreshold = props.getProperty("jvmxray.io.threshold.bytes.write", "10485760");
                        thresholdBytesRead = Long.parseLong(readThreshold);
                        thresholdBytesWrite = Long.parseLong(writeThreshold);
                        
                        // Load monitor patterns (sensitive files)
                        String monitorPatterns = props.getProperty("jvmxray.io.monitor.patterns", 
                                "(?i).*(password|credential|secret|token|key|auth|private).*");
                        if (monitorPatterns != null && !monitorPatterns.trim().isEmpty()) {
                            monitorPattern = java.util.regex.Pattern.compile(monitorPatterns);
                        }
                        
                        // Load ignore patterns (temp/cache files)
                        String ignorePatterns = props.getProperty("jvmxray.io.ignore.patterns",
                                "(?i).*[\\\\/](temp|tmp|cache)[\\\\/].*|.*\\.(tmp|cache|swp)$");
                        if (ignorePatterns != null && !ignorePatterns.trim().isEmpty()) {
                            ignorePattern = java.util.regex.Pattern.compile(ignorePatterns);
                        }
                        
                        configLoaded = true;
                    } catch (Exception e) {
                        // fallback to defaults
                        configLoaded = true;
                    }
                }
            }
        }
    }

    /**
     * Initialize the capture operations from agent properties.
     */
    private static void initCaptureOperations() {
        initConfiguration();
    }

    /**
     * Checks if the specified operation should be captured.
     */
    public static boolean shouldCapture(char operation) {
        return AbstractSensor.executeSafely(() -> {
            initCaptureOperations();
            return enabledOps != null && enabledOps.contains(operation);
        });
    }

    /**
     * Checks if a file path should be ignored (never tracked).
     */
    public static boolean shouldIgnore(String filePath) {
        initConfiguration();
        return ignorePattern != null && ignorePattern.matcher(filePath).matches();
    }

    /**
     * Checks if a file path should be monitored (always logged).
     */
    public static boolean shouldMonitor(String filePath) {
        initConfiguration();
        return monitorPattern != null && monitorPattern.matcher(filePath).matches();
    }

    /**
     * Determines if aggregate stats should be logged based on filtering rules.
     */
    public static boolean shouldLogAggregateStats(FileStats stats) {
        // Tier 1: Ignore patterns - never log
        if (stats.shouldIgnore()) {
            return false;
        }

        // Tier 2: Monitor patterns - always log
        if (stats.isSensitive()) {
            return true;
        }

        // Tier 3: Threshold rules - log if exceeds thresholds
        initConfiguration();
        return stats.getBytesRead() >= thresholdBytesRead ||
               stats.getBytesWritten() >= thresholdBytesWrite;
    }

    /**
     * Logs aggregate file statistics.
     */
    public static void logAggregateStats(FileStats stats) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("operation", stats.getOperationType());
        metadata.put("file", stats.getFilePath());
        metadata.put("is_new_file", String.valueOf(stats.isNewFile()));
        metadata.put("is_sensitive", String.valueOf(stats.isSensitive()));
        metadata.put("bytes_read", String.valueOf(stats.getBytesRead()));
        metadata.put("bytes_written", String.valueOf(stats.getBytesWritten()));
        metadata.put("read_operations", String.valueOf(stats.getReadOperations()));
        metadata.put("write_operations", String.valueOf(stats.getWriteOperations()));
        metadata.put("duration_ms", String.valueOf(stats.getDurationMs()));
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }

    /**
     * Logs a file I/O operation with the specified details.
     */
    public static void logFileOperation(String operation, String filePath, String status) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("operation", operation);
        metadata.put("file", filePath);
        metadata.put("status", status);
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }

    /**
     * Registers a file for aggregate statistics tracking.
     */
    public static void registerFile(Object streamInstance, String filePath, boolean isNewFile, boolean isWrite) {
        try {
            initConfiguration();

            // Check ignore patterns first
            boolean shouldIgnoreFile = shouldIgnore(filePath);
            if (shouldIgnoreFile) {
                return; // Don't track ignored files
            }

            // Check if sensitive
            boolean isSensitive = shouldMonitor(filePath);

            // Create FileStats and register
            int key = System.identityHashCode(streamInstance);
            FileStats stats = new FileStats(filePath, isNewFile, isSensitive, shouldIgnoreFile);
            activeFiles.put(key, stats);
        } catch (Exception e) {
            // Silently ignore errors
        }
    }

    /**
     * Records bytes read for a file stream.
     */
    public static void recordBytesRead(Object streamInstance, int bytesRead) {
        if (bytesRead > 0) {
            try {
                int key = System.identityHashCode(streamInstance);
                FileStats stats = activeFiles.get(key);
                if (stats != null) {
                    stats.addBytesRead(bytesRead);
                }
            } catch (Exception e) {
                // Silently ignore errors
            }
        }
    }

    /**
     * Records bytes written for a file stream.
     */
    public static void recordBytesWritten(Object streamInstance, int bytesWritten) {
        if (bytesWritten > 0) {
            try {
                int key = System.identityHashCode(streamInstance);
                FileStats stats = activeFiles.get(key);
                if (stats != null) {
                    stats.addBytesWritten(bytesWritten);
                }
            } catch (Exception e) {
                // Silently ignore errors
            }
        }
    }

    /**
     * Unregisters a file and logs aggregate statistics if appropriate.
     */
    public static void unregisterFileAndLog(Object streamInstance) {
        try {
            int key = System.identityHashCode(streamInstance);
            FileStats stats = activeFiles.remove(key);
            if (stats != null && shouldLogAggregateStats(stats)) {
                logAggregateStats(stats);
            }
        } catch (Exception e) {
            // Silently ignore errors
        }
    }

    // Unified File class interception for both delete() and createNewFile()
    public static class FileOps {
        @Advice.OnMethodEnter
        public static void enter() {
            MCCScope.enter("FileIO");
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.This File file, @Advice.Return boolean result, @Advice.Origin("#m") String method) {
            try {
                if ("delete".equals(method) && shouldCapture('D')) {
                    logFileOperation("DELETE", file.getAbsolutePath(),
                        result ? "deleted" : "delete_failed");
                } else if ("createNewFile".equals(method) && shouldCapture('C')) {
                    logFileOperation("CREATE", file.getAbsolutePath(),
                        result ? "created" : "create_failed");
                }
            } finally {
                MCCScope.exit("FileIO");
            }
        }
    }

    // Files class methods interception - handles write, read, delete, copy, createDirectories
    public static class FilesOps {
        @Advice.OnMethodEnter
        public static void enter() {
            MCCScope.enter("FileIO");
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Origin("#m") String method, @Advice.AllArguments Object[] args) {
            try {
                // Handle different Files methods based on method signature and arguments
                if ("write".equals(method) && args.length >= 2 && args[0] instanceof Path && shouldCapture('U')) {
                    Path path = (Path) args[0];
                    logFileOperation("UPDATE", path.toAbsolutePath().toString(), "written");
                } else if ("readString".equals(method) && args.length >= 1 && args[0] instanceof Path && shouldCapture('R')) {
                    Path path = (Path) args[0];
                    logFileOperation("READ", path.toAbsolutePath().toString(), "read_string");
                } else if ("readAllBytes".equals(method) && args.length >= 1 && args[0] instanceof Path && shouldCapture('R')) {
                    Path path = (Path) args[0];
                    logFileOperation("READ", path.toAbsolutePath().toString(), "read_bytes");
                } else if ("delete".equals(method) && args.length >= 1 && args[0] instanceof Path && shouldCapture('D')) {
                    Path path = (Path) args[0];
                    logFileOperation("DELETE", path.toAbsolutePath().toString(), "deleted");
                } else if ("copy".equals(method) && args.length >= 2 && args[0] instanceof Path && args[1] instanceof Path && shouldCapture('C')) {
                    Path source = (Path) args[0];
                    Path target = (Path) args[1];
                    logFileOperation("READ", source.toAbsolutePath().toString(), "copied_from");
                    logFileOperation("CREATE", target.toAbsolutePath().toString(), "copied_to");
                } else if ("createFile".equals(method) && args.length >= 1 && args[0] instanceof Path && shouldCapture('C')) {
                    Path path = (Path) args[0];
                    logFileOperation("CREATE", path.toAbsolutePath().toString(), "created");
                } else if ("createDirectories".equals(method) && args.length >= 1 && args[0] instanceof Path && shouldCapture('C')) {
                    Path path = (Path) args[0];
                    logFileOperation("CREATE", path.toAbsolutePath().toString(), "created_dir");
                }
            } catch (Exception e) {
                // Silently ignore errors in logging to avoid breaking the application
            } finally {
                MCCScope.exit("FileIO");
            }
        }
    }

    // FileInputStream constructor interception (Read access)
    public static class Read {
        @Advice.OnMethodEnter
        public static void enter() {
            MCCScope.enter("FileIO");
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis, @Advice.Argument(0) File file) {
            try {
                if (shouldCapture('R')) {
                    logFileOperation("READ", file.getAbsolutePath(), "read_access");
                }
                // Register for aggregate tracking
                registerFile(fis, file.getAbsolutePath(), false, false);
            } finally {
                MCCScope.exit("FileIO");
            }
        }
    }

    // FileOutputStream constructor interception (Update/Write access)
    public static class Update {
        @Advice.OnMethodEnter
        public static void enter() {
            MCCScope.enter("FileIO");
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos, @Advice.Argument(0) File file) {
            try {
                if (shouldCapture('U')) {
                    logFileOperation("UPDATE", file.getAbsolutePath(), "write_access");
                }
                // Determine if this is a new file
                boolean isNewFile = !file.exists();
                // Register for aggregate tracking
                registerFile(fos, file.getAbsolutePath(), isNewFile, true);
            } finally {
                MCCScope.exit("FileIO");
            }
        }
    }

    // FileInputStream.read() interception - single byte read
    public static class InputStreamReadByte {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis, @Advice.Return int bytesRead) {
            if (bytesRead != -1) {
                recordBytesRead(fis, 1);
            }
        }
    }

    // FileInputStream.read(byte[]) interception - array read
    public static class InputStreamReadArray {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis, @Advice.Return int bytesRead) {
            recordBytesRead(fis, bytesRead);
        }
    }

    // FileInputStream.read(byte[], int, int) interception - array with offset read
    public static class InputStreamReadArrayOffset {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis, @Advice.Return int bytesRead) {
            recordBytesRead(fis, bytesRead);
        }
    }

    // FileInputStream.close() interception
    public static class InputStreamClose {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis) {
            unregisterFileAndLog(fis);
        }
    }

    // FileOutputStream.write(int) interception - single byte write
    public static class OutputStreamWriteByte {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos) {
            recordBytesWritten(fos, 1);
        }
    }

    // FileOutputStream.write(byte[]) interception - array write
    public static class OutputStreamWriteArray {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos, @Advice.Argument(0) byte[] bytes) {
            if (bytes != null) {
                recordBytesWritten(fos, bytes.length);
            }
        }
    }

    // FileOutputStream.write(byte[], int, int) interception - array with offset write
    public static class OutputStreamWriteArrayOffset {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos, @Advice.Argument(2) int length) {
            recordBytesWritten(fos, length);
        }
    }

    // FileOutputStream.close() interception
    public static class OutputStreamClose {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos) {
            unregisterFileAndLog(fos);
        }
    }
}