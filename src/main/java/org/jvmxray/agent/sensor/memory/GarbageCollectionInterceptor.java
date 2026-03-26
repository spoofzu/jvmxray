package org.jvmxray.agent.sensor.memory;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for garbage collection operations to detect excessive GC requests
 * and memory pressure indicators.
 * 
 * @author Milton Smith
 */
public class GarbageCollectionInterceptor {
    
    // Namespace for logging memory events
    public static final String NAMESPACE = "org.jvmxray.events.memory";
    public static final LogProxy logProxy = LogProxy.getInstance();
    
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
            MemoryUtils.addMemoryStats(metadata);
            
            String context = MemoryUtils.analyzeCallContext();
            metadata.put("call_context", context);
            
            logProxy.logMessage(NAMESPACE + ".gc", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}