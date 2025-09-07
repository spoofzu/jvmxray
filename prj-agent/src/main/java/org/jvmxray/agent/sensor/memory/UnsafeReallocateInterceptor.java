package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Unsafe.reallocateMemory() to detect memory reallocation.
 * 
 * @author Milton Smith
 */
public class UnsafeReallocateInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Memory allocation threshold for flagging large allocations (100MB)
    private static final long LARGE_ALLOCATION_THRESHOLD = 100 * 1024 * 1024;

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
}