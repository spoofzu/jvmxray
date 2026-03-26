package org.jvmxray.platform.shared.log.intervallogger;

/**
 * Interface for storing and managing key-value property data in the JVMXRay logging
 * framework. Properties represent metrics, such as memory usage or thread states,
 * that are periodically updated and logged via an {@link IntervalLoggerController}.
 *
 * <p>Implementations store a name and value, with the ability to refresh the value
 * dynamically at runtime. The {@link #refresh()} method should be implemented to
 * update the value for metrics that change over time.</p>
 *
 * @author Milton Smith
 * @see DefaultIntervalProperty
 * @see ByteIntervalProperty
 */
public interface IntervalProperty {

    /**
     * Retrieves the name of the property, used as the key in logged status messages.
     *
     * @return The property name.
     */
    String getName();

    /**
     * Retrieves the current value of the property.
     *
     * @return The property value.
     */
    String getValue();

    /**
     * Signals the property to update its value. Implementations should override
     * this method to refresh the value for metrics that change at runtime.
     *
     * @see DefaultIntervalProperty#refresh()
     */
    void refresh();
}