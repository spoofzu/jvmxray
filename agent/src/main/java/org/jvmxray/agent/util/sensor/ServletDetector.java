package org.jvmxray.agent.util.sensor;

import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for detecting the availability of Servlet API (Jakarta EE or Java EE)
 * and Spring-specific servlet classes in the runtime environment using reflection.
 * Logs detection results via {@link LogProxy} and returns a map of detected classes.
 *
 * @author Milton Smith
 */
public class ServletDetector {
    // Namespace for logging servlet detection events
    private static final String NAMESPACE = "org.jvmxray.agent.core.util.sensor.ServletDetector";
    // Singleton instance of LogProxy for logging
    private static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Detects available Servlet-related classes and returns their {@code Class} objects.
     * Attempts to detect Jakarta EE Servlet API, Java EE Servlet API, and Spring's
     * {@code DispatcherServlet} in that order, returning the first successfully detected set.
     * Logs detection attempts and results, including errors if no Servlet API is found.
     *
     * @return A {@code Map} containing detected classes with keys "httpServlet", "request",
     *         and "response", or an empty map if no Servlet API is detected.
     */
    public static Map<String, Class<?>> detectServletApi() {
        // Initialize maps for detected classes and attempt logging
        Map<String, Class<?>> detectedClasses = new HashMap<>();
        Map<String, String> attempts = new HashMap<>();

        // Try Jakarta EE Servlet API (newer)
        try {
            Class<?> servletClass = Class.forName("jakarta.servlet.http.HttpServlet");
            Class<?> requestClass = Class.forName("jakarta.servlet.http.HttpServletRequest");
            Class<?> responseClass = Class.forName("jakarta.servlet.http.HttpServletResponse");

            // Store detected classes
            detectedClasses.put("httpServlet", servletClass);
            detectedClasses.put("request", requestClass);
            detectedClasses.put("response", responseClass);
            attempts.put("jakarta.servlet", "Detected");
            // Log successful detection
            logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                    "message", "Detected Jakarta Servlet API (jakarta.servlet.http.HttpServlet)"
            ));
            return detectedClasses; // Return immediately if Jakarta is found
        } catch (ClassNotFoundException e) {
            // Log failed attempt
            attempts.put("jakarta.servlet", "Not found: " + e.getMessage());
        }

        // Try Java EE Servlet API (older)
        try {
            Class<?> servletClass = Class.forName("javax.servlet.http.HttpServlet");
            Class<?> requestClass = Class.forName("javax.servlet.http.HttpServletRequest");
            Class<?> responseClass = Class.forName("javax.servlet.http.HttpServletResponse");

            // Store detected classes
            detectedClasses.put("httpServlet", servletClass);
            detectedClasses.put("request", requestClass);
            detectedClasses.put("response", responseClass);
            attempts.put("javax.servlet", "Detected");
            // Log successful detection
            logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                    "message", "Detected Java EE Servlet API (javax.servlet.http.HttpServlet)"
            ));
            return detectedClasses; // Return if Java EE is found
        } catch (ClassNotFoundException e) {
            // Log failed attempt
            attempts.put("javax.servlet", "Not found: " + e.getMessage());
        }

        // Try Spring DispatcherServlet as a fallback
        try {
            Class<?> dispatcherClass = Class.forName("org.springframework.web.servlet.DispatcherServlet");
            String requestClassName = "jakarta.servlet.http.HttpServletRequest";
            String responseClassName = "jakarta.servlet.http.HttpServletResponse";
            try {
                Class.forName(requestClassName);
            } catch (ClassNotFoundException e) {
                // Fallback to Java EE if Jakarta is not found
                requestClassName = "javax.servlet.http.HttpServletRequest";
                responseClassName = "javax.servlet.http.HttpServletResponse";
            }
            Class<?> requestClass = Class.forName(requestClassName);
            Class<?> responseClass = Class.forName(responseClassName);

            // Store detected classes, using DispatcherServlet as the servlet
            detectedClasses.put("httpServlet", dispatcherClass);
            detectedClasses.put("request", requestClass);
            detectedClasses.put("response", responseClass);
            attempts.put("spring.dispatcher", "Detected");
            // Log successful detection
            logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                    "message", "Detected Spring DispatcherServlet (org.springframework.web.servlet.DispatcherServlet)"
            ));
        } catch (ClassNotFoundException e) {
            // Log failed attempt
            attempts.put("spring.dispatcher", "Not found: " + e.getMessage());
        }

        // Log failure if no Servlet API was detected
        if (detectedClasses.isEmpty()) {
            logProxy.logEvent(NAMESPACE, "ERROR", Map.of(
                    "message", "No Servlet API detected",
                    "attempts", attempts.toString()
            ));
        }

        return detectedClasses;
    }
}