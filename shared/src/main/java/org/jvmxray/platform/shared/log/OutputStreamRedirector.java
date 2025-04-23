package org.jvmxray.platform.shared.log;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} implementation that redirects output to an SLF4J
 * {@link Logger}, allowing {@code System.out} and {@code System.err} streams to
 * be logged via the JVMXRay logging framework. Writes data to a buffer and logs
 * it at the INFO or ERROR level based on the stream type when flushed.
 *
 * <p>This class is typically used to capture console output and redirect it to
 * SLF4J for centralized logging, supporting JVMXRay's logging infrastructure.</p>
 *
 * @author Milton Smith
 * @see "http://stackoverflow.com/questions/11187461/redirect-system-out-and-system-err-to-slf4j"
 */
public class OutputStreamRedirector extends OutputStream {

    // Logger instance for outputting redirected stream data
    protected Logger log;
    // Flag indicating whether to log at ERROR (true) or INFO (false) level
    protected boolean isError;
    // System line separator for handling newline detection
    protected static final String LINE_SEPERATOR = System.getProperty("line.separator");

    /**
     * Flag indicating whether the stream has been closed.
     * Used to maintain the contract of {@link #close()}.
     */
    protected boolean hasBeenClosed = false;

    /**
     * Internal buffer for storing data before logging.
     */
    protected byte[] buf;

    /**
     * Number of valid bytes in the buffer, in the range 0 to {@code buf.length}.
     * Elements {@code buf[0]} through {@code buf[count-1]} contain valid data.
     */
    protected int count;

    /**
     * Size of the buffer, cached for performance.
     */
    private int bufLength;

    /**
     * Default buffer size (2048 bytes).
     */
    public static final int DEFAULT_BUFFER_LENGTH = 2048;

    /**
     * Private constructor to prevent instantiation without a logger.
     */
    private OutputStreamRedirector() {
        // Prevent instantiation without parameters
    }

    /**
     * Constructs a new {@code OutputStreamRedirector} that redirects output to
     * the specified SLF4J logger.
     *
     * @param log The {@link Logger} to write to.
     * @param isError If {@code true}, logs at ERROR level; if {@code false}, logs at INFO level.
     * @throws IllegalArgumentException If {@code log} is null.
     */
    public OutputStreamRedirector(Logger log, boolean isError) throws IllegalArgumentException {
        // Validate the logger
        if (log == null) {
            throw new IllegalArgumentException("log == null");
        }
        // Initialize logger and error flag
        this.isError = isError;
        this.log = log;
        // Set initial buffer size
        this.bufLength = DEFAULT_BUFFER_LENGTH;
        this.buf = new byte[DEFAULT_BUFFER_LENGTH];
        // Initialize buffer count
        this.count = 0;
    }

    /**
     * Closes the stream and releases associated resources, flushing any buffered
     * data to the logger. Once closed, the stream cannot be used for further
     * operations.
     */
    @Override
    public void close() {
        // Flush any remaining buffered data
        flush();
        // Mark the stream as closed
        hasBeenClosed = true;
    }

    /**
     * Writes a single byte to the stream, appending it to the internal buffer.
     * The byte is the eight low-order bits of the input integer.
     *
     * @param b The byte to write, as an integer.
     * @throws IOException If the stream is closed.
     */
    @Override
    public void write(final int b) throws IOException {
        // Check if the stream is closed
        if (hasBeenClosed) {
            throw new IOException("The stream has been closed.");
        }
        // Ignore null bytes
        if (b == 0) {
            return;
        }
        // Check if buffer needs to grow
        if (count == bufLength) {
            // Calculate new buffer size
            final int newBufLength = bufLength + DEFAULT_BUFFER_LENGTH;
            // Create new buffer
            final byte[] newBuf = new byte[newBufLength];
            // Copy existing data to new buffer
            System.arraycopy(buf, 0, newBuf, 0, bufLength);
            // Update buffer and size
            buf = newBuf;
            bufLength = newBufLength;
        }
        // Append byte to buffer
        buf[count] = (byte) b;
        // Increment buffer count
        count++;
    }

    /**
     * Flushes the stream, logging any buffered data to the {@link Logger} at the
     * appropriate level (ERROR or INFO). Clears the buffer after logging, unless
     * the data represents an empty line.
     */
    @Override
    public void flush() {
        // Return if buffer is empty
        if (count == 0) {
            return;
        }
        // Skip blank lines (e.g., PrintStream flush artifacts)
        if (count == LINE_SEPERATOR.length()) {
            if (((char) buf[0]) == LINE_SEPERATOR.charAt(0) &&
                    ((count == 1) || // Unix & Mac
                            (count == 2 && ((char) buf[1]) == LINE_SEPERATOR.charAt(1)))) { // Windows
                // Reset buffer for blank lines
                reset();
                return;
            }
        }
        // Create array for buffered data
        final byte[] theBytes = new byte[count];
        // Copy buffered data to array
        System.arraycopy(buf, 0, theBytes, 0, count);
        // Log data at appropriate level
        if (isError) {
            log.error(new String(theBytes));
        } else {
            log.info(new String(theBytes));
        }
        // Clear buffer after logging
        reset();
    }

    /**
     * Resets the buffer by clearing its contents, retaining the buffer size for
     * future use.
     */
    private void reset() {
        // Reset count without resizing buffer
        count = 0;
    }
}