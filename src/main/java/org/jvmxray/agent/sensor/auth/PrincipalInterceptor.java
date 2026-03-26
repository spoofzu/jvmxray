package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts Principal.getName() to capture principal identity lookups.
 * Tracks when application code queries the authenticated principal name,
 * which is useful for understanding identity resolution patterns.
 *
 * @author Milton Smith
 */
public class PrincipalInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void exit(@Advice.Return String principalName) {
        MCCScope.enter("Auth");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "get_principal");

            if (principalName != null) {
                metadata.put("principal_name", principalName);
            }

            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Auth");
        }
    }
}
