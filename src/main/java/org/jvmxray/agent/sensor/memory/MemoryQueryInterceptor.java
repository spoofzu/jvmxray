package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for Runtime memory query methods to track memory monitoring.
 * 
 * @author Milton Smith
 */
public class MemoryQueryInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts Runtime memory query methods to track memory monitoring.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
            MemoryUtils.addMemoryStats(metadata);
            
            logProxy.logMessage(NAMESPACE + ".query", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}