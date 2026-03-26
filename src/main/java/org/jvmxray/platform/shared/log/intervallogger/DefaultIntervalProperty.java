package org.jvmxray.platform.shared.log.intervallogger;

/**
 * Default implementation of the {@link IntervalProperty} interface, storing
 * key-value pairs for logging metrics in the JVMXRay framework. Serves as a base
 * class for properties that need to be updated periodically, such as system memory
 * or thread states.
 *
 * <p>Subclasses can override {@link #refresh()} to update the property value
 * dynamically. For example:</p>
 * <pre>{@code
 * DefaultIntervalProperty memoryProp = new DefaultIntervalProperty("TotalMemory") {
 *     public void refresh() {
 *         value = Long.toString(Runtime.getRuntime().totalMemory());
 *     }
 * };
 * }</pre>
 *
 * @author Milton Smith
 * @see IntervalProperty
 */
public class DefaultIntervalProperty implements IntervalProperty {

    // Name of the property, used as the key in logging
    private String name;
    // Value of the property, updated via refresh()
    protected String value = null;

    /**
     * Constructs a new {@code DefaultIntervalProperty} with the specified name.
     *
     * @param name The property key name, used to identify the value in logs.
     */
    public DefaultIntervalProperty(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
        }
        this.name = name;
    }

    /**
     * Returns the name of the property.
     *
     * @return The property name.
     */
    @Override
    public String getName() {
        // Return the property name
        return name;
    }

    /**
     * Returns the current value of the property.
     *
     * @return The property value, or null if not set.
     */
    @Override
    public String getValue() {
        // Return the current value
        return value;
    }

    /**
     * Signals the property to update its value. Subclasses should override this
     * method to implement dynamic updates for runtime-changing metrics. For example:
     * <pre>{@code
     * public void refresh() {
     *     value = Long.toString(Runtime.getRuntime().totalMemory());
     * }
     * }</pre>
     * <p>If the property value is static, consider logging it outside the status
     * message framework instead of using this class.</p>
     */
    @Override
    public void refresh() {
        // Default implementation does nothing
    }
}