package org.jvmxray.agent.sensor.http;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.util.sensor.RequestContextHolder;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Interceptor class for HTTP servlet requests and responses.
 * Uses Byte Buddy to instrument HTTP servlet methods, logging request and response details
 * via the LogProxy.
 *
 * @author Milton Smith
 */
public class HttpInterceptor {
    // Namespace for HTTP request events
    public static final String REQ_NAMESPACE = "org.jvmxray.events.http.request";
    // Namespace for HTTP response events
    public static final String RES_NAMESPACE = "org.jvmxray.events.http.response";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of an HTTP servlet method to log request details.
     *
     * @param servlet The servlet instance being intercepted.
     * @param request The HTTP request object.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This Object servlet, @Advice.Argument(0) Object request) {
        try {
            // todo Consider ramifications and protections to support sensitive cookie values.

            // Generate a unique request ID
            String requestId = UUID.randomUUID().toString().substring(0, 8); // Shortened for brevity
            RequestContextHolder.clearContext();
            Map<String, String> metadata = RequestContextHolder.getContext();
            metadata.put("request_id", requestId);

            // Retrieve all request headers
            Map<String, String> headers = getAllHeaders(request);

            // Capture client IP address
            String clientIp = (String) invokeMethod(request, "getRemoteAddr");
            metadata.put("client_ip", clientIp != null ? clientIp : "unknown");

            // Capture request URI
            String requestUri = (String) invokeMethod(request, "getRequestURI");
            metadata.put("uri", requestUri != null ? requestUri : "unknown");

            // Determine log level and metadata based on logging configuration
            String level = "INFO";
            if (logProxy.isLoggingAtLevel(REQ_NAMESPACE, "DEBUG")) {
                level = "DEBUG";
                metadata.putAll(headers); // Include all headers at DEBUG level
            } else {
                level = "INFO";
                // Include specific header (e.g., User-Agent) at INFO level
                String userAgent = headers.get("User-Agent");
                if (userAgent != null) {
                    metadata.put("user-agent", userAgent);
                }
            }
            // Log the request event
            logProxy.logMessage(REQ_NAMESPACE, level, metadata);
        } catch (Exception e) {
            System.err.println("enter: Failed: " + e.getMessage());
            e.printStackTrace();
            // Log error event
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process request: " + e.getMessage());
            logProxy.logMessage(REQ_NAMESPACE, "ERROR", errorMetadata);
        }
    }

    /**
     * Intercepts the exit of an HTTP servlet method to log response details.
     *
     * @param response The HTTP response object.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Argument(1) Object response) {
        try {
            //todo May consider adding support for returning server response headers.

            // Retrieve request_id for correlation of responses.
            Map<String, String> metadata =  RequestContextHolder.getContext();
            String requestId = metadata.get("request_id"); // correlate req to the response.
            String requestUri = metadata.get("request_uri"); // helpful to understand context.
            // Clear all the other headers not required for the response.
            metadata.clear();
            metadata.put("request_id", requestId);
            metadata.put("request_uri", requestUri != null ? requestUri : "unknown");

            // When DEBUG enabled log the response content length header value.
            String level = "INFO";
            if (logProxy.isLoggingAtLevel(RES_NAMESPACE, "DEBUG")) {
                level = "DEBUG";
                // Retrieve all request headers
                Map<String, String> headers = getAllHeaders(response);
                headers.remove("request_uri"); // remove it since we included it by default.
                metadata.putAll(headers);
            }

            // Capture response status code
            Integer status = (Integer) invokeMethod(response, "getStatus");
            metadata.put("status", status != null ? status.toString() : "unknown");

            // Log the response event
            logProxy.logMessage(RES_NAMESPACE, level, metadata);
        } catch (Exception e) {
            // Log error event
            Map<String, String> errorMetadata = new HashMap<>();
            errorMetadata.put("error", "Failed to process response: " + e.getMessage());
            logProxy.logMessage(RES_NAMESPACE, "ERROR", errorMetadata);
        }
    }

    /**
     * Retrieves the value of a specific cookie from the HTTP request.
     *
     * @param request The HTTP request object.
     * @param cookieName The name of the cookie to retrieve.
     * @return The cookie value, or {@code null} if the cookie is not found.
     * @throws Exception If an error occurs during method invocation.
     */
    public static String getCookieValue(Object request, String cookieName) throws Exception {
        // Retrieve cookies from the request
        Object[] cookies = (Object[]) invokeMethod(request, "getCookies");
        if (cookies != null) {
            for (Object cookie : cookies) {
                String name = (String) invokeMethod(cookie, "getName");
                if (cookieName.equals(name)) {
                    return (String) invokeMethod(cookie, "getValue");
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all headers from the HTTP request or response.
     *
     * @param request The HTTP request or response object.
     * @return A {@code Map<String, String>} containing header names and their values.
     * @throws Exception If an error occurs during method invocation.
     */
    public static Map<String, String> getAllHeaders(Object request) throws Exception {
        // Initialize map to store headers
        Map<String, String> headerMap = new HashMap<>();

        // Get the enumeration of header names
        @SuppressWarnings("unchecked")
        Object headers = invokeMethod(request, "getHeaderNames");
        Enumeration<String> headerNames = null;
        if(headers instanceof Collection) {
            headerNames = Collections.enumeration((Collection<String>) headers);
        } else {
            headerNames = (Enumeration<String>)headers;
        }
        //java.util.Enumeration<String> headerNames = (java.util.Enumeration<String>) invokeMethod(request, "getHeaderNames");

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (headerName != null) {
                    // Retrieve header value
                    String headerValue = (String) invokeMethod(request, "getHeader", headerName);
                    headerMap.put(headerName, headerValue != null ? headerValue : "");
                }
            }
        }
        return headerMap;
    }

    /**
     * Invokes a method on an object using reflection.
     *
     * @param obj The object on which to invoke the method.
     * @param methodName The name of the method to invoke.
     * @param args The arguments to pass to the method.
     * @return The result of the method invocation.
     * @throws Exception If an error occurs during method invocation.
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) throws Exception {
        // Determine parameter types for method lookup
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            // Handle primitive wrappers and null args
            parameterTypes[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }
        // Find and invoke the method
        Method method = obj.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(obj, args);
    }
}