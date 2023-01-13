package org.jvmxray.agent.util;

import java.io.Writer;
import java.net.*;
import java.nio.file.Files;
import java.util.TimerTask;
import org.jvmxray.agent.exception.JVMXRayRuntimeException;

import java.util.HashMap;
import java.util.Timer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Helps manage Agent and Server property settings.
 * @author Milton Smith
 */
public class PropertyUtil extends TimerTask {

	// Agent/server properties
	public static final String SYS_PROP_CONFIG_URL = "jvmxray.configuration.url";
	public static final String SYS_PROP_REFRESH_LAST_TIMESTAMP = "jvmxray.property.refresh.last.timestamp";
	public static final String SYS_PROP_PROP_REFRESH_INTERVAL = "jvmxray.property.refresh.interval";
	public static final String SYS_PROP_REST_WEBHOOK_EVENT = "jvmxray.rest.webhook.event";
	public static final String SYS_PROP_REST_WEBHOOK_CONFIG = "jvmxray.rest.webhook.config";

	//  Agent configuration properties and default values.
	public static final String SYS_PROP_AGENT_IDENTITY_FILE = "jvmxray.agent.id.file";
	public static final String SYS_PROP_AGENT_CONFIG_DEFAULT = "/jvmxrayagent.properties";
	public static final String SYS_PROP_AGENT_STATUS_LOGFILE_DEFAULT = "jvmxraystatus.log";
	public static final String SYS_PROP_AGENT_EVENT_LOGFILE_DEFAULT = "jvmxrayevent.log";
	public static final String SYS_PROP_AGENT_BASE_DIR = "jvmxray.agent.base.directory";
	public static final String SYS_PROP_AGENT_STATUS_LOGFILE_FN = "jvmxray.agent.status.filename";
	public static final String SYS_PROP_AGENT_EVENT_LOGFILE_FN = "jvmxray.agent.event.filename";

	//  Server configuration properties and default values.
	public static final String SYS_PROP_SERVER_CONFIG_DEFAULT = "/jvmxrayserver.properties";
	public static final String SYS_PROP_SERVER_IP_HOST ="jvmxray.server.ip.host";
	public static final String SYS_PROP_SERVER_IP_PORT = "jvmxray.server.ip.port";
	public static final String SYS_PROP_SERVER_BASE_DIRECTORY = "jvmxray.server.base.directory";
	public static final String SYS_PROP_SERVER_JDBC_DRIVER = "jvmxray.server.jdbc.driver";
	public static final String SYS_PROP_SERVER_JDBC_CONNECTION = "jvmxray.server.jdbc.connection";
	public static final String SYS_PROP_SERVER_JDBC_USERNAME = "jvmxray.server.jdbc.username";
	public static final String SYS_PROP_SERVER_JDBC_PASSWORD = "jvmxray.server.jdbc.password";

	// One unique map entry per property file/url source.
	private static final HashMap<String,PropertyUtil> propertiesmap = new HashMap<String,PropertyUtil>();
	// Refresh interval timer.
	Timer timer;
	// Raw source properties.  Any system properties have not been resolved.
	Properties srcprops = new Properties();
	// Internal property map.
	Properties modprops = new Properties();
	// Property source.
	String source = "";
	// Timestap of last property reload
	long ts=0;
	// Unique application id
	String aid = "";
	// Category.  For example, ci-test, ci-prod, etc.
	String cat = "";

	PropertyUtil() {
		this("PropertyUtil");
	}
	PropertyUtil( String thread_name ) {
		timer = new Timer(thread_name);
	}

	@Override
	public void run() {
		refreshProperties();
	}

	/**
	 * Return instance of PropertyUtil.  This API is primarily intended for
	 * Agents which need a configuration which depends on the agents aid
	 * and category properties.  If <code>jvmxray.configuration.url</code>
	 * property is specified it takes precence, for instance, when
	 * initialing agent properties from a remote server.  Example,
	 * -Djvmxray.configuration.url="http://localhost:9123/api/config/"
	 *
	 * @param aid Unique application instance id.  Category
	 *            may not be granular enough for exceptional cases.  It's
	 * @param category Category name.  For example, ci-prod, ci-test, etc.
	 *                 The category determines the type of properties the
	 *                 server will return.  Consider prod vs test configuration
	 *                 can be considerably different.
	 * @return Unique instance of PropertyUtil per source.
	 * @throws JVMXRayRuntimeException
	 */
	public static synchronized PropertyUtil getInstance( String source, String aid, String category ) {
		PropertyUtil pu = null;
		// Cache properties by application id.  Server can return props based on
		// either category or application id.  Caching by aid key supports both cases.
		if( propertiesmap.containsKey(aid) ) {
			pu = propertiesmap.get(aid);
		} else {
			try {
				pu = new PropertyUtil();
				propertiesmap.put(aid,pu);
				pu.source = source;
				pu.srcprops = pu.loadProperties(source, aid, category);
				pu.modprops = resolveJVMXRayProperties(pu.srcprops);
				// TODO: need smarter refresh.  Files: time/date of last modification.  URLs: HTTP HEAD timestamp
				String sTs = pu.modprops.getProperty(SYS_PROP_REFRESH_LAST_TIMESTAMP);
				pu.ts = Long.parseLong(sTs); // Assign timestamp from property file.
				pu.aid = aid;
				pu.cat = category;
				pu.startRefreshingCache();
				// Can be IOException or NumberFormatException
			} catch (Exception e) {
				throw new JVMXRayRuntimeException(e);
			}
		}
		return pu;
	}

	/**
	 * Return instance of PropertyUtil.  No application id or category required.  Primary use
	 * case is server where property files are stored locally.  For example, load a default
	 * prop file from the JAR or elsewhere in the classpath.
	 * @param source Path to a property resource.
	 * @return Unique instance of PropertyUtil per source.
	 * @throws JVMXRayRuntimeException
	 */
	public static synchronized PropertyUtil getInstance(String source) throws JVMXRayRuntimeException {
		PropertyUtil pu = null;
		if( propertiesmap.containsKey(source) ) {
			pu = propertiesmap.get(source);
		} else {
			try {
				pu = new PropertyUtil();
				pu.source = source;
				pu.srcprops = pu.loadProperties(source);
				pu.modprops = resolveJVMXRayProperties(pu.srcprops);
				propertiesmap.put(source,pu);
				// TODO: need smarter refresh.  Files: time/date of last modification.  URLs: HTTP HEAD timestamp
				String sTs = pu.modprops.getProperty(SYS_PROP_REFRESH_LAST_TIMESTAMP);
				pu.ts = Long.parseLong(sTs); // Assign timestamp from property file.
				pu.startRefreshingCache();
			// Can be IOException or NumberFormatException
			} catch (Exception e) {
				throw new JVMXRayRuntimeException(e);
			}
		}
		return pu;
	}

	/**
	 * Starts a timer for the refresh of properties from the source.
	 * Timer waits a minimum of 15-seconds then attempts a refresh
	 * at intervals as defined by, SYS_PROP_PROP_REFRESH_INTERVAL.
	 */
	public synchronized void startRefreshingCache() {
		int interval = getIntProperty(SYS_PROP_PROP_REFRESH_INTERVAL);
		timer.scheduleAtFixedRate(this,(long)15000,(long)interval);
	}

	/**
	 * Cancels cache refresh timer.  Any active refreshing will complete.
	 */
	public void finishRefreshingCache() {
		if( timer!= null ) timer.cancel();
	}

	/**
	 * Returns named property.
	 * @param name Property name.
	 * @return Property value.
	 */
	public synchronized String getStringProperty(String name) {
		String sValue = modprops.getProperty(name);
		sValue = (sValue!=null) ? sValue.trim() : sValue;
		return sValue;
	}

	public synchronized void setStringProperty(String name, String value) {
		modprops.setProperty(name,value);
	}

	/**
	 * Return named property.
	 * @param name Property name.
	 * @return Property value.
	 */
	public synchronized int getIntProperty(String name) throws NumberFormatException {
		String sValue = modprops.getProperty(name);
		sValue = (sValue!=null) ? sValue.trim() : sValue;
		int iValue = Integer.valueOf(sValue);
		return iValue;
	}

	public synchronized void setIntProperty(String name, int value ) {
		String sValue = Integer.toString(value);
		modprops.setProperty(name,sValue);
	}

	/**
	 * Returns named property with a default if none available.
	 * @param name Property name.
	 * @param defaultvalue Default value to assign.
	 * @return Property value.
	 */
	public synchronized String getStringProperty(String name, String defaultvalue ) {
		String sValue = modprops.getProperty(name, defaultvalue);
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
		String sProp = modprops.getProperty(name, sDefaultvalue);
		int value = Integer.valueOf(sProp);
		return value;
	}

	/**
	 * Return property names.
	 * @return Enumeration of property names.
	 */
	public synchronized Enumeration<?> getPropertyNames() {
		return modprops.propertyNames();
	}

	/**
	 * Periodic update of properties from source (eg, url/file).  If the
	 * timestamp in the properties as defined by <code>jvmxray.property.refresh.last.timestamp</code>
	 * has been updated (increased) then the cashed properties will be reloaded and replaced
	 * from the source.
	 */
	private synchronized void refreshProperties() {
		try {
			Properties newprops = null;
			// Refresh source from remote host if we can.  Agents
			if( aid == null || cat == null ) {
				newprops = loadProperties(source,aid,cat);
			//Refresh source from local file system.  Server
			}else{
				newprops = loadProperties(source);
			}
			if (newprops == null ) {
				System.err.println("Warn: Unable to refresh cached properties.");
				return;
			}
			String sTs = newprops.getProperty(SYS_PROP_REFRESH_LAST_TIMESTAMP);
			long cts = Long.parseLong(sTs);
			if ( cts < ts ) {
				modprops = newprops;
				ts = cts;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize a properties map from a source.  Used by agents and the
	 * server.  Sources properties from system property <code>jvmxray.configuration.url</code>
	 * or as specified by the source value.
	 * @param source URL, file, or classpath spec to property file.
	 * @param aid Unique application instance id.
	 * @param category Category name.  For example, ci-prod, ci-test, etc.
	 *                 The category determines the type of properties the
	 *                 server will return.  Consider prod vs test configuration
	 *                 can be considerably different.
	 * @return Initialized properties from specifiec source.
	 * @throws IOException Thrown on loading problems.
	 */
	private static final Properties loadProperties(String source, String aid, String category ) throws IOException {
		Properties p = new Properties();
    	InputStream in = null;
    	try {
			URL url = new URL(source);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			//TODO Need to be added as server properties.
			con.setRequestMethod("POST");
			con.setReadTimeout(20*1000);
			if (aid != null) con.setRequestProperty("aid",URLEncoder.encode(aid,"UTF-8"));
			if (category != null) con.setRequestProperty("cat",URLEncoder.encode(category,"UTF-8"));
			con.connect();
			in = new BufferedInputStream(con.getInputStream());
        	if(in!=null) {
				p.load(in);
			}
    	} finally {
       		if( in != null ) {
				   in.close();
			}
    	}
    	return p;
	}

	private static final Properties loadProperties(String source) throws IOException {
		Properties p = new Properties();
		InputStream in = null;
		try {
			// Try to load properties locally if remote does not work.
			in = PropertyUtil.class.getResourceAsStream(source);
			if(in!=null) {
				p.load(in);
			}
		} finally {
			if( in != null ) {
				in.close();
			}
		}
		return p;
	}

	// Process any unresolved variables.  Return new properties table to the
	// caller.  For System Property settings only: If there is an
	// existing setting, we overwrite it's value.  If the setting does not
	// exist, it's created.  Note, resolved or updated shell variables are
	// not propagated back to the shell.
	private static final Properties resolveJVMXRayProperties(Properties op) {
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
			}
			
		}
		return np;
 	}
	
	// String index of first character of match.  -1 if no match.
	private static final int varStartIndex( String value, int offset ) {
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
	private static final int varEndIndex( String value, int offset ) {
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
	private static final String varResolve( String value ) {
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

}
