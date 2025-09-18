package org.jvmxray.service.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing a security event.
 *
 * @author Milton Smith
 */
public class Event {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("config_file")
    private String configFile;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("thread_id")
    private String threadId;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("aid")
    private String aid;

    @JsonProperty("cid")
    private String cid;

    @JsonProperty("is_stable")
    private Boolean isStable;

    // Constructors
    public Event() {
    }

    public Event(String eventId, String configFile, Long timestamp, String threadId,
                 String priority, String namespace, String aid, String cid, Boolean isStable) {
        this.eventId = eventId;
        this.configFile = configFile;
        this.timestamp = timestamp;
        this.threadId = threadId;
        this.priority = priority;
        this.namespace = namespace;
        this.aid = aid;
        this.cid = cid;
        this.isStable = isStable;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public Boolean getIsStable() {
        return isStable;
    }

    public void setIsStable(Boolean isStable) {
        this.isStable = isStable;
    }
}