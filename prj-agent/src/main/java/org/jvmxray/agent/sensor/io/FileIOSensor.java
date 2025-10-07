package org.jvmxray.agent.sensor.io;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.*;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;

/**
 * Sensor implementation for instrumenting file I/O operations in the JVM.
 * Monitors Create, Read, Update, and Delete (CRUD) operations on files with configurable
 * operation tracking. Uses a unified interceptor approach rather than stream-based monitoring
 * for better performance and clearer file path tracking.
 *
 * @author Milton Smith
 */
public class FileIOSensor extends AbstractSensor implements InjectableSensor {

    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.agent.core.io.FileIOSensor";

    // Static sensor identity.
    private static final String SENSOR_GUID = "5uBxI2rXfG7pO3s2Ng9Bi5"; // Generated via uuidgen

    public FileIOSensor(String propertySuffix) {
        super(propertySuffix);
    }

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's identity is, "76C8F32B-300C-41DF-A17A-44204ED928E7".
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
     *         common sensor classes and the unified FileIOInterceptor.
     */
    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
                // Common sensor classes
                Sensor.class,
                InjectableSensor.class,
                AbstractSensor.class,
                LogProxy.class,
                // Sensor guard classes
                org.jvmxray.agent.guard.SensorGuard.class,
                // Property factory needed for configuration
                AgentInitializer.class,
                org.jvmxray.platform.shared.property.AgentProperties.class,
                org.jvmxray.platform.shared.property.PropertyBase.class,
                org.jvmxray.platform.shared.property.IProperties.class,
                // Utility classes
                org.jvmxray.platform.shared.util.MCCScope.class,
                org.jvmxray.platform.shared.util.MCC.class,
                // File statistics tracking
                FileStats.class,
                // Unified file I/O interceptor and inner classes
                FileIOInterceptor.class,
                FileIOInterceptor.FileOps.class,
                FileIOInterceptor.FilesOps.class,
                FileIOInterceptor.Read.class,
                FileIOInterceptor.Update.class,
                FileIOInterceptor.InputStreamReadByte.class,
                FileIOInterceptor.InputStreamReadArray.class,
                FileIOInterceptor.InputStreamReadArrayOffset.class,
                FileIOInterceptor.InputStreamClose.class,
                FileIOInterceptor.OutputStreamWriteByte.class,
                FileIOInterceptor.OutputStreamWriteArray.class,
                FileIOInterceptor.OutputStreamWriteArrayOffset.class,
                FileIOInterceptor.OutputStreamClose.class
        };
    }

    /**
     * Configures transformations for instrumenting file I/O CRUD operations.
     * Defines instrumentation for Create (File.createNewFile, Files.createFile),
     * Read (FileInputStream constructor), Update (FileOutputStream constructor),
     * and Delete (File.delete) operations with configurable operation tracking.
     *
     * @return An array of {@code Transform} objects defining the classes and methods to instrument.
     */
    @Override
    public Transform[] configure() {
        return new Transform[] {
                // Instrument File.delete() and File.createNewFile() - unified interceptor
                new Transform(
                        java.io.File.class,
                        FileIOInterceptor.FileOps.class,
                        new MethodSpec("delete"),
                        new MethodSpec("createNewFile")
                ),
                // Instrument Files NIO methods - Create, Read, Update, Delete operations
                new Transform(
                        java.nio.file.Files.class,
                        FileIOInterceptor.FilesOps.class,
                        new MethodSpec("createFile", java.nio.file.Path.class, java.nio.file.attribute.FileAttribute[].class),
                        new MethodSpec("createDirectories", java.nio.file.Path.class, java.nio.file.attribute.FileAttribute[].class),
                        new MethodSpec("write", java.nio.file.Path.class, byte[].class, java.nio.file.OpenOption[].class),
                        new MethodSpec("readString", java.nio.file.Path.class),
                        new MethodSpec("readAllBytes", java.nio.file.Path.class),
                        new MethodSpec("readAllLines", java.nio.file.Path.class),
                        new MethodSpec("delete", java.nio.file.Path.class),
                        new MethodSpec("deleteIfExists", java.nio.file.Path.class),
                        new MethodSpec("copy", java.nio.file.Path.class, java.nio.file.Path.class, java.nio.file.CopyOption[].class)
                ),
                // Instrument FileInputStream constructor and operations
                new Transform(
                        java.io.FileInputStream.class,
                        FileIOInterceptor.Read.class,
                        new MethodSpec("<init>", java.io.File.class)
                ),
                new Transform(
                        java.io.FileInputStream.class,
                        FileIOInterceptor.InputStreamReadByte.class,
                        new MethodSpec("read")
                ),
                new Transform(
                        java.io.FileInputStream.class,
                        FileIOInterceptor.InputStreamReadArray.class,
                        new MethodSpec("read", byte[].class)
                ),
                new Transform(
                        java.io.FileInputStream.class,
                        FileIOInterceptor.InputStreamReadArrayOffset.class,
                        new MethodSpec("read", byte[].class, int.class, int.class)
                ),
                new Transform(
                        java.io.FileInputStream.class,
                        FileIOInterceptor.InputStreamClose.class,
                        new MethodSpec("close")
                ),
                // Instrument FileOutputStream constructor and operations
                new Transform(
                        java.io.FileOutputStream.class,
                        FileIOInterceptor.Update.class,
                        new MethodSpec("<init>", java.io.File.class)
                ),
                new Transform(
                        java.io.FileOutputStream.class,
                        FileIOInterceptor.OutputStreamWriteByte.class,
                        new MethodSpec("write", int.class)
                ),
                new Transform(
                        java.io.FileOutputStream.class,
                        FileIOInterceptor.OutputStreamWriteArray.class,
                        new MethodSpec("write", byte[].class)
                ),
                new Transform(
                        java.io.FileOutputStream.class,
                        FileIOInterceptor.OutputStreamWriteArrayOffset.class,
                        new MethodSpec("write", byte[].class, int.class, int.class)
                ),
                new Transform(
                        java.io.FileOutputStream.class,
                        FileIOInterceptor.OutputStreamClose.class,
                        new MethodSpec("close")
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