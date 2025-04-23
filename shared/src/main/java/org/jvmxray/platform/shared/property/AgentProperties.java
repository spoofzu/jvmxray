package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

/**
 * A concrete implementation of {@link IProperties} for managing agent-specific
 * properties in the JVMXRay framework. Extends {@link PropertyBase} to load and
 * manage properties from a file named <code>agent.properties</code> in the specified
 * JVMXRay home directory.
 *
 * <p>This class is used to configure agent-related settings, such as logging or
 * monitoring parameters, by reading from the properties file.</p>
 *
 * @author Milton Smith
 */
public class AgentProperties extends PropertyBase implements IProperties {

    /**
     * Constructs an {@code XRAgentProperties} instance, initializing it with the
     * specified JVMXRay home directory and the properties file
     * <code>agent.properties</code>.
     *
     * @param jvmxrayHome The {@link Path} to the JVMXRay home directory containing
     *                    the properties file.
     */
    public AgentProperties(Path jvmxrayHome) {
        // Initialize the base class with agent-specific properties file
        super(jvmxrayHome, "agent", "agent.properties");
    }
}