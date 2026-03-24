package org.jvmxray.agent.sensor.system;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * Sensor implementation for detecting and logging process execution operations in the JVMXRay agent framework.
 * Monitors ProcessBuilder.start() and Runtime.exec() calls to track external process executions.
 * Uses ByteBuddy instrumentation to intercept process execution methods and logs events via {@link LogProxy}.
 * Integrates with the JVMXRay agent framework for system monitoring.
 *
 * @author Milton Smith
 */
public class ProcessSensor extends AbstractSensor implements InjectableSensor, Sensor {

    // Static sensor identity.
    private static final String SENSOR_GUID = "AB470FEA-039B-4C23-910C-54F7847A2505"; // Generated via uuidgen

    public ProcessSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    /**
     * Returns the classes that need to be injected into the bootstrap classloader
     * for this sensor to function properly with ByteBuddy instrumentation.
     *
     * @return Array of classes to inject into bootstrap classloader.
     */
    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
            // Common sensor classes
            Sensor.class,
            InjectableSensor.class,
            LogProxy.class,
            // Property factory needed for configuration
            AgentInitializer.class,
            org.jvmxray.platform.shared.property.AgentProperties.class,
            org.jvmxray.platform.shared.property.PropertyBase.class,
            org.jvmxray.platform.shared.property.IProperties.class,
            // Process interceptor and inner classes
            ProcessInterceptor.class,
            ProcessInterceptor.ProcessBuilderStart.class,
            ProcessInterceptor.RuntimeExec.class
        };
    }

    /**
     * Configures transformations for instrumenting process execution operations.
     * Defines instrumentation for ProcessBuilder.start() and Runtime.exec() methods
     * to track external process execution with configurable operation tracking.
     *
     * @return An array of {@code Transform} objects defining the classes and methods to instrument.
     */
    @Override
    public Transform[] configure() {
        return new Transform[] {
                // Instrument ProcessBuilder.start()
                new Transform(
                        ProcessBuilder.class,
                        ProcessInterceptor.ProcessBuilderStart.class,
                        new MethodSpec("start")
                ),
                // Instrument Runtime.exec() methods
                new Transform(
                        Runtime.class,
                        ProcessInterceptor.RuntimeExec.class,
                        new MethodSpec("exec", String.class),
                        new MethodSpec("exec", String[].class),
                        new MethodSpec("exec", String[].class, String[].class),
                        new MethodSpec("exec", String.class, String[].class),
                        new MethodSpec("exec", String[].class, String[].class, File.class),
                        new MethodSpec("exec", String.class, String[].class, File.class)
                )
        };
    }

    /**
     * Initializes the sensor. Process monitoring is handled entirely through ByteBuddy instrumentation,
     * so no additional initialization is required.
     *
     * @param properties Configuration properties for the sensor.
     * @param agentArgs  Arguments passed to the agent.
     * @param inst       The instrumentation instance provided by the JVM.
     */
    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // Process monitoring is handled entirely through ByteBuddy instrumentation
        // No additional initialization required
    }

    /**
     * Cleans up resources used by the sensor during shutdown.
     * This implementation performs no operations as no resources are held.
     */
    @Override
    public void shutdown() {
        // No cleanup required for process monitoring
    }
}