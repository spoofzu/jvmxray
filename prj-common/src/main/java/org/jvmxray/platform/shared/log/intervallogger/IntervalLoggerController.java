package org.jvmxray.platform.shared.log.intervallogger;

/**
 * Interface for controlling periodic background logging in the JVMXRay framework.
 * Orchestrates a background thread to periodically update property data via an
 * {@link IntervalLoggerModel} and format/log status messages via an
 * {@link IntervalLoggerView}. The default implementation logs system metrics every
 * 15 seconds.
 *
 * <p>Example usage to start and stop logging:</p>
 * <pre>{@code
 * IntervalLoggerController controller = SecurityLoggingFactory.getIntervalLoggerControllerInstance();
 * controller.start(); // Starts logging every 15 seconds
 * // Perform operations...
 * controller.stop(); // Stops background logging before exit
 * }</pre>
 *
 * <p>Example default log output with {@link DefaultIntervalLoggerView}:</p>
 * <pre>{@code
 * Watchdog: MemoryTotal=64.5MB, FreeMemory=58.2MB, MaxMemory=954.7MB, ThreadsTotal=5, ThreadsNew=0, ThreadsRunnable=3, ThreadsBlocked=0, ThreadsWaiting=2, ThreadsTerminated=0
 * }</pre>
 *
 * <p>Developers can customize logging by:</p>
 * <ul>
 *   <li>Adding or removing properties in the {@link IntervalLoggerModel}.</li>
 *   <li>Changing the log format or level in the {@link IntervalLoggerView}.</li>
 *   <li>Specifying a custom logging interval via {@link #start(int)}.</li>
 * </ul>
 *
 * @author Milton Smith
 * @see DefaultIntervalLoggerView
 * @see DefaultIntervalLoggerModel
 */
public interface IntervalLoggerController {

    /**
     * Starts the background logging thread with the default 15-second interval.
     * The thread periodically updates properties via the assigned
     * {@link IntervalLoggerModel} and logs them using the assigned
     * {@link IntervalLoggerView}.
     */
    void start();

    /**
     * Starts the background logging thread with a user-defined interval.
     * The thread periodically updates properties via the assigned
     * {@link IntervalLoggerModel} and logs them using the assigned
     * {@link IntervalLoggerView}.
     *
     * @param interval The logging interval in milliseconds.
     */
    void start(int interval);

    /**
     * Stops the background logging thread at the earliest opportunity.
     */
    void stop();

    /**
     * Sets the {@link IntervalLoggerView} used to format and log status messages.
     * If not set, a {@link DefaultIntervalLoggerView} is used by default.
     *
     * @param v The {@link IntervalLoggerView} instance to use.
     * @see DefaultIntervalLoggerView
     */
    void setStatusMessageView(IntervalLoggerView v);

    /**
     * Sets the {@link IntervalLoggerModel} used to manage and refresh property data.
     * If not set, a {@link DefaultIntervalLoggerModel} is used by default.
     *
     * @param m The {@link IntervalLoggerModel} instance to use.
     * @see DefaultIntervalLoggerModel
     */
    void setStatusMessageModel(IntervalLoggerModel m);
}