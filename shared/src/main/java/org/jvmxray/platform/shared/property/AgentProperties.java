package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

/**
 * A concrete implementation of {@link IProperties} for managing agent-specific
 * properties in the JVMXRay framework. Extends {@link PropertyBase} to load and
 * manage properties from a file named <code>agent.properties</code> in the specified
 * JVMXRay home directory. Provides functionality to load sensors dynamically.
 *
 * @author Milton Smith
 */
public class AgentProperties extends PropertyBase implements IProperties {

    //todo: AgentProperties should be moved to Agent project someday.

    public AgentProperties(Path jvmxrayHome) {
        super(jvmxrayHome, "agent", "agent.properties");
    }

}