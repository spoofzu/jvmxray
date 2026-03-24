package org.jvmxray.platform.shared.log.intervallogger;

/**
 * A wrapper implementation of the {@link IntervalLoggerController} interface that
 * delegates all operations to an underlying controller instance. Ensures caller
 * conformance to the controller’s interface specification by providing a consistent
 * interface while delegating to the wrapped implementation.
 *
 * <p>This class acts as a proxy, forwarding method calls to the underlying
 * {@link IntervalLoggerController} without adding additional logic or validation.</p>
 *
 * @author Milton Smith
 * @see IntervalLoggerController
 */
class IntervalLoggerControllerWrapper implements IntervalLoggerController {

    // The underlying controller instance to which all operations are delegated
    private IntervalLoggerController wd;

    /**
     * Constructs a new {@code IntervalLoggerControllerWrapper} that wraps the
     * specified controller.
     *
     * @param wd The {@link IntervalLoggerController} instance to wrap.
     */
    public IntervalLoggerControllerWrapper(IntervalLoggerController wd) {
        // Initialize the wrapped controller
        if (wd == null) {
            throw new IllegalArgumentException("Controller cannot be null");
        }
        this.wd = wd;
    }

    /**
     * Starts the background logging thread with a user-defined interval by
     * delegating to the wrapped controller.
     *
     * @param interval The logging interval in milliseconds.
     */
    @Override
    public void start(int interval) {
        // Delegate to the wrapped controller
        wd.start(interval);
    }

    /**
     * Starts the background logging thread with the default interval by
     * delegating to the wrapped controller.
     */
    @Override
    public void start() {
        // Delegate to the wrapped controller
        wd.start();
    }

    /**
     * Stops the background logging thread by delegating to the wrapped controller.
     */
    @Override
    public void stop() {
        // Delegate to the wrapped controller
        wd.stop();
    }

    /**
     * Sets the {@link IntervalLoggerView} used to format and log status messages
     * by delegating to the wrapped controller.
     *
     * @param v The {@link IntervalLoggerView} instance to use.
     */
    @Override
    public void setStatusMessageView(IntervalLoggerView v) {
        // Delegate to the wrapped controller
        wd.setStatusMessageView(v);
    }

    /**
     * Sets the {@link IntervalLoggerModel} used to manage and refresh property data
     * by delegating to the wrapped controller.
     *
     * @param m The {@link IntervalLoggerModel} instance to use.
     */
    @Override
    public void setStatusMessageModel(IntervalLoggerModel m) {
        // Delegate to the wrapped controller
        wd.setStatusMessageModel(m);
    }
}