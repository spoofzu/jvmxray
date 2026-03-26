package org.jvmxray.agent.sensor.auth;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts AuthenticationManager.authenticate(Authentication) to capture Spring Security
 * authentication attempts. Tracks success/failure and extracts the principal name from
 * the returned Authentication object on success.
 *
 * @author Milton Smith
 */
public class AuthenticateInterceptor {

    public static final String NAMESPACE = "org.jvmxray.events.auth.session";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodEnter
    public static void enter() {
        MCCScope.enter("Auth");
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Return Object authentication, @Advice.Thrown Throwable thrown) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("auth_action", "authenticate");
            metadata.put("auth_mechanism", "spring_security");

            if (thrown != null) {
                metadata.put("auth_success", "false");
                metadata.put("auth_failure_reason", thrown.getClass().getSimpleName());
            } else {
                metadata.put("auth_success", "true");

                // Reflectively extract principal name from the returned Authentication object
                String principalName = getPrincipalName(authentication);
                if (principalName != null) {
                    metadata.put("principal_name", principalName);
                }
            }

            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("Auth");
        }
    }

    /**
     * Reflectively calls getName() on the Authentication object to extract the principal name.
     * Tries Authentication.getName() first, then falls back to getPrincipal().toString().
     */
    private static String getPrincipalName(Object authentication) {
        if (authentication == null) {
            return null;
        }
        try {
            Method getNameMethod = authentication.getClass().getMethod("getName");
            Object name = getNameMethod.invoke(authentication);
            if (name != null) {
                return name.toString();
            }
        } catch (Exception e) {
            // Fail silently
        }
        return null;
    }
}
