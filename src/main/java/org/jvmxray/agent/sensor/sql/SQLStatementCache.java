package org.jvmxray.agent.sensor.sql;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe cache for storing SQL text associated with PreparedStatement instances.
 * Uses weak references via identity hash codes to avoid memory leaks when statements are GC'd.
 *
 * @author Milton Smith
 */
public class SQLStatementCache {

    // Map from PreparedStatement identity hash code to SQL text
    private static final ConcurrentHashMap<Integer, CachedSQL> sqlCache = new ConcurrentHashMap<>();

    // Statistics for monitoring
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);
    private static final AtomicLong entriesCreated = new AtomicLong(0);
    private static final AtomicLong entriesRemoved = new AtomicLong(0);

    // Maximum cache size to prevent memory issues
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * Stores SQL text for a PreparedStatement.
     *
     * @param statement The PreparedStatement instance
     * @param sql The SQL text used to create the statement
     */
    public static void put(Object statement, String sql) {
        if (statement == null || sql == null) {
            return;
        }

        // Evict old entries if cache is too large
        if (sqlCache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }

        int key = System.identityHashCode(statement);
        sqlCache.put(key, new CachedSQL(sql, System.currentTimeMillis()));
        entriesCreated.incrementAndGet();
    }

    /**
     * Retrieves the SQL text for a PreparedStatement.
     *
     * @param statement The PreparedStatement instance
     * @return The SQL text, or null if not found
     */
    public static String get(Object statement) {
        if (statement == null) {
            return null;
        }

        int key = System.identityHashCode(statement);
        CachedSQL cached = sqlCache.get(key);

        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached.sql;
        } else {
            cacheMisses.incrementAndGet();
            return null;
        }
    }

    /**
     * Removes the SQL text for a PreparedStatement (typically on close).
     *
     * @param statement The PreparedStatement instance
     */
    public static void remove(Object statement) {
        if (statement == null) {
            return;
        }

        int key = System.identityHashCode(statement);
        if (sqlCache.remove(key) != null) {
            entriesRemoved.incrementAndGet();
        }
    }

    /**
     * Returns the current cache size.
     */
    public static int size() {
        return sqlCache.size();
    }

    /**
     * Returns cache statistics as a formatted string.
     */
    public static String getStats() {
        return String.format("SQLStatementCache[size=%d, hits=%d, misses=%d, created=%d, removed=%d]",
                sqlCache.size(), cacheHits.get(), cacheMisses.get(),
                entriesCreated.get(), entriesRemoved.get());
    }

    /**
     * Evicts the oldest entries from the cache to prevent unbounded growth.
     */
    private static void evictOldestEntries() {
        // Remove entries older than 5 minutes, or oldest 10% if still too large
        long cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000);

        sqlCache.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoffTime);

        // If still too large, remove oldest 10%
        if (sqlCache.size() >= MAX_CACHE_SIZE) {
            int toRemove = MAX_CACHE_SIZE / 10;
            sqlCache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                    .limit(toRemove)
                    .forEach(e -> {
                        sqlCache.remove(e.getKey());
                        entriesRemoved.incrementAndGet();
                    });
        }
    }

    /**
     * Internal class to hold cached SQL with timestamp for eviction.
     */
    private static class CachedSQL {
        final String sql;
        final long timestamp;

        CachedSQL(String sql, long timestamp) {
            this.sql = sql;
            this.timestamp = timestamp;
        }
    }
}
