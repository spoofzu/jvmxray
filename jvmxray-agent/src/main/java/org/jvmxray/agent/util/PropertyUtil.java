package org.jvmxray.agent.util;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jvmxray.agent.exception.JVMXRayRuntimeException;

/**
 * Helps manage Agent and Server property settings.
 * @author Milton Smith
 */
public class PropertyUtil extends TimerTask {

	// Agent/server properties
	public static final String SYS_PROP_CONFIG_URL = "jvmxray.configuration.url";
	public static final String SYS_PROP_PROPERTY_TARGET = "jvmxray.property.target";
	public static final String SYS_PROP_PROPERTY_REFRESH = "jvmxray.property.refresh";
	public static final String SYS_PROP_PROPERTY_EXPIRES = "jvmxray.property.expires";
	public static final String SYS_PROP_PROPERTY_TTL = "jvmxray.property.ttl";
	public static final String SYS_PROP_REST_WEBHOOK_EVENT = "jvmxray.rest.webhook.event";
	public static final String SYS_PROP_REST_WEBHOOK_CONFIG = "jvmxray.rest.webhook.config";

	//  Agent configuration properties and default values.
	public static final String SYS_PROP_AGENT_IDENTITY_FILE = "jvmxray.agent.id.file";
	public static final String SYS_PROP_AGENT_CONFIG_DEFAULT = "/jvmxrayagent.properties";
	public static final String SYS_PROP_AGENT_EVENT_LOGFILE_DEFAULT = "jvmxrayevent.log";
	public static final String SYS_PROP_AGENT_BASE_DIR = "jvmxray.agent.base.directory";
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
	Properties srcprops = null;
	// Internal property map.
	Properties modprops = null;
	// Property source.
	String source = "";
	// Unique application id
	String aid = "";
	// Category.  For example, ci-test, ci-prod, etc.
	String cat = "";
	// System timestamp as of last reload.
	long timestamp=System.currentTimeMillis();
	// Interval in seconds caller will contact source for properties
	//   updates.  Default=3600 (1hr)
	long lrefresh = -1;
//	// Number of seconds after a failed attempt to validate
//	//   properties to consider the properties valid.
//	//   Default=259200 (3-days)
//	long lexpires=-1;
//	// time-to-live in seconds callers may consider properties valid.
//	//   Default=86400 (24hrs)
//	long lttl=-1;

	PropertyUtil() {
		this("PropertyUtil");
	}
	PropertyUtil( String thread_name ) {
		timer = new Timer(thread_name);
	}

	/**
	 * TimerTask support.
	 */
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
	 * @param source File/HTTP URL location to source.
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
		if( propertiesmap.containsKey(aid) ) {
			pu = propertiesmap.get(aid);
		} else {
			try {
				pu = new PropertyUtil();
				propertiesmap.put(aid,pu);
				pu.source = source;
				pu.aid = aid;
				pu.cat = category;
				//pu.startRefreshingCache();
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
	 * @param source File/HTTP URL location to source.
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
				propertiesmap.put(source,pu);
				pu.source = source;
				pu.aid = null;
				pu.cat = null;
				//pu.startRefreshingCache();
			} catch (Exception e) {
				throw new JVMXRayRuntimeException(e);
			}
		}
		return pu;
	}

	/**
	 * Starts a timer for the refresh of properties from the source.
	 * 1st begins immediately then N seconds after load().  Subsequent
	 * refreshes occur at regular N intervals.  N is described by
	 * the property setting, jvmxray.property.refresh
	 */
	public synchronized void startRefreshingCache() {
		timer.scheduleAtFixedRate(this,0,lrefresh);
	}

	/**
	 * Cancels cache refresh timer.  Any active refreshing will complete.
	 */
	public void finishRefreshingCache() {
		if( timer!= null ) timer.cancel();
	}

	/**
	 * Periodic update of properties from source (eg, url/file).
	 * Updated via Timer/TimerTask.  Called once in main thread
	 * upon initialization and thereafter only via the Timer.
	 */
	public synchronized void refreshProperties() {
		// Config properties in seconds.
		long currenttime = System.currentTimeMillis()/1000;
		Properties tmpprops = null;
		Number tValue = null;
		long lpttl = 0;
		long lpexpires=0;
		boolean bAgent = (aid != null && cat != null);
		// In-memory cached properties exist.
		if( modprops!=null ) {
			tValue = getNumberProperty(SYS_PROP_PROPERTY_TTL,-1);
			lpttl = tValue.longValue();
			if( timestamp + lpttl < currenttime ) {
				// In-memory properties exist but TTL elapsed.
				// Assume any cached disk properties are
				//  invalid so reload properties from
				//  server(Agent) or local
				//  file system(Server).
				//
				// If agent, load from server.
				if (bAgent) {
					try {
						tmpprops = loadPropertiesFromServer(source, aid, cat);
					}catch(IOException e) {
						// On server failure: continue to use in-memory cache
						//   until expires elapses.
						tValue = getNumberProperty(SYS_PROP_PROPERTY_EXPIRES,-1);
						lpexpires = tValue.longValue();
						// In-memory cache has not yet expired (still valid)
						//   Exit back to caller and use existing old
						//   in-memory properties.
						if( timestamp + lpexpires > currenttime ) {
							return;
						} else {
							// Worse case scenario for Agents: 1) can't contact server,
							//   2) in-memory properties expired.  Attempt to load
							//   default properties from agent jar distribution.
							try {
								tmpprops = loadProperties(SYS_PROP_AGENT_CONFIG_DEFAULT);
							}catch(IOException e1) {
								// Can't load properties from anywhere.  Not much we can do
								//   return to caller and wait another Timer interval.  Eeek.
								System.err.println("PropertyUtil.refreshProperties(): Unable to load agent properties.");
								return;
							}
						}
					}
				// For server, no application id/cat so load from disk.
				//   No caching on the server but we reload props from
				//   disk.
				} else {
					try {
						tmpprops = loadProperties(source);
					} catch (IOException e) {
						// Can't load properties from anywhere.  Not much we can do
						//   return to caller and wait another Timer interval.  Eeek.
						System.err.println("PropertyUtil.refreshProperties(): Unable to load server properties.");
						return;
					}
				}
			// In-memory properties exist and valid.
			// Nothing to do.  Return to caller and
			// use existing properties.
			} else {
				return;
			}
		// No cached in-memory properties.
		} else {
			// If agent, load from server.
			if (bAgent) {
				try {
					// If it's cached and loaded, it's valid.  TTL'S are relative
					//  and cannot be evaluated until there is a timestamp upon
					//  initial load into the cache.
					tmpprops = loadCachedProperties();
				} catch (IOException e) {}
				// No cache or it's not available so try server.
				if( tmpprops==null) {
					try {
						tmpprops = loadPropertiesFromServer(source, aid, cat);
						// Can't load properties from anywhere.  Not much we can do
						//   return to caller and wait another Timer interval.  Eeek.
					} catch (ConnectException e1) {
						System.err.println("PropertyUtil.refreshProperties(): Unable to load agent properties from server.  Loading defaults.  Check config or delete property cache.");
						try {
							tmpprops = loadProperties(source);
						} catch (IOException e) {
							System.err.println("PropertyUtil.refreshProperties(): Can't load properties from server or defaults.  No configuration loaded.  Check config or delete property cache.");
							timestamp = -1;
							return;
						}
					} catch(IOException e ) {
						System.err.println("PropertyUtil.refreshProperties(): Unable to load agent properties from server.");
						e.printStackTrace();
						timestamp = -1;
						return;
					}
				}
			// For server, no application id/cat so load from disk.
			//   No caching on the server for present but we do
			//   refresh properties.
			} else {
				try {
					tmpprops = loadProperties(source);
				} catch (IOException e) {}
			}
		}
		// If new properties were loaded resolve any variables
		//   and update the cache timestamp.
		if( tmpprops!=null ) {
			srcprops = tmpprops;
			modprops = resolveJVMXRayProperties(tmpprops);
			tValue = getNumberProperty(SYS_PROP_PROPERTY_REFRESH,-1);
			lrefresh = tValue.longValue();
			timestamp = currenttime;
			try {
				if( bAgent ) {
					saveCachedProperties();
				}
			} catch (IOException e) {
				System.err.println("PropertyUtil.refreshProperties(): Unable to cache properties to disk.");
				e.printStackTrace();
				return;
			}
		}

	}

	/**
	 * Initialize a properties map from a source.  Sources properties from system
	 * property <code>jvmxray.configuration.url</code>
	 * or as specified by the source value.  API used primarliy by agents
	 * providing an aid & category.
	 * @param source URL, file, or classpath spec to property file.
	 * @param aid Unique application instance id.
	 * @param category Category name.  For example, ci-prod, ci-test, etc.
	 *                 The category determines the type of properties the
	 *                 server will return.  Consider prod vs test configuration
	 *                 can be considerably different.
	 * @return Initialized properties from specifiec source.
	 * @throws IOException Thrown on loading problems.
	 */
	private static final Properties loadPropertiesFromServer(String source, String aid, String category ) throws IOException {
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

	/**
	 * Caches properties identified in source to local file system.
	 * @throws IOException Problems writing properties file.
	 */
	private synchronized void saveCachedProperties() throws IOException {
		// basepath
		String basedir = System.getProperty("user.home");
		File basepath = new File(basedir, "jvmxray-agent");
		// Create parent dirs if needed
		File cachedir = new File(basepath,"cache");
		if( !cachedir.exists() ) {
			cachedir.mkdirs();
		}
		// Update the timestamp
		Number value = new Long(System.currentTimeMillis());
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		Date ct = new Date(value.longValue());
		String fmtDate = df.format(ct);
		File cachefi = new File(cachedir,"cached.properties");
		Writer writer = Files.newBufferedWriter(cachefi.toPath());
		srcprops.store(writer,"Cached on "+fmtDate);
		writer.close();
	}

	/**
	 * Restores properties based upon previously cached properties.
	 * @throws IOException
	 */
	private synchronized Properties loadCachedProperties() throws IOException {
		// basepath
		String basedir = System.getProperty("user.home");
		File basepath = new File(basedir, "jvmxray-agent");
		// Create parent dirs if needed
		File cachedir = new File(basepath,"cache");
		if( !cachedir.exists() ) {
			cachedir.mkdirs();
		}
		File cachefi = new File(cachedir,"cached.properties");
		Properties properties = loadProperties(cachefi.toString());
		return properties;
	}


	/**
	 * Initialize a properties map from a source.  Sources properties from system
	 * property <code>jvmxray.configuration.url</code>
	 * or as specified by the source value.  API used primarily by the
	 * server.
	 * @param source URL, file, or classpath spec to property file.
	 * @return Initialized properties from specifiec source.
	 * @throws IOException Thrown on loading problems.
	 */
	private static final Properties loadProperties(String source) throws IOException {
		Properties p = null;
		InputStream in = null;
		try {
			// Load properties as a resource but if that fails then
			//   directly from the file system.
			in = PropertyUtil.class.getResourceAsStream(source);
			if (in==null) {
				File fi = new File(source);
				in = new FileInputStream(source);
			}
			if(in!=null) {
				Properties t = new Properties();
				t.load(in);
				p=t;
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

	/**
	 * Props read-only with some limited exceptions.  Used internally for
	 * caching properties.
	 * @param name Property name.
	 * @param value Property value.
	 * @param value
	 */
	public synchronized void setStringProperty(String name, String value) {
		modprops.setProperty(name,value);
	}

	/**
	 * Return named property.
	 * @param name Property name.
	 * @return Property value.
	 */
	public synchronized Number getNumberProperty(String name) throws NumberFormatException {
		String sValue = modprops.getProperty(name);
		sValue = (sValue!=null) ? sValue.trim() : sValue;
		long lValue = Long.parseLong(sValue);
		return new Long(lValue);
	}

	/**
	 * Props read-only with some limited exceptions.  Used internally for
	 * caching properties.
	 * @param name
	 * @param value
	 */
	public synchronized void setNumberProperty(String name, Number value ) {
		String sValue = value.toString();
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
	public synchronized Number getNumberProperty(String name, Number defaultvalue) throws NumberFormatException {
		String sDefaultvalue = defaultvalue.toString();
		String sValue = modprops.getProperty(name, sDefaultvalue);
		long lValue = Long.parseLong(sValue);
		return new Long(lValue);
	}

	/**
	 * Return property names.
	 * @return Enumeration of property names.
	 */
	public synchronized Enumeration<?> getPropertyNames() {
		return modprops.propertyNames();
	}

}
