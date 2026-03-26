package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts HttpSession.setAttribute(String, Object) to capture session attribute
 * mutations that may indicate authentication state changes (e.g., storing user identity,
 * roles, or tokens in the session).
 *
 * @author Milton Smith
 */
public class SessionSetAttributeInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodEnter
    public static void enter() {
        MCCScope.enter("Auth");
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.This Object session, @Advice.Argument(0) String attributeName) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "session_set");
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
