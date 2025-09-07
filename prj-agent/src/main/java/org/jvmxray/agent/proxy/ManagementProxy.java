package org.jvmxray.agent.proxy;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.Comparator;
import java.time.Duration;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Proxy class for retrieving system and JVM management information.
 * Collects metrics such as process ID, memory usage, disk space, command line,
 * top CPU-consuming processes, and open file descriptors.
 *
 * @author Milton Smith
 */
public class ManagementProxy {

    /**
     * Retrieves a map of system and JVM management information.
     *
     * @return A {@code Map<String, String>} containing key-value pairs of management metrics,
     *         including process ID, memory usage, disk space, command line, top processes,
     *         and open file descriptors.
     */
    public static Map<String, String> getManagementInfo() {
        // Initialize map to store management metrics
        Map<String, String> keypairs = new HashMap<>();

        // Collect process ID
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String processId = runtimeMXBean.getName().split("@")[0];
        keypairs.put("process_id", processId);

        // Collect memory usage information
        java.lang.management.MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        keypairs.put("heap_used_mb", String.format("%.2f", heapUsed / (1024.0 * 1024.0)));
        keypairs.put("heap_max_mb", String.format("%.2f", heapMax / (1024.0 * 1024.0)));
        keypairs.put("heap_committed_mb", String.format("%.2f", heapCommitted / (1024.0 * 1024.0)));
        keypairs.put("non_heap_used_mb", String.format("%.2f", nonHeapUsed / (1024.0 * 1024.0)));
        keypairs.put("non_heap_max_mb", nonHeapMax < 0 ? "unlimited" : String.format("%.2f", nonHeapMax / (1024.0 * 1024.0)));

        // Collect disk space information
        File root = new File(System.getProperty("user.dir"));
        long diskFree = root.getFreeSpace();
        long diskTotal = root.getTotalSpace();
        keypairs.put("disk_free_gb", String.format("%.2f", diskFree / (1024.0 * 1024.0 * 1024.0)));
        keypairs.put("disk_total_gb", String.format("%.2f", diskTotal / (1024.0 * 1024.0 * 1024.0)));

        // Collect command line arguments
        StringJoiner commandLine = new StringJoiner(" ");
        commandLine.add(System.getProperty("sun.java.command", "unknown"));
        runtimeMXBean.getInputArguments().forEach(commandLine::add);
        keypairs.put("command_line", commandLine.toString());

        // Collect top-10 CPU-consuming processes
        StringJoiner processes = new StringJoiner(", ");
        Stream<ProcessHandle> processStream = ProcessHandle.allProcesses()
                .sorted(Comparator.comparing(
                        p -> p.info().totalCpuDuration().orElse(Duration.ZERO),
                        Comparator.reverseOrder()))
                .limit(10);
        processStream.forEach(process -> {
            ProcessHandle.Info info = process.info();
            long cpuMillis = info.totalCpuDuration().map(Duration::toMillis).orElse(0L);
            processes.add(String.format("PID=%d, Command=%s, User=%s, CPU_ms=%d",
                    process.pid(), info.command().orElse("unknown"), info.user().orElse("unknown"), cpuMillis));
        });
        keypairs.put("processes", processes.toString());

        // Collect open file descriptor count (Linux and macOS only)
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                // Check /proc/self/fd for open file descriptors
                File fdDir = new File("/proc/self/fd");
                if (fdDir.exists() && fdDir.isDirectory()) {
                    int openFiles = fdDir.list().length;
                    keypairs.put("open_file_descriptors", String.valueOf(openFiles));
                } else {
                    keypairs.put("open_file_descriptors", "Unable to access /proc/self/fd");
                }
            } else if (osName.contains("mac")) {
                // Use lsof command to count open file descriptors
                ProcessBuilder pb = new ProcessBuilder("lsof", "-p", processId);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    long openFiles = reader.lines()
                            .filter(line -> !line.startsWith("COMMAND")) // Skip header
                            .count();
                    process.waitFor(); // Ensure process completes
                    keypairs.put("open_file_descriptors", String.valueOf(openFiles));
                }
            } else {
                // Unsupported OS
                keypairs.put("open_file_descriptors", "Unsupported OS: " + osName);
            }
        } catch (Exception e) {
            keypairs.put("open_file_descriptors", "Error: " + e.getMessage());
        }

        return keypairs;
    }
}