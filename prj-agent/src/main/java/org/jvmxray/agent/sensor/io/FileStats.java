package org.jvmxray.agent.sensor.io;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics tracker for file I/O operations.
 * Tracks aggregate read/write operations and bytes transferred for a single file.
 *
 * @author Milton Smith
 */
public class FileStats {
    private final String filePath;
    private final boolean isNewFile;
    private final boolean isSensitive;
    private final boolean shouldIgnore;
    private final long startTime;

    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong readOperations = new AtomicLong(0);
    private final AtomicLong writeOperations = new AtomicLong(0);

    /**
     * Creates a new FileStats tracker.
     *
     * @param filePath Absolute path to the file
     * @param isNewFile True if this is a newly created file
     * @param isSensitive True if file path matches sensitive patterns
     * @param shouldIgnore True if file path matches ignore patterns
     */
    public FileStats(String filePath, boolean isNewFile, boolean isSensitive, boolean shouldIgnore) {
        this.filePath = filePath;
        this.isNewFile = isNewFile;
        this.isSensitive = isSensitive;
        this.shouldIgnore = shouldIgnore;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Records bytes read from file.
     * @param bytes Number of bytes read
     */
    public void addBytesRead(long bytes) {
        if (bytes > 0) {
            bytesRead.addAndGet(bytes);
            readOperations.incrementAndGet();
        }
    }

    /**
     * Records bytes written to file.
     * @param bytes Number of bytes written
     */
    public void addBytesWritten(long bytes) {
        if (bytes > 0) {
            bytesWritten.addAndGet(bytes);
            writeOperations.incrementAndGet();
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isNewFile() {
        return isNewFile;
    }

    public boolean isSensitive() {
        return isSensitive;
    }

    public boolean shouldIgnore() {
        return shouldIgnore;
    }

    public long getBytesRead() {
        return bytesRead.get();
    }

    public long getBytesWritten() {
        return bytesWritten.get();
    }

    public long getReadOperations() {
        return readOperations.get();
    }

    public long getWriteOperations() {
        return writeOperations.get();
    }

    public long getDurationMs() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Returns the primary operation type for this file.
     */
    public String getOperationType() {
        long totalRead = bytesRead.get();
        long totalWrite = bytesWritten.get();

        if (totalWrite > 0 && totalRead == 0) {
            return isNewFile ? "create" : "write";
        } else if (totalRead > 0 && totalWrite == 0) {
            return "read";
        } else if (totalRead > 0 && totalWrite > 0) {
            return "read_write";
        } else {
            return "open";  // File was opened but no I/O occurred
        }
    }

    @Override
    public String toString() {
        return String.format("FileStats[path=%s, new=%b, sensitive=%b, read=%d bytes (%d ops), write=%d bytes (%d ops), duration=%dms]",
                filePath, isNewFile, isSensitive, bytesRead.get(), readOperations.get(),
                bytesWritten.get(), writeOperations.get(), getDurationMs());
    }
}
