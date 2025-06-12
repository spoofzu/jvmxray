package org.jvmxray.agent.util.sensor;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage request context for HTTP request-response correlation.
 */
public class RequestContextHolder {
    private static final ThreadLocal<Map<String, String>> requestContext = ThreadLocal.withInitial(HashMap::new);

    /**
     * Gets the request context for the current thread.
     * @return The context map.
     */
    public static Map<String, String> getContext() {
        return requestContext.get();
    }

    /**
     * Clears the request context for the current thread.
     */
    public static void clearContext() {
        requestContext.remove();
    }
}