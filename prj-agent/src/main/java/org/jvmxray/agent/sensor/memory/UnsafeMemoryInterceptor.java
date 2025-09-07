package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Unsafe memory operations to detect direct memory allocation,
 * deallocation, and reallocation.
 * 
 * @author Milton Smith
 */
public class UnsafeMemoryInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Memory allocation threshold for flagging large allocations (100MB)
    private static final long LARGE_ALLOCATION_THRESHOLD = 100 * 1024 * 1024;

    /**
     * Intercepts Unsafe.allocateMemory() to detect direct memory allocation.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
            
            String context = MemoryUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".unsafe", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}