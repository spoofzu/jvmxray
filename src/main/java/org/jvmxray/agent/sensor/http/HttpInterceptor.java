package org.jvmxray.agent.sensor.http;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.util.sensor.RequestContextHolder;
import org.jvmxray.platform.shared.util.GUID;

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
     * Initializes MCC (Mapped Correlation Context) with trace_id and request metadata
     * for event correlation across the entire request execution path.
     * Enhanced with security pattern detection and timing capture.
     *
     * @param servlet The servlet instance being intercepted.
     * @param request The HTTP request object.
     */
    @Advice.OnMethodEnter
    public static long enter(@Advice.This Object servlet, @Advice.Argument(0) Object request) {
        long startTime = System.nanoTime();
        try {
            // todo Consider ramifications and protections to support sensitive cookie values.

            // Enter HTTP correlation scope
            // If this is the first sensor to fire, generates new trace_id
            // If called within existing scope (e.g., from scheduled task), inherits trace_id
            try {
                Class<?> mccClass = Class.forName("org.jvmxray.platform.shared.util.MCC");
                java.lang.reflect.Method enterScopeMethod = mccClass.getMethod("enterScope", String.class);
                java.lang.reflect.Method putMethod = mccClass.getMethod("put", String.class, String.class);

                enterScopeMethod.invoke(null, "HTTP");

                // Enrich MCC with HTTP-specific correlation fields
                // Extract and set user_id (authenticated user)
                try {
                    Object principal = invokeMethod(request, "getUserPrincipal");
                    if (principal != null) {
                        String userName = (String) invokeMethod(principal, "getName");
                        if (userName != null && !userName.isEmpty()) {
                            putMethod.invoke(null, "user_id", userName);
                        }
                    } else {
                        // Fallback to getRemoteUser()
                        String remoteUser = (String) invokeMethod(request, "getRemoteUser");
                        if (remoteUser != null && !remoteUser.isEmpty()) {
                            putMethod.invoke(null, "user_id", remoteUser);
                        }
                    }
                } catch (Exception e) {
                    // User principal not available - not an error
                }

                // Extract and set session_id
                try {
                    Object session = invokeMethod(request, "getSession", false); // false = don't create
                    if (session != null) {
                        String sessionId = (String) invokeMethod(session, "getId");
                        if (sessionId != null && !sessionId.isEmpty()) {
                            putMethod.invoke(null, "session_id", sessionId);
                        }
                    }
                } catch (Exception e) {
                    // Session not available - not an error
                }

                // Set client_ip
                try {
                    String clientIp = (String) invokeMethod(request, "getRemoteAddr");
                    if (clientIp != null && !clientIp.isEmpty()) {
                        putMethod.invoke(null, "client_ip", clientIp);
                    }
                } catch (Exception e) {
                    // Client IP not available
                }

                // Set request_uri
                try {
                    String requestUri = (String) invokeMethod(request, "getRequestURI");
                    if (requestUri != null && !requestUri.isEmpty()) {
                        putMethod.invoke(null, "request_uri", requestUri);
                    }
                } catch (Exception e) {
                    // Request URI not available
                }

                // Set request_method
                try {
                    String method = (String) invokeMethod(request, "getMethod");
                    if (method != null && !method.isEmpty()) {
                        putMethod.invoke(null, "request_method", method);
                    }
                } catch (Exception e) {
                    // Request method not available
                }

            } catch (ClassNotFoundException e) {
                // MCC not available - this is okay for minimal deployments
            } catch (Exception e) {
                System.err.println("HttpInterceptor: Failed to enter MCC scope: " + e.getMessage());
            }

            // Generate request ID for backward compatibility with RequestContextHolder
            String requestId = GUID.generateShort();

            // Also maintain RequestContextHolder for backward compatibility
            RequestContextHolder.clearContext();
            Map<String, String> metadata = RequestContextHolder.getContext();
            metadata.put("request_id", requestId);

            // Retrieve all request headers
            Map<String, String> headers = getAllHeaders(request);

            // Capture client IP address
            String clientIp = (String) invokeMethod(request, "getRemoteAddr");
            metadata.put("client_ip", clientIp != null ? clientIp : "unknown");

            // Capture request method
            String requestMethod = (String) invokeMethod(request, "getMethod");
            metadata.put("request_method", requestMethod != null ? requestMethod : "unknown");

            // Capture request URI
            String requestUri = (String) invokeMethod(request, "getRequestURI");
            metadata.put("uri", requestUri != null ? requestUri : "unknown");

            // Capture query string for security analysis
            String queryString = null;
            try {
                queryString = (String) invokeMethod(request, "getQueryString");
                if (queryString != null) {
                    metadata.put("query_string_present", "true");
                }
            } catch (Exception e) {
                // Query string not available
            }

            // Capture content length
            try {
                Integer contentLength = (Integer) invokeMethod(request, "getContentLength");
                if (contentLength != null && contentLength >= 0) {
                    metadata.put("request_size_bytes", contentLength.toString());
                }
            } catch (Exception e) {
                // Content length not available
            }

            // Security pattern analysis
            String userAgent = headers.get("User-Agent");
            if (userAgent == null) {
                userAgent = headers.get("user-agent");
            }
            Map<String, String> securityAnalysis = HttpSecurityUtils.analyzeRequestPatterns(
                    requestUri, queryString, userAgent);
            metadata.putAll(securityAnalysis);

            // Determine log level and metadata based on logging configuration
            String level = "INFO";
            if (logProxy.isLoggingAtLevel(REQ_NAMESPACE, "DEBUG")) {
                level = "DEBUG";
                metadata.putAll(headers); // Include all headers at DEBUG level
            } else {
                level = "INFO";
                // Include specific header (e.g., User-Agent) at INFO level
                if (userAgent != null) {
                    metadata.put("user-agent", userAgent);
                }
            }
            // Log the request event
            logProxy.logMessage(REQ_NAMESPACE, level, metadata);
        } catch (Exception e) {
            // Log error to platform log with full stacktrace and available context
            // MCC context (trace_id, client_ip, request_method, etc.) will be automatically merged
            Map<String, String> errorContext = new HashMap<>();
            errorContext.put("sensor", "HttpInterceptor");
            errorContext.put("phase", "enter");
            logProxy.logPlatformError(errorContext, "Failed to process HTTP request", e);
        }
        return startTime;
    }

    /**
     * Intercepts the exit of an HTTP servlet method to log response details.
     * Clears MCC (Mapped Correlation Context) to prevent correlation data leakage
     * in thread pool environments where threads are reused.
     * Enhanced with security header analysis and response timing.
     *
     * @param response The HTTP response object.
     * @param startTime The request start time from enter method.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Argument(1) Object response, @Advice.Enter long startTime) {
        try {
            // Calculate response time
            double responseTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;

            // Retrieve request_id for correlation of responses.
            Map<String, String> metadata =  RequestContextHolder.getContext();
            String requestId = metadata.get("request_id"); // correlate req to the response.
            String requestUri = metadata.get("request_uri"); // helpful to understand context.
            // Clear all the other headers not required for the response.
            metadata.clear();
            metadata.put("request_id", requestId);
            metadata.put("request_uri", requestUri != null ? requestUri : "unknown");

            // Add response timing
            metadata.put("response_time_ms", String.format("%.2f", responseTimeMs));

            // Capture response status code
            Integer status = (Integer) invokeMethod(response, "getStatus");
            metadata.put("status", status != null ? status.toString() : "unknown");

            // Classify status
            if (status != null) {
                metadata.put("status_class", HttpSecurityUtils.classifyStatus(status));
            }

            // Get response headers for security analysis
            Map<String, String> responseHeaders = getAllHeaders(response);

            // Security header analysis
            Map<String, String> securityAnalysis = HttpSecurityUtils.analyzeSecurityHeaders(responseHeaders);
            metadata.putAll(securityAnalysis);

            // Capture content type
            String contentType = responseHeaders.get("Content-Type");
            if (contentType == null) {
                contentType = responseHeaders.get("content-type");
            }
            if (contentType != null) {
                metadata.put("content_type", contentType);
                if (HttpSecurityUtils.isSensitiveContentType(contentType)) {
                    metadata.put("sensitive_content_type", "true");
                }
            }

            // Capture content length (response size)
            String contentLength = responseHeaders.get("Content-Length");
            if (contentLength == null) {
                contentLength = responseHeaders.get("content-length");
            }
            if (contentLength != null) {
                metadata.put("response_size_bytes", contentLength);
            }

            // When DEBUG enabled log additional response headers
            String level = "INFO";
            if (logProxy.isLoggingAtLevel(RES_NAMESPACE, "DEBUG")) {
                level = "DEBUG";
                // Add all response headers except those already processed
                for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equalsIgnoreCase("Content-Type") &&
                        !key.equalsIgnoreCase("Content-Length")) {
                        metadata.put("header_" + key.toLowerCase().replace("-", "_"), entry.getValue());
                    }
                }
            }

            // Log the response event
            logProxy.logMessage(RES_NAMESPACE, level, metadata);
        } catch (Exception e) {
            // Log error to platform log with full stacktrace and available context
            // MCC context (trace_id, client_ip, request_method, etc.) will be automatically merged
            Map<String, String> errorContext = new HashMap<>();
            errorContext.put("sensor", "HttpInterceptor");
            errorContext.put("phase", "exit");
            logProxy.logPlatformError(errorContext, "Failed to process HTTP response", e);
        } finally {
            // Clean up RequestContextHolder ThreadLocal
            RequestContextHolder.clearContext();

            // Exit HTTP correlation scope
            // If this is the last scope, clears all correlation context (thread pool safety)
            // If nested within other scopes, preserves correlation for parent scope
            try {
                Class<?> mccClass = Class.forName("org.jvmxray.platform.shared.util.MCC");
                java.lang.reflect.Method exitScopeMethod = mccClass.getMethod("exitScope", String.class);
                exitScopeMethod.invoke(null, "HTTP");
            } catch (ClassNotFoundException e) {
                // MCC not available - this is okay
            } catch (Exception e) {
                System.err.println("HttpInterceptor: Failed to exit MCC scope: " + e.getMessage());
            }
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