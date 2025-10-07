package org.jvmxray.service.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks temporal information for libraries including first seen,
 * last seen, and removal timestamps organized by Application ID (AID).
 *
 * This class maintains in-memory tracking of library lifecycle events
 * to support the enrichment process and detect supply chain changes.
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public class LibraryTemporalTracker {

    private static final Logger logger = LoggerFactory.getLogger(LibraryTemporalTracker.class);

    // AID -> LibraryID -> TemporalInfo
    private final Map<String, Map<String, TemporalInfo>> aidLibraryTracking;

    /**
     * Constructs a new LibraryTemporalTracker.
     */
    public LibraryTemporalTracker() {
        this.aidLibraryTracking = new ConcurrentHashMap<>();
    }

    /**
     * Updates the timestamp for a library within a specific AID context.
     *
     * @param aid Application ID
     * @param libraryId Library identifier (typically SHA-256 hash)
     * @param timestamp Current timestamp
     */
    public void updateLibraryTimestamp(String aid, String libraryId, long timestamp) {
        if (aid == null || libraryId == null) {
            logger.warn("Cannot update timestamp: aid={}, libraryId={}", aid, libraryId);
            return;
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.computeIfAbsent(aid,
            k -> new ConcurrentHashMap<>());

        TemporalInfo info = libraryMap.get(libraryId);
        if (info == null) {
            // First time seeing this library for this AID
            info = new TemporalInfo(timestamp, timestamp);
            libraryMap.put(libraryId, info);
            logger.debug("First seen: AID={}, Library={}, Timestamp={}",
                       aid, libraryId.substring(0, 8) + "...", timestamp);
        } else {
            // Update last seen timestamp
            info.updateLastSeen(timestamp);
            logger.debug("Updated last seen: AID={}, Library={}, Timestamp={}",
                       aid, libraryId.substring(0, 8) + "...", timestamp);
        }
    }

    /**
     * Checks if this is the first time seeing a library for a given AID.
     *
     * @param aid Application ID
     * @param libraryId Library identifier
     * @return true if this is first occurrence, false otherwise
     */
    public boolean isFirstSeen(String aid, String libraryId) {
        if (aid == null || libraryId == null) {
            return false;
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        return libraryMap == null || !libraryMap.containsKey(libraryId);
    }

    /**
     * Gets the first seen timestamp for a library within an AID.
     *
     * @param aid Application ID
     * @param libraryId Library identifier
     * @return First seen timestamp, or -1 if not found
     */
    public long getFirstSeen(String aid, String libraryId) {
        if (aid == null || libraryId == null) {
            return -1;
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        if (libraryMap == null) {
            return -1;
        }

        TemporalInfo info = libraryMap.get(libraryId);
        return info != null ? info.firstSeen : -1;
    }

    /**
     * Gets the last seen timestamp for a library within an AID.
     *
     * @param aid Application ID
     * @param libraryId Library identifier
     * @return Last seen timestamp, or -1 if not found
     */
    public long getLastSeen(String aid, String libraryId) {
        if (aid == null || libraryId == null) {
            return -1;
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        if (libraryMap == null) {
            return -1;
        }

        TemporalInfo info = libraryMap.get(libraryId);
        return info != null ? info.lastSeen : -1;
    }

    /**
     * Marks a library as removed for a specific AID.
     *
     * @param aid Application ID
     * @param libraryId Library identifier
     * @param removedTimestamp When the library was removed
     */
    public void markLibraryRemoved(String aid, String libraryId, long removedTimestamp) {
        if (aid == null || libraryId == null) {
            logger.warn("Cannot mark as removed: aid={}, libraryId={}", aid, libraryId);
            return;
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        if (libraryMap == null) {
            logger.warn("No tracking data found for AID={}, cannot mark library as removed", aid);
            return;
        }

        TemporalInfo info = libraryMap.get(libraryId);
        if (info != null) {
            info.markRemoved(removedTimestamp);
            logger.info("Marked library as removed: AID={}, Library={}, RemovedAt={}",
                      aid, libraryId.substring(0, 8) + "...", removedTimestamp);
        }
    }

    /**
     * Gets all active (non-removed) libraries for an AID.
     *
     * @param aid Application ID
     * @return Map of libraryId -> TemporalInfo for active libraries
     */
    public Map<String, TemporalInfo> getActiveLibraries(String aid) {
        if (aid == null) {
            return new HashMap<>();
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        if (libraryMap == null) {
            return new HashMap<>();
        }

        Map<String, TemporalInfo> activeLibraries = new HashMap<>();
        for (Map.Entry<String, TemporalInfo> entry : libraryMap.entrySet()) {
            if (!entry.getValue().isRemoved()) {
                activeLibraries.put(entry.getKey(), entry.getValue());
            }
        }

        return activeLibraries;
    }

    /**
     * Gets statistics about tracked libraries for an AID.
     *
     * @param aid Application ID
     * @return LibraryStats object with counts and information
     */
    public LibraryStats getLibraryStats(String aid) {
        if (aid == null) {
            return new LibraryStats(0, 0, 0);
        }

        Map<String, TemporalInfo> libraryMap = aidLibraryTracking.get(aid);
        if (libraryMap == null) {
            return new LibraryStats(0, 0, 0);
        }

        int totalLibraries = libraryMap.size();
        int activeLibraries = 0;
        int removedLibraries = 0;

        for (TemporalInfo info : libraryMap.values()) {
            if (info.isRemoved()) {
                removedLibraries++;
            } else {
                activeLibraries++;
            }
        }

        return new LibraryStats(totalLibraries, activeLibraries, removedLibraries);
    }

    /**
     * Clears tracking data for a specific AID.
     * Use with caution - typically only for testing or cleanup.
     *
     * @param aid Application ID to clear
     */
    public void clearAidTracking(String aid) {
        if (aid != null) {
            aidLibraryTracking.remove(aid);
            logger.info("Cleared tracking data for AID: {}", aid);
        }
    }

    /**
     * Gets the total number of AIDs being tracked.
     *
     * @return Number of tracked AIDs
     */
    public int getTrackedAidCount() {
        return aidLibraryTracking.size();
    }

    /**
     * Inner class to hold temporal information for a library.
     */
    public static class TemporalInfo {
        private final long firstSeen;
        private volatile long lastSeen;
        private volatile long removedAt;
        private volatile boolean removed;

        /**
         * Creates new temporal info with first and last seen timestamps.
         */
        public TemporalInfo(long firstSeen, long lastSeen) {
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.removedAt = -1;
            this.removed = false;
        }

        /**
         * Updates the last seen timestamp.
         */
        public synchronized void updateLastSeen(long timestamp) {
            if (timestamp > this.lastSeen) {
                this.lastSeen = timestamp;
            }
        }

        /**
         * Marks the library as removed.
         */
        public synchronized void markRemoved(long removedTimestamp) {
            this.removed = true;
            this.removedAt = removedTimestamp;
        }

        // Getters
        public long getFirstSeen() { return firstSeen; }
        public long getLastSeen() { return lastSeen; }
        public long getRemovedAt() { return removedAt; }
        public boolean isRemoved() { return removed; }

        @Override
        public String toString() {
            return String.format("TemporalInfo{firstSeen=%d, lastSeen=%d, removed=%s, removedAt=%d}",
                               firstSeen, lastSeen, removed, removedAt);
        }
    }

    /**
     * Statistics about libraries for an AID.
     */
    public static class LibraryStats {
        public final int totalLibraries;
        public final int activeLibraries;
        public final int removedLibraries;

        public LibraryStats(int totalLibraries, int activeLibraries, int removedLibraries) {
            this.totalLibraries = totalLibraries;
            this.activeLibraries = activeLibraries;
            this.removedLibraries = removedLibraries;
        }

        @Override
        public String toString() {
            return String.format("LibraryStats{total=%d, active=%d, removed=%d}",
                               totalLibraries, activeLibraries, removedLibraries);
        }
    }
}