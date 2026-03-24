package org.jvmxray.agent.sensor.net;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;

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
    // Thread-local flag to skip logging for agent internal operations
    public static final ThreadLocal<Boolean> skipLogging = ThreadLocal.withInitial(() -> false);
    // Namespace for logging socket connect events
    public static final String NAMESPACE = "org.jvmxray.events.net.socket.connect";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Checks if the current connection is from agent's internal operations.
     * This filters out noise from the agent's SocketAppender for log shipping.
     */
    private static boolean isAgentInternalConnection() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                // Skip agent classes, logback appenders, and known framework classes
                if (className.startsWith("org.jvmxray") ||
                    className.contains("SocketAppender") ||
                    className.contains("logback") ||
                    className.contains("ShadedSQLite")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors during check
        }
        return false;
    }

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
        // Check if this is an agent internal operation
        if (isAgentInternalConnection()) {
            skipLogging.set(true);
            return;
        }
        skipLogging.set(false);
        // Enter MCC correlation scope
        MCCScope.enter("Network");
        // Record the start time of the connect operation
        startTime.set(System.currentTimeMillis());
    }

    /**
     * Intercepts the exit of the {@code Socket.connect} method to log the connect result.
     * Captures both successful connections and exceptions, and cleans up thread-local storage.
     * Enhanced with network metadata and TLS/SSL session information.
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
        // Skip logging for agent internal operations
        if (skipLogging.get()) {
            skipLogging.remove();
            return;
        }

        try {
            // Calculate connection time
            Long start = startTime.get();
            long connectionTimeMs = start != null ? System.currentTimeMillis() - start : -1;

            // Initialize metadata with enhanced network info
            Map<String, String> metadata = NetworkUtils.extractSocketMetadata(socket, endpoint, "CONNECT");

            // Add connection timing
            if (connectionTimeMs >= 0) {
                metadata.put("connection_time_ms", String.valueOf(connectionTimeMs));
            }

            // Determine local address, defaulting to localhost if not bound
            String local = socket.isBound() ? socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() : "localhost:0";
            metadata.put("bind_src", local);

            // Set status based on success or failure
            if (thrown != null) {
                metadata.put("status", "failed");
                metadata.put("error_class", thrown.getClass().getSimpleName());
                metadata.put("error_message", thrown.getMessage());
            } else {
                metadata.put("status", "connected");

                // Extract TLS/SSL metadata for SSL sockets (after successful connection)
                Map<String, String> tlsMetadata = NetworkUtils.extractTLSMetadata(socket);
                metadata.putAll(tlsMetadata);
            }

            // Log the socket connect event
            logProxy.logMessage(NAMESPACE, "INFO", metadata);

            // Clean up thread-local storage
            startTime.remove();
        } finally {
            skipLogging.remove();
            MCCScope.exit("Network");
        }
    }
}