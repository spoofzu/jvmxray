package org.jvmxray.agent.sensor.system;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.util.MCCScope;

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

    // ProcessBuilder.start() interception
    public static class ProcessBuilderStart {
        @Advice.OnMethodEnter
        public static long enter() {
            MCCScope.enter("Process");
            return System.nanoTime();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter long startTime,
                                @Advice.This ProcessBuilder pb,
                                @Advice.Thrown Throwable thrown) {
            try {
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

                            Map<String, String> metadata = new HashMap<>();
                            metadata.put("operation", "EXECUTE");
                            metadata.put("command", executable);
                            if (args.length > 0) {
                                metadata.put("args", String.join(" ", args));
                            }
                            if (workingDirPath != null) {
                                metadata.put("working_dir", workingDirPath);
                            }
                            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                            metadata.put("execution_time_ms", String.valueOf(durationMs));
                            if (thrown != null) {
                                metadata.put("status", "failed");
                                metadata.put("error_class", thrown.getClass().getSimpleName());
                                metadata.put("error_message", thrown.getMessage());
                            } else {
                                metadata.put("status", "started");
                            }
                            logProxy.logMessage(NAMESPACE, "INFO", metadata);
                        }
                    } catch (Exception e) {
                        // Silently ignore errors in logging to avoid breaking the application
                    }
                }
            } finally {
                MCCScope.exit("Process");
            }
        }
    }

    // Runtime.exec() interceptions
    public static class RuntimeExec {
        @Advice.OnMethodEnter
        public static long enter() {
            MCCScope.enter("Process");
            return System.nanoTime();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter long startTime,
                                @Advice.AllArguments Object[] args,
                                @Advice.Thrown Throwable thrown) {
            try {
                if (shouldCapture('E')) {
                    try {
                        if (args.length >= 1) {
                            String command = null;
                            String[] processArgs = null;
                            String workingDirPath = null;

                            if (args[0] instanceof String) {
                                // Runtime.exec(String command) or Runtime.exec(String command, String[] envp)
                                // or Runtime.exec(String command, String[] envp, File dir)
                                command = (String) args[0];
                                File workingDir = args.length >= 3 && args[2] instanceof File ? (File) args[2] : null;
                                workingDirPath = workingDir != null ? workingDir.getAbsolutePath() : null;
                            } else if (args[0] instanceof String[]) {
                                // Runtime.exec(String[] cmdarray) or Runtime.exec(String[] cmdarray, String[] envp)
                                // or Runtime.exec(String[] cmdarray, String[] envp, File dir)
                                String[] cmdarray = (String[]) args[0];
                                if (cmdarray.length > 0) {
                                    command = cmdarray[0];
                                    processArgs = cmdarray.length > 1 ?
                                        java.util.Arrays.copyOfRange(cmdarray, 1, cmdarray.length) :
                                        new String[0];
                                    File workingDir = args.length >= 3 && args[2] instanceof File ? (File) args[2] : null;
                                    workingDirPath = workingDir != null ? workingDir.getAbsolutePath() : null;
                                }
                            }

                            if (command != null) {
                                Map<String, String> metadata = new HashMap<>();
                                metadata.put("operation", "EXECUTE");
                                metadata.put("command", command);
                                if (processArgs != null && processArgs.length > 0) {
                                    metadata.put("args", String.join(" ", processArgs));
                                }
                                if (workingDirPath != null) {
                                    metadata.put("working_dir", workingDirPath);
                                }
                                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                                metadata.put("execution_time_ms", String.valueOf(durationMs));
                                if (thrown != null) {
                                    metadata.put("status", "failed");
                                    metadata.put("error_class", thrown.getClass().getSimpleName());
                                    metadata.put("error_message", thrown.getMessage());
                                } else {
                                    metadata.put("status", "started");
                                }
                                logProxy.logMessage(NAMESPACE, "INFO", metadata);
                            }
                        }
                    } catch (Exception e) {
                        // Silently ignore errors in logging to avoid breaking the application
                    }
                }
            } finally {
                MCCScope.exit("Process");
            }
        }
    }
}