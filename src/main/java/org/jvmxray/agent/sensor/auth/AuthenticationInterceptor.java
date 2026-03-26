package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.auth";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void sessionOperation(@Advice.Origin String method) {
        MCCScope.enter("Auth");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "session_operation");
            metadata.put("method", method);
            logProxy.logMessage(NAMESPACE + ".session", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Auth");
        }
    }
}