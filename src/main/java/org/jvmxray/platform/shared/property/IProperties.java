package org.jvmxray.platform.shared.property;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Interface for managing properties in the JVMXRay framework. Provides methods to
 * initialize, access, modify, and save properties, typically stored in a file.
 * Supports string, integer, and long property types with default values and
 * enumeration of property names.
 *
 * <p>Implementations, such as {@link AgentProperties}, handle loading properties
 * from a file in the JVMXRay home directory and provide persistence for configuration
 * settings like logging or monitoring parameters.</p>
 *
 * @author Milton Smith
 * @see AgentProperties
 * @see PropertyBase
 */
public interface IProperties {

    /**
     * Initializes the properties by loading them from the associated file.
     *
     * @throws IOException If an I/O error occurs while loading the properties.
     */
    void init() throws IOException;

    /**
     * Checks if the properties have been modified since the last load or save.
     *
     * @return {@code true} if the properties have been modified, {@code false} otherwise.
     */
    boolean isModified();

    /**
     * Retrieves the value of the specified property.
     *
     * @param name The name of the property.
     * @return The property value, or {@code null} if the property does not exist.
     */
    String getProperty(String name);

    /**
     * Retrieves the value of the specified property, returning a default value if
     * the property does not exist.
     *
     * @param name The name of the property.
     * @param defaultvalue The default value to return if the property is not found.
     * @return The property value, or the default value if the property does not exist.
     */
    String getProperty(String name, String defaultvalue);

    /**
     * Sets the value of the specified property.
     *
     * @param name The name of the property.
     * @param value The value to set.
     */
    void setProperty(String name, String value);

    /**
     * Retrieves the integer value of the specified property.
     *
     * @param name The name of the property.
     * @return The integer value of the property.
     * @throws NumberFormatException If the property value is not a valid integer.
     */
    int getIntProperty(String name) throws NumberFormatException;

    /**
     * Retrieves the integer value of the specified property, returning a default
     * value if the property does not exist or is not a valid integer.
     *
     * @param name The name of the property.
     * @param defaultvalue The default value to return if the property is not found
     *                     or invalid.
     * @return The integer value of the property, or the default value if not found
     *         or invalid.
     * @throws NumberFormatException If the property value is not a valid integer
     *                               and no default is provided.
     */
    int getIntProperty(String name, int defaultvalue) throws NumberFormatException;

    /**
     * Sets the integer value of the specified property.
     *
     * @param name The name of the property.
     * @param value The integer value to set.
     */
    void setIntProperty(String name, int value);

    /**
     * Retrieves the long value of the specified property.
     *
     * @param name The name of the property.
     * @return The long value of the property.
     * @throws NumberFormatException If the property value is not a valid long.
     */
    long getLongProperty(String name);

    /**
     * Retrieves the long value of the specified property, returning a default value
     * if the property does not exist or is not a valid long.
     *
     * @param name The name of the property.
     * @param defaultValue The default value to return if the property is not found
     *                     or invalid.
     * @return The long value of the property, or the default value if not found
     *         or invalid.
     */
    long getLongProperty(String name, long defaultValue);

    /**
     * Sets the long value of the specified property.
     *
     * @param name The name of the property.
     * @param value The long value to set.
     */
    void setLongProperty(String name, long value);

    /**
     * Retrieves an enumeration of all property names.
     *
     * @return An {@link Enumeration} of property names.
     */
    Enumeration<String> getPropertyNames();

    /**
     * Saves the properties to the associated file.
     *
     * @throws IOException If an I/O error occurs while saving the properties.
     */
    void saveProperties() throws IOException;
}