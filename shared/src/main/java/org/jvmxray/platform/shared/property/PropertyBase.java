package org.jvmxray.platform.shared.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Abstract base class for managing properties in a standard Java properties file
 * within the JVMXRay framework. Provides functionality to read, write, and modify
 * properties, supporting multi-line values and preserving file comments. Designed
 * for subclasses like {@link AgentProperties} to manage component-specific
 * configurations, such as agent settings, with required properties like Agent ID
 * (AID) and Category (CAT).
 *
 * <p>Properties are stored in a file (e.g., <code>agent.properties</code>) in a
 * component-specific subdirectory of the JVMXRay home directory. Changes are tracked
 * to determine if saving is needed. Property files must contain valid key-value pairs
 * (key=value or key:value) with non-empty keys and values; malformed lines are logged
 * as warnings and preserved in the file content. Security measures include file and
 * line size limits to prevent resource exhaustion.</p>
 *
 * @author Milton Smith
 * @see AgentProperties
 * @see IProperties
 */
public abstract class PropertyBase {

    // Logger for reporting property parsing issues
    private static final Logger logger = LoggerFactory.getLogger(PropertyBase.class);
    // Default maximum line length for multi-line properties
    private static final int MAX_LINE_LENGTH_DEFAULT = 80;
    // Maximum allowed line length to prevent resource exhaustion
    private static final int MAX_LINE_LENGTH = 1024;
    // Maximum allowed file size (1MB) to prevent resource exhaustion
    private static final long MAX_FILE_SIZE = 1024 * 1024;

    // Current properties loaded from or modified in memory
    protected Properties properties = new Properties();
    // Original properties as loaded from file, for modification tracking
    private Properties propertiesOriginal = new Properties();
    // List of original file content lines (including comments)
    private final List<String> fileContent = new ArrayList<>();
    // Flag indicating if properties have been modified
    private volatile boolean bModifiedProperties = false;

    // JVMXRay home directory
    protected Path jvmxrayHome;
    // Component-specific subdirectory
    protected Path componentHome;
    // Properties file
    private File propertyFile = null;
    // Name of the component (e.g., "agent")
    protected String componentName;
    // Name of the properties file (e.g., "agent.properties")
    protected String propertyFileName;

    /**
     * Default constructor for subclasses to initialize.
     * Subclasses must call {@link #init()} before use.
     */
    public PropertyBase() {
        // Empty constructor for subclass initialization
    }

    /**
     * Constructs a new {@code XRPropertyBase} instance for the specified component
     * and properties file.
     *
     * @param jvmxrayHome The {@link Path} to the JVMXRay home directory.
     * @param componentName The name of the component (e.g., "agent").
     * @param propertyFileName The name of the properties file (e.g., "agent.properties").
     */
    public PropertyBase(Path jvmxrayHome, String componentName, String propertyFileName) {
        // Initialize home directory, component name, and file name
        this.jvmxrayHome = jvmxrayHome;
        this.componentName = componentName;
        this.propertyFileName = propertyFileName;
    }

    /**
     * Initializes the properties by creating the component directory and loading
     * the properties file.
     *
     * @throws IOException If an I/O error occurs while creating directories or loading the file.
     */
    public void init() throws IOException {
        // Create component-specific subdirectory
        this.componentHome = Paths.get(jvmxrayHome.toFile().getAbsolutePath(), componentName);
        Files.createDirectories(componentHome);
        // Initialize properties file
        propertyFile = new File(componentHome.toFile(), propertyFileName);
        // Load properties from file
        readPropertiesFile();
        // Store original properties for modification tracking
        propertiesOriginal.putAll(properties);
    }

    /**
     * Retrieves the value of the specified property.
     *
     * @param name The name of the property.
     * @return The property value, or {@code null} if not found.
     */
    public String getProperty(String name) {
        // Get property value and trim if non-null
        String sValue = properties.getProperty(name);
        sValue = (sValue != null) ? sValue.trim() : sValue;
        return sValue;
    }

    /**
     * Retrieves the value of the specified property, returning a default if not found.
     *
     * @param name The name of the property.
     * @param defaultvalue The default value to return if the property is not found.
     * @return The property value, or the default value if not found.
     */
    public String getProperty(String name, String defaultvalue) {
        // Get property value with default and trim if non-null
        String sValue = properties.getProperty(name, defaultvalue);
        sValue = (sValue != null) ? sValue.trim() : sValue;
        return sValue;
    }

    /**
     * Sets the value of the specified property.
     *
     * @param name The name of the property.
     * @param value The value to set.
     */
    public void setProperty(String name, String value) {
        // Set property value and mark as modified
        properties.setProperty(name, value);
        bModifiedProperties = true;
    }

    /**
     * Retrieves the boolean value of the specified property.
     *
     * @param name The name of the property.
     * @return The boolean value of the property.
     * @throws IllegalArgumentException If the property is not set or not a valid boolean.
     */
    public boolean getBooleanProperty(String name) {
        // Get property value
        String sValue = properties.getProperty(name);
        // Parse boolean if value exists
        if (sValue != null) {
            return Boolean.parseBoolean(sValue.trim());
        }
        // Throw exception if property is missing or invalid
        throw new IllegalArgumentException("Property for " + name + " is not set or not a valid boolean");
    }

    /**
     * Retrieves the boolean value of the specified property, returning a default if
     * not found or invalid.
     *
     * @param name The name of the property.
     * @param defaultValue The default value to return if the property is not found.
     * @return The boolean value of the property, or the default value if not found.
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        // Get property value
        String sValue = properties.getProperty(name);
        // Parse boolean if value exists, else return default
        if (sValue != null) {
            return Boolean.parseBoolean(sValue.trim());
        }
        return defaultValue;
    }

    /**
     * Sets the boolean value of the specified property.
     *
     * @param name The name of the property.
     * @param value The boolean value to set.
     */
    public void setBooleanProperty(String name, boolean value) {
        // Set property as string and mark as modified
        properties.setProperty(name, Boolean.toString(value));
        bModifiedProperties = true;
    }

    /**
     * Retrieves the integer value of the specified property.
     *
     * @param name The name of the property.
     * @return The integer value of the property.
     * @throws NumberFormatException If the property is not set or not a valid integer.
     */
    public int getIntProperty(String name) throws NumberFormatException {
        // Get property value and trim if non-null
        String sValue = properties.getProperty(name);
        sValue = (sValue != null) ? sValue.trim() : sValue;
        // Parse integer value
        int iValue = Integer.parseInt(sValue);
        return iValue;
    }

    /**
     * Retrieves the integer value of the specified property, returning a default if
     * not found or invalid.
     *
     * @param name The name of the property.
     * @param defaultvalue The default value to return if the property is not found.
     * @return The integer value of the property, or the default value if not found.
     * @throws NumberFormatException If the property value is not a valid integer.
     */
    public int getIntProperty(String name, int defaultvalue) throws NumberFormatException {
        // Convert default value to string
        String sDefaultvalue = Integer.toString(defaultvalue);
        // Get property value with default
        String sProp = properties.getProperty(name, sDefaultvalue);
        // Parse integer value
        int value = Integer.parseInt(sProp);
        return value;
    }

    /**
     * Sets the integer value of the specified property.
     *
     * @param name The name of the property.
     * @param value The integer value to set.
     */
    public void setIntProperty(String name, int value) {
        // Convert value to string and set property
        String sValue = Integer.toString(value);
        properties.setProperty(name, sValue);
        bModifiedProperties = true;
    }

    /**
     * Retrieves the long value of the specified property.
     *
     * @param name The name of the property.
     * @return The long value of the property.
     * @throws NumberFormatException If the property is not set or not a valid long.
     */
    public long getLongProperty(String name) throws NumberFormatException {
        // Get property value
        String sValue = properties.getProperty(name);
        // Parse long if value exists
        if (sValue != null) {
            return Long.parseLong(sValue.trim());
        }
        // Throw exception if property is missing
        throw new NumberFormatException("Property for " + name + " is not set or not a valid long");
    }

    /**
     * Retrieves the long value of the specified property, returning a default if
     * not found or invalid.
     *
     * @param name The name of the property.
     * @param defaultValue The default value to return if the property is not found.
     * @return The long value of the property, or the default value if not found.
     */
    public long getLongProperty(String name, long defaultValue) {
        // Attempt to get long property
        try {
            return getLongProperty(name);
        } catch (NumberFormatException ex) {
            // Return default if parsing fails
            return defaultValue;
        }
    }

    /**
     * Sets the long value of the specified property.
     *
     * @param name The name of the property.
     * @param value The long value to set.
     */
    public void setLongProperty(String name, long value) {
        // Convert value to string and set property
        properties.setProperty(name, Long.toString(value));
        bModifiedProperties = true;
    }

    /**
     * Retrieves an enumeration of all property names.
     *
     * @return An {@link Enumeration} of property names.
     */
    public Enumeration<String> getPropertyNames() {
        // Return property names as enumeration
        return (Enumeration<String>) properties.propertyNames();
    }

    /**
     * Reads the properties file, preserving comments and handling multi-line values.
     * Supports key-value pairs delimited by {@code =} or {@code :} with non-empty keys
     * and values. Validates file readability, type, and size. Logs warnings for malformed
     * lines (e.g., missing delimiters, empty keys, or empty values) and preserves them
     * in the file content. Limits line and file sizes to prevent resource exhaustion.
     *
     * @throws IOException If an I/O error occurs while reading the file, the file is
     *                     invalid (not a file, not readable, too large), or a line exceeds
     *                     the maximum length.
     */
    private void readPropertiesFile() throws IOException {
        // Clear existing file content
        fileContent.clear();
        // Initialize multi-line value buffer
        StringBuilder multiLineValue = new StringBuilder();
        // Current property key for multi-line values
        String currentKey = null;
        // Flag for multi-line value parsing
        boolean isMultiLine = false;

        // Create file if it doesn't exist
        if (!propertyFile.exists()) {
            logger.info("Properties file does not exist: {}. Creating new empty file.", propertyFile.getAbsolutePath());
            // Ensure parent directory exists
            File parentDir = propertyFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directory for properties file: " + parentDir.getAbsolutePath());
                }
            }
            // Create empty file
            if (!propertyFile.createNewFile()) {
                logger.warn("Properties file already created by another process: {}", propertyFile.getAbsolutePath());
            }
            return; // Proceed with empty properties
        }

        // Check if properties file is valid
        if (propertyFile.isFile() && propertyFile.canRead()) {
            // Validate file size
            if (propertyFile.length() == 0) {
                logger.info("Properties file is empty: {}. Proceeding with empty properties.", propertyFile.getAbsolutePath());
                return; // Empty file is valid
            }
            if (propertyFile.length() > MAX_FILE_SIZE) {
                throw new IOException("Properties file exceeds maximum size (" + MAX_FILE_SIZE + " bytes): " + propertyFile.getAbsolutePath());
            }
            // Read file with buffered reader
            try (BufferedReader reader = Files.newBufferedReader(propertyFile.toPath())) {
                String line;
                // Process each line
                while ((line = reader.readLine()) != null) {
                    // Validate line length
                    if (line.length() > MAX_LINE_LENGTH) {
                        throw new IOException("Line exceeds maximum length (" + MAX_LINE_LENGTH + " characters) in file: " + propertyFile.getAbsolutePath());
                    }
                    // Skip comments and empty lines
                    if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                        if (isMultiLine) {
                            // Handle continuation of multi-line value
                            if (line.endsWith("\\")) {
                                multiLineValue.append(line, 0, line.length() - 1);
                            } else {
                                // End of multi-line value
                                multiLineValue.append(line);
                                // Validate non-empty value
                                String finalValue = multiLineValue.toString().trim();
                                if (!finalValue.isEmpty()) {
                                    properties.setProperty(currentKey, finalValue);
                                } else {
                                    logger.warn("Skipping multi-line property with empty value for key: {}", currentKey);
                                }
                                isMultiLine = false;
                                multiLineValue = new StringBuilder();
                            }
                        } else {
                            // Parse new key-value pair
                            int delimiterIndex = -1;
                            for (int i = 0; i < line.length(); i++) {
                                char c = line.charAt(i);
                                if (c == '=' || c == ':') {
                                    delimiterIndex = i;
                                    break;
                                }
                            }
                            if (delimiterIndex >= 0) {
                                String key = line.substring(0, delimiterIndex).trim();
                                String value = line.substring(delimiterIndex + 1).trim();
                                if (!key.isEmpty() && !value.isEmpty()) {
                                    currentKey = key;
                                    if (line.endsWith("\\")) {
                                        // Start of multi-line value
                                        multiLineValue.append(line.substring(delimiterIndex + 1), 0, line.length() - 1);
                                        isMultiLine = true;
                                    } else {
                                        // Single-line property
                                        properties.setProperty(currentKey, value);
                                    }
                                } else {
                                    // Log warning for invalid key or value
                                    logger.warn("Skipping property with empty key or value: {}", line);
                                }
                            } else {
                                // Log warning for malformed line
                                logger.warn("Skipping malformed property line (missing '=' or ':' delimiter): {}", line);
                            }
                        }
                    }
                    // Store line in file content
                    fileContent.add(line);
                }
                // Check for unclosed multi-line value
                if (isMultiLine) {
                    logger.warn("Incomplete multi-line property for key: {}", currentKey);
                }
            }
        } else {
            throw new IOException("Properties file is invalid or not readable: " + propertyFile.getAbsolutePath());
        }
    }

    /**
     * Saves the properties to the file with a default timestamp header.
     *
     * @throws IOException If an I/O error occurs while writing the file.
     */
    public void saveProperties() throws IOException {
        // Write properties with timestamp
        writePropertiesWithTimestamp();
        bModifiedProperties = false;
        propertiesOriginal = (Properties) properties.clone();
    }

    /**
     * Saves the properties to the file with a custom header.
     *
     * @param header The header comment to include in the file.
     * @throws IOException If an I/O error occurs while writing the file.
     */
    public void saveProperties(String header) throws IOException {
        // Write properties with header and timestamp
        try (BufferedWriter writer = Files.newBufferedWriter(propertyFile.toPath())) {
            // Write custom header if provided
            if (header != null && !header.isEmpty()) {
                writer.write("#" + header);
                writer.newLine();
            }
            // Write current timestamp
            writer.write("#" + getCurrentTimestamp());
            writer.newLine();
            // Write file content and properties
            writeContentAndProperties(writer);
            // Reset modification flag and update original properties
            bModifiedProperties = false;
            propertiesOriginal = (Properties) properties.clone();
        }
    }

    /**
     * Writes properties to the file with a default timestamp for new files.
     *
     * @throws IOException If an I/O error occurs while writing the file.
     */
    private void writePropertiesWithTimestamp() throws IOException {
        // Write properties with buffered writer
        try (BufferedWriter writer = Files.newBufferedWriter(propertyFile.toPath())) {
            // Add timestamp for new files
            if (!propertyFile.exists()) {
                writer.write("#" + getCurrentTimestamp());
                writer.newLine();
            }
            // Write file content and properties
            writeContentAndProperties(writer);
        }
    }

    /**
     * Writes file content and properties, preserving comments and handling new properties.
     *
     * @param writer The {@link BufferedWriter} to write to.
     * @throws IOException If an I/O error occurs while writing.
     */
    private void writeContentAndProperties(BufferedWriter writer) throws IOException {
        // Process existing file content
        for (String line : fileContent) {
            if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                // Extract key from non-comment, non-empty line
                String key = line.split("[=:]", 2)[0].trim();
                if (properties.containsKey(key)) {
                    // Write updated property value
                    String value = properties.getProperty(key);
                    writeMultilineProperty(writer, key, value);
                } else {
                    // Preserve original line if key is not in properties
                    writer.write(line);
                }
            } else {
                // Preserve comments and empty lines
                writer.write(line);
            }
            writer.newLine();
        }
        // Add new properties not in original file
        for (String key : properties.stringPropertyNames()) {
            if (!propertiesOriginal.containsKey(key)) {
                // Write new property
                String value = properties.getProperty(key);
                writeMultilineProperty(writer, key, value);
                writer.newLine();
            }
        }
    }

    /**
     * Generates a timestamp for file headers.
     *
     * @return The current timestamp in the format "EEE MMM dd HH:mm:ss z yyyy".
     */
    private String getCurrentTimestamp() {
        // Format current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        return dateFormat.format(new Date());
    }

    /**
     * Writes a property to the file, handling multi-line values.
     *
     * @param writer The {@link BufferedWriter} to write to.
     * @param key The property key.
     * @param value The property value.
     * @throws IOException If an I/O error occurs while writing.
     */
    private void writeMultilineProperty(BufferedWriter writer, String key, String value) throws IOException {
        // Write property with default line length
        writeMultilineProperty(writer, key, value, MAX_LINE_LENGTH_DEFAULT);
    }

    /**
     * Writes a property to the file, splitting long values into multiple lines.
     *
     * @param writer The {@link BufferedWriter} to write to.
     * @param key The property key.
     * @param value The property value.
     * @param maxLineLength The maximum length of each line.
     * @throws IOException If an I/O error occurs while writing.
     */
    private void writeMultilineProperty(BufferedWriter writer, String key, String value, int maxLineLength) throws IOException {
        // Combine key and value
        String fullProperty = key + "=" + value;
        // Write single line if within length limit
        if (fullProperty.length() <= maxLineLength) {
            writer.write(fullProperty);
        } else {
            // Write key and start multi-line value
            writer.write(key + "=");
            int index = 0;
            // Split value into lines
            while (index < value.length()) {
                // Calculate substring end
                int end = Math.min(index + maxLineLength, value.length());
                String substring = value.substring(index, end);
                // Add indentation for continuation lines
                if (index > 0) {
                    writer.write("    ");
                }
                // Write substring with continuation character if needed
                writer.write(substring + (end < value.length() ? "\\" : ""));
                writer.newLine();
                index = end;
            }
        }
    }

    /**
     * Checks if the properties have been modified since the last load or save.
     * Modifications include added, removed, or changed properties, or a missing
     * properties file.
     *
     * @return {@code true} if properties are modified or the file does not exist,
     *         {@code false} otherwise.
     */
    public boolean isModified() {
        // Check if file does not exist
        if (!propertyFile.exists()) {
            bModifiedProperties = true;
        } else if (propertiesOriginal.size() != properties.size()) {
            // Check if property count differs
            bModifiedProperties = true;
        } else {
            // Check for changed or removed properties
            for (String key : propertiesOriginal.stringPropertyNames()) {
                if (!properties.containsKey(key) || !properties.getProperty(key).equals(propertiesOriginal.getProperty(key))) {
                    bModifiedProperties = true;
                    break;
                }
            }
        }
        return bModifiedProperties;
    }
}