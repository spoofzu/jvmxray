package org.jvmxray.platform.shared.property;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Utility for creating/reading the Agent properties in a standard
 * Java properties file.  May contain any number of property settings
 * but at a minimum musts contain an Agent ID (e.g, AID) and
 * Category (e.g., CAT) settings.
 *
 * @author Milton Smith
 */
public abstract class XRPropertyBase implements XRIProperties {

    private static final int MAX_LINE_LENGTH_DEFAULT = 80; // Maximum multi-line default length.

    private Properties properties = new Properties();
    private Properties propertiesOriginal = new Properties();
    private final List<String> fileContent = new ArrayList<>();
    private volatile boolean bModifiedProperties = false;

    private File jvmxrayHome;
    private File componentHome;
    private File propertyFile = null;
    private String componentName;
    private String propertyFileName;

    /**
     * Representation of JVMXRay properties.
     * @param jvmxrayHome Initialized jvmxray home.
     * @param componentName JVMXRay module name.  For example, <code>agent</code>.
     * @param propertyFileName Name of property propertyFileName.  For example, <code>jvmxmrayagent.properties</code>.
     */
    public XRPropertyBase(File jvmxrayHome, String componentName, String propertyFileName) {
        this.jvmxrayHome = jvmxrayHome;
        this.componentName = componentName;
        this.propertyFileName = propertyFileName;
    }

    /**
     * Called to initialize properties prior to use.
     * @throws IOException Thrown on problems loading properties file.
     */
    @Override
    public synchronized void init() throws IOException {
        this.componentHome = new File(jvmxrayHome,componentName);
        componentHome.mkdirs();
        propertyFile = new File(componentHome, propertyFileName);
        readPropertiesFile();
        propertiesOriginal.putAll(properties);
    }

    /**
     * Returns named property.
     * @param name Property name.
     * @return Property value.
     */
    @Override
    public synchronized String getProperty(String name) {
        String sValue = properties.getProperty(name);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        return sValue;
    }

    /**
     * Returns named property with a default if none available.
     * @param name Property name.
     * @param defaultvalue Default value to assign.
     * @return Property value.
     */
    @Override
    public synchronized String getProperty(String name, String defaultvalue) {
        String sValue = properties.getProperty(name, defaultvalue);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        return sValue;
    }


    @Override
    public synchronized void setProperty(String name, String value) {
        properties.setProperty(name,value);
    }

    /**
     * Return named property.
     * @param name Property name.
     * @return Property value.
     */
    @Override
    public synchronized int getIntProperty(String name) throws NumberFormatException {
        String sValue = properties.getProperty(name);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        int iValue = Integer.valueOf(sValue);
        return iValue;
    }

    /**
     * Returns named property with a default if none available.
     * @param name Property name.
     * @param defaultvalue Default value to assign.
     * @return Property value.
     */
    @Override
    public synchronized int getIntProperty(String name, int defaultvalue) throws NumberFormatException {
        String sDefaultvalue = Integer.toString(defaultvalue);
        String sProp = properties.getProperty(name, sDefaultvalue);
        int value = Integer.valueOf(sProp);
        return value;
    }

    @Override
    public synchronized void setIntProperty(String name, int value) {
        String sValue = Integer.toString(value);
        properties.setProperty(name,sValue);
    }

    @Override
    public synchronized long getLongProperty(String name) throws NumberFormatException {
        String sValue = properties.getProperty(name);
        if (sValue != null) {
            return Long.parseLong(sValue.trim());
        }
        throw new NumberFormatException("Property for " + name + " is not set or not a valid long");
    }

    @Override
    public synchronized long getLongProperty(String name, long defaultValue) {
        try {
            return getLongProperty(name);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public synchronized void setLongProperty(String name, long value) {
        properties.setProperty(name, Long.toString(value));
    }

    /**
     * Return property names.
     * @return Enumeration of property names.
     */
    @Override
    public synchronized Enumeration<String> getPropertyNames() {
        return (Enumeration<String>)properties.propertyNames();
    }

    private void readPropertiesFile() throws IOException {
        fileContent.clear();
        StringBuilder multiLineValue = new StringBuilder();
        String currentKey = null;
        boolean isMultiLine = false;
        if (propertyFile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(propertyFile.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                        if (isMultiLine) {
                            if (line.endsWith("\\")) {
                                multiLineValue.append(line, 0, line.length() - 1);
                            } else {
                                multiLineValue.append(line);
                                properties.setProperty(currentKey, multiLineValue.toString());
                                isMultiLine = false;
                                multiLineValue = new StringBuilder();
                            }
                        } else {
                            String[] keyValue = line.split("=", 2);
                            if (keyValue.length == 2) {
                                currentKey = keyValue[0].trim();
                                if (line.endsWith("\\")) {
                                    multiLineValue.append(keyValue[1], 0, keyValue[1].length() - 1);
                                    isMultiLine = true;
                                } else {
                                    properties.setProperty(currentKey, keyValue[1]);
                                }
                            }
                        }
                    }
                    fileContent.add(line);
                }
            }
        }
    }

    public synchronized void saveProperties() throws IOException {
        writePropertiesWithTimestamp();
    }

    public synchronized void saveProperties(String header) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(propertyFile.toPath())) {
            // Write the header and current timestamp
            if (header != null && !header.isEmpty()) {
                writer.write("#" + header);
                writer.newLine();
            }
            writer.write("#" + getCurrentTimestamp());
            writer.newLine();
            // Write the rest of the file content and properties
            writeContentAndProperties(writer);
            bModifiedProperties = false;
            propertiesOriginal = (Properties)properties.clone();
        }
    }

    private void writePropertiesWithTimestamp() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(propertyFile.toPath())) {
            if (!propertyFile.exists()) {
                // Write the current timestamp for a new file
                writer.write("#" + getCurrentTimestamp());
                writer.newLine();
            }

            // Write the rest of the file content and properties
            writeContentAndProperties(writer);
        }
    }

    private void writeContentAndProperties(BufferedWriter writer) throws IOException {
        for (String line : fileContent) {
            if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                String key = line.split("=", 2)[0].trim();
                if (properties.containsKey(key)) {
                    String value = properties.getProperty(key);
                    writeMultilineProperty(writer, key, value);
                } else {
                    writer.write(line); // Original line if key not found
                }
            } else {
                writer.write(line); // Comment or empty line as is
            }
            writer.newLine();
        }
        // Handle new properties not in the original file
        for (String key : properties.stringPropertyNames()) {
            if (!propertiesOriginal.containsKey(key)) {
                // This is a new property, write it to the file
                String value = properties.getProperty(key);
                writeMultilineProperty(writer, key, value);
                writer.newLine();
            }
        }
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        return dateFormat.format(new Date());
    }


    private void writeMultilineProperty(BufferedWriter writer, String key, String value) throws IOException {
        writeMultilineProperty(writer, key, value, MAX_LINE_LENGTH_DEFAULT);
    }

    private void writeMultilineProperty(BufferedWriter writer, String key, String value, int maxLineLength) throws IOException {
        String fullProperty = key + "=" + value;
        // Check if the property (including key and value) exceeds the maximum line length
        if (fullProperty.length() <= maxLineLength) {
            writer.write(fullProperty);
        } else {
            // Write the key and start the multi-line value
            writer.write(key + "=");
            int index = 0;
            while (index < value.length()) {
                // Determine the end index for the substring
                int end = Math.min(index + maxLineLength, value.length());
                String substring = value.substring(index, end);
                // If it's not the first line, indent the line for readability
                if (index > 0) {
                    writer.write("    "); // Indentation with spaces
                }
                // Write the substring and, if not the last part, add a line continuation character
                writer.write(substring + (end < value.length() ? "\\" : ""));
                // Move to the next line
                writer.newLine();
                index = end;
            }
        }
    }

    /**
     * Flag indicating modified properties.
     * @return true, returns true if any of the following are true, properties keypairs added, removed,
     *   or properties file does not exist.  false, for everything else.
     */
    @Override
    public synchronized boolean isModified() {
        if( !propertyFile.exists() ) {
            bModifiedProperties = true;
        } else if  (propertiesOriginal.size() != properties.size()) {
            bModifiedProperties = true;
        } else {
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
