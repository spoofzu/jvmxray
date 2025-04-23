package org.jvmxray.agent.sensor.net;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.net.SocketAddress;

/**
 * Sensor implementation for instrumenting network socket operations in the JVM.
 * Targets classes such as {@link java.net.ServerSocket} and {@link java.net.Socket}
 * to monitor accept, bind, connect, and close activities using Byte Buddy interceptors.
 * Integrates with the JVMXRay agent framework for event logging.
 *
 * @author Milton Smith
 */
public class SocketSensor implements InjectableSensor {
    // Namespace for logging sensor events
    private static final String NAMESPACE = "org.jvmxray.agent.core.net.SocketSensor";

    /**
     * Returns the unique identifier for this sensor, used for logging and configuration.
     *
     * @return The sensor's name, "SocketSensor".
     */
    @Override
    public String getName() {
        return "SocketSensor";
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
     * @return An array of classes required for socket instrumentation, including
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
                AcceptInterceptor.class,
                BindInterceptor.class,
                CloseInterceptor.class,
                ConnectInterceptor.class
        };
    }

    /**
     * Configures transformations for instrumenting socket operations.
     * Defines instrumentation for the {@code accept} and {@code bind} methods of
     * {@link java.net.ServerSocket}, and the {@code connect} and {@code close} methods
     * of {@link java.net.Socket}.
     *
     * @return An array of {@code Transform} objects defining the classes and methods to instrument.
     */
    @Override
    public Transform[] configure() {
        return new Transform[] {
                // Instrument ServerSocket.accept()
                new Transform(
                        java.net.ServerSocket.class,
                        AcceptInterceptor.class,
                        new MethodSpec("accept")
                ),
                // Instrument ServerSocket.bind(SocketAddress, int)
                new Transform(
                        java.net.ServerSocket.class,
                        BindInterceptor.class,
                        new MethodSpec("bind", SocketAddress.class, int.class)
                ),
                // Instrument Socket.connect(SocketAddress, int)
                new Transform(
                        java.net.Socket.class,
                        ConnectInterceptor.class,
                        new MethodSpec("connect", SocketAddress.class, int.class)
                ),
                // Instrument Socket.close()
                new Transform(
                        java.net.Socket.class,
                        CloseInterceptor.class,
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