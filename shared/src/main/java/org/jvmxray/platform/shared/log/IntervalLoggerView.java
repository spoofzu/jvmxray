package org.jvmxray.platform.shared.log;

/**
 * Interface for formatting and logging key-value pairs in the JVMXRay framework.
 * Implementations format an array of {@link IntervalProperty} objects into a status
 * message and log it, typically for periodic system metrics like memory or thread states.
 *
 * <p>This interface is used by an {@link IntervalLoggerController} to produce and
 * log status messages based on properties from an {@link IntervalLoggerModel}. The
 * default implementation, {@link DefaultIntervalLoggerView}, formats messages in the
 * style: <code>Watchdog: property1=value1, property2=value2, ...</code>.</p>
 *
 * @author Milton Smith
 * @see DefaultIntervalLoggerView
 * @see IntervalProperty
 * @see IntervalLoggerController
 */
public interface IntervalLoggerView {

    /**
     * Formats an array of properties into a status message for logging.
     *
     * @param p An array of {@link IntervalProperty} objects to format.
     * @return The formatted status message (e.g., "Watchdog: MemoryTotal=512.0MB, ThreadRunnable=5").
     * @see DefaultIntervalLoggerView
     */
    String formatStatusMessage(IntervalProperty[] p);

    /**
     * Logs the formatted status message.
     *
     * @param message The status message to log.
     * @see DefaultIntervalLoggerView
     */
    void logMessage(String message);
}