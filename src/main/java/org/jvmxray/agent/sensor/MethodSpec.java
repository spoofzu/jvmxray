package org.jvmxray.agent.sensor;

/**
 * Represents a method specification for instrumentation in the JVMXRay agent framework.
 * Encapsulates the name and parameter types of a method to be targeted by a {@link Transform},
 * enabling sensors to define which methods within a class should be intercepted by Byte Buddy
 * interceptors.
 *
 * @author Milton Smith
 */
public class MethodSpec {
    // The name of the method to be instrumented (e.g., "accept")
    private final String methodName;

    // The parameter types of the method, defining its signature (e.g., empty array or {SocketAddress.class, int.class})
    private final Class<?>[] parameterTypes;

    /**
     * Constructs a new {@code MethodSpec} with the specified method name and parameter types.
     *
     * @param methodName     The name of the method to instrument (e.g., "accept").
     * @param parameterTypes Variable-length array of {@link Class} objects representing
     *                       the method's parameter types. Use an empty array for methods
     *                       with no parameters.
     */
    public MethodSpec(String methodName, Class<?>... parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    /**
     * Returns the name of the method specified by this {@code MethodSpec}.
     *
     * @return The method name as a {@code String}.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the parameter types of the method specified by this {@code MethodSpec}.
     *
     * @return An array of {@code Class} objects representing the method's parameter types.
     */
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
}