package org.jvmxray.service.log.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced socket server for receiving logging events from JVMXRay agents.
 * Extends SimpleSocketServer to provide connection lifecycle management,
 * multi-agent support, and enhanced event routing capabilities.
 *
 * @author Milton Smith
 */
public class SocketServer extends SimpleSocketServer {

    // Logger initialized in constructor after LogServiceInitializer has run
    private final Logger logger;
    
    // Track active connections
    private final Map<SocketAddress, ClientConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCounter = new AtomicInteger(0);
    private final int serverPort;
    
    /**
     * Creates a new SocketServer instance.
     * Logger is initialized here to ensure LogServiceInitializer has already run.
     *
     * @param lc The LoggerContext to use for processing events
     * @param port The port to listen on for agent connections
     */
    public SocketServer(LoggerContext lc, int port) {
        super(lc, port);
        this.serverPort = port;
        // Safe to initialize logger here because LogService.main() calls LogServiceInitializer first
        this.logger = LoggerFactory.getLogger(SocketServer.class);
    }

    /**
     * Override the parent run method to use our custom client handling
     * instead of SocketNode which has deserialization security restrictions.
     */
    @Override
    public void run() {
        logger.info("CUSTOM SocketServer run() method started - bypassing SimpleSocketServer");
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            
            logger.info("CUSTOM SocketServer listening on port {} with shaded class support", serverPort);
            
            while (!isClosed()) {
                logger.info("CUSTOM SocketServer waiting to accept a new client.");
                handleClient(serverSocket);
            }
            
        } catch (IOException e) {
            logger.error("Error in CUSTOM SocketServer run loop: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles new client connections with enhanced lifecycle management.
     */
    protected void handleClient(ServerSocket serverSocket) throws IOException {
        Socket client = serverSocket.accept();
        int connectionId = connectionCounter.incrementAndGet();
        SocketAddress clientAddress = client.getRemoteSocketAddress();
        
        logger.info("Agent connection established from {} (ID: {})", clientAddress, connectionId);
        
        // Create and track the connection
        ClientConnection connection = new ClientConnection(client, connectionId);
        activeConnections.put(clientAddress, connection);
        
        // Handle the connection in a separate thread
        Thread connectionThread = new Thread(() -> handleConnection(connection), 
                                            "JvmxrayAgent-" + connectionId);
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
    
    /**
     * Handles a client connection, processing incoming events.
     *
     * @param connection The client connection to handle
     */
    private void handleConnection(ClientConnection connection) {
        Socket client = connection.getSocket();
        String agentId = "agent-" + connection.getId();
        
        try (ObjectInputStream ois = new ShadedClassObjectInputStream(client.getInputStream())) {
            logger.info("Started processing events from agent {}", agentId);
            
            while (!client.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Read the logging event from the agent
                    Object obj = ois.readObject();
                    
                    if (obj instanceof ILoggingEvent) {
                        ILoggingEvent event = (ILoggingEvent) obj;
                        processEvent(event, agentId);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warn("Unknown class received from agent {}: {}", agentId, e.getMessage());
                } catch (IOException e) {
                    // Connection likely closed
                    break;
                }
            }
        } catch (IOException e) {
            logger.warn("Error handling connection from agent {}: {}", agentId, e.getMessage());
        } finally {
            handleDisconnect(client);
        }
    }
    
    /**
     * Processes a logging event from an agent with enhanced routing.
     *
     * @param event The logging event to process
     * @param agentId The ID of the agent that sent the event
     */
    private void processEvent(ILoggingEvent event, String agentId) {
        try {
            // Add agent ID to MDC for this thread
            MDC.put("agentId", agentId);
            
            // Get the appropriate logger for this event
            String loggerName = "jvmxray.agent." + event.getLoggerName();
            Logger eventLogger = LoggerFactory.getLogger(loggerName);
            
            // Log the event with the appropriate level
            switch (event.getLevel().levelInt) {
                case ch.qos.logback.classic.Level.ERROR_INT:
                    eventLogger.error(event.getFormattedMessage());
                    break;
                case ch.qos.logback.classic.Level.WARN_INT:
                    eventLogger.warn(event.getFormattedMessage());
                    break;
                case ch.qos.logback.classic.Level.INFO_INT:
                    eventLogger.info(event.getFormattedMessage());
                    break;
                case ch.qos.logback.classic.Level.DEBUG_INT:
                    eventLogger.debug(event.getFormattedMessage());
                    break;
                case ch.qos.logback.classic.Level.TRACE_INT:
                    eventLogger.trace(event.getFormattedMessage());
                    break;
                default:
                    eventLogger.info(event.getFormattedMessage());
                    break;
            }
        } finally {
            // Clear MDC to avoid leaking context
            MDC.clear();
        }
    }
    
    /**
     * Handles client disconnection and cleanup.
     *
     * @param client The client socket that disconnected
     */
    private void handleDisconnect(Socket client) {
        SocketAddress clientAddress = client.getRemoteSocketAddress();
        ClientConnection connection = activeConnections.remove(clientAddress);
        
        if (connection != null) {
            logger.info("Agent connection closed from {} (ID: {})", clientAddress, connection.getId());
            connection.close();
        }
        
        try {
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing client socket: {}", e.getMessage());
        }
    }
    
    /**
     * Gets the number of active agent connections.
     *
     * @return The number of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * Shuts down the server and closes all active connections.
     */
    @Override
    public void close() {
        logger.info("Shutting down SocketServer with {} active connections",
                   activeConnections.size());
        
        // Close all active connections
        for (ClientConnection connection : activeConnections.values()) {
            connection.close();
        }
        activeConnections.clear();
        
        // Call parent close method
        super.close();
    }
    
    /**
     * Represents a client connection with metadata.
     */
    private static class ClientConnection {
        private final Socket socket;
        private final int id;
        private final long connectedAt;
        
        public ClientConnection(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
            this.connectedAt = System.currentTimeMillis();
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public int getId() {
            return id;
        }
        
        public long getConnectedAt() {
            return connectedAt;
        }
        
        public void close() {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Log but don't throw - use fallback logger since initialization is safe here
                LoggerFactory.getLogger(ClientConnection.class)
                    .warn("Error closing connection {}: {}", id, e.getMessage());
            }
        }
    }
    
    /**
     * Custom ObjectInputStream that maps shaded logback classes to standard ones.
     */
    private class ShadedClassObjectInputStream extends ObjectInputStream {
        
        public ShadedClassObjectInputStream(InputStream in) throws IOException {
            super(in);
        }
        
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            
            // Map shaded logback classes to their standard equivalents
            if (name.startsWith("agent.shadow.logback.")) {
                String standardClassName = name.replace("agent.shadow.logback.", "ch.qos.logback.");
                
                logger.debug("Mapping shaded class {} to {}", name, standardClassName);
                
                try {
                    return Class.forName(standardClassName);
                } catch (ClassNotFoundException e) {
                    logger.warn("Failed to find standard class for {}, trying original", standardClassName);
                    // Fall through to original resolution
                }
            }
            
            return super.resolveClass(desc);
        }
    }
}