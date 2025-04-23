package org.jvmxray.agent.sensor.sql;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sensor implementation for detecting and instrumenting JDBC {@link PreparedStatement} implementations
 * to monitor SQL query execution. Uses the Instrumentation API and a {@link ClassFileTransformer} to
 * identify loaded classes and instruments their {@code execute} method with Byte Buddy interceptors.
 * Note: This implementation is not yet fully operational and requires further development, particularly
 * in conjunction with the {@link SQLInterceptor}.
 *
 * @author Milton Smith
 */
public class SQLSensor implements InjectableSensor {
    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.agent.core.io.SQLSensor";
    // Singleton instance of LogProxy for logging
    private static final LogProxy logProxy = LogProxy.getInstance();
    // List of detected PreparedStatement implementation classes
    private final List<Class<?>> detectedPreparedStatementClasses = new ArrayList<>();
    // Instrumentation instance for class detection and transformation
    private Instrumentation instrumentation;

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's name, "SQLSensor".
     */
    @Override
    public String getName() {
        return "SQLSensor";
    }

    /**
     * Initializes the sensor by setting up the instrumentation instance and detecting
     * {@link PreparedStatement} implementations. Registers a transformer to detect
     * late-loaded classes.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        this.instrumentation = inst;
        try {
            // Detect initial PreparedStatement implementations
            detectPreparedStatementImpls(inst);
            if (!detectedPreparedStatementClasses.isEmpty()) {
                // Log detected classes
                logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                        "message", "Initially detected PreparedStatement implementations: " +
                                detectedPreparedStatementClasses.stream()
                                        .map(Class::getName)
                                        .reduce((a, b) -> a + ", " + b)
                                        .get()
                ));
            } else {
                // Log if no classes detected
                logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                        "message", "No PreparedStatement implementations detected at initialization. " +
                                "Transformer will detect late-loaded classes."
                ));
            }

            if (inst != null) {
                // Register transformer for late-loaded classes
                inst.addTransformer(new PreparedStatementTransformer(), true);
                logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                        "message", "Registered transformer to detect late-loaded PreparedStatement implementations"
                ));
            } else {
                // Log warning if instrumentation is unavailable
                logProxy.logEvent(NAMESPACE, "WARN", Map.of(
                        "message", "Instrumentation null; late detection unavailable"
                ));
            }
        } catch (Exception e) {
            // Log initialization errors
            logProxy.logEvent(NAMESPACE, "ERROR", Map.of(
                    "message", "Failed to initialize SQLSensor: " + e.getMessage()
            ));
        }
    }

    /**
     * Provides classes to be injected into the bootstrap class loader.
     *
     * @return An array of classes required for SQL query instrumentation, including
     *         common sensor classes and the {@link SQLInterceptor} class.
     */
    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
                // Common sensor classes
                Sensor.class,
                InjectableSensor.class,
                LogProxy.class,
                // Sensor-specific interceptor class
                SQLInterceptor.class
        };
    }

    /**
     * Configures transformations for instrumenting the {@code execute} method of detected
     * {@link PreparedStatement} implementations. Returns an empty array if no classes are detected
     * or if instrumentation is unavailable.
     *
     * @return An array of {@code Transform} objects defining the classes and methods to instrument.
     */
    @Override
    public Transform[] configure() {
        // Check if instrumentation is available
        if (instrumentation == null) {
            logProxy.logEvent(NAMESPACE, "ERROR", Map.of(
                    "message", "Instrumentation not available in configure(). SQLSensor cannot function. Skipping."
            ));
            return new Transform[0];
        }

        // Detect PreparedStatement implementations if none found yet
        if (detectedPreparedStatementClasses.isEmpty()) {
            detectPreparedStatementImpls(instrumentation);
            if (detectedPreparedStatementClasses.isEmpty()) {
                logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                        "message", "No PreparedStatement implementations detected yet. Awaiting transformer."
                ));
                return new Transform[0];
            }
        }

        // Create transformations for each detected class
        List<Transform> transforms = new ArrayList<>();
        for (Class<?> clazz : detectedPreparedStatementClasses) {
            transforms.add(new Transform(
                    clazz,
                    SQLInterceptor.class,
                    new MethodSpec("execute")
            ));
            logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                    "message", "Instrumenting PreparedStatement implementation: " + clazz.getName()
            ));
        }
        return transforms.toArray(new Transform[0]);
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

    /**
     * Detects loaded classes that implement {@link PreparedStatement} using the Instrumentation API.
     * Adds detected classes to the {@code detectedPreparedStatementClasses} list.
     *
     * @param inst The instrumentation instance, or null if unavailable.
     */
    private void detectPreparedStatementImpls(Instrumentation inst) {
        if (inst == null) {
            // Log if instrumentation is unavailable
            logProxy.logEvent(NAMESPACE, "DEBUG", Map.of(
                    "message", "Instrumentation not available; skipping detection"
            ));
            return;
        }

        try {
            // Iterate through all loaded classes
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (PreparedStatement.class.isAssignableFrom(clazz) && !clazz.isInterface() &&
                        !detectedPreparedStatementClasses.contains(clazz)) {
                    // Add detected implementation to the list
                    detectedPreparedStatementClasses.add(clazz);
                    logProxy.logEvent(NAMESPACE, "DEBUG", Map.of(
                            "message", "Detected via Instrumentation: " + clazz.getName()
                    ));
                }
            }
        } catch (Exception e) {
            // Log detection errors
            logProxy.logEvent(NAMESPACE, "ERROR", Map.of(
                    "message", "Failed to detect PreparedStatement implementations: " + e.getMessage()
            ));
        }
    }

    /**
     * Transformer for detecting late-loaded {@link PreparedStatement} implementations.
     * Triggers retransformation of newly detected classes to apply instrumentation.
     */
    private class PreparedStatementTransformer implements ClassFileTransformer {
        /**
         * Inspects loaded classes to identify {@link PreparedStatement} implementations
         * and triggers retransformation for instrumentation.
         *
         * @param loader              The class loader loading the class.
         * @param className           The internal name of the class (e.g., "java/lang/String").
         * @param classBeingRedefined The class being redefined, if applicable.
         * @param protectionDomain    The protection domain of the class.
         * @param classfileBuffer     The class file bytes.
         * @return {@code null} to indicate no transformation of the class file.
         */
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                java.security.ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (className != null) {
                try {
                    // Load the class to check if it implements PreparedStatement
                    Class<?> clazz = Class.forName(className.replace('/', '.'), false, loader);
                    if (PreparedStatement.class.isAssignableFrom(clazz) && !clazz.isInterface() &&
                            !detectedPreparedStatementClasses.contains(clazz)) {
                        // Add newly detected implementation to the list
                        detectedPreparedStatementClasses.add(clazz);
                        logProxy.logEvent(NAMESPACE, "INFO", Map.of(
                                "message", "Late-detected PreparedStatement implementation: " + clazz.getName()
                        ));
                        if (instrumentation != null && instrumentation.isModifiableClass(clazz)) {
                            try {
                                // Trigger retransformation to apply instrumentation
                                instrumentation.retransformClasses(clazz);
                                logProxy.logEvent(NAMESPACE, "DEBUG", Map.of(
                                        "message", "Retransformed class: " + clazz.getName()
                                ));
                            } catch (Exception e) {
                                // Log retransformation errors
                                logProxy.logEvent(NAMESPACE, "WARN", Map.of(
                                        "message", "Failed to retransform " + clazz.getName() + ": " + e.getMessage()
                                ));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log transformer errors
                    logProxy.logEvent(NAMESPACE, "DEBUG", Map.of(
                            "message", "Transformer error for " + className + ": " + e.getMessage()
                    ));
                }
            }
            // Return null to indicate no class file transformation
            return null;
        }
    }
}