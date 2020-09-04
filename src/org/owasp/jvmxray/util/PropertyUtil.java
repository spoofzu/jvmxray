package org.owasp.jvmxray.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

public class PropertyUtil {
	
	/**
	 * System property name that specifies the URL to load the jvmxray properties
	 */
	public static final String SYS_PROP_CONFIG_URL = "jvmxray.configuration";
	public static final String SYS_PROP_DEFAULT = "/jvmxray.properties";
	
	/**
	 * System property name of the security manager to use, <code>nullsecuritymanager.securitymanager</code>
	 * If the property is unspecified then no security manager is used, this is the default.  If a 
	 * specified security manager is provided, NullSecurityManager will pass-thru it's calls.  To use
	 * Java's default SecurityManager specify it's fully qualified.<br/>
	 * THIS PROPERTY IS NOT SUPPORTED AT THIS TIME
	 * class name, <code>java.lang.SecurityManager</code>.
	 */
	public static final String SYS_PROP_SECURITY_MANAGER = "jvmxray.securitymanager";
	
	/**
	 * Server identity.  System property providing the globally unique identity for the application.  Useful for
	 * identifying the specific instance of a cloud application that generated a particular message.
	 */
	public static final String SYS_PROP_EVENT_SERV_IDENTITY = "jvmxray.event.nullsecuritymanager.server.identity";
	
	/**
	 * File containing server identity on local file system.
	 */
	public static final String CONF_PROP_SERV_IDENTITY_FILE="jvmxray.event.nullsecuritymanager.id.file";
	
	/**
	 * Filename property. JVMXRay event spool.
	 */
	public static final String CONF_PROP_EVENT_SPOOL_FILE = "jvmxray.event.nullsecuritymanager.spool.filename";
	
	/**
	 * Directory property. JVMXRay base directory.
	 */
	public static final String CONF_PROP_EVENT_DIRECTORY = "jvmxray.event.nullsecuritymanager.directory";
	
	/**
	 * Max spool file size in bytes
	 */
	public static final String CONF_PROP_EVENT_SPOOL_LIMIT = "jvmxray.event.nullsecuritymanager.spool.limit";
	
	/**
	 * Filename property. Optional property for the JVMXRayEventAggregator and
	 * described in the jvmxray.properties file.  Name of the output file from the
	 * aggregated events.
	 */
	public static final String CONF_PROP_EVENT_AGG_FILE = "jvmxray.adaptor.jvmxrayeventaggregator.filename";
	
	/**
	 * Interval in seconds. Optional property for the JVMXRayEventAggregator and
	 * described in the jvmxray.properties file.  Interval in seconds to update
	 * the aggregated events file.
	 */
	public static final String CONF_PROP_EVENT_AGG_FILE_INTERVAL = "jvmxray.adaptor.jvmxrayeventaggregator.fileupdateinterval";
	
	
	/**
	 * Maximum time delay to wait during initialization before failure.
	 */
	public static final String CONF_PROP_MAXWAIT_INITIALIZATION = "jvmxray.event.nullsecuritymanager.server.maxwait.initialization";
	
	public static final String CONF_PROP_WEBHOOK_URL= "jvmxray.event.webhook.target";
	
	private static PropertyUtil pu;	
	
	private PropertyUtil() {}
	
	public static final synchronized PropertyUtil getInstance() {
		if ( pu == null ) {
			pu = new PropertyUtil();
		}
		return pu;
	}

	// Saves cloud identity to the local filesystem.
	public final void saveServerId( String id ) throws MalformedURLException, IOException {
		
		// Get the server identity file to use on local file system.
		Properties p = PropertyUtil.getInstance().getJVMXRayProperties();
		String basedir = p.getProperty(CONF_PROP_EVENT_DIRECTORY);
		String idfile = p.getProperty(CONF_PROP_SERV_IDENTITY_FILE);
		File f = new File(basedir, idfile);	
		
		// If a file does not exist then create one.  If one exists, then skip and return.
		// To force a new id creation simply delete a file, a new id will be created.
		if( f.exists() ) return;
		Properties np = new Properties();
		Writer propWriter = Files.newBufferedWriter(f.toPath());
		np.put( "id", id );
		np.store(propWriter, "JVMXRay Unique Server Identity");
		propWriter.close();
		
	}
	
	// Return the servers cloud identity from the local file system.
	public final String getServerId() throws MalformedURLException, IOException {
		
		// Get the server identity file to use on local file system.
		Properties p = PropertyUtil.getInstance().getJVMXRayProperties();	
		String basedir = p.getProperty(CONF_PROP_EVENT_DIRECTORY);
		String idfile = p.getProperty(CONF_PROP_SERV_IDENTITY_FILE);
		File f = new File(basedir, idfile);	
		
		// If a file does not exist then create one.  If one exists, then skip and return.
		// To force a new id creation simply delete a file, a new id will be created.
		String id = "";
		Properties np = new Properties();
		if( f.exists() ) {
			Reader propReader = Files.newBufferedReader(f.toPath());
			np.load(propReader);
			id = np.getProperty("id");
			propReader.close();
			
		} else {
			throw new IOException( "Server identity is unavailable.  f="+f.toString() );
		}
		
		return id;
		
	}
	
	/**
	 * Load jvmproperties.  Attempt to load from URL, if that fails, then load from resources.
	 * @return Properties file.
	 * @throws IOException
	 */
	public final Properties getJVMXRayProperties() throws IOException, MalformedURLException {
		
		Properties p = new Properties();
    	InputStream in = null;
    	
    	try {    		
        	String surl = System.getProperty(SYS_PROP_CONFIG_URL, SYS_PROP_DEFAULT);
        	try {
	        	URL url = new URL(surl);
	   	     	HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
	   	     	in = new BufferedInputStream(con.getInputStream());
        	} catch( MalformedURLException e ) {
        		// If can't load fm url check to see if this is the default
        		// configuration.  If so, load the default.  Otherwise, it's
        		// likely user mistyped the URL so we throw exception.
        		if( !surl.equals(SYS_PROP_DEFAULT) ) {
            		e.printStackTrace();
            		throw e;
        		} else {
        			in = PropertyUtil.class.getResourceAsStream(surl);	
        		}
        	}
	    	p.load(in);
    		
    	} finally {
       	 if( in != null )
			try {
				in.close();
				in = null;
			} catch (IOException e) {}
    	}
    	
    	return p;
	
	}
	
	/**
	 * Reformat VMID's 
	 * @param value String to filter.  Any character outside the set 
	 * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" is replaced with
	 * an - symbol.  A simpler VMID id format suitable for use with file systems, etc.
	 * @return Filtered String.
	 */
	public static final String formatVMID(String vmid) {
		String result = vmid.replace(":-","-");
		result = result.replace(":","-");
		return result;
	}
	
	
}
