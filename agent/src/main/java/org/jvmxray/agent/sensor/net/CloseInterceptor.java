package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging socket close events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code close} method of the
 * {@link Socket} class, capturing the local and remote addresses, ports, and operation status.
 * Events are logged with contextual metadata using the {@link LogProxy} for detailed tracking of
 * socket close operations.
 *
 * @author Milton Smith
 */
public class CloseInterceptor {
    // Thread-local storage for recording the start time of the close operation
    public static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    // Namespace for logging socket close events
    public static final String NAMESPACE = "org.jvmxray.events.net.socket.close";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code Socket.close} method to record the start time.
     *
     * @param socket The {@code Socket} instance being closed.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This Socket socket) {
        // Record the start time of the close operation
        startTime.set(System.currentTimeMillis());
    }

    /**
     * Intercepts the exit of the {@code Socket.close} method to log the close result.
     * Captures both successful closes and exceptions, and cleans up thread-local storage.
     *
     * @param socket The {@code Socket} instance being closed.
     * @param thrown The {@code Throwable} thrown during the close operation, or null if successful.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This Socket socket,
            @Advice.Thrown Throwable thrown) {
        // Retrieve remote address and port, handling null case
        InetSocketAddress remoteAddr = (InetSocketAddress) socket.getRemoteSocketAddress();
        String remote = remoteAddr != null ? remoteAddr.getAddress().getHostAddress() + ":" + remoteAddr.getPort() : "unknown:0";
        // Retrieve local address and port
        String local = socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();

        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bind_src", local);
        metadata.put("dst", remote);
        metadata.put("status", thrown != null ?
                "threw " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : "closed");

        // Log the socket close event
        logProxy.logEvent(NAMESPACE, "INFO", metadata);

        // Clean up thread-local storage
        startTime.remove();
    }
}