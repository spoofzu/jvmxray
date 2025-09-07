package org.jvmxray.agent.sensor.http;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.agent.util.sensor.RequestContextHolder;
import org.jvmxray.agent.util.sensor.ServletDetector;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Sensor implementation for detecting and instrumenting HTTP servlet classes
 * (e.g., javax.servlet.http.HttpServlet, jakarta.servlet.http.HttpServlet, or
 * org.springframework.web.servlet.DispatcherServlet) to monitor HTTP requests and responses.
 * Instruments the {@code service} method to capture HTTP traffic using Byte Buddy.
 *
 * @author Milton Smith
 */
public class HttpSensor extends AbstractSensor implements InjectableSensor {

    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.agent.core.io.HttpSensor";

    // Static sensor identity.
    private static final String SENSOR_GUID = "E8D8E24C-E278-4752-AAEF-704F600FD46F"; // Generated via uuidgen

    public HttpSensor(String propertySuffix) {
        super(propertySuffix);
    }

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's identity is, "CCF1EE82-F58A-4866-A1D4-09A3B7B25A2D".
     */
    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    /**
     * Initializes the sensor with the provided configuration properties.
     * This implementation performs no operations as Byte Buddy sensors typically
     * do not require initialization.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // No initialization required for Byte Buddy sensors
    }

    /**
     * Provides classes to be injected into the bootstrap class loader.
     *
     * @return An array of classes required for HTTP instrumentation, including
     *         common sensor classes and the {@code HttpInterceptor} class.
     */
    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
                // Common sensor classes
                Sensor.class,
                InjectableSensor.class,
                LogProxy.class,
                // Sensor-specific class
                HttpInterceptor.class,
                RequestContextHolder.class  // req for req/resp correlation
        };
    }

    /**
     * Configures transformations for instrumenting the HTTP servlet's {@code service} method.
     *
     * @return An array of {@code Transform} objects defining the classes and methods
     *         to instrument, or an empty array if no suitable Servlet API is found.
     */
    @Override
    public Transform[] configure() {
        // Detect available Servlet API classes
        Map<String, Class<?>> detectedClasses = ServletDetector.detectServletApi();

        // Handle case where no Servlet API is found
        if (detectedClasses.isEmpty()) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                    "message", "No suitable Servlet API found for instrumentation"
            ));
            return new Transform[0];
        }

        // Retrieve detected classes
        Class<?> servletClass = detectedClasses.get("httpServlet");
        Class<?> requestClass = detectedClasses.get("request");
        Class<?> responseClass = detectedClasses.get("response");

        // Log detected classes to system log for debugging
        Map<String, String> logMetadata = new HashMap<>();
        logMetadata.put("Instrumenting httpServlet", servletClass.getName());
        logMetadata.put("request", requestClass.getName());
        logMetadata.put("response", responseClass.getName());
        LogProxy.getInstance().logMessage(NAMESPACE, "INFO", logMetadata);

        // Define transformation for the service method
        return new Transform[] {
                new Transform(
                        servletClass,
                        HttpInterceptor.class,
                        new MethodSpec("service", requestClass, responseClass)
                )
        };
    }

    /**
     * Cleans up resources used by the sensor during shutdown.
     * This implementation performs no operations as Byte Buddy sensors typically
     * do not require cleanup.
     */
    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}