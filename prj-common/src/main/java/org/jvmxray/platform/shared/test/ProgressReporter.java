package org.jvmxray.platform.shared.test;

/**
 * Progress reporter utility for displaying test execution progress with visual indicators.
 *
 * This utility provides a text-based progress bar using ANSI block characters to show
 * test execution progress in real-time. It supports percentage-based progress display
 * with elapsed and estimated time reporting.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Visual progress bar with ANSI block characters (█ for complete, ░ for incomplete)</li>
 *   <li>Percentage display with elapsed time and total duration</li>
 *   <li>Configurable update intervals to prevent excessive output</li>
 *   <li>Cross-platform compatibility with terminal escape sequence handling</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * Progress: ████████░░ 80% [24s/30s] Generating events...
 * </pre>
 *
 * @author JVMXRay Development Team
 */
public class ProgressReporter {

    // ANSI block characters for progress visualization
    private static final String COMPLETED_BLOCK = "█";
    private static final String INCOMPLETE_BLOCK = "░";
    private static final int PROGRESS_BAR_WIDTH = 10;

    // Progress tracking fields
    private final long startTime;
    private final long totalDurationMs;
    private final String taskDescription;
    private final boolean enabled;

    private long lastUpdateTime = 0;
    private int lastReportedPercent = -1;

    // Minimum interval between progress updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL_MS = 1000;

    /**
     * Creates a new ProgressReporter for tracking task execution progress.
     *
     * @param totalDurationMs Total expected duration of the task in milliseconds
     * @param taskDescription Description of the task being performed
     * @param enabled Whether progress reporting is enabled
     */
    public ProgressReporter(long totalDurationMs, String taskDescription, boolean enabled) {
        this.startTime = System.currentTimeMillis();
        this.totalDurationMs = totalDurationMs;
        this.taskDescription = taskDescription != null ? taskDescription : "Running";
        this.enabled = enabled;
    }

    /**
     * Creates a new ProgressReporter with progress reporting enabled by default.
     *
     * @param totalDurationMs Total expected duration of the task in milliseconds
     * @param taskDescription Description of the task being performed
     */
    public ProgressReporter(long totalDurationMs, String taskDescription) {
        this(totalDurationMs, taskDescription, true);
    }

    /**
     * Reports current progress and updates the progress bar if necessary.
     * Updates are throttled to prevent excessive console output.
     */
    public void reportProgress() {
        if (!enabled) return;

        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - startTime;

        // Calculate current percentage
        int currentPercent = (int) Math.min(100, (elapsedMs * 100) / totalDurationMs);

        // Only update if enough time has passed or we've reached a new 10% increment
        boolean shouldUpdate = (currentTime - lastUpdateTime >= MIN_UPDATE_INTERVAL_MS) ||
                              (currentPercent / 10 > lastReportedPercent / 10);

        if (shouldUpdate) {
            printProgress(currentPercent, elapsedMs);
            lastUpdateTime = currentTime;
            lastReportedPercent = currentPercent;
        }
    }

    /**
     * Reports completion of the task with 100% progress.
     */
    public void reportCompletion() {
        if (!enabled) return;

        long elapsedMs = System.currentTimeMillis() - startTime;
        printProgress(100, elapsedMs, "Complete!");
        System.out.println(); // Add newline after completion
    }

    /**
     * Prints the formatted progress bar to console.
     *
     * @param percent Current completion percentage (0-100)
     * @param elapsedMs Elapsed time in milliseconds
     */
    private void printProgress(int percent, long elapsedMs) {
        printProgress(percent, elapsedMs, taskDescription);
    }

    /**
     * Prints the formatted progress bar to console with custom message.
     *
     * @param percent Current completion percentage (0-100)
     * @param elapsedMs Elapsed time in milliseconds
     * @param message Custom message to display
     */
    private void printProgress(int percent, long elapsedMs, String message) {
        // Build progress bar
        StringBuilder progressBar = new StringBuilder();
        int completedBlocks = (percent * PROGRESS_BAR_WIDTH) / 100;

        for (int i = 0; i < PROGRESS_BAR_WIDTH; i++) {
            if (i < completedBlocks) {
                progressBar.append(COMPLETED_BLOCK);
            } else {
                progressBar.append(INCOMPLETE_BLOCK);
            }
        }

        // Format time display
        long elapsedSeconds = elapsedMs / 1000;
        long totalSeconds = totalDurationMs / 1000;
        String timeDisplay = String.format("[%ds/%ds]", elapsedSeconds, totalSeconds);

        // Print progress line with carriage return to overwrite previous line
        System.out.printf("\rProgress: %s %3d%% %-12s %s",
                         progressBar.toString(), percent, timeDisplay, message);
        System.out.flush();
    }

    /**
     * Gets the elapsed time since progress tracking started.
     *
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets the current progress percentage.
     *
     * @return Current progress percentage (0-100)
     */
    public int getCurrentPercent() {
        long elapsedMs = System.currentTimeMillis() - startTime;
        return (int) Math.min(100, (elapsedMs * 100) / totalDurationMs);
    }

    /**
     * Checks if progress reporting is enabled.
     *
     * @return true if progress reporting is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Forces a progress update regardless of timing constraints.
     * Useful for important milestones or completion reporting.
     */
    public void forceUpdate() {
        if (!enabled) return;

        long elapsedMs = System.currentTimeMillis() - startTime;
        int currentPercent = (int) Math.min(100, (elapsedMs * 100) / totalDurationMs);
        printProgress(currentPercent, elapsedMs);
        lastUpdateTime = System.currentTimeMillis();
        lastReportedPercent = currentPercent;
    }
}