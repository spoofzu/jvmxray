package org.jvmxray.agent.sensor.thread;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import java.util.HashMap;
import java.util.Map;

public class ThreadInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.thread";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void threadOperation(@Advice.Origin String method, @Advice.This Object thread) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "thread_operation");
            metadata.put("method", method);
            if (thread != null) {
                metadata.put("thread_class", thread.getClass().getName());
            }
            logProxy.logMessage(NAMESPACE + ".lifecycle", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        }
    }
}