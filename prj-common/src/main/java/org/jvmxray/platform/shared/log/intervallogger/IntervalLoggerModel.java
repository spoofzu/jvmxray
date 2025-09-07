package org.jvmxray.platform.shared.log.intervallogger;

/**
 * Interface for managing property data in the JVMXRay logging framework. Provides
 * methods to add, remove, retrieve, and refresh properties used in periodic status
 * messages. Implementations maintain a collection of {@link IntervalProperty}
 * objects, which store key-value pairs for metrics such as memory usage or thread states.
 *
 * <p>This interface is used by an {@link IntervalLoggerController} to update property
 * data before logging via an {@link IntervalLoggerView}. The default implementation,
 * {@link DefaultIntervalLoggerModel}, includes properties like memory and thread metrics.</p>
 *
 * @author Milton Smith
 * @see DefaultIntervalLoggerModel
 * @see IntervalProperty
 * @see IntervalLoggerController
 */
public interface IntervalLoggerModel {

    /**
     * Adds a new property to be included in status messages.
     *
     * @param action The {@link IntervalProperty} to add.
     */
    void addProperty(IntervalProperty action);

    /**
     * Removes a property from status messages.
     *
     * @param action The {@link IntervalProperty} to remove.
     */
    void removeProperty(IntervalProperty action);

    /**
     * Retrieves all properties currently managed by the model.
     *
     * @return An array of {@link IntervalProperty} objects available for inclusion
     *         in status messages.
     */
    IntervalProperty[] getProperties();

    /**
     * Signals all managed properties to update their values. Implementations should
     * invoke {@link IntervalProperty#refresh()} on each property.
     */
    void refresh();
}