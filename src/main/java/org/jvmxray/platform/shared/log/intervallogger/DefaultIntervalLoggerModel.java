package org.jvmxray.platform.shared.log.intervallogger;

import java.lang.Thread.State;
import java.util.ArrayList;

/**
 * Default implementation of the {@link IntervalLoggerModel} interface, providing
 * system metrics for logging in the JVMXRay framework. Collects properties such as
 * memory usage (total, free, max) and thread states (new, runnable, blocked, waiting,
 * terminated). Developers can add or remove properties to customize the model.
 *
 * <p>Example to add a custom property:</p>
 * <pre>{@code
 * DefaultIntervalLoggerModel model = new DefaultIntervalLoggerModel();
 * model.addProperty(new DefaultIntervalProperty("CustomProperty") {
 *     public void refresh() {
 *         value = "custom value";
 *     }
 * });
 * }</pre>
 *
 * <p>Example to remove the "ThreadNew" property:</p>
 * <pre>{@code
 * IntervalProperty[] properties = model.getProperties();
 * for (IntervalProperty p : properties) {
 *     if (p.getName().equals("ThreadNew")) {
 *         model.removeProperty(p);
 *     }
 * }
 * }</pre>
 *
 * @author Milton Smith
 */
public class DefaultIntervalLoggerModel implements IntervalLoggerModel {

    // Root thread group for enumerating all threads (cached for efficiency)
    private static ThreadGroup rootThreadGroup = null;
    // List of properties to log (e.g., memory and thread metrics)
    private ArrayList<IntervalProperty> list = new ArrayList<IntervalProperty>();

    /**
     * Constructs a new {@code DefaultIntervalLoggerModel}, initializing default
     * properties for memory metrics (total, free, max) and thread states (new,
     * runnable, blocked, waiting, terminated).
     */
    public DefaultIntervalLoggerModel() {
        // Initialize parent class (if any)
        super();

        // Add memory total property
        addProperty(new ByteIntervalProperty("MemoryTotal") {
            @Override
            public void refresh() {
                // Update value with total memory in bytes, formatted with SI units
                value = addUnits(Long.toString(Runtime.getRuntime().totalMemory()));
            }
        });

        // Add memory free property
        addProperty(new ByteIntervalProperty("MemoryFree") {
            @Override
            public void refresh() {
                // Update value with free memory in bytes, formatted with SI units
                value = addUnits(Long.toString(Runtime.getRuntime().freeMemory()));
            }
        });

        // Add memory max property
        addProperty(new ByteIntervalProperty("MemoryMax") {
            @Override
            public void refresh() {
                // Update value with maximum memory in bytes, formatted with SI units
                value = addUnits(Long.toString(Runtime.getRuntime().maxMemory()));
            }
        });

        // Add thread new state property
        addProperty(new DefaultIntervalProperty("ThreadNew") {
            @Override
            public void refresh() {
                // Update value with count of threads in NEW state
                value = Long.toString(getThreadState(State.NEW));
            }
        });

        // Add thread runnable state property
        addProperty(new DefaultIntervalProperty("ThreadRunnable") {
            @Override
            public void refresh() {
                // Update value with count of threads in RUNNABLE state
                value = Long.toString(getThreadState(State.RUNNABLE));
            }
        });

        // Add thread blocked state property
        addProperty(new DefaultIntervalProperty("ThreadBlocked") {
            @Override
            public void refresh() {
                // Update value with count of threads in BLOCKED state
                value = Long.toString(getThreadState(State.BLOCKED));
            }
        });

        // Add thread waiting state property
        addProperty(new DefaultIntervalProperty("ThreadWaiting") {
            @Override
            public void refresh() {
                // Update value with count of threads in WAITING state
                value = Long.toString(getThreadState(State.WAITING));
            }
        });

        // Add thread terminated state property
        addProperty(new DefaultIntervalProperty("ThreadTerminated") {
            @Override
            public String getValue() {
                // Return count of threads in TERMINATED state
                return Long.toString(getThreadState(State.TERMINATED));
            }
        });

        // TODO: Implement ThreadTotal property to sum all thread states
        // addProperty(new DefaultIntervalProperty("ThreadTotal") {
        //     public String getValue() {
        //         return Long.toString(t_new + t_);
        //     }
        // });
    }

    /**
     * Adds a new property to the model for inclusion in status messages.
     *
     * @param action The property to add.
     */
    @Override
    public synchronized void addProperty(IntervalProperty action) {
        // Add the property to the list
        list.add(action);
    }

    /**
     * Removes a property from the model, excluding it from status messages.
     *
     * @param action The property to remove.
     */
    @Override
    public synchronized void removeProperty(IntervalProperty action) {
        // Remove the property from the list
        list.remove(action);
    }

    /**
     * Returns all properties currently in the model.
     *
     * @return An array of all properties available for inclusion in status messages.
     */
    @Override
    public synchronized IntervalProperty[] getProperties() {
        // Convert the property list to an array
        return list.toArray(new IntervalProperty[0]);
    }

    /**
     * Signals all properties to update their values.
     */
    @Override
    public synchronized void refresh() {
        // Retrieve all properties
        IntervalProperty[] properties = getProperties();
        // Update each property's value
        for (IntervalProperty p : properties) {
            p.refresh();
        }
    }

    /**
     * Counts the number of threads in the specified state.
     *
     * @param state The target {@link Thread.State} to count.
     * @return The total number of threads in the specified state.
     */
    private int getThreadState(Thread.State state) {
        // Get all threads in the system
        Thread[] threads = getAllThreads();
        int ct = 0;
        // Count threads matching the specified state
        for (Thread thread : threads) {
            if (state.equals(thread.getState())) {
                ct++;
            }
        }
        return ct;
    }

    /**
     * Retrieves all threads in the system owned by the root thread group.
     *
     * @return An array of all threads.
     */
    private Thread[] getAllThreads() {
        // Get the root thread group
        final ThreadGroup root = getRootThreadGroup();
        // Estimate the number of active threads
        int ct = Thread.activeCount();
        int n = 0;
        Thread[] threads;
        // Dynamically resize array until all threads are captured
        do {
            ct *= 2;
            threads = new Thread[ct];
            n = root.enumerate(threads, true);
        } while (n == ct);
        // Return a correctly sized array of threads
        return java.util.Arrays.copyOf(threads, n);
    }

    /**
     * Retrieves the root thread group, where the parent is null.
     *
     * @return The root {@link ThreadGroup}.
     */
    private ThreadGroup getRootThreadGroup() {
        // Return cached root thread group if available
        if (rootThreadGroup != null) {
            return rootThreadGroup;
        }
        // Traverse up the thread group hierarchy to find the root
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ((ptg = tg.getParent()) != null) {
            tg = ptg;
        }
        // Cache the root thread group
        rootThreadGroup = tg;
        return tg;
    }
}