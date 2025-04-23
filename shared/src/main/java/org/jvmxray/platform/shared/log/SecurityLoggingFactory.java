package org.jvmxray.platform.shared.log;

/**
 * Singleton factory for obtaining a wrapped {@link IntervalLoggerController}
 * instance in the JVMXRay framework. Provides a single, thread-safe point of access
 * to a logging controller, ensuring a new instance is created if the existing one
 * is not running.
 *
 * <p>The factory returns an {@link IntervalLoggerControllerWrapper} that delegates
 * to a {@link DefaultIntervalLoggerController}, allowing customization of logging
 * behavior while maintaining a consistent interface.</p>
 *
 * @author Milton Smith
 */
public class SecurityLoggingFactory {

    // Singleton instance of the unwrapped controller for checking running state
    private static DefaultIntervalLoggerController instance;

    /**
     * Private constructor to prevent instantiation by callers.
     */
    private SecurityLoggingFactory() {
        // Prevent external instantiation
    }

    /**
     * Returns a thread-safe singleton instance of an {@link IntervalLoggerController}.
     * Creates a new {@link DefaultIntervalLoggerController} wrapped in an
     * {@link IntervalLoggerControllerWrapper} if no instance exists or if the
     * existing instance is not running. If the existing instance is running,
     * returns a new wrapper around it to ensure interface conformance.
     *
     * @return The {@link IntervalLoggerController} instance, never null.
     */
    public synchronized static final IntervalLoggerController getControllerInstance() {
        // Initialize the controller to return
        IntervalLoggerController ic;

        // Check if no controller instance exists or is not running
        if (instance == null || !instance.isRunning()) {
            // Create a new unwrapped controller
            instance = new DefaultIntervalLoggerController();
            // Wrap it for interface conformance
            ic = new IntervalLoggerControllerWrapper(instance);
        } else {
            // Reuse the running controller with a new wrapper
            ic = new IntervalLoggerControllerWrapper(instance);
        }

        // Return the wrapped controller
        return ic;
    }

}