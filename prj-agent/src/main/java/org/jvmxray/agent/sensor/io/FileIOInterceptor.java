package org.jvmxray.agent.sensor.io;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unified interceptor class for monitoring and logging file I/O operations in the JVMXRay agent framework.
 * Supports Create, Read, Update, and Delete (CRUD) operations with configurable operation tracking.
 * Uses Byte Buddy's {@link Advice} annotations to instrument various file operations and logs events
 * with contextual metadata using the {@link LogProxy}.
 *
 * @author Milton Smith
 */
public class FileIOInterceptor {
    // Namespace for logging file I/O events
    public static final String NAMESPACE = "org.jvmxray.events.io.fileio";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();
    // Configuration for which operations to capture
    private static volatile String captureOperations = "CUD"; // Default: Create, Update, Delete
    private static volatile Set<Character> enabledOps = null;

    /**
     * Initialize the capture operations from agent properties.
     */
    private static void initCaptureOperations() {
        if (enabledOps == null) {
            synchronized (FileIOInterceptor.class) {
                if (enabledOps == null) {
                    try {
                        AgentProperties props = AgentInitializer.getInstance().getProperties();
                        captureOperations = props.getProperty("jvmxray.agent.sensor.fileio.captures", "CUD");
                    } catch (Exception e) {
                        captureOperations = "CUD"; // fallback
                    }
                    enabledOps = captureOperations.chars()
                            .mapToObj(c -> (char) c)
                            .collect(java.util.stream.Collectors.toSet());
                }
            }
        }
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
     * Logs a file I/O operation with the specified details.
     */
    public static void logFileOperation(String operation, String filePath, String status) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("operation", operation);
        metadata.put("file", filePath);
        metadata.put("status", status);
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }

    // Unified File class interception for both delete() and createNewFile()
    public static class FileOps {
        @Advice.OnMethodExit
        public static void exitDelete(@Advice.This File file, @Advice.Return boolean result, @Advice.Origin("#m") String method) {
            if ("delete".equals(method) && shouldCapture('D')) {
                logFileOperation("DELETE", file.getAbsolutePath(), 
                    result ? "deleted" : "delete_failed");
            } else if ("createNewFile".equals(method) && shouldCapture('C')) {
                logFileOperation("CREATE", file.getAbsolutePath(), 
                    result ? "created" : "create_failed");
            }
        }
    }

    // Files class methods interception - handles write, read, delete, copy, createDirectories
    public static class FilesOps {
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
            }
        }
    }

    // FileInputStream constructor interception (Read access)
    public static class Read {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileInputStream fis, @Advice.Argument(0) File file) {
            if (shouldCapture('R')) {
                logFileOperation("READ", file.getAbsolutePath(), "read_access");
            }
        }
    }

    // FileOutputStream constructor interception (Update/Write access)
    public static class Update {
        @Advice.OnMethodExit
        public static void exit(@Advice.This FileOutputStream fos, @Advice.Argument(0) File file) {
            if (shouldCapture('U')) {
                logFileOperation("UPDATE", file.getAbsolutePath(), "write_access");
            }
        }
    }
}