package org.jvmxray.platform.shared.property;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility for creating/reading the Agent properties in a standard
 * Java properties file.  May contain any number of property settings
 * but at a minimum musts contain an Agent ID (e.g, AID) and
 * Category (e.g., CAT) settings.
 *
 * @author Milton Smith
 */
public abstract class XRPropertyBase {

    private Properties properties = null;
    private Properties propertiesOriginal = null;
    private File source = null;
    private volatile boolean bModifiedProperties = false;

    private File jvmxrayHome;
    private File componentHome = null;
    private String componentName;
    private String propertyFileName;

    /**
     * Representation of JVMXRay properties.
     * @param jvmxrayHome Initialized jvmxray home.
     * @param componentName JVMXRay module name.  For example, <code>agent</code>.
     * @param file Name of property file.  For example, <code>jvmxmrayagent.properties</code>.
     */
    public XRPropertyBase(File jvmxrayHome, String componentName, String file) {
        this.jvmxrayHome = jvmxrayHome;
        this.componentName = componentName;
        this.propertyFileName = file;
    }

    /**
     * Called to initialize properties prior to use.
     * @throws IOException Thrown on problems loading properties file.
     */
    public synchronized void init() throws IOException {
        componentHome = new File(jvmxrayHome, componentName);
        componentHome.mkdirs();
        File propertyFile = new File(componentHome, propertyFileName);
        properties = new Properties();
        if (propertyFile.exists()) {
            Reader reader = Files.newBufferedReader(propertyFile.toPath());
            properties.load(reader);
        } else {
            bModifiedProperties = true;
        }
        this.source = propertyFile;
        // Save copy of original to detect property changes.
        propertiesOriginal = (Properties)properties.clone();
    }

    public String getComponentHome() {
        return componentHome.getAbsolutePath();
    }

    public String getComponentName() {
        return componentName;
    }

    /**
     * Flag indicating modified properties.
     * @return true, properties modified or properties file does not exist.  false, properties file exists
     * and in-memory properties same as saved properties.  Note, no test is performed to determine if user
     * manually edits property file on disk.
     */
    public synchronized boolean isModified() {
        if (propertiesOriginal.size() != properties.size()) {
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

    /**
     * Returns named property.
     * @param name Property name.
     * @return Property value.
     */
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
    public synchronized String getProperty(String name, String defaultvalue ) {
        String sValue = properties.getProperty(name, defaultvalue);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        return sValue;
    }


    public synchronized void setProperty(String name, String value) {
        properties.setProperty(name,value);
    }

    /**
     * Return named property.
     * @param name Property name.
     * @return Property value.
     */
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
    public synchronized int getIntProperty(String name, int defaultvalue) throws NumberFormatException {
        String sDefaultvalue = Integer.toString(defaultvalue);
        String sProp = properties.getProperty(name, sDefaultvalue);
        int value = Integer.valueOf(sProp);
        return value;
    }

    public synchronized void setIntProperty(String name, int value ) {
        String sValue = Integer.toString(value);
        properties.setProperty(name,sValue);
    }

    /**
     * Return property names.
     * @return Enumeration of property names.
     */
    public synchronized Enumeration<?> getPropertyNames() {
        return properties.propertyNames();
    }

    /**
     * Saves properties to file identified in source,
     * AgentIdentifyUtil.getInstance(File source).
     * @param header Property file header comment.
     * @throws IOException Problems writing properties file.
     */
    public synchronized void saveProperties(String header) throws IOException {
        Writer writer = Files.newBufferedWriter(source.toPath());
        properties.store(writer, header);
        writer.close();
    }

}
