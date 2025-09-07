package org.jvmxray.platform.client.mcp.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP Socket MCP Bridge Client for Claude Desktop
 * 
 * This client forwards MCP protocol messages between Claude Desktop (via stdio)
 * and the JVMXRay TCP socket MCP server. It handles the authentication protocol
 * and bidirectional message forwarding.
 * 
 * Enhanced error handling and logging based on user feedback.
 * 
 * @author Milton Smith
 */
public class McpStdioToSocketBridge {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    // Command line options
    private String serverHost = "localhost";
    private int serverPort = 9000;
    private String apiKey = null;
    private String debugFilePath = null;
    private boolean debugMode = false;
    
    // Socket connection state
    private Socket socket;
    private BufferedReader socketReader;
    private BufferedWriter socketWriter;
    private BufferedReader stdinReader;
    private PrintWriter stdoutWriter;
    
    // Session management
    private final String clientSessionId = UUID.randomUUID().toString().substring(0, 8);
    private String serverSessionId = null;
    private boolean isAuthenticated = false;
    
    // Debug logging
    private PrintWriter debugWriter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    // Message queuing
    private final BlockingQueue<String> outboundMessages = new LinkedBlockingQueue<>();
    
    public static void main(String[] args) {
        McpStdioToSocketBridge bridge = new McpStdioToSocketBridge();
        
        // Parse command line arguments
        if (!bridge.parseArguments(args)) {
            System.exit(1);
        }
        
        // Start the bridge
        try {
            bridge.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private boolean parseArguments(String[] args) {
        Options options = new Options();
        
        // Define command line options
        options.addOption("h", "help", false, "Show this help message");
        options.addOption(null, "host", true, "Server hostname (default: localhost)");
        options.addOption("p", "port", true, "Server port (default: 9000)");
        options.addOption(null, "api-key", true, "API key for authentication");
        options.addOption(null, "debug", true, "Debug file path for logging");
        
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            
            if (cmd.hasOption("help")) {
                printHelp(options);
                return false;
            }
            
            if (cmd.hasOption("host")) {
                serverHost = cmd.getOptionValue("host");
            }
            
            if (cmd.hasOption("port")) {
                try {
                    serverPort = Integer.parseInt(cmd.getOptionValue("port"));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + cmd.getOptionValue("port"));
                    return false;
                }
            }
            
            if (cmd.hasOption("api-key")) {
                apiKey = cmd.getOptionValue("api-key");
            }
            
            if (cmd.hasOption("debug")) {
                debugFilePath = cmd.getOptionValue("debug");
                debugMode = true;
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printHelp(options);
            return false;
        }
        
        // API key can also come from environment variable
        if (apiKey == null) {
            apiKey = System.getenv("JVMXRAY_API_KEY");
        }
        
        if (apiKey == null) {
            System.err.println("Error: API key required (use --api-key or JVMXRAY_API_KEY env var)");
            printHelp(options);
            return false;
        }
        
        return true;
    }
    
    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar prj-mcp-client-bridge.jar [options]", 
            "JVMXRay MCP Socket Bridge Client", options, null);
    }
    
    private void start() throws IOException {
        // Initialize debug logging if enabled
        if (debugMode && debugFilePath != null) {
            try {
                File debugFile = new File(debugFilePath);
                File parentDir = debugFile.getParentFile();
                if (parentDir != null) {
                    parentDir.mkdirs();
                }
                debugWriter = new PrintWriter(new FileWriter(debugFile, true), true);
                logDebugHeader();
            } catch (IOException e) {
                System.err.println("Warning: Failed to initialize debug logging: " + e.getMessage());
                debugMode = false;
            }
        }
        
        // Initialize stdio
        stdinReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        stdoutWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        
        // Connect to TCP socket server
        connectToSocketServer();
        
        // Authenticate with server
        authenticateWithServer();
        
        // Start message processing threads
        Thread stdinProcessor = new Thread(this::processStdinMessages, "stdin-processor");
        Thread socketProcessor = new Thread(this::processSocketMessages, "socket-processor");
        Thread outboundProcessor = new Thread(this::processOutboundMessages, "outbound-processor");
        
        stdinProcessor.start();
        socketProcessor.start();
        outboundProcessor.start();
        
        // Wait for threads to complete
        try {
            stdinProcessor.join();
            socketProcessor.join();
            outboundProcessor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cleanup
        cleanup();
    }
    
    private void connectToSocketServer() throws IOException {
        logDebug("CONNECT", String.format("Connecting to %s:%d", serverHost, serverPort));
        
        try {
            socket = new Socket(serverHost, serverPort);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            
            logDebug("CONNECT", "Successfully connected to server");
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to server at " + serverHost + ":" + serverPort + ". Please ensure the server is running and the port is correct.", e);
        } catch (java.net.UnknownHostException e) {
            throw new IOException("Unknown host: " + serverHost + ". Please check the hostname and try again.", e);
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("Connection timeout. Please check network connectivity and server status.", e);
        } catch (java.net.BindException e) {
            throw new IOException("Port " + serverPort + " is not available. Please check if the server is running on the correct port.", e);
        }
    }
    
    private void authenticateWithServer() throws IOException {
        // Send authentication request
        ObjectNode authRequest = MAPPER.createObjectNode();
        authRequest.put("type", "auth");
        authRequest.put("apiKey", apiKey);
        authRequest.put("clientVersion", "1.0.0");
        authRequest.put("clientSessionId", clientSessionId);
        
        String authMessage = MAPPER.writeValueAsString(authRequest);
        logDebug("AUTH_REQUEST", authMessage);
        
        try {
            socketWriter.write(authMessage);
            socketWriter.newLine();
            socketWriter.flush();
        } catch (IOException e) {
            throw new IOException("Failed to send authentication request: " + e.getMessage(), e);
        }
        
        // Read authentication response
        String authResponse;
        try {
            authResponse = socketReader.readLine();
            if (authResponse == null) {
                throw new IOException("No authentication response received from server");
            }
        } catch (IOException e) {
            throw new IOException("Failed to read authentication response: " + e.getMessage(), e);
        }
        
        logDebug("AUTH_RESPONSE", authResponse);
        
        try {
            JsonNode authJson = MAPPER.readTree(authResponse);
            String status = authJson.get("status").asText();
            
            if (!"authenticated".equals(status)) {
                String errorMessage = "Authentication failed";
                if (authJson.has("message")) {
                    errorMessage += ": " + authJson.get("message").asText();
                } else {
                    errorMessage += ": " + status;
                }
                throw new IOException(errorMessage);
            }
            
            if (!authJson.has("sessionId")) {
                throw new IOException("Authentication response missing session ID");
            }
            
            serverSessionId = authJson.get("sessionId").asText();
            isAuthenticated = true;
            
            logDebug("AUTH", "Authenticated with session ID: " + serverSessionId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IOException("Invalid authentication response format: " + e.getMessage(), e);
        }
    }
    
    private void processStdinMessages() {
        try {
            String line;
            while (running.get() && (line = stdinReader.readLine()) != null) {
                logDebug("CLIENT_REQUEST", prettyPrintJson(line));
                
                // Queue wrapped message for sending to server
                ObjectNode wrappedMessage = MAPPER.createObjectNode();
                wrappedMessage.put("sessionId", serverSessionId);
                wrappedMessage.set("request", MAPPER.readTree(line));
                
                outboundMessages.offer(MAPPER.writeValueAsString(wrappedMessage));
            }
        } catch (IOException e) {
            if (running.get()) {
                logDebug("ERROR", "Error reading from stdin: " + e.getMessage());
                running.set(false);
            }
        }
    }
    
    private void processSocketMessages() {
        try {
            String line;
            while (running.get() && (line = socketReader.readLine()) != null) {
                logDebug("SERVER_RESPONSE", prettyPrintJson(line));
                
                try {
                    JsonNode wrapped = MAPPER.readTree(line);
                    
                    // Extract response from wrapper and forward to stdout
                    if (wrapped.has("response")) {
                        JsonNode response = wrapped.get("response");
                        
                        // The response from the socket server should already be a complete JSON-RPC response
                        // but if it's missing the jsonrpc field, we need to add it
                        String responseStr;
                        if (response.has("jsonrpc")) {
                            // Response already has JSON-RPC fields, use as-is
                            responseStr = MAPPER.writeValueAsString(response);
                        } else {
                            // Response is missing JSON-RPC wrapper, add it
                            // This happens when the response object only contains the result data
                            com.fasterxml.jackson.databind.node.ObjectNode jsonRpcResponse = MAPPER.createObjectNode();
                            jsonRpcResponse.put("jsonrpc", "2.0");
                            
                            // Copy id and result/error from the response
                            if (response.has("id")) {
                                jsonRpcResponse.set("id", response.get("id"));
                            }
                            if (response.has("result")) {
                                jsonRpcResponse.set("result", response.get("result"));
                            }
                            if (response.has("error")) {
                                jsonRpcResponse.set("error", response.get("error"));
                            }
                            
                            responseStr = MAPPER.writeValueAsString(jsonRpcResponse);
                        }
                        
                        stdoutWriter.println(responseStr);
                        stdoutWriter.flush();
                        
                        logDebug("CLIENT_FORWARD", prettyPrintJson(responseStr));
                    }
                    
                } catch (Exception e) {
                    logDebug("ERROR", "Failed to process server message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                logDebug("ERROR", "Error reading from socket: " + e.getMessage());
                running.set(false);
            }
        }
    }
    
    private void processOutboundMessages() {
        while (running.get()) {
            try {
                String message = outboundMessages.poll(1, TimeUnit.SECONDS);
                if (message != null && isAuthenticated) {
                    socketWriter.write(message);
                    socketWriter.newLine();
                    socketWriter.flush();
                    
                    logDebug("SERVER_FORWARD", prettyPrintJson(message));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logDebug("ERROR", "Error sending message to server: " + e.getMessage());
                running.set(false);
                break;
            }
        }
    }
    
    private String prettyPrintJson(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return json; // Return original if pretty printing fails
        }
    }
    
    private void logDebugHeader() {
        if (debugWriter != null) {
            debugWriter.println();
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 MCP Socket Bridge Debug Session Started [%s]: %s%n", 
                             clientSessionId, 
                             DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            debugWriter.println("================================================================================");
            debugWriter.printf("Server: %s:%d%n", serverHost, serverPort);
            debugWriter.printf("Session ID: pending%n");
            debugWriter.println();
        }
    }
    
    private void logDebug(String type, String message) {
        if (debugWriter != null) {
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            String icon = getIconForType(type);
            
            debugWriter.printf("%s [%s] [%s] %s:%n", icon, clientSessionId, timestamp, type);
            debugWriter.println(message);
            debugWriter.println();
            debugWriter.flush();
        }
    }
    
    private String getIconForType(String type) {
        switch (type) {
            case "CLIENT_REQUEST": return "📥";
            case "SERVER_FORWARD": return "➡️";
            case "SERVER_RESPONSE": return "📤";
            case "CLIENT_FORWARD": return "⬅️";
            case "AUTH_REQUEST": return "🔐";
            case "AUTH_RESPONSE": return "✅";
            case "AUTH": return "📝";
            case "ERROR": return "❌";
            case "CONNECT": return "🔌";
            case "DISCONNECT": return "🔌❌";
            default: return "📝";
        }
    }
    
    private void cleanup() {
        running.set(false);
        
        // Close socket connection
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
        
        // Close readers and writers
        try {
            if (socketReader != null) socketReader.close();
            if (socketWriter != null) socketWriter.close();
            if (stdinReader != null) stdinReader.close();
            if (stdoutWriter != null) stdoutWriter.close();
        } catch (IOException ignored) {}
        
        // Close debug logging
        if (debugWriter != null) {
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 MCP Socket Bridge Debug Session Ended [%s]: %s%n", 
                             clientSessionId, 
                             DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            debugWriter.println("================================================================================");
            debugWriter.close();
            debugWriter = null;
        }
    }
}
