package org.jvmxray.platform.shared.log.intervallogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of the {@link IntervalLoggerController} interface, using
 * a {@link ScheduledExecutorService} to periodically execute a logging task for
 * monitoring JVM metrics in the JVMXRay framework. Manages a model-view architecture
 * to refresh and log status messages at fixed intervals (default: 15 seconds).
 *
 * <p>The controller initiates a logging task immediately upon starting and repeats
 * it at a configurable interval. It uses a custom thread factory for named threads,
 * supports graceful shutdown with a timeout, and includes validation for robustness.</p>
 *
 * @author Milton Smith
 */
class DefaultIntervalLoggerController implements IntervalLoggerController {

    // Logger for reporting errors and warnings
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIntervalLoggerController.class);

    // Default logging interval (15 seconds, in milliseconds)
    private static final int INTERVAL_DEFAULT = 15000;
    // Configured interval for periodic logging
    private int interval = INTERVAL_DEFAULT;
    // Flag indicating whether the controller is running (volatile for thread safety)
    private volatile boolean running = false;
    // Scheduler for executing periodic logging tasks
    private ScheduledExecutorService scheduler;
    // View for formatting and logging status messages
    private IntervalLoggerView view = new DefaultIntervalLoggerView();
    // Model for managing properties to log
    private IntervalLoggerModel model = new DefaultIntervalLoggerModel();

    /**
     * Custom {@link ThreadFactory} for creating named threads in the scheduler.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        // Prefix for thread names (e.g., "jvmxray.monitor")
        private final String namePrefix;
        // Counter for unique thread numbering
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        /**
         * Constructs a new {@code NamedThreadFactory} with the specified name prefix.
         *
         * @param namePrefix The prefix for thread names.
         */
        NamedThreadFactory(String namePrefix) {
            // Initialize the thread name prefix
            this.namePrefix = namePrefix;
        }

        /**
         * Creates a new thread with a unique name based on the prefix and a counter.
         *
         * @param r The runnable task to execute in the thread.
         * @return A new {@code Thread} with a formatted name (e.g., "jvmxray.monitor-1").
         */
        @Override
        public Thread newThread(Runnable r) {
            // Create a new thread with the specified runnable
            Thread thread = new Thread(r);
            // Set a unique name for debugging
            thread.setName(namePrefix + "-" + threadNumber.getAndIncrement());
            return thread;
        }
    }

    /**
     * Starts the controller with a custom interval for periodic logging.
     *
     * @param interval The interval in milliseconds between logging events.
     * @throws IllegalArgumentException If the interval is non-positive.
     */
    @Override
    public synchronized void start(int interval) {
        // Validate the interval
        if (interval <= 0) {
            LOGGER.error("Invalid interval: {}ms. Interval must be positive.", interval);
            throw new IllegalArgumentException("Interval must be positive");
        }
        // Set the custom interval
        this.interval = interval;
        // Start the controller
        start();
    }

    /**
     * Starts the controller using the default or previously set interval.
     * The logging task runs immediately and then periodically at the configured
     * interval. Does nothing if the controller is already running.
     */
    @Override
    public synchronized void start() {
        // Check if the controller is not running
        if (!running) {
            // Initialize a single-threaded scheduler with named threads
            scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("jvmxray.monitor"));
            // Mark the controller as running
            running = true;
            // Schedule the logging task to run immediately and at fixed intervals
            scheduler.scheduleAtFixedRate(this::fireIntervalElapsed, 0, interval, TimeUnit.MILLISECONDS);
            LOGGER.info("Started controller with interval {}ms.", interval);
        }
    }

    /**
     * Stops the controller and shuts down the scheduler, waiting up to 5 seconds
     * for termination. Forces immediate shutdown if the timeout is exceeded.
     */
    @Override
    public synchronized void stop() {
        // Check if the controller is running and has a scheduler
        if (running && scheduler != null) {
            // Mark the controller as stopped
            running = false;
            // Initiate graceful shutdown
            scheduler.shutdown();
            try {
                // Wait for tasks to complete within 5 seconds
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    // Force shutdown if tasks do not complete
                    scheduler.shutdownNow();
                    LOGGER.warn("Scheduler did not terminate within 5 seconds; forced shutdown.");
                } else {
                    LOGGER.info("Scheduler terminated gracefully.");
                }
            } catch (InterruptedException e) {
                // Force shutdown and restore interrupted status
                scheduler.shutdownNow();
                LOGGER.warn("Interrupted during scheduler shutdown.", e);
                Thread.currentThread().interrupt();
            }
            // Clear the scheduler reference
            scheduler = null;
        }
    }

    /**
     * Checks if the controller is currently running.
     *
     * @return {@code true} if the scheduler is active, {@code false} otherwise.
     */
    synchronized boolean isRunning() {
        // Return the current running state
        return running;
    }

    /**
     * Assigns a custom {@link IntervalLoggerView} for formatting and logging status messages.
     *
     * @param view The view instance to use.
     * @throws NullPointerException If the view is null.
     */
    @Override
    public synchronized void setStatusMessageView(IntervalLoggerView view) {
        // Validate the view
        if (view == null) {
            LOGGER.error("Attempted to set null IntervalLoggerView.");
            throw new NullPointerException("View must not be null");
        }
        // Assign the custom view
        this.view = view;
        LOGGER.debug("Set custom IntervalLoggerView: {}.", view.getClass().getName());
    }

    /**
     * Assigns a custom {@link IntervalLoggerModel} for managing properties to log.
     *
     * @param model The model instance to use.
     * @throws NullPointerException If the model is null.
     */
    @Override
    public synchronized void setStatusMessageModel(IntervalLoggerModel model) {
        // Validate the model
        if (model == null) {
            LOGGER.error("Attempted to set null IntervalLoggerModel.");
            throw new NullPointerException("Model must not be null");
        }
        // Assign the custom model
        this.model = model;
        LOGGER.debug("Set custom IntervalLoggerModel: {}.", model.getClass().getName());
    }

    /**
     * Executes the periodic logging task, refreshing the model and logging the
     * formatted status message through the view. Handles exceptions to ensure
     * robustness.
     */
    private void fireIntervalElapsed() {
        // Ensure model and view are not null
        if (model == null || view == null) {
            LOGGER.error("Cannot log: model or view is null (model={}, view={}).", model, view);
            return;
        }
        try {
            // Retrieve current properties from the model
            IntervalProperty[] properties = model.getProperties();
            // Refresh the model to update property values
            model.refresh();
            // Format the status message using the view
            String msg = view.formatStatusMessage(properties);
            // Log the formatted message
            view.logMessage(msg);
        } catch (Exception e) {
            // Log any errors during logging task to prevent task failure
            LOGGER.error("Failed to execute logging task.", e);
        }
    }
}