package org.jvmxray.agent.sensor.sql;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sensor for instrumenting JDBC PreparedStatement implementations to monitor SQL query execution.
 * Uses ByteBuddy to instrument execute methods and detect loaded classes dynamically.
 *
 * @author Milton Smith
 */
public class SQLSensor extends AbstractSensor implements InjectableSensor {
    private static final String NAMESPACE = "org.jvmxray.agent.core.io.SQLSensor";
    private static final LogProxy logProxy = LogProxy.getInstance();
    private Instrumentation instrumentation;
    private final List<String> detectedClasses = new ArrayList<>(); // Store class names instead of Class objects

    // Static sensor identity.
    private static final String SENSOR_GUID = "2aHdO8s3lM4vU9y8Tm6Ho2";

    public SQLSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        this.instrumentation = inst;
        try {
            // Configure ByteBuddy agent for dynamic class detection
            new AgentBuilder.Default()
                    .type(ElementMatchers.isSubTypeOf(PreparedStatement.class)
                            .and(ElementMatchers.not(ElementMatchers.isInterface())))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                        String className = typeDescription.getName();
                        if (!detectedClasses.contains(className)) {
                            detectedClasses.add(className);
                            logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                                    "message", "Detected PreparedStatement implementation: " + className
                            ));
                        }
                        // Apply transformation immediately for detected classes
                        return builder.method(ElementMatchers.named("execute")
                                        .or(ElementMatchers.named("executeQuery"))
                                        .or(ElementMatchers.named("executeUpdate"))
                                        .or(ElementMatchers.named("executeBatch")))
                                .intercept(Advice.to(SQLInterceptor.class));
                    })
                    .installOn(instrumentation);

            String priority = (detectedClasses.size() < 1 ) ? "WARN" : "INFO";
                logProxy.logMessage(NAMESPACE, priority, Map.of(
                        "message", "SQLSensor initialized with ByteBuddy transformer, detected classes: " + detectedClasses.size()));
        } catch (Exception e) {
            logProxy.logMessage(NAMESPACE, "ERROR", Map.of(
                    "message", "Failed to initialize SQLSensor: " + e.getMessage()
            ));
        }
    }

    @Override
    public Class<?>[] inject() {
        return new Class<?>[]{
                LogProxy.class,
                SQLInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        if (instrumentation == null) {
            logProxy.logMessage(NAMESPACE, "ERROR", Map.of(
                    "message", "Instrumentation unavailable. Skipping configuration."
            ));
            return new Transform[0];
        }

        if (detectedClasses.isEmpty()) {
            logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                    "message", "No PreparedStatement implementations detected. Checking loaded classes."
            ));
            // Fallback: Check all loaded classes for PreparedStatement implementations
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                if (PreparedStatement.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                    if (!detectedClasses.contains(clazz.getName())) {
                        detectedClasses.add(clazz.getName());
                        logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                                "message", "Fallback detection: Found PreparedStatement implementation: " + clazz.getName()
                        ));
                    }
                }
            }
        }

        List<Transform> transforms = new ArrayList<>();
        MethodSpec[] methodsToInstrument = new MethodSpec[]{
                new MethodSpec("execute"),
                new MethodSpec("executeQuery"),
                new MethodSpec("executeUpdate"),
                new MethodSpec("executeBatch")
        };

        for (String className : detectedClasses) {
            try {
                Class<?> clazz = Class.forName(className, false, instrumentation.getAllLoadedClasses()[0].getClassLoader());
                for (MethodSpec method : methodsToInstrument) {
                    transforms.add(new Transform(clazz, SQLInterceptor.class, method));
                    logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                            "message", "Instrumenting " + clazz.getName() + "." + method.getMethodName()
                    ));
                }
            } catch (ClassNotFoundException e) {
                logProxy.logMessage(NAMESPACE, "ERROR", Map.of(
                        "message", "Failed to load class for transformation: " + className,
                        "error", e.getMessage()
                ));
            }
        }
        logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                "message", "Configured " + transforms.size() + " transformations for SQLSensor"
        ));
        return transforms.toArray(new Transform[0]);
    }

    @Override
    public void shutdown() {
        logProxy.logMessage(NAMESPACE, "INFO", Map.of(
                "message", "SQLSensor shutting down"
        ));
    }
}