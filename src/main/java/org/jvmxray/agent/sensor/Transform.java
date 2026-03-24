package org.jvmxray.agent.sensor;

/**
 * Represents a transformation specification for instrumentation in the JVMXRay agent framework.
 * Defines a target class, the methods to instrument, and the Byte Buddy interceptor to apply,
 * serving as a core data structure for sensors to configure runtime monitoring.
 *
 * @author Milton Smith
 */
public class Transform {
    // The class to be instrumented (e.g., java.net.ServerSocket)
    private final Class<?> targetClass;

    // Array of method specifications defining which methods to instrument
    private final MethodSpec[] methods;

    // The interceptor class to apply to the target methods (e.g., AcceptInterceptor.class)
    private final Class<?> interceptor;

    /**
     * Constructs a new {@code Transform} with the specified target class, interceptor,
     * and method specifications.
     *
     * @param targetClass The {@code Class} to instrument (e.g., {@code java.net.ServerSocket}).
     * @param interceptor The interceptor {@code Class} to apply (e.g., {@code AcceptInterceptor.class}).
     * @param methods     Variable-length array of {@code MethodSpec} objects specifying the methods to instrument.
     */
    public Transform(Class<?> targetClass, Class<?> interceptor, MethodSpec... methods) {
        this.targetClass = targetClass;
        this.methods = methods;
        this.interceptor = interceptor;
    }

    /**
     * Returns the target class to be instrumented by this transformation.
     *
     * @return The {@code Class} object representing the target class.
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the array of method specifications defining which methods to instrument.
     *
     * @return An array of {@code MethodSpec} objects.
     */
    public MethodSpec[] getMethods() {
        return methods;
    }

    /**
     * Returns the interceptor class to be applied to the target methods.
     *
     * @return The {@code Class} object representing the interceptor.
     */
    public Class<?> getInterceptor() {
        return interceptor;
    }
}