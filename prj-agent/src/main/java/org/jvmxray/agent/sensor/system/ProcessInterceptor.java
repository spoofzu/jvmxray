package org.jvmxray.agent.sensor.system;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interceptor class for monitoring and logging process execution operations in the JVMXRay agent framework.
 * Monitors ProcessBuilder.start() and Runtime.exec() calls to track external process executions.
 * Uses Byte Buddy's {@link Advice} annotations to instrument process execution methods and logs events
 * with contextual metadata using the {@link LogProxy}.
 *
 * @author Milton Smith
 */
public class ProcessInterceptor {
    // Namespace for logging process execution events
    public static final String NAMESPACE = "org.jvmxray.events.system.process";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();
    // Configuration for which operations to capture
    private static volatile String captureOperations = "E"; // Default: Execute
    private static volatile Set<Character> enabledOps = null;

    /**
     * Initialize the capture operations from agent properties.
     */
    private static void initCaptureOperations() {
        if (enabledOps == null) {
            synchronized (ProcessInterceptor.class) {
                if (enabledOps == null) {
                    try {
                        AgentProperties props = AgentInitializer.getInstance().getProperties();
                        captureOperations = props.getProperty("jvmxray.agent.sensor.process.captures", "E");
                    } catch (Exception e) {
                        captureOperations = "E"; // fallback
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
     * Logs a process execution operation with the specified details.
     */
    public static void logProcessExecution(String command, String[] args, String workingDir) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("operation", "EXECUTE");
        metadata.put("command", command);
        if (args != null && args.length > 0) {
            metadata.put("args", String.join(" ", args));
        }
        if (workingDir != null) {
            metadata.put("working_dir", workingDir);
        }
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }

    // ProcessBuilder.start() interception
    public static class ProcessBuilderStart {
        @Advice.OnMethodEnter
        public static void enter(@Advice.This ProcessBuilder pb) {
            if (shouldCapture('E')) {
                try {
                    List<String> command = pb.command();
                    if (command != null && !command.isEmpty()) {
                        String executable = command.get(0);
                        String[] args = command.size() > 1 ? 
                            command.subList(1, command.size()).toArray(new String[0]) : 
                            new String[0];
                        File workingDir = pb.directory();
                        String workingDirPath = workingDir != null ? workingDir.getAbsolutePath() : null;
                        logProcessExecution(executable, args, workingDirPath);
                    }
                } catch (Exception e) {
                    // Silently ignore errors in logging to avoid breaking the application
                }
            }
        }
    }

    // Runtime.exec() interceptions
    public static class RuntimeExec {
        @Advice.OnMethodEnter
        public static void enter(@Advice.AllArguments Object[] args) {
            if (shouldCapture('E')) {
                try {
                    if (args.length >= 1) {
                        if (args[0] instanceof String) {
                            // Runtime.exec(String command) or Runtime.exec(String command, String[] envp) 
                            // or Runtime.exec(String command, String[] envp, File dir)
                            String command = (String) args[0];
                            File workingDir = args.length >= 3 && args[2] instanceof File ? (File) args[2] : null;
                            String workingDirPath = workingDir != null ? workingDir.getAbsolutePath() : null;
                            logProcessExecution(command, null, workingDirPath);
                        } else if (args[0] instanceof String[]) {
                            // Runtime.exec(String[] cmdarray) or Runtime.exec(String[] cmdarray, String[] envp) 
                            // or Runtime.exec(String[] cmdarray, String[] envp, File dir)
                            String[] cmdarray = (String[]) args[0];
                            if (cmdarray.length > 0) {
                                String executable = cmdarray[0];
                                String[] processArgs = cmdarray.length > 1 ? 
                                    java.util.Arrays.copyOfRange(cmdarray, 1, cmdarray.length) : 
                                    new String[0];
                                File workingDir = args.length >= 3 && args[2] instanceof File ? (File) args[2] : null;
                                String workingDirPath = workingDir != null ? workingDir.getAbsolutePath() : null;
                                logProcessExecution(executable, processArgs, workingDirPath);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silently ignore errors in logging to avoid breaking the application
                }
            }
        }
    }
}