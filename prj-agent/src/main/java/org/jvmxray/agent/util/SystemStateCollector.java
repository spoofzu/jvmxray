package org.jvmxray.agent.util;

import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility class for collecting comprehensive system state information for diagnostic purposes.
 * Provides methods to gather JVM, OS, memory, threading, and runtime information.
 * Used primarily by UncaughtExceptionInterceptor for enterprise-grade bug tracking.
 * 
 * @author Milton Smith
 */
public class SystemStateCollector {
    
    // MXBean references for efficient access
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    
    /**
     * Collects basic command line information.
     */
    public static Map<String, String> collectCommandLine() {
        Map<String, String> data = new HashMap<>();
        try {
            StringJoiner cmdLine = new StringJoiner(" ");
            for (String arg : runtimeMXBean.getInputArguments()) {
                // Sanitize sensitive information
                if (!arg.contains("password") && !arg.contains("secret") && !arg.contains("key")) {
                    cmdLine.add(arg);
                }
            }
            data.put("command_line", cmdLine.toString());
            data.put("main_class", System.getProperty("sun.java.command", "unknown"));
        } catch (Exception e) {
            data.put("command_line_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects memory statistics.
     * @param detailed If true, includes detailed memory pool information
     */
    public static Map<String, String> collectMemoryStats(boolean detailed) {
        Map<String, String> data = new HashMap<>();
        try {
            MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
            
            data.put("heap_used_mb", String.format("%.2f", heap.getUsed() / (1024.0 * 1024.0)));
            data.put("heap_max_mb", String.format("%.2f", heap.getMax() / (1024.0 * 1024.0)));
            data.put("heap_committed_mb", String.format("%.2f", heap.getCommitted() / (1024.0 * 1024.0)));
            data.put("heap_utilization_pct", String.format("%.2f", (double) heap.getUsed() / heap.getMax() * 100));
            
            data.put("non_heap_used_mb", String.format("%.2f", nonHeap.getUsed() / (1024.0 * 1024.0)));
            data.put("non_heap_committed_mb", String.format("%.2f", nonHeap.getCommitted() / (1024.0 * 1024.0)));
            
            if (detailed) {
                // Add memory pool details if available
                try {
                    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                        MemoryUsage usage = pool.getUsage();
                        if (usage != null) {
                            String poolName = pool.getName().toLowerCase().replace(" ", "_");
                            data.put("pool_" + poolName + "_used_mb", String.format("%.2f", usage.getUsed() / (1024.0 * 1024.0)));
                        }
                    }
                } catch (Exception e) {
                    data.put("memory_pools_error", e.getMessage());
                }
            }
        } catch (Exception e) {
            data.put("memory_stats_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects thread statistics.
     * @param detailed If true, includes detailed thread information
     */
    public static Map<String, String> collectThreadStats(boolean detailed) {
        Map<String, String> data = new HashMap<>();
        try {
            data.put("thread_count", String.valueOf(threadMXBean.getThreadCount()));
            data.put("peak_thread_count", String.valueOf(threadMXBean.getPeakThreadCount()));
            data.put("daemon_thread_count", String.valueOf(threadMXBean.getDaemonThreadCount()));
            data.put("total_started_threads", String.valueOf(threadMXBean.getTotalStartedThreadCount()));
            
            if (detailed && threadMXBean.isCurrentThreadCpuTimeSupported()) {
                data.put("current_thread_cpu_time_ms", String.valueOf(threadMXBean.getCurrentThreadCpuTime() / 1_000_000));
            }
        } catch (Exception e) {
            data.put("thread_stats_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects runtime information.
     */
    public static Map<String, String> collectRuntimeInfo() {
        Map<String, String> data = new HashMap<>();
        try {
            data.put("jvm_name", runtimeMXBean.getVmName());
            data.put("jvm_version", runtimeMXBean.getVmVersion());
            data.put("jvm_vendor", runtimeMXBean.getVmVendor());
            data.put("java_version", System.getProperty("java.version", "unknown"));
            data.put("uptime_ms", String.valueOf(runtimeMXBean.getUptime()));
            data.put("start_time", String.valueOf(runtimeMXBean.getStartTime()));
        } catch (Exception e) {
            data.put("runtime_info_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects garbage collection statistics.
     */
    public static Map<String, String> collectGCStats() {
        Map<String, String> data = new HashMap<>();
        try {
            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                String gcName = gcBean.getName().toLowerCase().replace(" ", "_");
                data.put("gc_" + gcName + "_collections", String.valueOf(gcBean.getCollectionCount()));
                data.put("gc_" + gcName + "_time_ms", String.valueOf(gcBean.getCollectionTime()));
            }
        } catch (Exception e) {
            data.put("gc_stats_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects class loading statistics.
     */
    public static Map<String, String> collectClassLoadingStats() {
        Map<String, String> data = new HashMap<>();
        try {
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            data.put("loaded_class_count", String.valueOf(classLoadingBean.getLoadedClassCount()));
            data.put("total_loaded_class_count", String.valueOf(classLoadingBean.getTotalLoadedClassCount()));
            data.put("unloaded_class_count", String.valueOf(classLoadingBean.getUnloadedClassCount()));
        } catch (Exception e) {
            data.put("class_loading_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects operating system information.
     */
    public static Map<String, String> collectOSInfo() {
        Map<String, String> data = new HashMap<>();
        try {
            data.put("os_name", osMXBean.getName());
            data.put("os_version", osMXBean.getVersion());
            data.put("os_arch", osMXBean.getArch());
            data.put("available_processors", String.valueOf(osMXBean.getAvailableProcessors()));
            
            // Try to get load average if available
            double loadAverage = osMXBean.getSystemLoadAverage();
            if (loadAverage >= 0) {
                data.put("system_load_average", String.format("%.2f", loadAverage));
            }
        } catch (Exception e) {
            data.put("os_info_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects basic security information.
     */
    public static Map<String, String> collectSecurityInfo() {
        Map<String, String> data = new HashMap<>();
        try {
            SecurityManager sm = System.getSecurityManager();
            data.put("security_manager", sm != null ? sm.getClass().getName() : "none");
            data.put("java_security_policy", System.getProperty("java.security.policy", "default"));
        } catch (Exception e) {
            data.put("security_info_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects disk space information for temp directory.
     */
    public static Map<String, String> collectDiskSpace() {
        Map<String, String> data = new HashMap<>();
        try {
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir", "/tmp"));
            data.put("temp_dir_free_space_mb", String.format("%.2f", tempDir.getFreeSpace() / (1024.0 * 1024.0)));
            data.put("temp_dir_total_space_mb", String.format("%.2f", tempDir.getTotalSpace() / (1024.0 * 1024.0)));
        } catch (Exception e) {
            data.put("disk_space_error", e.getMessage());
        }
        return data;
    }
    
    /**
     * Collects specific information about a thread.
     * @param thread The thread to analyze
     */
    public static Map<String, String> collectThreadInfo(Thread thread) {
        Map<String, String> data = new HashMap<>();
        try {
            ThreadGroup group = thread.getThreadGroup();
            data.put("target_thread_group", group != null ? group.getName() : "null");
            data.put("target_thread_context_classloader", 
                thread.getContextClassLoader() != null ? thread.getContextClassLoader().getClass().getName() : "null");
            data.put("target_thread_interrupted", String.valueOf(thread.isInterrupted()));
        } catch (Exception e) {
            data.put("thread_info_error", e.getMessage());
        }
        return data;
    }
}