package org.jvmxray.agent.sensor.monitor;

import com.sun.management.UnixOperatingSystemMXBean;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sensor implementation for monitoring and logging JVM system statistics.
 * Periodically collects metrics such as memory usage, thread states, CPU load,
 * garbage collection activity, and open file descriptors, logging them via
 * {@link LogProxy}. Runs in a background thread to provide continuous monitoring.
 *
 * @author Milton Smith
 */
public class MonitorSensor extends AbstractSensor implements Sensor {
    // Namespace for logging system statistics events
    private static final String NAMESPACE = "org.jvmxray.events.monitor";
    // Singleton instance of LogProxy for logging
    private static final LogProxy logProxy = LogProxy.getInstance();
    // Decimal format for formatting memory and CPU metrics
    private static final DecimalFormat df = new DecimalFormat("#.#");
    // Background thread for periodic monitoring
    private Thread monitoringThread;

    // Static sensor identity.
    private static final String SENSOR_GUID = "74AD1687-A129-4535-A82A-ED30E0435ADA"; // Generated via uuidgen

    public MonitorSensor(String propertySuffix) {
        super(propertySuffix);
    }

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's identity is, "74AD1687-A129-4535-A82A-ED30E0435ADA".
     */
    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    /**
     * Initializes the sensor by starting a background thread to periodically
     * collect and log system statistics.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // Create and start a daemon thread for monitoring
        monitoringThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    logSystemStats();
                    Thread.sleep(60000); // Log every 60 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    break;
                } catch (Exception e) {
                    // Log any errors during monitoring
                    logProxy.logMessage(NAMESPACE, "ERROR", Map.of("message", "Monitoring failed: " + e.getMessage()));
                }
            }
        }, "jvmxray.monitor-1");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    /**
     * Shuts down the sensor by interrupting the background monitoring thread.
     */
    @Override
    public void shutdown() {
        if (monitoringThread != null) {
            // Interrupt the monitoring thread to stop it
            monitoringThread.interrupt();
        }
    }

    /**
     * Collects and logs system statistics, including memory, threads, CPU usage,
     * garbage collection, non-heap memory, and deadlocked threads.
     */
    private void logSystemStats() {
        // Initialize management beans
        Runtime rt = Runtime.getRuntime();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // Initialize metadata for logging
        Map<String, String> stats = new HashMap<>();

        // Collect memory metrics
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long maxMemory = rt.maxMemory();
        stats.put("MemoryTotal", formatMB(totalMemory));
        stats.put("MemoryFree", formatMB(freeMemory));
        stats.put("MemoryMax", formatMB(maxMemory));

        // Collect thread state counts
        stats.put("ThreadNew", String.valueOf(countThreads(Thread.State.NEW)));
        stats.put("ThreadRunnable", String.valueOf(countThreads(Thread.State.RUNNABLE)));
        stats.put("ThreadBlocked", String.valueOf(countThreads(Thread.State.BLOCKED)));
        stats.put("ThreadWaiting", String.valueOf(countThreads(Thread.State.WAITING) + countThreads(Thread.State.TIMED_WAITING)));
        stats.put("ThreadTerminated", String.valueOf(countThreads(Thread.State.TERMINATED)));

        // Collect open file descriptor count (Unix systems only)
        if (osMXBean instanceof UnixOperatingSystemMXBean) {
            stats.put("OpenFiles", Long.toString(((UnixOperatingSystemMXBean) osMXBean).getOpenFileDescriptorCount()));
        } else {
            stats.put("OpenFiles", "Unavailable");
        }

        // Collect CPU usage
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            double processCpuLoad = ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
            if (processCpuLoad >= 0) {
                stats.put("ProcessCpuLoad", df.format(processCpuLoad * 100) + "%");
            }
        }

        // Collect garbage collection statistics
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
            gcCount += gcMXBean.getCollectionCount();
            gcTime += gcMXBean.getCollectionTime();
        }
        stats.put("GCCount", String.valueOf(gcCount));
        stats.put("GCTime", gcTime + "ms");

        // Collect non-heap memory usage
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        stats.put("NonHeapUsed", formatMB(nonHeap.getUsed()));

        // Collect deadlocked thread count
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        stats.put("DeadlockedThreads", String.valueOf(deadlockedThreads != null ? deadlockedThreads.length : 0));

        // Log the system statistics
        logProxy.logMessage(NAMESPACE, "INFO", stats);
    }

    /**
     * Counts the number of threads in the specified state.
     *
     * @param state The {@link Thread.State} to count.
     * @return The number of threads in the specified state.
     */
    private long countThreads(Thread.State state) {
        // Allocate array with extra capacity to handle thread count fluctuations
        Thread[] threads = new Thread[Thread.activeCount() + 10];
        int count = Thread.enumerate(threads);
        long stateCount = 0;
        // Count threads in the specified state
        for (int i = 0; i < count; i++) {
            if (threads[i].getState() == state) {
                stateCount++;
            }
        }
        return stateCount;
    }

    /**
     * Formats a byte value into megabytes (MB) or gigabytes (GB) as appropriate.
     *
     * @param bytes The number of bytes to format.
     * @return A formatted string representing the size in MB or GB.
     */
    private String formatMB(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb > 1024) {
            return df.format(mb / 1024) + "GB";
        }
        return df.format(mb) + "MB";
    }
}