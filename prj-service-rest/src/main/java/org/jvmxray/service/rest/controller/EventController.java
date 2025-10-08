package org.jvmxray.service.rest.controller;

import org.jvmxray.service.rest.config.RestServiceConfig;
import org.jvmxray.service.rest.model.Event;
import org.jvmxray.service.rest.model.PaginatedResponse;
import org.jvmxray.service.rest.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * REST controller for event endpoints.
 *
 * @author Milton Smith
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private static final Logger logger = Logger.getLogger(EventController.class.getName());

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RestServiceConfig config;

    /**
     * Get a single event by ID.
     *
     * @param eventId The event ID
     * @return The event or 404 if not found
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable String eventId) {
        long startTime = System.currentTimeMillis();

        Event event = eventRepository.findById(eventId);

        if (event == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Event not found: " + eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        logger.fine("Retrieved event: " + eventId + " in " +
                   (System.currentTimeMillis() - startTime) + "ms");

        return ResponseEntity.ok(event);
    }

    /**
     * Query events with filters and pagination.
     *
     * @param namespace Namespace pattern (supports wildcards with *)
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @param aid Application ID
     * @param cid Correlation ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param estimateOnly If true, return only the count without data
     * @return Paginated response with events
     */
    @GetMapping
    public ResponseEntity<?> queryEvents(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String aid,
            @RequestParam(required = false) String cid,
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
        long totalElements = eventRepository.countEvents(namespace, startTime, endTime, aid, cid);

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

        // Query events
        List<Event> events = eventRepository.queryEvents(namespace, startTime, endTime,
                                                         aid, cid, offset, size);

        // Create paginated response
        PaginatedResponse<Event> response = new PaginatedResponse<>();
        response.setData(events);
        response.setPagination(new PaginatedResponse.PaginationInfo(page, size, totalElements));

        long queryTime = System.currentTimeMillis() - queryStartTime;
        response.setMetadata(new PaginatedResponse.ResponseMetadata(queryTime, false));

        logger.info("Queried events - filters: namespace=" + namespace +
                   ", startTime=" + startTime + ", endTime=" + endTime +
                   ", aid=" + aid + ", cid=" + cid +
                   " - returned " + events.size() + " of " + totalElements +
                   " total in " + queryTime + "ms");

        return ResponseEntity.ok(response);
    }
}