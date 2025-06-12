package org.jvmxray.agent.sensor.io;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;

/**
 * Sensor implementation for instrumenting file I/O operations in the JVM.
 * Targets classes such as {@link java.io.File}, {@link java.io.FileInputStream}, and
 * {@link java.io.FileOutputStream} to monitor file deletion, read, and write activities
 * using Byte Buddy interceptors. Integrates with the JVMXRay agent framework for event logging.
 *
 * @author Milton Smith
 */
public class FileIOSensor extends AbstractSensor implements InjectableSensor {
    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.agent.core.io.FileIOSensor";

    // Static sensor identity.
    private static final String SENSOR_GUID = "B871EC6C-7361-429F-B6AB-8956F509239D"; // Generated via uuidgen

    public FileIOSensor(String propertySuffix) {
        super(propertySuffix);
    }

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's identity is, "B871EC6C-7361-429F-B6AB-8956F509239D".
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
     * @return An array of classes required for file I/O instrumentation, including
     *         common sensor classes and specific interceptor classes.
     */
    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
                // Common sensor classes
                Sensor.class,
                InjectableSensor.class,
                LogProxy.class,
                // Sensor-specific interceptor classes
                DeleteInterceptor.class,
                ReadInterceptor.class,
                WriteInterceptor.class
        };
    }

    /**
     * Configures transformations for instrumenting file I/O operations.
     * Defines instrumentation for the {@code delete} method of {@link java.io.File},
     * the no-argument {@code read} method of {@link java.io.FileInputStream}, and
     * the {@code write} method of {@link java.io.FileOutputStream} with an integer parameter.
     *
     * @return An array of {@code Transform} objects defining the classes and methods to instrument.
     */
    @Override
    public Transform[] configure() {
        return new Transform[] {
                // Instrument File.delete()
                new Transform(
                        java.io.File.class,
                        DeleteInterceptor.class,
                        new MethodSpec("delete")
                ),
                // Instrument FileInputStream.read() (no arguments)
                new Transform(
                        java.io.FileInputStream.class,
                        ReadInterceptor.class,
                        new MethodSpec("read")
                ),
                // Instrument FileOutputStream.write(int)
                new Transform(
                        java.io.FileOutputStream.class,
                        WriteInterceptor.class,
                        new MethodSpec("write", int.class)
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