package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for memory operations to detect memory-based attacks,
 * resource exhaustion, and unsafe memory manipulation.
 * 
 * @author Milton Smith
 */
public class MemoryInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Memory allocation threshold for flagging large allocations (100MB)
    private static final long LARGE_ALLOCATION_THRESHOLD = 100 * 1024 * 1024;
    
    // Track memory pressure indicators
    private static volatile long lastGcTime = 0;
    private static volatile int gcCallCount = 0;

    /**
     * Intercepts Runtime.gc() and System.gc() calls to detect excessive GC requests.
     */
    @Advice.OnMethodExit
    public static void garbageCollection(@Advice.This Object caller,
                                       @Advice.Thrown Throwable throwable) {
        try {
            long currentTime = System.currentTimeMillis();
            gcCallCount++;
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "garbage_collection");
            metadata.put("caller_class", caller != null ? caller.getClass().getName() : "static");
            metadata.put("gc_call_count", String.valueOf(gcCallCount));
            
            // Check for excessive GC calls
            if (lastGcTime > 0) {
                long timeSinceLastGc = currentTime - lastGcTime;
                metadata.put("time_since_last_gc_ms", String.valueOf(timeSinceLastGc));
                
                if (timeSinceLastGc < 1000) { // Less than 1 second
                    metadata.put("frequent_gc_calls", "true");
                    metadata.put("risk_level", "MEDIUM");
                    metadata.put("potential_issue", "memory_pressure");
                }
            }
            
            lastGcTime = currentTime;
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            // Get current memory stats
            addMemoryStats(metadata);
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".gc", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Runtime memory query methods to track memory monitoring.
     */
    @Advice.OnMethodExit
    public static void memoryQuery(@Advice.This Object runtime,
                                 @Advice.Return long result,
                                 @Advice.Origin String methodName,
                                 @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "memory_query");
            metadata.put("method", methodName);
            metadata.put("result_bytes", String.valueOf(result));
            metadata.put("result_mb", String.format("%.2f", result / (1024.0 * 1024.0)));
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            // Add comprehensive memory stats
            addMemoryStats(metadata);
            
            logProxy.logMessage(NAMESPACE + ".query", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Unsafe.allocateMemory() to detect direct memory allocation.
     */
    @Advice.OnMethodExit
    public static void unsafeAllocateMemory(@Advice.This Object unsafe,
                                          @Advice.Argument(0) long size,
                                          @Advice.Return long address,
                                          @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "unsafe_allocate_memory");
            metadata.put("unsafe_class", unsafe.getClass().getName());
            metadata.put("requested_size", String.valueOf(size));
            metadata.put("size_mb", String.format("%.2f", size / (1024.0 * 1024.0)));
            metadata.put("allocation_success", throwable == null ? "true" : "false");
            
            if (throwable == null && address != 0) {
                metadata.put("allocated_address", "0x" + Long.toHexString(address));
            }
            
            // Flag large allocations
            if (size > LARGE_ALLOCATION_THRESHOLD) {
                metadata.put("large_allocation", "true");
                metadata.put("risk_level", "HIGH");
                metadata.put("potential_issue", "memory_exhaustion");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".unsafe", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Unsafe.freeMemory() to detect direct memory deallocation.
     */
    @Advice.OnMethodExit
    public static void unsafeFreeMemory(@Advice.This Object unsafe,
                                      @Advice.Argument(0) long address,
                                      @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "unsafe_free_memory");
            metadata.put("unsafe_class", unsafe.getClass().getName());
            metadata.put("freed_address", "0x" + Long.toHexString(address));
            metadata.put("free_success", throwable == null ? "true" : "false");
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".unsafe", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Unsafe.reallocateMemory() to detect memory reallocation.
     */
    @Advice.OnMethodExit
    public static void unsafeReallocateMemory(@Advice.This Object unsafe,
                                            @Advice.Argument(0) long address,
                                            @Advice.Argument(1) long newSize,
                                            @Advice.Return long newAddress,
                                            @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "unsafe_reallocate_memory");
            metadata.put("unsafe_class", unsafe.getClass().getName());
            metadata.put("old_address", "0x" + Long.toHexString(address));
            metadata.put("new_size", String.valueOf(newSize));
            metadata.put("new_size_mb", String.format("%.2f", newSize / (1024.0 * 1024.0)));
            metadata.put("reallocation_success", throwable == null ? "true" : "false");
            
            if (throwable == null && newAddress != 0) {
                metadata.put("new_address", "0x" + Long.toHexString(newAddress));
            }
            
            // Flag large reallocations
            if (newSize > LARGE_ALLOCATION_THRESHOLD) {
                metadata.put("large_reallocation", "true");
                metadata.put("risk_level", "MEDIUM");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".unsafe", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts ByteBuffer.allocateDirect() to monitor direct buffer allocation.
     */
    @Advice.OnMethodExit
    public static void byteBufferAllocateDirect(@Advice.Argument(0) int capacity,
                                              @Advice.Return Object buffer,
                                              @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "bytebuffer_allocate_direct");
            metadata.put("capacity", String.valueOf(capacity));
            metadata.put("capacity_mb", String.format("%.2f", capacity / (1024.0 * 1024.0)));
            metadata.put("allocation_success", throwable == null && buffer != null ? "true" : "false");
            
            if (buffer != null) {
                metadata.put("buffer_class", buffer.getClass().getName());
            }
            
            // Flag large direct buffer allocations
            if (capacity > LARGE_ALLOCATION_THRESHOLD) {
                metadata.put("large_direct_allocation", "true");
                metadata.put("risk_level", "MEDIUM");
                metadata.put("potential_issue", "off_heap_memory_pressure");
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            String context = analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".direct_buffer", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    // Helper methods
    private static void addMemoryStats(Map<String, String> metadata) {
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

    private static String analyzeCallContext() {
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