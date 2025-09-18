package org.jvmxray.service.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing a key-value pair associated with an event.
 *
 * @author Milton Smith
 */
public class KeyPair {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;

    // Constructors
    public KeyPair() {
    }

    public KeyPair(String eventId, String key, String value) {
        this.eventId = eventId;
        this.key = key;
        this.value = value;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}