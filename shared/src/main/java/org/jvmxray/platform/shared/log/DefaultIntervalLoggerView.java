package org.jvmxray.platform.shared.log;

import org.slf4j.Logger;

/**
 * Default implementation of the {@link IntervalLoggerView} interface, responsible
 * for formatting and logging key-value pairs in the JVMXRay framework. Formats
 * properties in the style: <code>Watchdog: property1=value1, property2=value2, ...</code>,
 * with all values logged on a single line at the INFO level.
 *
 * <p>Example output: <code>Watchdog: MemoryTotal=512.0MB, ThreadRunnable=5</code></p>
 *
 * <p>Developers can override {@link #logMessage(String)} to customize logging, such as
 * adding security markers or changing log levels. Example:</p>
 * <pre>{@code
 * IntervalLoggerController controller = SecurityLoggingFactory.getControllerInstance();
 * controller.setStatusMessageView(new DefaultIntervalLoggerView() {
 *     public void logMessage(String message) {
 *         getLogger().debug(SecurityMarkers.RESTRICTED, message);
 *     }
 * });
 * }</pre>
 *
 * @author Milton Smith
 */
public class DefaultIntervalLoggerView implements IntervalLoggerView {

    // Logger for outputting formatted status messages
    private static final Logger logger = JVMXRayLogFactory.getInstance().getLogger("org.jvmxray.platform.shared.logutil.DefaultIntervalLoggerView");

    /**
     * Retrieves the logger instance used for logging status messages.
     *
     * @return The {@link Logger} instance.
     */
    protected Logger getLogger() {
        // Return the static logger instance
        return logger;
    }

    /**
     * Formats an array of properties into a single-line status message.
     *
     * @param properties An array of {@link IntervalProperty} objects to format.
     * @return The formatted status message (e.g., "Watchdog: MemoryTotal=512.0MB, ThreadRunnable=5").
     */
    @Override
    public String formatStatusMessage(IntervalProperty[] properties) {
        if (properties == null) {
            return "";
        }
        // Initialize buffer for building the status message
        StringBuffer buff = new StringBuffer(500);
        // Append each property as "name=value"
        for (IntervalProperty p : properties) {
            buff.append(p.getName());
            buff.append("=");
            buff.append(p.getValue());
            buff.append(", ");
        }
        // Remove trailing ", " if present
        if (buff.toString().endsWith(", ")) {
            buff.setLength(buff.length() - 2);
        }
        // Return the formatted message
        return buff.toString();
    }

    /**
     * Logs the formatted status message at the INFO level using the configured logger.
     *
     * <p>Override this method to customize logging behavior, such as using different
     * log levels or adding security markers. Example:</p>
     * <pre>{@code
     * controller.setStatusMessageView(new DefaultIntervalLoggerView() {
     *     public void logMessage(String message) {
     *         getLogger().debug(SecurityMarkers.RESTRICTED, message);
     *     }
     * });
     * }</pre>
     *
     * @param message The status message to log.
     */
    @Override
    public void logMessage(String message) {
        // Get the logger instance
        Logger locallogger = getLogger();
        // Log the message at INFO level
        locallogger.info(message);
    }
}