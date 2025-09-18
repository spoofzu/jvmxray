package org.jvmxray.platform.client.mcp.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.util.GUID;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JvmxrayMcpClient {

    // ---------- Tunables ----------
    private static final int DEFAULT_PORT = 8080;
    private static final int CONNECT_TIMEOUT_SECS = 10;
    private static final int HTTP_TIMEOUT_SECS = 30;
    private static final int WORKERS_DEFAULT = 4;
    private static final int QUEUE_CAP_DEFAULT = 256;
    private static final int CLIENT_TIMEOUT_SECS = 0; // 0 = no CF timeout; rely on HTTP timeouts
    private static final String DEFAULT_PROTOCOL_FALLBACK = "2024-11-05";
    // --------------------------------

    private String serverHost = "localhost";
    private int serverPort = DEFAULT_PORT;
    private String baseUrl;
    private String apiKey = null;
    private String debugFilePath = null;
    private boolean debugMode = false;
    private int workers = WORKERS_DEFAULT;
    private int queueCap = QUEUE_CAP_DEFAULT;
    private int batchWindowMs = 0; // 0 = disabled (immediate)

    private HttpClient httpClient;
    private PrintWriter debugWriter;
    private final String sessionId = GUID.generateShort();

    McpJsonMapper jsonMapper;
    private RequestProcessor requestProcessor;

    private BufferedReader stdinReader;
    private PrintWriter stdoutWriter;

    private enum ConnectionState { CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED }
    private volatile ConnectionState state = ConnectionState.CONNECTING;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public static void main(String[] args) {
        JvmxrayMcpClient client = new JvmxrayMcpClient();
        if (!client.parseArguments(args)) System.exit(1);
        try {
            client.start();
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
        options.addOption("p", "port", true, "Server port (default: 8080)");
        options.addOption(null, "api-key", true, "API key for authentication");
        options.addOption(null, "debug", true, "Debug file path for logging");
        options.addOption(null, "workers", true, "Worker threads (default: " + WORKERS_DEFAULT + ")");
        options.addOption(null, "queue", true, "Max in-flight queue (default: " + QUEUE_CAP_DEFAULT + ")");
        options.addOption(null, "batch-window-ms", true, "Coalesce list-calls for N ms (default: 0=off)");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp(options);
                return false;
            }
            if (cmd.hasOption("host")) serverHost = cmd.getOptionValue("host");
            if (cmd.hasOption("port")) {
                try { serverPort = Integer.parseInt(cmd.getOptionValue("port")); }
                catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + cmd.getOptionValue("port"));
                    return false;
                }
            }
            if (cmd.hasOption("api-key")) apiKey = cmd.getOptionValue("api-key");
            if (cmd.hasOption("debug")) { debugFilePath = cmd.getOptionValue("debug"); debugMode = true; }
            if (cmd.hasOption("workers")) workers = Math.max(1, Integer.parseInt(cmd.getOptionValue("workers")));
            if (cmd.hasOption("queue")) queueCap = Math.max(8, Integer.parseInt(cmd.getOptionValue("queue")));
            if (cmd.hasOption("batch-window-ms")) batchWindowMs = Math.max(0, Integer.parseInt(cmd.getOptionValue("batch-window-ms")));

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(options);
            return false;
        }

        if (apiKey == null) apiKey = System.getenv("JVMXRAY_API_KEY");
        if (apiKey == null) {
            System.err.println("Error: API key required (use --api-key or JVMXRAY_API_KEY env var)");
            printHelp(options);
            return false;
        }

        baseUrl = String.format("http://%s:%d/api/mcp", serverHost, serverPort);
        return true;
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar prj-mcp-client-bridge.jar [options]",
                "JVMXRay MCP Client (stdio <-> REST)", options, null);
    }

    private void start() throws Exception {
        if (debugMode && debugFilePath != null) {
            try {
                File f = new File(debugFilePath);
                File parent = f.getParentFile();
                if (parent != null) parent.mkdirs();
                debugWriter = new PrintWriter(new FileWriter(f, true), true);
                logDebugHeader();
            } catch (IOException e) {
                System.err.println("Warning: Failed to initialize debug logging: " + e.getMessage());
                debugMode = false;
            }
        }

        installShutdownHook();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECS))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        jsonMapper = new JacksonMcpJsonMapper(objectMapper);

        stdinReader  = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        stdoutWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

        testServerConnection();

        requestProcessor = new RequestProcessor(this, workers, queueCap);

        state = ConnectionState.CONNECTED;
        logDebug("STATE_TRANSITION", "Connection state changed from CONNECTING to CONNECTED");
        logDebug("MCP", "Starting MCP stdio transport loop (no-stall, immediate processing)");
        runMcpLoop();
    }

    private void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShuttingDown.compareAndSet(false, true)) return;
            logDebug("SHUTDOWN", "Shutdown hook triggered");
            state = ConnectionState.DISCONNECTING;
            logDebug("STATE_TRANSITION", "Connection state changed to DISCONNECTING (shutdown hook)");
            // Force-unblock readLine()
            try { System.in.close(); } catch (IOException ignored) {}
            cleanup();
        }, "shutdown-hook"));
    }

    private void testServerConnection() throws Exception {
        logDebug("CONNECT", String.format("Testing connection to %s", baseUrl));
        String healthUrl = String.format("http://%s:%d/api/mcp/health", serverHost, serverPort);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECS))
                    .header("X-API-Key", apiKey)
                    .header("X-Session-Id", sessionId)
                    .header("User-Agent", "jvmxray-mcp-client/0.0.1")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logDebug("CONNECT", "Successfully connected to JVMXRay REST server");
            } else if (response.statusCode() == 401) {
                throw new Exception("Authentication failed: Invalid API key");
            } else if (response.statusCode() == 403) {
                throw new Exception("Access denied: API key lacks required permissions");
            } else {
                throw new Exception("Server health check failed with status: " + response.statusCode());
            }
        } catch (java.net.ConnectException e) {
            throw new Exception("Cannot connect to JVMXRay server at " + baseUrl + ". Ensure the server is running.");
        } catch (java.net.http.HttpConnectTimeoutException e) {
            throw new Exception("Connection timeout to JVMXRay server at " + baseUrl + ".");
        }
    }

    private void runMcpLoop() throws Exception {
        try {
            String line;
            logDebug("MCP_LOOP", "Reading from stdin...");
            // Optional micro-coalescer for *only* list calls if you enable --batch-window-ms
            final Coalescer coalescer = (batchWindowMs > 0) ? new Coalescer(batchWindowMs) : null;

            while (!isShuttingDown.get() && (line = stdinReader.readLine()) != null) {
                final String msg = line;
                try {
                    logDebug("REQUEST", prettyJson(msg));

                    if (coalescer != null && isBatchableRequest(msg)) {
                        coalescer.enqueue(msg, this::processRequestNow);
                    } else {
                        processRequestNow(msg);
                    }
                } catch (Exception e) {
                    logDebug("ERROR", "Error processing request: " + e.getMessage());
                    writeToStdout(createErrorResponse(msg, e.getMessage()));
                }
            }

            if (isShuttingDown.get()) {
                logDebug("SHUTDOWN", "stdin loop exited - shutdown flag set");
            } else {
                logDebug("SHUTDOWN", "stdin EOF - client closed connection");
                logDebug("STDIN_EOF", "readLine() returned null (client closed stdin)");
            }
        } finally {
            cleanup();
        }
    }

    private void processRequestNow(String request) {
        CompletableFuture<String> future = requestProcessor.processRequest(request);

        if (CLIENT_TIMEOUT_SECS > 0) {
            future = future.orTimeout(CLIENT_TIMEOUT_SECS, TimeUnit.SECONDS);
        }

        future.whenComplete((resp, err) -> {
            if (err != null) {
                logDebug("ERROR", "Request failed: " + err.getMessage());
                writeToStdout(createErrorResponse(request, err.getMessage()));
                return;
            }
            if (resp != null) writeToStdout(resp);
        });
    }

    // --- batching heuristics (coalescer uses only for list calls when enabled) ---
    private boolean isBatchableRequest(String requestJson) {
        try {
            var node = jsonMapper.readValue(requestJson, com.fasterxml.jackson.databind.JsonNode.class);
            String method = node.has("method") ? node.get("method").asText() : null;
            return "tools/list".equals(method) || "prompts/list".equals(method) || "resources/list".equals(method);
        } catch (Exception e) {
            return false;
        }
    }

    /** JSON-RPC one-line, newline-sanitized write */
    private synchronized void writeToStdout(String message) {
        if (isShuttingDown.get()) {
            logDebug("WARNING", "Attempted to write during shutdown");
            return;
        }
        try {
            // Ensure single line (MCP stdio framing)
            String oneLine = message.replace("\r", "").replace("\n", " ");
            stdoutWriter.println(oneLine);
            stdoutWriter.flush();
            logDebug("RESPONSE", prettyJson(oneLine));
        } catch (Exception e) {
            logDebug("ERROR", "Failed to write to stdout: " + e.getMessage());
            isShuttingDown.set(true);
            state = ConnectionState.DISCONNECTED;
            logDebug("STATE_TRANSITION", "Connection state -> DISCONNECTED (stdout failure)");
        }
    }

    String forwardToRestApiWithRetry(String mcpRequest, int maxRetries) throws Exception {
        Exception last = null;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return forwardToRestApi(mcpRequest);
            } catch (Exception e) {
                last = e;
                if (attempt < maxRetries) {
                    long backoff = (1L << (attempt - 1)) * 1000L;
                    long jitter = rnd.nextLong(200, 600);
                    long delayMs = backoff + jitter;
                    logDebug("RETRY", String.format("Attempt %d/%d failed: %s. Retrying in %dms...",
                            attempt, maxRetries, e.getMessage(), delayMs));
                    try { Thread.sleep(delayMs); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new Exception("Retry interrupted", ie); }
                } else {
                    logDebug("ERROR", String.format("All %d attempts failed. Final error: %s", maxRetries, e.getMessage()));
                }
            }
        }
        throw last;
    }

    private String forwardToRestApi(String mcpRequest) throws Exception {
        logDebug("REST_REQUEST", String.format("Forwarding to %s: %s", baseUrl, prettyJson(mcpRequest)));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECS))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-API-Key", apiKey)
                    .header("X-Session-Id", sessionId)
                    .header("User-Agent", "jvmxray-mcp-client/0.0.1")
                    .POST(HttpRequest.BodyPublishers.ofString(mcpRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int sc = response.statusCode();
            String body = response.body();
            if (sc == 200) {
                // Normalize to single-line JSON
                Object parsed = jsonMapper.readValue(body, Object.class);
                return jsonMapper.writeValueAsString(parsed);
            } else if (sc == 401) {
                throw new Exception("Authentication failed: Invalid API key");
            } else if (sc == 403) {
                throw new Exception("Access denied: API key lacks required permissions");
            } else {
                throw new Exception("REST call failed: " + sc + " - " + body);
            }
        } catch (java.net.ConnectException e) {
            throw new Exception("Cannot connect to JVMXRay server at " + baseUrl + ".");
        } catch (java.net.http.HttpConnectTimeoutException e) {
            throw new Exception("Connection timeout to JVMXRay server at " + baseUrl + ".");
        }
    }

    // ---- JSON-RPC helpers ----

    /** Initialize reply (echo client's version if present, otherwise fallback). */
    String handleInitializeRequest(McpSchema.JSONRPCRequest request) throws Exception {
        String protocolVersion = DEFAULT_PROTOCOL_FALLBACK;
        try {
            String paramsJson = jsonMapper.writeValueAsString(request.params());
            var node = jsonMapper.readValue(paramsJson, com.fasterxml.jackson.databind.JsonNode.class);
            if (node.has("protocolVersion")) protocolVersion = node.get("protocolVersion").asText();
        } catch (Exception ignored) {}

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", new LinkedHashMap<>());
        capabilities.put("prompts", new LinkedHashMap<>());
        capabilities.put("resources", new LinkedHashMap<>());

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", "jvmxray");
        serverInfo.put("version", "0.0.1");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);

        return createResultResponse(request.id(), result);
    }

    String createResultResponse(Object id, Map<String, Object> result) throws Exception {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("result", result);
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        return jsonMapper.writeValueAsString(resp);
    }

    String createMethodNotFoundError(Object id) throws Exception {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", -32601);
        error.put("message", "Method not found");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", error);
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        return jsonMapper.writeValueAsString(resp);
    }

    private String createErrorResponse(String originalRequest, String errorMessage) {
        try {
            McpSchema.JSONRPCRequest req = jsonMapper.readValue(originalRequest, McpSchema.JSONRPCRequest.class);
            McpSchema.JSONRPCResponse.JSONRPCError error =
                    new McpSchema.JSONRPCResponse.JSONRPCError(-32603, errorMessage, null);
            McpSchema.JSONRPCResponse errorResponse =
                    new McpSchema.JSONRPCResponse("2.0", req.id(), null, error);
            return jsonMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }

    String prettyJson(String json) {
        try {
            Object obj = jsonMapper.readValue(json, Object.class);
            return jsonMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }

    private void logDebugHeader() {
        if (debugWriter != null) {
            debugWriter.println();
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 JVMXRay MCP Client Started [%s]: %s%n",
                    sessionId, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
            debugWriter.println("================================================================================");
            debugWriter.printf("Server: %s%n", baseUrl);
            debugWriter.printf("Session ID: %s%n", sessionId);
            debugWriter.println();
        }
    }

    void logDebug(String type, String message) {
        if (debugWriter != null) {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
            String threadName = Thread.currentThread().getName();
            String icon = getIconForType(type);
            debugWriter.printf("%s [%s] [%s] [%s] %s:%n", icon, sessionId, timestamp, threadName, type);
            debugWriter.println(message);
            debugWriter.println();
            debugWriter.flush();
        }
    }

    private String getIconForType(String type) {
        switch (type) {
            case "REQUEST": return "📥";
            case "REST_REQUEST": case "REST_RESPONSE": case "RESPONSE": return "📤";
            case "ERROR": case "ERROR_RESPONSE": return "❌";
            case "CONNECT": return "🔌";
            case "SHUTDOWN": return "🛑";
            case "MCP": case "MCP_LOOP": return "🔄";
            case "NOTIFICATION": return "🔔";
            case "WAITING": return "⏳";
            case "WARNING": return "⚠️";
            case "RETRY": return "🔄";
            case "BATCH": return "📦";
            case "PROCESSING": return "⚙️";
            case "SUCCESS": return "✅";
            case "STATE_TRANSITION": return "🔀";
            case "STDIN_EOF": return "📞";
            default: return "📝";
        }
    }

    private void cleanup() {
        if (state == ConnectionState.DISCONNECTED) return; // idempotent
        state = ConnectionState.DISCONNECTED;
        logDebug("STATE_TRANSITION", "Connection state -> DISCONNECTED (cleanup)");

        if (requestProcessor != null) requestProcessor.shutdown();

        if (debugWriter != null) {
            debugWriter.println("================================================================================");
            debugWriter.printf("🔧 JVMXRay MCP Client Ended [%s]: %s%n",
                    sessionId, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
            debugWriter.println("================================================================================");
            debugWriter.close();
        }

        try { if (stdinReader != null) stdinReader.close(); } catch (IOException ignored) {}
        try { if (stdoutWriter != null) stdoutWriter.close(); } catch (Exception ignored) {}
    }

    // ---- tiny coalescer for list* when --batch-window-ms > 0 ----
    private static final class Coalescer {
        private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-coalescer");
            t.setDaemon(true);
            return t;
        });
        private final int windowMs;
        private final List<String> buffer = new ArrayList<>();
        private ScheduledFuture<?> task;

        Coalescer(int windowMs) { this.windowMs = windowMs; }

        synchronized void enqueue(String msg, java.util.function.Consumer<String> drainConsumer) {
            buffer.add(msg);
            if (task != null && !task.isDone()) task.cancel(false);
            task = timer.schedule(() -> {
                List<String> toDrain;
                synchronized (this) { toDrain = new ArrayList<>(buffer); buffer.clear(); }
                // Drain in arrival order
                for (String s : toDrain) drainConsumer.accept(s);
            }, windowMs, TimeUnit.MILLISECONDS);
        }
    }
}

/** Concurrent request processor with bounded queue + backpressure. */
class RequestProcessor {
    private final ThreadPoolExecutor executor;
    private final ConcurrentHashMap<Object, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private final JvmxrayMcpClient client;
    private volatile boolean shutdown = false;

    RequestProcessor(JvmxrayMcpClient client, int threadPoolSize, int queueCap) {
        this.client = client;
        this.executor = new ThreadPoolExecutor(
                threadPoolSize, threadPoolSize,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCap),
                r -> {
                    Thread t = new Thread(r, "mcp-request-processor");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // backpressure on producer
        );
        this.executor.allowCoreThreadTimeOut(true);
    }

    public CompletableFuture<String> processRequest(String requestJson) {
        if (shutdown) return CompletableFuture.failedFuture(new IllegalStateException("Processor shutdown"));
        try {
            var node = client.jsonMapper.readValue(requestJson, com.fasterxml.jackson.databind.JsonNode.class);
            String method = node.has("method") ? node.get("method").asText() : null;
            boolean hasId = node.has("id") && !node.get("id").isNull();

            CompletableFuture<String> cf = new CompletableFuture<>();

            // Notifications
            if (!hasId && method != null && method.startsWith("notifications/")) {
                client.logDebug("NOTIFICATION", "Processing notification: " + method);
                if ("notifications/initialized".equals(method)) client.logDebug("MCP", "Client ready");
                cf.complete(null);
                return cf;
            }

            if (!hasId) {
                String msg = String.format("Invalid message: method=%s has no id", method);
                client.logDebug("ERROR", msg);
                return CompletableFuture.failedFuture(new IllegalArgumentException(msg));
            }

            McpSchema.JSONRPCRequest request = client.jsonMapper.readValue(requestJson, McpSchema.JSONRPCRequest.class);
            Object requestId = request.id();
            pendingRequests.put(requestId, cf);

            executor.execute(() -> {
                try {
                    String resp = processRequestSync(requestJson, request);
                    var pending = pendingRequests.remove(requestId);
                    if (pending != null) pending.complete(resp);
                } catch (Exception e) {
                    var pending = pendingRequests.remove(requestId);
                    if (pending != null) pending.completeExceptionally(e);
                    client.logDebug("ERROR", "Request processing failed: " + e.getMessage());
                }
            });

            return cf;
        } catch (Exception e) {
            client.logDebug("ERROR", "Failed to parse request JSON: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private String processRequestSync(String requestJson, McpSchema.JSONRPCRequest request) throws Exception {
        String method = request.method();

        // Local quick paths
        if ("initialize".equals(method)) return client.handleInitializeRequest(request);
        if ("ping".equals(method)) return client.createResultResponse(request.id(), Map.of("ok", true));
        if ("logging/setLevel".equals(method)) return client.createResultResponse(request.id(), Map.of("ok", true));

        // Spec-friendly empties to keep clients happy
        if ("prompts/list".equals(method))   return client.createResultResponse(request.id(), Map.of("prompts", List.of()));
        if ("resources/list".equals(method)) return client.createResultResponse(request.id(), Map.of("resources", List.of()));

        // Graceful shutdown support
        if ("shutdown".equals(method)) {
            CompletableFuture.runAsync(() -> {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                client.logDebug("SHUTDOWN", "Client requested shutdown()");
                System.exit(0);
            });
            return client.createResultResponse(request.id(), Map.of("ok", true));
        }

        // Everything else -> REST
        return client.forwardToRestApiWithRetry(requestJson, 3);
    }

    public CompletableFuture<List<String>> processBatch(List<String> requests) {
        List<CompletableFuture<String>> futures = requests.stream().map(this::processRequest).collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public void shutdown() {
        shutdown = true;
        pendingRequests.values().forEach(f -> f.completeExceptionally(new IllegalStateException("Processor shutdown")));
        pendingRequests.clear();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
