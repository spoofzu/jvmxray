package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging socket connect events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code connect} method of the
 * {@link Socket} class, capturing the local and remote addresses, ports, and operation status.
 * Events are logged with contextual metadata using the {@link LogProxy} for detailed tracking of
 * socket connect operations.
 *
 * @author Milton Smith
 */
public class ConnectInterceptor {
    // Thread-local storage for recording the start time of the connect operation
    public static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    // Namespace for logging socket connect events
    public static final String NAMESPACE = "org.jvmxray.events.net.socket.connect";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code Socket.connect} method to record the start time.
     *
     * @param socket   The {@code Socket} instance initiating the connection.
     * @param endpoint The {@code SocketAddress} to connect to.
     * @param timeout  The connection timeout in milliseconds.
     */
    @Advice.OnMethodEnter
    public static void enter(
            @Advice.This Socket socket,
            @Advice.Argument(0) SocketAddress endpoint,
            @Advice.Argument(1) int timeout) {
        // Record the start time of the connect operation
        startTime.set(System.currentTimeMillis());
    }

    /**
     * Intercepts the exit of the {@code Socket.connect} method to log the connect result.
     * Captures both successful connections and exceptions, and cleans up thread-local storage.
     *
     * @param socket   The {@code Socket} instance initiating the connection.
     * @param endpoint The {@code SocketAddress} to connect to.
     * @param thrown   The {@code Throwable} thrown during the connect operation, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This Socket socket,
            @Advice.Argument(0) SocketAddress endpoint,
            @Advice.Thrown Throwable thrown) {
        // Cast endpoint to InetSocketAddress for address and port details
        InetSocketAddress remoteAddr = (InetSocketAddress) endpoint;
        // Determine local address, defaulting to localhost if not bound
        String local = socket.isBound() ? socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() : "localhost:0";

        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bind_src", local);
        metadata.put("dst", remoteAddr.getAddress().getHostAddress() + ":" + remoteAddr.getPort());
        metadata.put("status", thrown != null ?
                "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "connected");

        // Log the socket connect event
        logProxy.logMessage(NAMESPACE, "INFO", metadata);

        // Clean up thread-local storage
        startTime.remove();
    }
}