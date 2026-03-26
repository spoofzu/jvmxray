package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts HttpSession.invalidate() to capture session destruction events.
 * Session invalidation is a security-relevant event that indicates logout,
 * session timeout, or forced session termination.
 *
 * @author Milton Smith
 */
public class SessionInvalidateInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodEnter
    public static void enter(@Advice.This Object session) {
        MCCScope.enter("Auth");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "session_invalidate");

            // Capture session ID before invalidation (it may not be accessible after)
            String hashedId = getHashedSessionId(session);
            if (hashedId != null) {
                metadata.put("session_id", hashedId);
            }

            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        }
    }

    @Advice.OnMethodExit
    public static void exit() {
        MCCScope.exit("Auth");
    }

    /**
     * Reflectively calls getId() on the session object and hashes the result for privacy.
     */
    private static String getHashedSessionId(Object session) {
        try {
            Method getIdMethod = session.getClass().getMethod("getId");
            Object sessionId = getIdMethod.invoke(session);
            if (sessionId != null) {
                return Integer.toHexString(sessionId.hashCode());
            }
        } catch (Exception e) {
            // Fail silently
        }
        return null;
    }
}
