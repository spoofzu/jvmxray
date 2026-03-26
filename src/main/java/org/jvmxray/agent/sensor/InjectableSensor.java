package org.jvmxray.agent.sensor;

/**
 * Interface for sensors that utilize Byte Buddy to inject classes and configure
 * transformations for JVM instrumentation within the JVMXRay agent framework.
 * Extends the {@link Sensor} interface to include methods for specifying classes
 * to inject into the bootstrap class loader and defining instrumentation transformations.
 *
 * @author Milton Smith
 */
public interface InjectableSensor extends Sensor {

    /**
     * Provides an array of classes to be injected into the bootstrap class loader.
     * Implementations should return classes required for the sensor's functionality,
     * such as interceptors and utility classes.
     *
     * @return An array of {@link Class} objects to be injected.
     */
    Class<?>[] inject();

    /**
     * Defines the transformations to apply to target classes for instrumentation.
     * Implementations should return an array of {@link Transform} objects, each
     * specifying a target class, interceptor, and method to instrument.
     *
     * @return An array of {@code Transform} objects specifying the instrumentation targets.
     */
    Transform[] configure();
}