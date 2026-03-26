package org.jvmxray.agent.sensor.api;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;
import java.util.HashMap;
import java.util.Map;

public class APICallInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.api";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodEnter
    public static long enter() {
        MCCScope.enter("API");
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter long startTime,
                            @Advice.Argument(0) Object request,
                            @Advice.Return Object response,
                            @Advice.Thrown Throwable thrown) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "http_client_send");

            // Extract from HttpRequest via reflection
            if (request != null) {
                // request.uri() returns java.net.URI
                Object uri = invokeMethod(request, "uri");
                if (uri != null) {
                    metadata.put("request_uri", uri.toString());
                    String host = (String) invokeMethod(uri, "getHost");
                    String scheme = (String) invokeMethod(uri, "getScheme");
                    Object port = invokeMethod(uri, "getPort");
                    if (host != null) metadata.put("request_host", host);
                    if (scheme != null) {
                        metadata.put("request_scheme", scheme);
                        metadata.put("uses_tls", String.valueOf("https".equalsIgnoreCase(scheme)));
                    }
                    if (port != null) metadata.put("request_port", String.valueOf(port));
                }
                // request.method() returns String
                Object method = invokeMethod(request, "method");
                if (method != null) metadata.put("request_method", method.toString());
            }

            // Calculate response time
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            metadata.put("response_time_ms", String.valueOf(durationMs));

            // Extract from HttpResponse via reflection
            if (response != null) {
                Object statusCode = invokeMethod(response, "statusCode");
                if (statusCode != null) {
                    int status = (Integer) statusCode;
                    metadata.put("response_status", String.valueOf(status));
                    if (status >= 400 && status < 500) {
                        metadata.put("status_class", "client_error");
                    } else if (status >= 500) {
                        metadata.put("status_class", "server_error");
                    } else if (status >= 200 && status < 300) {
                        metadata.put("status_class", "success");
                    } else if (status >= 300 && status < 400) {
                        metadata.put("status_class", "redirect");
                    }
                }
                // Try to get content-type from response headers
                try {
                    Object headers = invokeMethod(response, "headers");
                    if (headers != null) {
                        java.lang.reflect.Method firstValueMethod = headers.getClass().getMethod("firstValue", String.class);
                        Object optional = firstValueMethod.invoke(headers, "content-type");
                        if (optional != null) {
                            java.lang.reflect.Method isPresentMethod = optional.getClass().getMethod("isPresent");
                            if ((Boolean) isPresentMethod.invoke(optional)) {
                                java.lang.reflect.Method getMethod = optional.getClass().getMethod("get");
                                metadata.put("content_type", getMethod.invoke(optional).toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip content-type extraction on failure
                }
            }

            // Error info
            if (thrown != null) {
                metadata.put("status", "failed");
                metadata.put("error_class", thrown.getClass().getSimpleName());
                metadata.put("error_message", thrown.getMessage() != null ? thrown.getMessage() : "");
            } else {
                metadata.put("status", "completed");
            }

            logProxy.logMessage(NAMESPACE + ".call", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("API");
        }
    }

    private static Object invokeMethod(Object target, String methodName) {
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }
}
