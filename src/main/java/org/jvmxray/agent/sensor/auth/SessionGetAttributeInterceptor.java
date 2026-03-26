package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts HttpSession.getAttribute(String) to capture session attribute reads
 * that may indicate authentication context lookups (e.g., retrieving user identity,
 * roles, or session tokens).
 *
 * @author Milton Smith
 */
public class SessionGetAttributeInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void exit(@Advice.This Object session, @Advice.Argument(0) String attributeName) {
        MCCScope.enter("Auth");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "session_get");
            metadata.put("attribute_name", attributeName != null ? attributeName : "unknown");

            // Reflectively get session ID and hash it for privacy
            String hashedId = getHashedSessionId(session);
            if (hashedId != null) {
                metadata.put("session_id", hashedId);
            }

            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Auth");
        }
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
