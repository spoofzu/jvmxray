package org.jvmxray.agent.sensor.api;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;
import java.util.HashMap;
import java.util.Map;

public class APICallInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.api";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void apiCall(@Advice.Origin String method) {
        MCCScope.enter("API");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "api_call");
            metadata.put("method", method);
            logProxy.logMessage(NAMESPACE + ".call", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("API");
        }
    }
}