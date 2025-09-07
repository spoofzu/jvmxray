package org.jvmxray.platform.client.mcp.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.*;

import java.io.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.HttpEntity;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REST HTTP MCP Bridge Client for Claude Desktop
 *
 * Forwards MCP protocol messages between Claude Desktop (via stdio)
 * and the JVMXRay REST API MCP server.
 * 
 * Enhanced error handling and logging based on user feedback.
 */
public class McpStdioToRestBridge {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Command line options
    private String serverHost = "localhost";
    private int serverPort = 9000;
    private String baseUrl;
    private String apiKey = null;
    private String debugFilePath = null;
    private boolean debugMode = false;

    // HTTP client
    private CloseableHttpClient httpClient;

    // stdio streams
    private BufferedReader stdinReader;
    private PrintWriter stdoutWriter;

    // Session management
    private final String clientSessionId = UUID.randomUUID().toString().substring(0, 8);
    private final AtomicBoolean isAuthenticated = new AtomicBoolean(false);

    // Debug logging
    private PrintWriter debugWriter;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Message queuing
    private final BlockingQueue<String> outboundMessages = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        McpStdioToRestBridge bridge = new McpStdioToRestBridge();
        if (!bridge.parseArguments(args)) {
            System.exit(1);
        }
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

        // Build base URL
        baseUrl = String.format("http://%s:%d/api/mcp", serverHost, serverPort);
        return true;
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar prj-mcp-client-bridge.jar [options]",
                "JVMXRay MCP REST Bridge Client", options, null);
    }

    private void start() throws IOException {
        // Initialize debug logging if enabled
        if (debugMode && debugFilePath != null) {
            try {
                File debugFile = new File(debugFilePath);
                File parentDir = debugFile.getParentFile();
                if (parentDir != null) parentDir.mkdirs();
                debugWriter = new PrintWriter(new FileWriter(debugFile, true), true);
                logDebugHeader();
            } catch (IOException e) {
                System.err.println("Warning: Failed to initialize debug logging: " + e.getMessage());
                debugMode = false;
            }
        }

        // Initialize HTTP client with timeouts
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(30000)
                .build();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();

        // Initialize stdio
        stdinReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        stdoutWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

        // Test server connection (send Authorization header too)
        testServerConnection();

        // Start message processing threads
        Thread stdinProcessor = new Thread(this::processStdinMessages, "stdin-processor");
        Thread outboundProcessor = new Thread(this::processOutboundMessages, "outbound-processor");

        stdinProcessor.start();
        outboundProcessor.start();

        try {
            stdinProcessor.join();
            outboundProcessor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        cleanup();
    }

    private void testServerConnection() throws IOException {
        logDebug("CONNECT", String.format("Testing connection to %s", baseUrl));

        String healthUrl = String.format("http://%s:%d/api/health", serverHost, serverPort);

        int maxRetries = 5;
        long retryDelayMs = 2000;
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logDebug("CONNECT", String.format("Connection attempt %d/%d to %s", attempt, maxRetries, healthUrl));

                HttpGet request = new HttpGet(healthUrl);
                if (apiKey != null) {
                    request.setHeader("Authorization", "Bearer " + apiKey);
                }

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        logDebug("CONNECT", String.format("Successfully connected to server (attempt %d)", attempt));
                        isAuthenticated.set(true);
                        return;
                    } else if (statusCode == 401) {
                        throw new IOException("Authentication failed: Invalid API key. Please check your API key and try again.");
                    } else if (statusCode == 403) {
                        throw new IOException("Access denied: API key does not have required permissions.");
                    } else if (statusCode == 404) {
                        throw new IOException("Server endpoint not found. Please verify the server is running and the port is correct.");
                    } else {
                        throw new IOException("Server health check failed with status: " + statusCode + ". Please check server logs for details.");
                    }
                }

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (e instanceof java.net.ConnectException) {
                    errorMessage = "Cannot connect to server at " + serverHost + ":" + serverPort + ". Please ensure the server is running.";
                } else if (e instanceof java.net.UnknownHostException) {
                    errorMessage = "Unknown host: " + serverHost + ". Please check the hostname and try again.";
                } else if (e instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Connection timeout. Please check network connectivity and server status.";
                }
                
                lastException = new IOException(errorMessage, e);
                if (attempt < maxRetries) {
                    logDebug("CONNECT", String.format("Connection attempt %d failed: %s. Retrying in %dms...",
                            attempt, errorMessage, retryDelayMs));
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted during retry", ie);
                    }
                } else {
                    logDebug("CONNECT", String.format("All %d connection attempts failed. Final error: %s",
                            maxRetries, errorMessage));
                }
            }
        }

        throw lastException;
    }

    private void processStdinMessages() {
        try {
            String line;
            while (running.get() && (line = stdinReader.readLine()) != null) {
                logDebug("CLIENT_REQUEST", prettyPrintJson(line));
                outboundMessages.offer(line);
            }
        } catch (IOException e) {
            if (running.get()) {
                logDebug("ERROR", "Error reading from stdin: " + e.getMessage());
                running.set(false);
            }
        }
    }

    private void processOutboundMessages() {
        while (running.get()) {
            try {
                String message = outboundMessages.poll(1, TimeUnit.SECONDS);
                if (message != null && isAuthenticated.get()) {
                    processAndForwardMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logDebug("ERROR", "Error processing outbound message: " + e.getMessage());
            }
        }
    }

    private void processAndForwardMessage(String message) {
        try {
            JsonNode mcpRequest = MAPPER.readTree(message);
            JsonNode methodNode = mcpRequest.get("method");
            String method = (methodNode != null && !methodNode.isNull()) ? methodNode.asText() : null;

            // Skip notifications - they don't need responses and shouldn't be forwarded
            if (method != null && method.startsWith("notifications/")) {
                logDebug("NOTIFICATION", String.format("Ignoring notification: %s", method));
                return;
            }

            // Determine the appropriate REST endpoint
            String endpoint = determineEndpoint(method);
            String fullUrl = baseUrl + endpoint;

            logDebug("REST_REQUEST", String.format("Sending %s to %s", String.valueOf(method), fullUrl));

            HttpPost request = new HttpPost(fullUrl);
            request.setHeader("Content-Type", "application/json");
            if (apiKey != null) request.setHeader("Authorization", "Bearer " + apiKey);

            request.setEntity(new StringEntity(message, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = "";
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                }

                logDebug("REST_RESPONSE", String.format("Status: %d, Body: %s",
                        statusCode, prettyPrintJson(responseBody)));

                if (statusCode == 200) {
                    String formattedResponse = ensureJsonRpcFormat(responseBody);
                    stdoutWriter.println(formattedResponse);
                    stdoutWriter.flush();
                    logDebug("CLIENT_FORWARD", prettyPrintJson(formattedResponse));
                } else {
                    handleErrorResponse(statusCode, responseBody, mcpRequest);
                }
            }

        } catch (Exception e) {
            logDebug("ERROR", "Failed to process message: " + e.getMessage());
            sendErrorResponse(message, "Internal server error: " + e.getMessage());
        }
    }

    private String determineEndpoint(String method) {
        // New pass-through controller handles all methods via single endpoint
        // The JSON method field determines behavior
        return "";
    }

    private void handleErrorResponse(int statusCode, String responseBody, JsonNode originalRequest) {
        try {
            ObjectNode errorResponse = MAPPER.createObjectNode();
            errorResponse.put("jsonrpc", "2.0");
            if (originalRequest.has("id")) {
                errorResponse.set("id", originalRequest.get("id")); // preserve id type
            } else {
                errorResponse.set("id", null);
            }

            ObjectNode error = MAPPER.createObjectNode();
            error.put("code", -32603);
            
            // Enhanced error messages based on status code
            String errorMessage;
            switch (statusCode) {
                case 400:
                    errorMessage = "Bad request: Invalid request format or parameters";
                    break;
                case 401:
                    errorMessage = "Unauthorized: API key authentication failed";
                    break;
                case 403:
                    errorMessage = "Forbidden: Insufficient permissions for this operation";
                    break;
                case 404:
                    errorMessage = "Not found: Requested resource or method not available";
                    break;
                case 429:
                    errorMessage = "Too many requests: Rate limit exceeded";
                    break;
                case 500:
                    errorMessage = "Internal server error: Server encountered an unexpected condition";
                    break;
                case 502:
                    errorMessage = "Bad gateway: Server received an invalid response from upstream";
                    break;
                case 503:
                    errorMessage = "Service unavailable: Server temporarily unavailable";
                    break;
                default:
                    errorMessage = "Server error: HTTP " + statusCode;
                    break;
            }
            error.put("message", errorMessage);

            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonNode responseJson = MAPPER.readTree(responseBody);
                    if (responseJson.has("error")) {
                        error.set("data", responseJson.get("error"));
                    } else {
                        error.set("data", responseJson);
                    }
                } catch (Exception ignored) {
                    error.put("data", responseBody);
                }
            }

            errorResponse.set("error", error);

            String errorResponseStr = MAPPER.writeValueAsString(errorResponse);
            stdoutWriter.println(errorResponseStr);
            stdoutWriter.flush();

            logDebug("CLIENT_ERROR", prettyPrintJson(errorResponseStr));

        } catch (Exception e) {
            logDebug("ERROR", "Failed to create error response: " + e.getMessage());
        }
    }

    private void sendErrorResponse(String originalMessage, String errorMessage) {
        try {
            JsonNode originalRequest = MAPPER.readTree(originalMessage);
            ObjectNode errorResponse = MAPPER.createObjectNode();
            errorResponse.put("jsonrpc", "2.0");
            if (originalRequest.has("id")) {
                errorResponse.set("id", originalRequest.get("id")); // preserve id type
            } else {
                errorResponse.set("id", null);
            }

            ObjectNode error = MAPPER.createObjectNode();
            error.put("code", -32603);
            error.put("message", errorMessage);
            errorResponse.set("error", error);

            String errorResponseStr = MAPPER.writeValueAsString(errorResponse);
            stdoutWriter.println(errorResponseStr);
            stdoutWriter.flush();

            logDebug("CLIENT_ERROR", prettyPrintJson(errorResponseStr));

        } catch (Exception e) {
            logDebug("ERROR", "Failed to send error response: " + e.getMessage());
        }
    }

    private String ensureJsonRpcFormat(String responseBody) {
        try {
            JsonNode response = MAPPER.readTree(responseBody);

            if (response.has("jsonrpc")) {
                return responseBody; // already JSON-RPC 2.0
            }

            ObjectNode jsonRpcResponse = MAPPER.createObjectNode();
            jsonRpcResponse.put("jsonrpc", "2.0");
            jsonRpcResponse.set("id", response.has("id") ? response.get("id") : null);

            if (response.has("error")) {
                jsonRpcResponse.set("error", response.get("error"));
            } else {
                JsonNode result = response.has("result") ? response.get("result") : response;
                jsonRpcResponse.set("result", result);
            }

            return MAPPER.writeValueAsString(jsonRpcResponse);

        } catch (Exception e) {
            return responseBody; // best effort
        }
    }

    private String prettyPrintJson(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return json;
        }
    }

    private void logDebugHeader() {
        if (debugWriter != null) {
            debugWriter.println();
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 MCP REST Bridge Debug Session Started [%s]: %s%n",
                    clientSessionId,
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            debugWriter.println("================================================================================");
            debugWriter.printf("Server: %s%n", baseUrl);
            debugWriter.printf("Session ID: %s%n", clientSessionId);
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
            case "REST_REQUEST":   return "➡️";
            case "REST_RESPONSE":  return "📤";
            case "CLIENT_FORWARD": return "⬅️";
            case "CLIENT_ERROR":   return "❌";
            case "ERROR":          return "❌";
            case "CONNECT":        return "🔌";
            case "DISCONNECT":     return "🔌❌";
            default:               return "📝";
        }
    }

    private void cleanup() {
        running.set(false);
        try {
            if (stdinReader != null) stdinReader.close();
            if (stdoutWriter != null) stdoutWriter.close();
            if (httpClient != null) httpClient.close();
        } catch (IOException ignored) {}

        if (debugWriter != null) {
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 MCP REST Bridge Debug Session Ended [%s]: %s%n",
                    clientSessionId,
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            debugWriter.println("================================================================================");
            debugWriter.close();
            debugWriter = null;
        }
    }
}
