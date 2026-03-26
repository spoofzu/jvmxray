package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging socket bind events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code bind} method of the
 * {@link ServerSocket} class, capturing the bind address, port, and operation status.
 * Events are logged with contextual metadata using the {@link LogProxy} for detailed tracking of
 * network socket bind operations.
 *
 * @author Milton Smith
 */
public class BindInterceptor {

    // Namespace for logging socket bind events
    public static final String NAMESPACE = "org.jvmxray.events.net.socket.bind";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code ServerSocket.bind} method.
     * Enters MCC correlation scope for network event tracking.
     *
     * @param serverSocket The {@code ServerSocket} instance being bound.
     * @param endpoint     The {@code SocketAddress} to bind to.
     * @param backlog      The backlog for the socket's connection queue.
     */
    @Advice.OnMethodEnter
    public static void enter(
            @Advice.This ServerSocket serverSocket,
            @Advice.Argument(0) SocketAddress endpoint,
            @Advice.Argument(1) int backlog) {
        MCCScope.enter("Network");
    }

    /**
     * Intercepts the exit of the {@code ServerSocket.bind} method to log the bind result.
     * Captures both successful binds and exceptions.
     *
     * @param serverSocket The {@code ServerSocket} instance being bound.
     * @param endpoint     The {@code SocketAddress} to bind to.
     * @param backlog      The backlog for the socket's connection queue.
     * @param thrown       The {@code Throwable} thrown during the bind operation, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This ServerSocket serverSocket,
            @Advice.Argument(0) SocketAddress endpoint,
            @Advice.Argument(1) int backlog,
            @Advice.Thrown Throwable thrown) {
        try {
            // Initialize metadata for logging
            Map<String, String> metadata = new HashMap<>();

            // Extract bind address details
            try {
                InetSocketAddress localAddr = (InetSocketAddress) endpoint;
                metadata.put("bind_src", localAddr.getAddress().getHostAddress() + ":" + localAddr.getPort());
                metadata.put("local_port", String.valueOf(localAddr.getPort()));
            } catch (Exception e) {
                metadata.put("bind_src", "unknown:0");
            }

            // Add backlog value
            metadata.put("backlog", String.valueOf(backlog));

            // Set status based on success or failure
            metadata.put("status", thrown != null ?
                    "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "bound");

            // Log the socket bind event
            logProxy.logMessage(NAMESPACE, "INFO", metadata);
        } finally {
            MCCScope.exit("Network");
        }
    }
}
