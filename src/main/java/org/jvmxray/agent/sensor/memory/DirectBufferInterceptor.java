package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for ByteBuffer.allocateDirect() to monitor direct buffer allocation.
 * 
 * @author Milton Smith
 */
public class DirectBufferInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Memory allocation threshold for flagging large allocations (100MB)
    private static final long LARGE_ALLOCATION_THRESHOLD = 100 * 1024 * 1024;

    /**
     * Intercepts ByteBuffer.allocateDirect() to monitor direct buffer allocation.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
            
            String context = MemoryUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".direct_buffer", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}