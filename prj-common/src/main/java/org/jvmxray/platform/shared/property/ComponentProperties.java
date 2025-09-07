package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

/**
 * A generic concrete implementation of {@link PropertyBase} for managing
 * component-specific properties in the JVMXRay framework.
 * <p>
 * This class provides a standard property handling implementation that can be used
 * by any JVMXRay component for managing configuration files. It extends
 * {@link PropertyBase} to load and manage properties from a file named
 * {componentName}.properties in the component's config directory.
 * </p>
 * 
 * @author JVMXRay Platform
 * @since 0.0.1
 */
public class ComponentProperties extends PropertyBase implements IProperties {

    /**
     * Constructs a new ComponentProperties instance for the specified component.
     * The properties file will be located at {componentHome}/config/{componentName}.properties
     *
     * @param componentHome The path to the component's home directory (e.g., ~/.jvmxray/agent)
     * @param componentName The name of the component (e.g., "agent", "common", "integration")
     */
    public ComponentProperties(Path componentHome, String componentName) {
        super(componentHome, componentName);
    }
    /**
     * Constructs a new ComponentProperties instance from a component home directory.
     * The component name is derived from the directory name.
     * The properties file will be located at {componentHome}/config/{componentName}.properties
     *
     * @param componentHome The path to the component's home directory (e.g., ~/.jvmxray/agent)
     */
    public ComponentProperties(Path componentHome) {
        super(componentHome);
    }
}