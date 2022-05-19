package org.jvmxray.util;

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

/**
 * Helps manage Agent and Server property settings.
 * @author Milton Smith
 */
public class PropertyUtil {

	//  Agent configuration properties and default values.
	public static final String SYS_PROP_AGENT_IDENTITY = "jvmxray.agent.identity";
	public static final String SYS_PROP_AGENT_CONFIG_URL = "jvmxray.agent.configuration";
	public static final String SYS_PROP_AGENT_CONFIG_DEFAULT = "/jvmxrayclient.properties";
	public static final String SYS_PROP_AGENT_STATUS_LOGFILE_DEFAULT = "jvmxraystatus.log";
	public static final String SYS_PROP_AGENT_EVENT_LOGFILE_DEFAULT = "jvmxrayevent.log";
	public static final String SYS_PROP_AGENT_BASE_DIR = "jvmxray.agent.base.directory";
	public static final String SYS_PROP_AGENT_STATUS_LOGFILE_FN = "jvmxray.agent.status.filename";
	public static final String SYS_PROP_AGENT_EVENT_LOGFILE_FN = "jvmxray.agent.event.filename";
	public static final String SYS_PROP_AGENT_TOPICNAME = "jvmxray.agent.processor.jms.topicname";
	public static final String SYS_PROP_AGENT_CONNECTION_FACTORY = "jvmxray.agent.processor.jms.connectionfactoryname";
	public static final String SYS_PROP_AGENT_BROKER_URL="jvmxray.agent.processor.jms.broker.url";

	//  Server configuration properties and default values.
	public static final String CONF_PROP_SERVER_IDENTITY_FILE ="jvmxray.agent.id.file";
	public static final String CONF_PROP_SERVER_DIRECTORY = "jvmxray.server.directory";
	public static final String SYS_PROP_SERVER_CONFIG_DEFAULT = "/jvmxrayserver.properties";
	public static final String SYS_PROP_SERVER_TOPICNAME = "jvmxray.server.processor.jms.topicname";
	public static final String SYS_PROP_SERVER_CONNECTION_FACTORY = "jvmxray.server.processor.jms.connectionfactoryname";
	public static final String SYS_PROP_SERVER_BROKER_URL="jvmxray.server.processor.jms.broker.url";
	public static final String SYS_PROP_SERVER_THREAD_POOL_MAX ="jvmxray.server.thread.pool.max";
	public static final String SYS_PROP_SERVER_POOL_SLEEP="jvmxray.server.thread.pool.sleep";
	public static final String CONF_PROP_WEBHOOK_EVENT_END_POINT = "jvmxray.webhook.event.endpoint";
	public static final String CONF_PROP_EVENT_SERVER_DIRECTORY = "jvmxray.server.directory";
	public static final String CONF_PROP_SERVER_HDFS_SERVER = "jvmxray.server.hdfs";
	public static final String CONF_PROP_SERVER_HDFS_FILENAME = "jvmxray.server.hdfs.path.file.name";
	public static final String CONF_PROP_SERVER_HDFS_FANOUT = "jvmxray.server.hdfs.path.file.fanout";

	private LiteLogger ltlogger = LiteLogger.getLoggerinstance();
	private static PropertyUtil pu;
	private static Properties modprops = null;
	
	private PropertyUtil() {}
	
	public static final synchronized PropertyUtil getInstance(String propertyfile) {
		if ( pu == null ) {
			pu = new PropertyUtil();
		}
		// Initialize properties
		try {
			modprops = pu.initProperties(propertyfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pu;
	}

	// Saves cloud identity to the local filesystem.
	public final void saveAgentId(String id ) throws IOException {
		// Get the server identity file to use on local file system.
		String basedir = modprops.getProperty(SYS_PROP_AGENT_BASE_DIR);
		String idfile = modprops.getProperty(CONF_PROP_SERVER_IDENTITY_FILE);
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
	public final String getAgentId() throws IOException {
		// Get the server identity file to use on local file system.
		String basedir = modprops.getProperty(SYS_PROP_AGENT_BASE_DIR);
		String idfile = modprops.getProperty(CONF_PROP_SERVER_IDENTITY_FILE);
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
	 * Any system or environment variables are resolved at runtime prior to returning the
	 * properties to the caller.
	 * @return Properties file.
	 * @throws IOException
	 */
	public final Properties getProperties() {
		return modprops;
	}

	// Initialize client/server properties.
	private final Properties initProperties(String propertyfile) throws IOException {
		Properties p = new Properties();
    	InputStream in = null;
    	try {
    		// NOTE: command line property, not from properties file.
        	String surl = System.getProperty(SYS_PROP_AGENT_CONFIG_URL, propertyfile);
        	try {
			//	ltlogger.debug("PropertyUtil.getJVMXRayProperties(): raw url="+surl);
	        	URL url = new URL(surl);
				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): properties loaded from url="+url.toString());
	   	     	URLConnection con = url.openConnection();
	   	     	// NOTE: Cannot ready stream or it's altered.  Any reading the stream alters it
				// so that it cannot be used to deserialize a properties table.  Mark/reset
				// should allows stream to be read again but it does not work.
				in = new BufferedInputStream(con.getInputStream());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content length="+con.getContentLength());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content type="+con.getContentType());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): content encoding="+con.getContentEncoding());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): date="+con.getDate());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): expiration="+con.getExpiration());
//				ltlogger.debug("PropertyUtil.getJVMXRayProperties(): last modified="+con.getLastModified());
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
        	if(in!=null) {
				p.load(in);
			} else {
        		ltlogger.debug("PropertyUtil.getJVMXRayProperties(String): Unable to initialize properties.  Stream is null.");
			}
//        	StringBuffer buff = new StringBuffer();
//        	buff.append("PropertyUtil.getJVMXRayProperties(): PropertyTable[");
//        	if( p==null ) {
//        		buff.append("<null>]");
//			}else{
//        		buff.append(p.hashCode());
//        		buff.append("]");
//        		buff.append(" keys/value pair count="+p.size());
//			}
//			ltlogger.debug(buff.toString());
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
    	// modprops = resolveJVMXRayProperties(p);
//		ltlogger.debug("PropertyUtil.getJVMXRayProperties(): Returning new properties. Properties("+modprops.hashCode()+")");
//		last_refresh = System.currentTimeMillis();
    	return resolveJVMXRayProperties(p);
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
				ltlogger.debug("Configuration variable resolved.  original="+oprop+" new="+nprop);//logger.info
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
