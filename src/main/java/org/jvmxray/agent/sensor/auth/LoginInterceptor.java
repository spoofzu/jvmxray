package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts LoginContext.login() to capture JAAS authentication attempts.
 * Tracks both successful and failed login attempts, including the failure reason
 * when authentication is rejected.
 *
 * @author Milton Smith
 */
public class LoginInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodEnter
    public static void enter() {
        MCCScope.enter("Auth");
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Thrown Throwable thrown) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "login");
            metadata.put("auth_mechanism", "jaas");

            if (thrown != null) {
                metadata.put("auth_success", "false");
                metadata.put("auth_failure_reason", thrown.getClass().getSimpleName());
            } else {
                metadata.put("auth_success", "true");
            }

            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Auth");
        }
    }
}
