package org.jvmxray.agent.sensor.memory;

import java.util.Map;

/**
 * Utility methods shared by memory interceptors.
 * 
 * @author Milton Smith
 */
public class MemoryUtils {
    
    /**
     * Adds current memory statistics to the metadata map.
     */
    public static void addMemoryStats(Map<String, String> metadata) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            metadata.put("total_memory_mb", String.format("%.2f", totalMemory / (1024.0 * 1024.0)));
            metadata.put("used_memory_mb", String.format("%.2f", usedMemory / (1024.0 * 1024.0)));
            metadata.put("free_memory_mb", String.format("%.2f", freeMemory / (1024.0 * 1024.0)));
            metadata.put("max_memory_mb", String.format("%.2f", maxMemory / (1024.0 * 1024.0)));
            
            // Calculate memory utilization percentage
            double utilizationPercent = (double) usedMemory / maxMemory * 100;
            metadata.put("memory_utilization_percent", String.format("%.2f", utilizationPercent));
            
            // Flag high memory utilization
            if (utilizationPercent > 85) {
                metadata.put("high_memory_utilization", "true");
                if (!metadata.containsKey("risk_level")) {
                    metadata.put("risk_level", "MEDIUM");
                }
            }
            
        } catch (Exception e) {
            metadata.put("memory_stats_error", e.getMessage());
        }
    }

    /**
     * Analyzes the call context to identify the originating class/method.
     */
    public static String analyzeCallContext() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("org.jvmxray") && 
                    !className.startsWith("java.lang.Runtime") &&
                    !className.startsWith("java.lang.System") &&
                    !className.startsWith("java.nio.ByteBuffer") &&
                    !className.startsWith("sun.misc") &&
                    !className.startsWith("jdk.internal") &&
                    !className.startsWith("net.bytebuddy")) {
                    return className + "." + element.getMethodName();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }
}