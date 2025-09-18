package org.jvmxray.service.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jvmxray.service.rest.config.RestServiceConfig;
import org.jvmxray.service.rest.model.Event;
import org.jvmxray.service.rest.model.KeyPair;
import org.jvmxray.service.rest.model.PaginatedResponse;
import org.jvmxray.service.rest.repository.EventRepository;
import org.jvmxray.service.rest.repository.KeyPairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MCP (Model Context Protocol) controller for bridging between Claude Desktop and JVMXRay REST API.
 * Handles JSON-RPC 2.0 MCP protocol messages and routes them to appropriate JVMXRay services.
 *
 * @author Milton Smith
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private static final Logger logger = Logger.getLogger(McpController.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private KeyPairRepository keyPairRepository;

    @Autowired
    private RestServiceConfig config;

    /**
     * Health check endpoint for MCP service.
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "JVMXRay MCP Bridge");
        return ResponseEntity.ok(health);
    }

    /**
     * Handle MCP protocol messages from Claude Desktop via MCP bridge client.
     *
     * @param mcpRequest The JSON-RPC 2.0 MCP message
     * @return JSON-RPC 2.0 response
     */
    @PostMapping
    public ResponseEntity<?> handleMcpMessage(@RequestBody JsonNode mcpRequest) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate JSON-RPC 2.0 format
            if (!mcpRequest.has("jsonrpc") || !"2.0".equals(mcpRequest.get("jsonrpc").asText())) {
                return createErrorResponse(mcpRequest, -32600, "Invalid Request: Missing or invalid jsonrpc field");
            }

            if (!mcpRequest.has("method")) {
                return createErrorResponse(mcpRequest, -32600, "Invalid Request: Missing method field");
            }

            String method = mcpRequest.get("method").asText();
            JsonNode params = mcpRequest.get("params");
            JsonNode id = mcpRequest.get("id");

            logger.info("Processing MCP request: " + method);

            // Route to appropriate handler based on method
            ObjectNode response = createSuccessResponse(id);

            switch (method) {
                case "initialize":
                    response.set("result", handleInitialize(params));
                    break;
                case "tools/list":
                    response.set("result", handleToolsList());
                    break;
                case "tools/call":
                    response.set("result", handleToolsCall(params));
                    break;
                case "prompts/list":
                    response.set("result", handlePromptsList());
                    break;
                case "resources/list":
                    response.set("result", handleResourcesList());
                    break;
                case "resources/read":
                    response.set("result", handleResourcesRead(params));
                    break;
                default:
                    return createErrorResponse(mcpRequest, -32601, "Method not found: " + method);
            }

            // Finalize response with proper field order (result, jsonrpc, id)
            response = finalizeResponse(response, id);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("MCP request " + method + " completed in " + duration + "ms");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing MCP request", e);
            return createErrorResponse(mcpRequest, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle MCP initialize request.
     */
    private JsonNode handleInitialize(JsonNode params) {
        // Extract client's protocol version for negotiation
        String protocolVersion = "2025-06-18";
        if (params != null && params.has("protocolVersion")) {
            protocolVersion = params.get("protocolVersion").asText();
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", protocolVersion);

        // MCP capabilities must be objects, not booleans
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.set("tools", objectMapper.createObjectNode());
        capabilities.set("resources", objectMapper.createObjectNode());
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "JVMXRay Security Monitor");
        serverInfo.put("version", "0.0.1");
        result.set("serverInfo", serverInfo);

        return result;
    }

    /**
     * Handle tools/list - return available JVMXRay tools for Claude.
     */
    private JsonNode handleToolsList() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();

        // Query Events Tool
        ObjectNode queryEventsTool = objectMapper.createObjectNode();
        queryEventsTool.put("name", "query_events");
        queryEventsTool.put("description", "Search and filter JVMXRay security events by namespace, time range, and IDs");

        ObjectNode eventsSchema = objectMapper.createObjectNode();
        eventsSchema.put("type", "object");
        ObjectNode eventsProps = objectMapper.createObjectNode();

        ObjectNode namespaceParam = objectMapper.createObjectNode();
        namespaceParam.put("type", "string");
        namespaceParam.put("description", "Namespace pattern (supports wildcards with *)");
        eventsProps.set("namespace", namespaceParam);

        ObjectNode startTimeParam = objectMapper.createObjectNode();
        startTimeParam.put("type", "integer");
        startTimeParam.put("description", "Start timestamp (milliseconds since epoch)");
        eventsProps.set("startTime", startTimeParam);

        ObjectNode endTimeParam = objectMapper.createObjectNode();
        endTimeParam.put("type", "integer");
        endTimeParam.put("description", "End timestamp (milliseconds since epoch)");
        eventsProps.set("endTime", endTimeParam);

        ObjectNode aidParam = objectMapper.createObjectNode();
        aidParam.put("type", "string");
        aidParam.put("description", "Application ID filter");
        eventsProps.set("aid", aidParam);

        ObjectNode cidParam = objectMapper.createObjectNode();
        cidParam.put("type", "string");
        cidParam.put("description", "Correlation ID filter");
        eventsProps.set("cid", cidParam);

        ObjectNode limitParam = objectMapper.createObjectNode();
        limitParam.put("type", "integer");
        limitParam.put("description", "Maximum number of results to return (default: 100, max: 1000)");
        limitParam.put("default", 100);
        eventsProps.set("limit", limitParam);

        eventsSchema.set("properties", eventsProps);
        queryEventsTool.set("inputSchema", eventsSchema);
        tools.add(queryEventsTool);

        // Query KeyPairs Tool
        ObjectNode queryKeyPairsTool = objectMapper.createObjectNode();
        queryKeyPairsTool.put("name", "query_keypairs");
        queryKeyPairsTool.put("description", "Search key-value pairs from JVMXRay events");

        ObjectNode keypairsSchema = objectMapper.createObjectNode();
        keypairsSchema.put("type", "object");
        ObjectNode keypairsProps = objectMapper.createObjectNode();

        ObjectNode keyParam = objectMapper.createObjectNode();
        keyParam.put("type", "string");
        keyParam.put("description", "Key filter (supports wildcards with *)");
        keypairsProps.set("key", keyParam);

        ObjectNode valueParam = objectMapper.createObjectNode();
        valueParam.put("type", "string");
        valueParam.put("description", "Value filter (supports wildcards with *)");
        keypairsProps.set("value", valueParam);

        ObjectNode eventIdParam = objectMapper.createObjectNode();
        eventIdParam.put("type", "string");
        eventIdParam.put("description", "Filter by specific event ID");
        keypairsProps.set("eventId", eventIdParam);

        ObjectNode limitParam2 = objectMapper.createObjectNode();
        limitParam2.put("type", "integer");
        limitParam2.put("description", "Maximum number of results to return (default: 100, max: 1000)");
        limitParam2.put("default", 100);
        keypairsProps.set("limit", limitParam2);

        keypairsSchema.set("properties", keypairsProps);
        queryKeyPairsTool.set("inputSchema", keypairsSchema);
        tools.add(queryKeyPairsTool);

        // Get Event Details Tool
        ObjectNode getEventTool = objectMapper.createObjectNode();
        getEventTool.put("name", "get_event_details");
        getEventTool.put("description", "Get detailed information about a specific JVMXRay security event including its key-value pairs");

        ObjectNode eventSchema = objectMapper.createObjectNode();
        eventSchema.put("type", "object");
        ObjectNode eventProps = objectMapper.createObjectNode();

        ObjectNode eventIdParam2 = objectMapper.createObjectNode();
        eventIdParam2.put("type", "string");
        eventIdParam2.put("description", "The event ID to retrieve");
        eventProps.set("eventId", eventIdParam2);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("eventId");
        eventSchema.set("required", required);

        eventSchema.set("properties", eventProps);
        getEventTool.set("inputSchema", eventSchema);
        tools.add(getEventTool);

        result.set("tools", tools);
        return result;
    }

    /**
     * Handle prompts/list - return available prompts (currently none).
     */
    private JsonNode handlePromptsList() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode prompts = objectMapper.createArrayNode();
        // Currently no prompts available
        result.set("prompts", prompts);
        return result;
    }

    /**
     * Handle tools/call - execute a tool with given parameters.
     */
    private JsonNode handleToolsCall(JsonNode params) throws Exception {
        if (!params.has("name")) {
            throw new IllegalArgumentException("Tool name is required");
        }

        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        switch (toolName) {
            case "query_events":
                return handleQueryEvents(arguments);
            case "query_keypairs":
                return handleQueryKeyPairs(arguments);
            case "get_event_details":
                return handleGetEventDetails(arguments);
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    }

    private JsonNode handleQueryEvents(JsonNode arguments) throws Exception {
        String namespace = arguments.has("namespace") ? arguments.get("namespace").asText() : null;
        Long startTime = arguments.has("startTime") ? arguments.get("startTime").asLong() : null;
        Long endTime = arguments.has("endTime") ? arguments.get("endTime").asLong() : null;
        String aid = arguments.has("aid") ? arguments.get("aid").asText() : null;
        String cid = arguments.has("cid") ? arguments.get("cid").asText() : null;
        int limit = arguments.has("limit") ? arguments.get("limit").asInt() : 100;

        // Enforce limit constraints
        if (limit > config.getMaxPageSize()) {
            limit = config.getMaxPageSize();
        }

        PaginatedResponse<Event> response = eventRepository.findEvents(
            namespace, startTime, endTime, aid, cid, 0, limit, false
        );

        ObjectNode toolData = objectMapper.createObjectNode();
        toolData.set("events", objectMapper.valueToTree(response.getData()));
        toolData.put("total_found", response.getPagination().getTotalElements());
        toolData.put("limit_applied", limit);

        return wrapToolResponse(toolData);
    }

    private JsonNode handleQueryKeyPairs(JsonNode arguments) throws Exception {
        String key = arguments.has("key") ? arguments.get("key").asText() : null;
        String value = arguments.has("value") ? arguments.get("value").asText() : null;
        String eventId = arguments.has("eventId") ? arguments.get("eventId").asText() : null;
        int limit = arguments.has("limit") ? arguments.get("limit").asInt() : 100;

        // Enforce limit constraints
        if (limit > config.getMaxPageSize()) {
            limit = config.getMaxPageSize();
        }

        PaginatedResponse<KeyPair> response = keyPairRepository.findKeyPairs(
            key, value, eventId, 0, limit, false
        );

        ObjectNode toolData = objectMapper.createObjectNode();
        toolData.set("keypairs", objectMapper.valueToTree(response.getData()));
        toolData.put("total_found", response.getPagination().getTotalElements());
        toolData.put("limit_applied", limit);

        return wrapToolResponse(toolData);
    }

    private JsonNode handleGetEventDetails(JsonNode arguments) throws Exception {
        if (!arguments.has("eventId")) {
            throw new IllegalArgumentException("eventId is required");
        }

        String eventId = arguments.get("eventId").asText();

        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        // Get key-value pairs for this event
        List<KeyPair> keyPairs = keyPairRepository.findByEventId(eventId);

        ObjectNode toolData = objectMapper.createObjectNode();
        toolData.set("event", objectMapper.valueToTree(event));
        toolData.set("keypairs", objectMapper.valueToTree(keyPairs));

        return wrapToolResponse(toolData);
    }

    /**
     * Handle resources/list - list available data resources.
     */
    private JsonNode handleResourcesList() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode resources = objectMapper.createArrayNode();

        ObjectNode eventsResource = objectMapper.createObjectNode();
        eventsResource.put("uri", "jvmxray://events");
        eventsResource.put("name", "Security Events");
        eventsResource.put("description", "JVMXRay security monitoring events");
        eventsResource.put("mimeType", "application/json");
        resources.add(eventsResource);

        ObjectNode keypairsResource = objectMapper.createObjectNode();
        keypairsResource.put("uri", "jvmxray://keypairs");
        keypairsResource.put("name", "Event Key-Value Pairs");
        keypairsResource.put("description", "Key-value pairs extracted from security events");
        keypairsResource.put("mimeType", "application/json");
        resources.add(keypairsResource);

        result.set("resources", resources);
        return result;
    }

    /**
     * Handle resources/read - read a specific resource.
     */
    private JsonNode handleResourcesRead(JsonNode params) {
        if (!params.has("uri")) {
            throw new IllegalArgumentException("Resource URI is required");
        }

        String uri = params.get("uri").asText();
        ObjectNode result = objectMapper.createObjectNode();

        switch (uri) {
            case "jvmxray://events":
                // Return recent events as a sample
                PaginatedResponse<Event> events = eventRepository.findEvents(
                    null, null, null, null, null, 0, 50, false
                );
                result.set("contents", objectMapper.valueToTree(events.getData()));
                result.put("mimeType", "application/json");
                break;
            case "jvmxray://keypairs":
                // Return recent keypairs as a sample
                PaginatedResponse<KeyPair> keypairs = keyPairRepository.findKeyPairs(
                    null, null, null, 0, 50, false
                );
                result.set("contents", objectMapper.valueToTree(keypairs.getData()));
                result.put("mimeType", "application/json");
                break;
            default:
                throw new IllegalArgumentException("Unknown resource URI: " + uri);
        }

        return result;
    }

    /**
     * Wrap a tool response in MCP-compliant content array format.
     * Tool call results must be wrapped in content array according to MCP specification.
     */
    private JsonNode wrapToolResponse(Object toolData) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode contentItem = objectMapper.createObjectNode();
        contentItem.put("type", "text");
        contentItem.put("text", objectMapper.writeValueAsString(toolData));
        content.add(contentItem);
        result.set("content", content);
        return result;
    }

    /**
     * Create a successful JSON-RPC 2.0 response (using Chrome's field order).
     */
    private ObjectNode createSuccessResponse(JsonNode id) {
        ObjectNode response = objectMapper.createObjectNode();
        // Note: result will be added first by caller, then we add jsonrpc and id
        return response;
    }

    /**
     * Finalize response with proper field order: result, jsonrpc, id (like Chrome).
     */
    private ObjectNode finalizeResponse(ObjectNode response, JsonNode id) {
        // Add jsonrpc and id after result has been set
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        return response;
    }

    /**
     * Create an error JSON-RPC 2.0 response.
     */
    private ResponseEntity<?> createErrorResponse(JsonNode originalRequest, int code, String message) {
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("jsonrpc", "2.0");

        if (originalRequest != null && originalRequest.has("id")) {
            errorResponse.set("id", originalRequest.get("id"));
        } else {
            errorResponse.set("id", null);
        }

        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        errorResponse.set("error", error);

        return ResponseEntity.ok(errorResponse);
    }
}