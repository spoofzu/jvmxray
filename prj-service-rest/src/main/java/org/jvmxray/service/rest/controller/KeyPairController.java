package org.jvmxray.service.rest.controller;

import org.jvmxray.service.rest.config.RestServiceConfig;
import org.jvmxray.service.rest.model.KeyPair;
import org.jvmxray.service.rest.model.PaginatedResponse;
import org.jvmxray.service.rest.repository.KeyPairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * REST controller for key-value pair endpoints.
 *
 * @author Milton Smith
 */
@RestController
@RequestMapping("/api/v1/keypairs")
public class KeyPairController {

    private static final Logger logger = Logger.getLogger(KeyPairController.class.getName());

    @Autowired
    private KeyPairRepository keyPairRepository;

    @Autowired
    private RestServiceConfig config;

    /**
     * Get all key-value pairs for a specific event.
     *
     * @param eventId The event ID
     * @return List of key-value pairs
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getKeyPairsByEventId(@PathVariable String eventId) {
        long startTime = System.currentTimeMillis();

        List<KeyPair> keyPairs = keyPairRepository.findByEventId(eventId);

        if (keyPairs.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "No key pairs found for event: " + eventId);
            return ResponseEntity.ok(error);
        }

        logger.fine("Retrieved " + keyPairs.size() + " key pairs for event: " + eventId +
                   " in " + (System.currentTimeMillis() - startTime) + "ms");

        return ResponseEntity.ok(keyPairs);
    }

    /**
     * Query key-value pairs with filters and pagination.
     *
     * @param key Key filter (exact match or pattern with *)
     * @param value Value filter (exact match or pattern with *)
     * @param eventId Optional event ID filter
     * @param page Page number (0-based)
     * @param size Page size
     * @param estimateOnly If true, return only the count without data
     * @return Paginated response with key-value pairs
     */
    @GetMapping
    public ResponseEntity<?> queryKeyPairs(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String eventId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size,
            @RequestParam(defaultValue = "false") boolean estimateOnly) {

        long queryStartTime = System.currentTimeMillis();

        // Enforce maximum page size
        if (size > config.getMaxPageSize()) {
            size = config.getMaxPageSize();
        }

        // Calculate offset
        int offset = page * size;

        // Get total count
        long totalElements = keyPairRepository.countKeyPairs(key, value, eventId);

        // Check if result size exceeds maximum
        if (totalElements > config.getMaxResultSize() && !estimateOnly) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Query would return too many results");
            error.put("total_elements", totalElements);
            error.put("max_allowed", config.getMaxResultSize());
            error.put("suggestion", "Use more specific filters or set estimateOnly=true");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // If only estimate is requested, return count
        if (estimateOnly) {
            Map<String, Object> estimate = new HashMap<>();
            estimate.put("total_elements", totalElements);
            estimate.put("total_pages", (int) Math.ceil((double) totalElements / size));
            return ResponseEntity.ok(estimate);
        }

        // Query key pairs
        List<KeyPair> keyPairs = keyPairRepository.queryKeyPairs(key, value, eventId,
                                                                 offset, size);

        // Create paginated response
        PaginatedResponse<KeyPair> response = new PaginatedResponse<>();
        response.setData(keyPairs);
        response.setPagination(new PaginatedResponse.PaginationInfo(page, size, totalElements));

        long queryTime = System.currentTimeMillis() - queryStartTime;
        response.setMetadata(new PaginatedResponse.ResponseMetadata(queryTime, false));

        logger.info("Queried key pairs - filters: key=" + key +
                   ", value=" + value + ", eventId=" + eventId +
                   " - returned " + keyPairs.size() + " of " + totalElements +
                   " total in " + queryTime + "ms");

        return ResponseEntity.ok(response);
    }
}