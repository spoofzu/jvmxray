package org.owasp.jvmxray.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Properties;


public class PropertyUtil {
	
	//TODOMS: Once/if we decide to dynamically update configuration on server this class will need to be 
	//        improved to include TTL to track out of date properties.  They will also need to be made
	//        thread safe since properties can be updated at any moment on the server.  Including this
	//        reminder in the code for now.
	//
	
	/**
	 * System property name that specifies the URL to load the jvmxray properties
	 */
	public static final String SYS_PROP_CONFIG_URL = "jvmxray.configuration";
	public static final String SYS_PROP_CLIENT_DEFAULT = "/jvmxrayclient.properties";
	public static final String SYS_PROP_SERVER_DEFAULT = "/jvmxrayserver.properties";
	
	/**
	 * System property name of the security manager to use, <code>nullsecuritymanager.securitymanager</code>
	 * If the property is unspecified then no security manager is used, this is the default.  If a 
	 * specified security manager is provided, NullSecurityManager will pass-thru it's calls.  To use
	 * Java's default SecurityManager specify it's fully qualified.<br/>
	 * THIS PROPERTY IS NOT SUPPORTED AT THIS TIME
	 * class name, <code>java.lang.SecurityManager</code>.
	 */
	//public static final String SYS_PROP_SECURITY_MANAGER = "jvmxray.securitymanager";
	
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
	 * Directory property. JVMXRay event spool.
	 */
	public static final String CONF_PROP_EVENT_SPOOL_DIRECTORY = "jvmxray.event.nullsecuritymanager.spool.directory";

	/**
	 * Directory property. JVMXRay base directory.
	 */
	public static final String CONF_PROP_EVENT_DIRECTORY = "jvmxray.event.nullsecuritymanager.directory";
	
	///**
	// * Interval in seconds. Optional property for the JVMXRayEventAggregator and
	// * described in the jvmxrayclient.properties file.  Interval in seconds to update
	// * the aggregated events file.
	// */
	//public static final String CONF_PROP_EVENT_AGG_FILE_INTERVAL = "jvmxray.adaptor.jvmxrayeventaggregator.fileupdateinterval";
	
	/**
	 * Maximum time delay to wait during initialization before failure.
	 */
	public static final String CONF_PROP_MAXWAIT_INITIALIZATION = "jvmxray.event.nullsecuritymanager.server.maxwait.initialization";
	
	public static final String CONF_PROP_WEBHOOK_URL= "jvmxray.event.webhook.target";

	private LiteLogger ltlogger = LiteLogger.getLoggerinstance();

	private static PropertyUtil pu;

	// Cache properties for 60-sec.
	private long TTL = 60* 1000;

	private long last_refresh = 0;

	private Properties modprops = null;
	
	/** Get logger instance. */
	//private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.util.PropertyUtil");
	
	private PropertyUtil() {}
	
	public static final synchronized PropertyUtil getInstance() {
		if ( pu == null ) {
			pu = new PropertyUtil();
		}
		return pu;
	}

	// Saves cloud identity to the local filesystem.
	public final synchronized void saveServerId( String id ) throws MalformedURLException, IOException {
		
		// Get the server identity file to use on local file system.
		Properties p = getJVMXRayProperties(PropertyUtil.SYS_PROP_CLIENT_DEFAULT);
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
		Properties p = getJVMXRayProperties(PropertyUtil.SYS_PROP_CLIENT_DEFAULT);
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

	public final synchronized Properties getClientProperties() throws IOException, MalformedURLException {
		return getJVMXRayProperties(SYS_PROP_CLIENT_DEFAULT);
	}

	public final synchronized Properties getServerProperties() throws IOException, MalformedURLException {
		return getJVMXRayProperties(SYS_PROP_SERVER_DEFAULT);
	}

	/**
	 * Load jvmproperties.  Attempt to load from URL, if that fails, then load from resources.
	 * Any system or environment variables are resolved at runtime prior to returning the
	 * properties to the caller.
	 * @return Properties file.
	 * @throws IOException
	 */
	private final Properties getJVMXRayProperties(String propertyfile) throws IOException, MalformedURLException {
		// Return cached properties if not expired.
		long current_timestamp = System.currentTimeMillis();
		if( modprops != null && ((last_refresh + TTL)>current_timestamp)) {
			ltlogger.info("PropertyUtil.getJVMXRayProperties(): returning cashed properties. Properties("+modprops.hashCode()+")");
			return modprops;
		}
		Properties p = new Properties();
    	InputStream in = null;
    	try {    		
        	String surl = System.getProperty(SYS_PROP_CONFIG_URL, propertyfile);
        	try {
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): raw url="+surl);
	        	URL url = new URL(surl);
				ltlogger.info("PropertyUtil.getJVMXRayProperties(): properties loaded from url="+url.toString());
	   	     	URLConnection con = (URLConnection)url.openConnection();
	   	     	// NOTE: Cannot ready stream or it's altered.  Any reading the stream alters it
				// so that it cannot be used to deserialize a properties table.  Mark/reset
				// should allows stream to be read again but it does not work.
				in = new BufferedInputStream(con.getInputStream());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content length="+con.getContentLength());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content type="+con.getContentType());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content encoding="+con.getContentEncoding());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): date="+con.getDate());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): expiration="+con.getExpiration());
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): last modified="+con.getLastModified());
        	} catch( MalformedURLException e ) {
        		// If can't load fm url check to see if this is the default
        		// configuration.  If so, load the default.  Otherwise, it's
        		// likely user mistyped the URL so we throw exception.
        		if( !surl.equals(propertyfile) ) {
            		throw e;
        		} else {
        			in = PropertyUtil.class.getResourceAsStream(surl);	
        		}
        	}
	    	p.load(in);
        	StringBuffer buff = new StringBuffer();
        	buff.append("PropertyUtil.getJVMXRayProperties(): PropertyTable[");
        	if( p==null ) {
        		buff.append("<null>]");
			}else{
        		buff.append(p.hashCode());
        		buff.append("]");
        		buff.append(" keys/value pair count="+p.size());
			}
			ltlogger.debug(buff.toString());
    	} finally {
       		if( in != null ) {
				try {
					in.close();
					in = null;
				} catch (IOException e) {
				}
			}
    	}
    	// Save properties with variables resolved and update cache timestamp.
    	modprops = resolveJVMXRayProperties(p);
		ltlogger.info("PropertyUtil.getJVMXRayProperties(): Returning new properties. Properties("+modprops.hashCode()+")");
		current_timestamp= System.currentTimeMillis();
    	return modprops;
	}
	
	// Process any unresolved variables.  Return new properties table to the
	// caller.  For System Property settings only: If there is an
	// existing setting, we overwrite it's value.  If the setting does not
	// exist, it's created.  Note, resolved or updated shell variables are
	// not propagated back to the shell.
	private final Properties resolveJVMXRayProperties(Properties op) {
		Properties np = new Properties();
		Enumeration<String> e = (Enumeration<String>)op.propertyNames();
		while( e.hasMoreElements() ) {
			// Copy all properties (with or without variables).
			String key = e.nextElement();
			String value = op.getProperty(key);
			np.setProperty(key,value);			
			// Resolve sys and env variables.
			String nsKey = varResolve(key);
			String nsValue = varResolve(value);
			// If key has a variable, update it.
			if(!nsKey.equals(key) || !nsValue.equals(value)) {
				String oprop = key + "=" + value;
				String nprop = nsKey + "=" + nsValue;
				// When a variable is resolved, apply it.
				System.setProperty(nsKey,nsValue);
				np.setProperty(nsKey,nsValue);
				System.out.println("Configuration variable resolved.  original="+oprop+" new="+nprop);//logger.info
			}
			
		}
		return np;
 	}
	
	
	// String index of first character of match.  -1 if no match.
	private final int varStartIndex( String value, int offset ) {
		int idx = -1;
		if( value != null ) {
			if( value.length()>0) {
				if( offset>value.length() || offset<0) throw new RuntimeException("Bad offset.");
				idx = value.indexOf("${sys:", offset);
				if( idx < 0 ) {
					idx = value.indexOf("${env:"); 
				}
			}
		}
		return idx;
	}
	
	// String index of first character of end match.  -1 if no match.
	// Call varStartIndex() use offset when calling varEndIndex() to
	// ensure match ends the target tag.
	private final int varEndIndex( String value, int offset ) {
		int idx = -1;
		if( value != null ) {
			if( value.length()>0) {
				idx = value.indexOf("}", offset);
			}
		}
		return idx;
	}
	
	// Reentrant tag resolver.  Resolves system properties or environment
	// variables used in jvmxray configuration.
	private final String varResolve( String value ) {
		if( value == null ) return null;
		StringBuilder build = new StringBuilder(250);
		int sidx = varStartIndex( value, 0);
		int eidx = varEndIndex( value, 0);
		if( sidx > -1 ) {
			String type = value.substring(sidx+2,sidx+5);
			String key = value.substring(sidx+6, eidx);
			boolean isSystemVarType = type.equals("sys");
			String rval = (isSystemVarType) ? System.getProperty(key): System.getenv(key);
			build.append(value.substring(0,sidx));
			build.append(rval);
			build.append(value.substring(eidx+1,value.length()));
		} else {
			build.append(value);
		}
		String result = build.toString();
		return (varStartIndex(result,0) > -1) ? varResolve(result) : result;
	}
	
	/**
	 * Reformat VMID's 
	 * @param vmid String to filter.  Any character outside the set
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
