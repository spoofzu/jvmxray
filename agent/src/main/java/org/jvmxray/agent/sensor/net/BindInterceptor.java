package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

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
     * This implementation performs no operations but is included for potential future use.
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
        // No operations required on method entry
    }

    /**
     * Intercepts the exit of the {@code ServerSocket.bind} method to log the bind result.
     * Captures both successful binds and exceptions.
     *
     * @param serverSocket The {@code ServerSocket} instance being bound.
     * @param endpoint     The {@code SocketAddress} to bind to.
     * @param thrown       The {@code Throwable} thrown during the bind operation, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This ServerSocket serverSocket,
            @Advice.Argument(0) SocketAddress endpoint,
            @Advice.Thrown Throwable thrown) {
        // Cast endpoint to InetSocketAddress for address and port details
        InetSocketAddress localAddr = (InetSocketAddress) endpoint;

        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bind_src", localAddr.getAddress().getHostAddress() + ":" + localAddr.getPort());
        // Note: The following line overwrites the previous "status" entry, likely a bug.
        // Only the second "status" value ("accepted" or exception details) is logged.
        metadata.put("status", thrown != null ?
                "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "bound");
        metadata.put("status", thrown != null ?
                "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "accepted");

        // Log the socket bind event
        logProxy.logEvent(NAMESPACE, "INFO", metadata);
    }
}