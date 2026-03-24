package org.jvmxray.agent.sensor.monitor;

import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for enhanced JVM monitoring metrics.
 * Provides rate calculations, anomaly detection, and additional system metrics.
 *
 * @author Milton Smith
 */
public class MonitorUtils {

    // Previous values for rate calculations
    private static final AtomicLong previousGcCount = new AtomicLong(0);
    private static final AtomicLong previousGcTime = new AtomicLong(0);
    private static final AtomicLong previousThreadsStarted = new AtomicLong(0);
    private static final AtomicLong previousHeapUsed = new AtomicLong(0);
    private static final AtomicLong lastSampleTime = new AtomicLong(System.currentTimeMillis());

    // Baseline metrics for anomaly detection
    private static volatile double baselineHeapUsed = -1;
    private static volatile double baselineCpuLoad = -1;
    private static volatile int baselineThreadCount = -1;
    private static volatile int sampleCount = 0;
    private static final int BASELINE_SAMPLES = 5; // Number of samples to establish baseline

    // Anomaly thresholds
    private static final double MEMORY_ANOMALY_THRESHOLD = 0.3; // 30% deviation
    private static final double CPU_ANOMALY_THRESHOLD = 0.5; // 50% deviation
    private static final double THREAD_ANOMALY_THRESHOLD = 0.5; // 50% deviation

    /**
     * Collects enhanced monitoring metrics.
     *
     * @return Map of enhanced metrics
     */
    public static Map<String, String> getEnhancedMetrics() {
        Map<String, String> metrics = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastSampleTime.get();

        // Collect classloader metrics
        collectClassloaderMetrics(metrics);

        // Collect native memory metrics
        collectNativeMemoryMetrics(metrics);

        // Collect rate-based metrics
        collectRateMetrics(metrics, timeDelta);

        // Collect anomaly detection metrics
        collectAnomalyMetrics(metrics);

        // Collect security and agent health metrics
        collectSecurityMetrics(metrics);
        collectAgentHealthMetrics(metrics);

        // Update last sample time
        lastSampleTime.set(currentTime);

        return metrics;
    }

    /**
     * Collects classloader-related metrics.
     */
    private static void collectClassloaderMetrics(Map<String, String> metrics) {
        try {
            ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

            metrics.put("classloader_loaded_count", String.valueOf(classLoadingMXBean.getLoadedClassCount()));
            metrics.put("classloader_total_loaded", String.valueOf(classLoadingMXBean.getTotalLoadedClassCount()));
            metrics.put("classloader_unloaded_count", String.valueOf(classLoadingMXBean.getUnloadedClassCount()));

        } catch (Exception e) {
            metrics.put("classloader_metrics_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Collects native/direct memory metrics from BufferPoolMXBeans.
     */
    private static void collectNativeMemoryMetrics(Map<String, String> metrics) {
        try {
            List<BufferPoolMXBean> bufferPools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

            long totalDirectMemory = 0;
            long totalMappedMemory = 0;

            for (BufferPoolMXBean pool : bufferPools) {
                String name = pool.getName().toLowerCase();
                long memoryUsed = pool.getMemoryUsed();
                long count = pool.getCount();

                if (name.contains("direct")) {
                    totalDirectMemory += memoryUsed;
                    metrics.put("direct_buffer_count", String.valueOf(count));
                    metrics.put("direct_buffer_memory_bytes", String.valueOf(memoryUsed));
                } else if (name.contains("mapped")) {
                    totalMappedMemory += memoryUsed;
                    metrics.put("mapped_buffer_count", String.valueOf(count));
                    metrics.put("mapped_buffer_memory_bytes", String.valueOf(memoryUsed));
                }
            }

            metrics.put("native_memory_used_bytes", String.valueOf(totalDirectMemory + totalMappedMemory));

        } catch (Exception e) {
            metrics.put("native_memory_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Collects rate-based metrics (GC frequency, thread creation rate).
     */
    private static void collectRateMetrics(Map<String, String> metrics, long timeDeltaMs) {
        try {
            // GC rate calculation
            List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
            long currentGcCount = 0;
            long currentGcTime = 0;

            for (GarbageCollectorMXBean gcBean : gcMXBeans) {
                currentGcCount += gcBean.getCollectionCount();
                currentGcTime += gcBean.getCollectionTime();
            }

            if (timeDeltaMs > 0) {
                long gcCountDelta = currentGcCount - previousGcCount.get();
                double gcFrequencyPerMinute = (gcCountDelta * 60000.0) / timeDeltaMs;
                metrics.put("gc_frequency_per_minute", String.format("%.2f", gcFrequencyPerMinute));

                // GC time percentage
                long gcTimeDelta = currentGcTime - previousGcTime.get();
                double gcTimePercent = (gcTimeDelta * 100.0) / timeDeltaMs;
                metrics.put("gc_time_percent", String.format("%.2f", gcTimePercent));
            }

            previousGcCount.set(currentGcCount);
            previousGcTime.set(currentGcTime);

            // Thread creation rate
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long currentThreadsStarted = threadMXBean.getTotalStartedThreadCount();

            if (timeDeltaMs > 0) {
                long threadsDelta = currentThreadsStarted - previousThreadsStarted.get();
                double threadCreationRate = (threadsDelta * 60000.0) / timeDeltaMs;
                metrics.put("thread_creation_rate_per_minute", String.format("%.2f", threadCreationRate));
            }

            previousThreadsStarted.set(currentThreadsStarted);
            metrics.put("total_threads_started", String.valueOf(currentThreadsStarted));

        } catch (Exception e) {
            metrics.put("rate_metrics_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Collects anomaly detection metrics.
     */
    private static void collectAnomalyMetrics(Map<String, String> metrics) {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

            // Current values
            long currentHeapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            int currentThreadCount = threadMXBean.getThreadCount();
            double currentCpuLoad = -1;

            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                currentCpuLoad = ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
            }

            // Memory leak indicator (consecutive increases)
            long prevHeap = previousHeapUsed.get();
            if (prevHeap > 0 && currentHeapUsed > prevHeap) {
                double increasePercent = ((double) (currentHeapUsed - prevHeap) / prevHeap) * 100;
                if (increasePercent > 10) {
                    metrics.put("memory_leak_indicator", "possible");
                    metrics.put("memory_increase_percent", String.format("%.2f", increasePercent));
                }
            }
            previousHeapUsed.set(currentHeapUsed);

            // Baseline establishment
            sampleCount++;
            if (sampleCount <= BASELINE_SAMPLES) {
                // Build baseline
                if (baselineHeapUsed < 0) {
                    baselineHeapUsed = currentHeapUsed;
                    baselineCpuLoad = currentCpuLoad >= 0 ? currentCpuLoad : 0;
                    baselineThreadCount = currentThreadCount;
                } else {
                    // Rolling average for baseline
                    baselineHeapUsed = (baselineHeapUsed * (sampleCount - 1) + currentHeapUsed) / sampleCount;
                    if (currentCpuLoad >= 0) {
                        baselineCpuLoad = (baselineCpuLoad * (sampleCount - 1) + currentCpuLoad) / sampleCount;
                    }
                    baselineThreadCount = (int) ((baselineThreadCount * (sampleCount - 1) + currentThreadCount) / sampleCount);
                }
                metrics.put("baseline_status", "establishing");
            } else {
                // Anomaly detection
                metrics.put("baseline_status", "established");

                boolean anomalyDetected = false;
                StringBuilder anomalyTypes = new StringBuilder();

                // Memory anomaly
                double memoryDeviation = Math.abs(currentHeapUsed - baselineHeapUsed) / baselineHeapUsed;
                metrics.put("memory_baseline_deviation", String.format("%.2f", memoryDeviation * 100) + "%");
                if (memoryDeviation > MEMORY_ANOMALY_THRESHOLD) {
                    anomalyDetected = true;
                    anomalyTypes.append("memory_spike,");
                }

                // CPU anomaly
                if (currentCpuLoad >= 0 && baselineCpuLoad > 0) {
                    double cpuDeviation = Math.abs(currentCpuLoad - baselineCpuLoad) / Math.max(baselineCpuLoad, 0.01);
                    metrics.put("cpu_baseline_deviation", String.format("%.2f", cpuDeviation * 100) + "%");
                    if (cpuDeviation > CPU_ANOMALY_THRESHOLD) {
                        anomalyDetected = true;
                        anomalyTypes.append("cpu_spike,");
                    }
                }

                // Thread anomaly
                double threadDeviation = Math.abs(currentThreadCount - baselineThreadCount) / (double) Math.max(baselineThreadCount, 1);
                metrics.put("thread_baseline_deviation", String.format("%.2f", threadDeviation * 100) + "%");
                if (threadDeviation > THREAD_ANOMALY_THRESHOLD) {
                    anomalyDetected = true;
                    anomalyTypes.append("thread_anomaly,");
                }

                metrics.put("anomaly_detected", String.valueOf(anomalyDetected));
                if (anomalyDetected) {
                    String types = anomalyTypes.toString();
                    metrics.put("anomaly_type", types.endsWith(",") ? types.substring(0, types.length() - 1) : types);
                }
            }

        } catch (Exception e) {
            metrics.put("anomaly_metrics_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Collects security-related metrics.
     */
    private static void collectSecurityMetrics(Map<String, String> metrics) {
        try {
            // Security Manager
            SecurityManager sm = System.getSecurityManager();
            metrics.put("security_manager_present", String.valueOf(sm != null));
            if (sm != null) {
                metrics.put("security_manager_class", sm.getClass().getName());
            }

            // JMX remote check
            String jmxRemote = System.getProperty("com.sun.management.jmxremote");
            String jmxPort = System.getProperty("com.sun.management.jmxremote.port");
            metrics.put("jmx_remote_enabled", String.valueOf(jmxRemote != null || jmxPort != null));

            // Debug mode check
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<String> jvmArgs = runtimeMXBean.getInputArguments();
            boolean debugEnabled = false;
            for (String arg : jvmArgs) {
                if (arg.contains("-agentlib:jdwp") || arg.contains("-Xdebug")) {
                    debugEnabled = true;
                    break;
                }
            }
            metrics.put("debug_mode_enabled", String.valueOf(debugEnabled));

        } catch (Exception e) {
            metrics.put("security_metrics_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Collects agent health metrics.
     */
    private static void collectAgentHealthMetrics(Map<String, String> metrics) {
        try {
            // Calculate agent health based on various factors
            int healthScore = 100;
            StringBuilder healthIssues = new StringBuilder();

            // Check memory pressure
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();

            if (heapUtilization > 0.9) {
                healthScore -= 30;
                healthIssues.append("high_memory_pressure,");
            } else if (heapUtilization > 0.8) {
                healthScore -= 15;
                healthIssues.append("elevated_memory,");
            }

            // Check for deadlocks
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                healthScore -= 50;
                healthIssues.append("deadlocks_detected,");
            }

            // Check thread count
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            if (threadCount > peakThreadCount * 0.95 && threadCount > 100) {
                healthScore -= 10;
                healthIssues.append("high_thread_count,");
            }

            // Check GC pressure
            List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gcBean : gcMXBeans) {
                totalGcTime += gcBean.getCollectionTime();
            }
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            long uptime = runtimeMXBean.getUptime();
            if (uptime > 0) {
                double gcTimePercent = (totalGcTime * 100.0) / uptime;
                if (gcTimePercent > 10) {
                    healthScore -= 20;
                    healthIssues.append("high_gc_overhead,");
                }
            }

            // Ensure health score is in valid range
            healthScore = Math.max(0, Math.min(100, healthScore));

            // Determine health status
            String healthStatus;
            if (healthScore >= 80) {
                healthStatus = "healthy";
            } else if (healthScore >= 50) {
                healthStatus = "degraded";
            } else {
                healthStatus = "critical";
            }

            metrics.put("agent_health_score", String.valueOf(healthScore));
            metrics.put("agent_health_status", healthStatus);
            if (healthIssues.length() > 0) {
                String issues = healthIssues.toString();
                metrics.put("agent_health_issues", issues.endsWith(",") ? issues.substring(0, issues.length() - 1) : issues);
            }

            // Peak thread count
            metrics.put("peak_thread_count", String.valueOf(peakThreadCount));
            metrics.put("heap_utilization_percent", String.format("%.1f", heapUtilization * 100));

        } catch (Exception e) {
            metrics.put("agent_health_error", e.getClass().getSimpleName());
        }
    }

    /**
     * Resets baseline metrics (useful for testing or after major application changes).
     */
    public static void resetBaseline() {
        baselineHeapUsed = -1;
        baselineCpuLoad = -1;
        baselineThreadCount = -1;
        sampleCount = 0;
    }
}
