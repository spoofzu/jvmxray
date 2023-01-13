package org.jvmxray.agent.util;

import org.jvmxray.agent.exception.JVMXRayRuntimeException;

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
        AgentIdentityUtil result = null;
        if( ai == null ) {
            ai = new AgentIdentityUtil();
            ai.source = source;
            if( source.exists() ) {
                Reader reader = Files.newBufferedReader(source.toPath());
                ai.properties.load(reader);
            } else {
                // Identity file does exist.  Create one with initial defaults.
                String vmid = new VMID().toString();
                ai.properties.put("aid",formatVMID(vmid));
                ai.properties.put("cat",CATEGORY_DEFAULT);
                ai.properties.put("bootstrapurl","http://localhost:9123/api/config/");
            }
        }
        result = ai;
        return result;
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

//    // Saves agent cloud identity to the local filesystem.
//    public final void saveAgentId(File file, String id) throws IOException {
//        // Get the server identity file to use on local file system.
////        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
////        String basedir = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
////        String idfile = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_IDENTITY_FILE);
////        String basedir = "/Users/milton/";
////        String idfile = "jvmxrayinstanceid.properties";
//       // File f = new File(basedir, idfile);
//        // If a file does not exist then create one.  If one exists, then skip and return.
//        // To force a new id creation simply delete a file, a new id will be created.
//        if( file.exists() ) return;
//        Properties np = new Properties();
//        Writer propWriter = Files.newBufferedWriter(file.toPath());
//        np.put( "id", id );
//        np.put( "cat", CATEGORY_DEFAULT );
//        np.store(propWriter, "JVMXRay Unique Agent Identity");
//        propWriter.close();
//    }

//    // Returns agent cloud identity from the local file system.
//    public final String getAgentId(File file) throws IOException {
//        // Get the server identity file to use on local file system.
////        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
////        String basedir = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
////        String idfile = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_IDENTITY_FILE);
////        String basedir = "/Users/milton/";
////        String idfile = "jvmxrayinstanceid.properties";
// //       File f = new File(basedir, idfile);
//        // If a file does not exist then create one.  If one exists, then skip and return.
//        // To force a new id creation simply delete a file, a new id will be created.
//        String id = "";
//        Properties np = new Properties();
//        if( file.exists() ) {
//            Reader propReader = Files.newBufferedReader(file.toPath());
//            np.load(propReader);
//            id = np.getProperty("id");
//            propReader.close();
//        } else {
//            throw new IOException( "Agent identity is unavailable.  file="+file.toString() );
//        }
//        return id;
//    }

//    // Returns agent cloud category from the local file system.
//    public final String getCategory(File file) throws IOException {
//        // Get the server identity file to use on local file system.
////        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
////        String basedir = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
////        String idfile = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_IDENTITY_FILE);
////        String basedir = "/Users/milton/";
////        String idfile = "jvmxrayinstanceid.properties";
////        File f = new File(basedir, idfile);
//        // If a file does not exist then create one.  If one exists, then skip and return.
//        // To force a new id creation simply delete a file, a new id will be created.
//        String cat = "";
//        Properties np = new Properties();
//        if( file.exists() ) {
//            Reader propReader = Files.newBufferedReader(file.toPath());
//            np.load(propReader);
//            cat = np.getProperty("cat");
//            propReader.close();
//        } else {
//            throw new IOException( "Agent category is unavailable.  file="+file.toString() );
//        }
//        return cat;
//    }

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

}
