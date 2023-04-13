package org.jvmxray.agent.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.rmi.dgc.VMID;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility for creating Agent identity property file.  Each agent
 * requires a unique identity in the cloud.  The identity will be
 * used when persisting event data and interacting with servers.
 * @author Milton Smith
 */
public class AgentIdentityUtil  {

    public static final String PROPERTY_AID = "aid";
    public static final String PROPERTY_CATEGORY = "cat";
    private static final String CATEGORY_DEFAULT = "unit-test";
    private static AgentIdentityUtil ai = null;
    private Properties properties = new Properties();
    private File source = null;

    /**
     * Retrives the agent identity properties file.  If one does not exist it
     * creates one in mem and returns to the caller.  To persist the state
     * of the current properties call, saveProperties().
     * @param source
     * @return
     * @throws IOException
     */
    public static synchronized AgentIdentityUtil getInstance(File source) throws IOException {
        if (ai == null) {
            ai = new AgentIdentityUtil();
            ai.source = source;
            if (source.exists()) {
                Reader reader = Files.newBufferedReader(source.toPath());
                ai.properties.load(reader);
            } else {
                // Identity file does exist.  Create one with initial defaults.
                String vmid = new VMID().toString();
                ai.setStringProperty("aid", formatVMID(vmid));
                ai.setStringProperty("cat", CATEGORY_DEFAULT);
            }
        }
        return ai;
    }

    /**
     * Returns named property.
     * @param name Property name.
     * @return Property value.
     */
    public synchronized String getStringProperty(String name) {
        String sValue = properties.getProperty(name);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        return sValue;
    }

    public synchronized void setStringProperty(String name, String value) {
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

    public synchronized void setIntProperty(String name, int value ) {
        String sValue = Integer.toString(value);
        properties.setProperty(name,sValue);
    }

    /**
     * Returns named property with a default if none available.
     * @param name Property name.
     * @param defaultvalue Default value to assign.
     * @return Property value.
     */
    public synchronized String getStringProperty(String name, String defaultvalue ) {
        String sValue = properties.getProperty(name, defaultvalue);
        sValue = (sValue!=null) ? sValue.trim() : sValue;
        return sValue;
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
     * @throws IOException Problems writing properties file.
     */
    public synchronized void saveProperties() throws IOException {
        Writer writer = Files.newBufferedWriter(source.toPath());
        properties.store(writer, "JVMXRay Unique Agent Identity");
        writer.close();
    }

    /**
     * Reformat VMID's
     * @param vmid String to filter.  Any character outside the set
     * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" is replaced with
     * an - symbol.  A simpler VMID id format suitable for use with file systems, etc.
     * @return Filtered String.
     */
    private static final String formatVMID(String vmid) {
        String result = vmid.replace(":-","-");
        result = result.replace(":","-");
        return result;
    }

    public static final String getVMID() {
        return formatVMID(new VMID().toString());
    }

}
