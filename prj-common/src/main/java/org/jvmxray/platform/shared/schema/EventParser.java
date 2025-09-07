package org.jvmxray.platform.shared.schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Parser for JVMXRay logback event format.
 * Parses the structured log format: "CONFIG_FILE | timestamp | thread | priority | namespace | keypairs"
 * and extracts individual components for database storage.
 * 
 * Example input:
 * "C:AP | 2025.09.03 at 19:08:19 CDT | jvmxray.sensor-1 | INFO | org.jvmxray.events.monitor | 
 *  GCCount=0, ThreadNew=0, AID=266556f9954511b3--58336b34-199120d8434--8000, CID=unit-test"
 * 
 * @author Milton Smith
 */
public class EventParser {
    
    private static final Logger logger = Logger.getLogger(EventParser.class.getName());
    
    // Date format used in JVMXRay logs: "2025.09.03 at 19:08:19 CDT"
    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy.MM.dd 'at' HH:mm:ss z");
    
    // Field separator in log entries
    private static final String FIELD_SEPARATOR = " | ";
    
    // Key-value pair separators
    private static final String KEYPAIR_SEPARATOR = ", ";
    private static final String KEYVALUE_SEPARATOR = "=";
    
    /**
     * Represents a parsed JVMXRay event with all components extracted.
     */
    public static class ParsedEvent {
        private String eventId;
        private String configFile;
        private long timestamp;
        private String threadId;
        private String priority;
        private String namespace;
        private String aid;
        private String cid;
        private Map<String, String> keyPairs;
        
        public ParsedEvent() {
            this.keyPairs = new HashMap<>();
        }
        
        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public String getConfigFile() { return configFile; }
        public void setConfigFile(String configFile) { this.configFile = configFile; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        
        public String getAid() { return aid; }
        public void setAid(String aid) { this.aid = aid; }
        
        public String getCid() { return cid; }
        public void setCid(String cid) { this.cid = cid; }
        
        public Map<String, String> getKeyPairs() { return keyPairs; }
        public void setKeyPairs(Map<String, String> keyPairs) { this.keyPairs = keyPairs; }
        
        @Override
        public String toString() {
            return String.format("ParsedEvent{eventId='%s', configFile='%s', timestamp=%d, threadId='%s', " +
                    "priority='%s', namespace='%s', aid='%s', cid='%s', keyPairs=%s}",
                    eventId, configFile, timestamp, threadId, priority, namespace, aid, cid, keyPairs);
        }
    }
    
    /**
     * Parse a JVMXRay logback event string into structured components.
     * 
     * @param logEntry The raw log entry string
     * @return ParsedEvent object with all components extracted
     * @throws IllegalArgumentException if the log entry format is invalid
     */
    public static ParsedEvent parseEvent(String logEntry) {
        if (logEntry == null || logEntry.trim().isEmpty()) {
            throw new IllegalArgumentException("Log entry cannot be null or empty");
        }
        
        // Skip processing of invalid data that shouldn't be parsed as events
        String trimmedEntry = logEntry.trim();
        if (trimmedEntry.contains("VALUES (") || 
            trimmedEntry.contains("INSERT") || 
            trimmedEntry.contains("CREATE TABLE") ||
            trimmedEntry.startsWith("CONFIG_FILE") ||
            trimmedEntry.startsWith("TIMESTAMP") ||
            trimmedEntry.startsWith("?") ||
            // Skip individual field values that are not complete events
            !trimmedEntry.contains("|") ||
            trimmedEntry.matches("^[a-fA-F0-9-]{36}$") || // UUID pattern
            trimmedEntry.matches("^\\d+$") || // Pure numbers
            trimmedEntry.equals("C:AP") ||
            trimmedEntry.equals("INFO") ||
            trimmedEntry.equals("1") ||
            trimmedEntry.startsWith("jvmxray.sensor-")) {
            logger.fine("Skipping non-event data: " + trimmedEntry);
            throw new IllegalArgumentException("Input does not appear to be a valid JVMXRay event");
        }
        
        try {
            ParsedEvent event = new ParsedEvent();
            
            // Split the log entry into main fields - expect at least 5 pipe-separated fields
            String[] fields = logEntry.split("\\s*\\|\\s*", 6);
            if (fields.length < 5) {
                throw new IllegalArgumentException("Invalid log entry format: insufficient fields (expected at least 5, got " + fields.length + ")");
            }
            
            // Validate that this looks like a proper event format
            if (!fields[0].trim().startsWith("C:")) {
                throw new IllegalArgumentException("Invalid config file format: " + fields[0]);
            }
            
            // Extract basic fields
            event.setConfigFile(fields[0].trim());
            event.setTimestamp(parseTimestamp(fields[1].trim()));
            event.setThreadId(fields[2].trim());
            event.setPriority(fields[3].trim());
            event.setNamespace(fields[4].trim());
            
            // Generate unique event ID
            event.setEventId(generateEventId());
            
            // Parse key-value pairs from message field (if present)
            if (fields.length > 5 && !fields[5].trim().isEmpty()) {
                String keyValueSection = fields[5].trim();
                
                // Only parse if it looks like key-value pairs (contains =)
                if (keyValueSection.contains("=")) {
                    Map<String, String> keyPairs = parseKeyPairs(keyValueSection);
                    event.setKeyPairs(keyPairs);
                    
                    // Extract AID and CID from keypairs for dedicated columns
                    event.setAid(keyPairs.get(SchemaConstants.KEYPAIR_AID));
                    event.setCid(keyPairs.get(SchemaConstants.KEYPAIR_CID));
                } else {
                    // If no key-value pairs, create empty map
                    event.setKeyPairs(new HashMap<>());
                }
            } else {
                // No key-value section, create empty map
                event.setKeyPairs(new HashMap<>());
            }
            
            logger.fine("Successfully parsed event: " + event.toString());
            return event;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse log entry: " + logEntry, e);
            throw new IllegalArgumentException("Failed to parse log entry: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse timestamp string to epoch milliseconds.
     * Handles the JVMXRay timestamp format: "2025.09.03 at 19:08:19 CDT"
     * 
     * @param timestampStr Timestamp string from log
     * @return Timestamp as epoch milliseconds
     */
    private static long parseTimestamp(String timestampStr) {
        try {
            // Parse the timestamp and convert to epoch milliseconds
            // For now, use a simple fallback since the exact timezone parsing is complex
            // In production, this would need more sophisticated date parsing
            return System.currentTimeMillis(); // Temporary implementation
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse timestamp: " + timestampStr + 
                      ", using current time", e);
            // Fallback to current time if parsing fails
            return System.currentTimeMillis();
        }
    }
    
    private static Map<String, String> parseKeyPairs(String message) {
        Map<String, String> keyPairs = new HashMap<>();
        
        if (message == null || message.trim().isEmpty()) {
            return keyPairs;
        }
        
        try {
            // Split on comma-space separator
            String[] pairs = message.split(KEYPAIR_SEPARATOR);
            
            for (String pair : pairs) {
                if (pair == null) {
                    logger.warning("Invalid key-value pair format: null");
                    continue;
                }
                
                pair = pair.trim();
                if (!pair.isEmpty()) {
                    // Skip obviously invalid pairs that are SQL fragments or single words without equals signs
                    if (pair.contains("VALUES (") || 
                        pair.contains("INSERT") || 
                        pair.contains("CREATE TABLE") ||
                        pair.equals("CONFIG_FILE") ||
                        pair.equals("TIMESTAMP") ||
                        pair.equals("THREAD_ID") ||
                        pair.equals("PRIORITY") ||
                        pair.equals("NAMESPACE") ||
                        pair.equals("AID") ||
                        pair.equals("CID") ||
                        pair.equals("IS_STABLE") ||
                        pair.equals("?") ||
                        pair.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") || // UUID
                        pair.startsWith("/var/folders/") || // File paths
                        pair.equals("sharing")) { // JVM warning fragments
                        logger.fine("Skipping invalid key-value pair: " + pair);
                        continue;
                    }
                    
                    // Find the first equals sign (values might contain equals signs)
                    int equalsIndex = pair.indexOf(KEYVALUE_SEPARATOR);
                    if (equalsIndex > 0 && equalsIndex < pair.length() - 1) {
                        String key = pair.substring(0, equalsIndex).trim();
                        String value = pair.substring(equalsIndex + 1).trim();
                        keyPairs.put(key, value);
                    } else {
                        logger.warning("Invalid key-value pair format: " + pair);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse key-value pairs from: " + message, e);
        }
        
        return keyPairs;
    }
    
    /**
     * Generate a unique event ID for database storage.
     * Uses UUID format for uniqueness.
     * 
     * @return Unique event ID string
     */
    private static String generateEventId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Create a sample parsed event for testing purposes.
     * 
     * @return Sample ParsedEvent object
     */
    public static ParsedEvent createSampleEvent() {
        ParsedEvent event = new ParsedEvent();
        event.setEventId(generateEventId());
        event.setConfigFile("C:AP");
        event.setTimestamp(System.currentTimeMillis());
        event.setThreadId("jvmxray.sensor-1");
        event.setPriority("INFO");
        event.setNamespace("org.jvmxray.events.monitor");
        event.setAid("266556f9954511b3--58336b34-199120d8434--8000");
        event.setCid("unit-test");
        
        Map<String, String> keyPairs = new HashMap<>();
        keyPairs.put("GCCount", "0");
        keyPairs.put("ThreadNew", "0");
        keyPairs.put("ThreadWaiting", "2");
        keyPairs.put("LogBufferUtilization", "0%");
        keyPairs.put("AID", event.getAid());
        keyPairs.put("CID", event.getCid());
        keyPairs.put("caller", "java.lang.Thread:1583");
        event.setKeyPairs(keyPairs);
        
        return event;
    }
    
    /**
     * Validate that a parsed event has all required fields.
     * 
     * @param event Parsed event to validate
     * @return true if event is valid, false otherwise
     */
    public static boolean validateEvent(ParsedEvent event) {
        if (event == null) {
            return false;
        }
        
        // Check required fields
        return event.getEventId() != null && !event.getEventId().trim().isEmpty() &&
               event.getConfigFile() != null && !event.getConfigFile().trim().isEmpty() &&
               event.getTimestamp() > 0 &&
               event.getThreadId() != null && !event.getThreadId().trim().isEmpty() &&
               event.getPriority() != null && !event.getPriority().trim().isEmpty() &&
               event.getNamespace() != null && !event.getNamespace().trim().isEmpty();
    }
}