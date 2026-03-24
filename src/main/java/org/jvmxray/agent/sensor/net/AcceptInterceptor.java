package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging socket accept events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code accept} method of the
 * {@link ServerSocket} class, capturing the source and destination addresses and operation status.
 * Events are logged with contextual metadata using the {@link LogProxy} for detailed tracking of
 * network connection accept operations.
 *
 * @author Milton Smith
 */
public class AcceptInterceptor {

    // Namespace for logging socket accept events
    public static final String NAMESPACE = "org.jvmxray.events.net.socket.accept";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code ServerSocket.accept} method.
     * This implementation performs no operations but is included for potential future use.
     *
     * @param serverSocket The {@code ServerSocket} instance accepting connections.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This ServerSocket serverSocket) {
        // No operations required on method entry
    }

    /**
     * Intercepts the exit of the {@code ServerSocket.accept} method to log the accept result.
     * Captures both successful accepts and exceptions.
     *
     * @param serverSocket The {@code ServerSocket} instance accepting connections.
     * @param clientSocket The {@code Socket} instance representing the accepted connection, or null if an exception occurred.
     * @param thrown       The {@code Throwable} thrown during the accept operation, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This ServerSocket serverSocket,
            @Advice.Return Socket clientSocket,
            @Advice.Thrown Throwable thrown) {
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bind_src", serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
        metadata.put("dst", ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress() + ":" +
                ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getPort());
        metadata.put("status", thrown != null ?
                "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "accepted");
        // Log the socket accept event
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }
}