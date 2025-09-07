package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Unsafe.freeMemory() to detect direct memory deallocation.
 * 
 * @author Milton Smith
 */
public class UnsafeFreeInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

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
}