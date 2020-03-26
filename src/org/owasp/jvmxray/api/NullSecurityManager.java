package org.owasp.jvmxray.api;


import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.security.Permission;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ServiceConfigurationError;

import org.owasp.jvmxray.util.DBUtil;
import org.owasp.jvmxray.util.FileUtil;
import org.owasp.jvmxray.util.PropertyUtil;

public class NullSecurityManager  extends SecurityManager {
	
	/**
	 * Stacktrace detail property.  Settings are specified in NullSecurityManager.Callstack
	 * and described in the jvmxray.properties file.
	 */
	public static final String CONF_PROP_STACKTRACE = "jvmxray.event.stacktrace";
	
	private volatile boolean bLocked = true;
	
	// jvmxray.properties
	protected Properties p = new Properties();
	
	// Hold list of filters to process.
	private JVMXRayFilterList rulelist = new JVMXRayFilterList();
	
	// Level of detail for callstack.  Disabled by default.
	private Callstack callstackopt = Callstack.NONE;
	
	// Events to process.
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	// Server identity
	private String id;
	
	private Connection dbconn;
	
	private DBUtil dbutil;
	private FileUtil fiutil;
	
	/**
	 * Event types supported the <code>NullSecurityManager</code>.  
	 */
	public enum Events {
		ACCESS_SECURITY,
		ACCESS_THREAD,
		ACCESS_THREADGROUP,
		CLASSLOADER_CREATE,
		EXIT,
		FACTORY,
		FILE_DELETE,
		FILE_EXECUTE,
		FILE_READ,
		FILE_READ_WITH_CONTEXT,
		FILE_READ_WITH_FILEDESCRIPTOR,
		FILE_WRITE,
		FILE_WRITE_WITH_FILEDESCRIPTOR,
		LINK,
		PACKAGE_ACCESS,
		PACKAGE_DEFINE,
		PERMISSION,
		PERMISSION_WITH_CONTEXT,
		PRINT,
		PROPERTIES_ANY,
		PROPERTIES_NAMED,
		SOCKET_ACCEPT,
		SOCKET_CONNECT,
		SOCKET_CONNECT_WITH_CONTEXT,
		SOCKET_LISTEN,
		SOCKET_MULTICAST,
		SOCKET_MULTICAST_WITH_TTL
	}
	
	/**
	 * Implemented by filters but generally, <br/>
	 * ALLOW events captured on match.
	 * NEUTRAL, events pass through to next filter.
	 * DENY, events removed on match.
	 */
	public enum FilterActions {
		ALLOW,
		NEUTRAL,
		DENY
	}
	
	/**
	 * Callstack options for captured events. <br/>
	 * NONE Do not include stacktrace, default.
	 * LIMITED, Include limited information.  Class call stack, without method or line number references.
	 * FULL, Include full stack trace information.
	 */
	public enum Callstack {
		NONE,
		LIMITED,
		SOURCEPATH,
		FULL
	}
	
	
	public NullSecurityManager ()  {
		
		try {
		
			// Can't occur in initialize() since isEventEnabled() must be properly initialized
			// prior to SM functions being called.
			initializeFromProperties();
			
			String sdelay = p.getProperty(PropertyUtil.CONF_PROP_MAXWAIT_INITIALIZATION);
			int delay = Integer.parseInt(sdelay);
			
			// Due to the architecture SecurityManager and our use of it
			// beyond it's intended design some features cause exceptions if used
			// prior to full VM initialization.  In the case of JDBC, ServiceConfigurationError
			// exceptions occur if connections are initialized too early.  It's also
			// a problem with early use of other features like JMX and Classloaders.
			// To work around, we attempt to initialize a connection immediately.  This 
			// works fine for unit testing but fails in containers.  To support containers
			// we start a background thread and delay event processing for a number
			// of seconds specified by 
			// jvmxray.event.nullsecuritymanager.server.delay.initialization
			// or a non-null database connection is returned, whichever comes
			// first.  This allows container initialization
			// will proceed, but any events issued by the service prior to
			// initialization are lost.  If a db connection is established prior
			// to timer elapse, the thread exits successfully.
			//
			// Specific exception thrown is, 
			// java.util.ServiceConfigurationError: java.sql.Driver: not accessible to 
			// module java.sql during VM init
			fiutil = FileUtil.getInstance(p);
			dbutil = DBUtil.getInstance(p);
			try {
				dbconn = dbutil.createConnection();
			}catch(ServiceConfigurationError e) {
				Thread t = new Thread() {
					public void run() {			
						long start = System.currentTimeMillis();
						while( System.currentTimeMillis() - start < delay ) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {}
							Thread.yield();
							try {
								dbconn = dbutil.createConnection();
							} catch (ServiceConfigurationError e) {
								
							} catch (SQLException e) {
								e.printStackTrace();
							}
							if ( dbconn != null ) {
								break;
							}
						}
						setLocked(false);
					}
				};
				t.start();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(30);
		}
		
	}
	
	@Override
	public synchronized void checkAccept(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_ACCEPT) ) {
			setLocked(true);
			processEvent(Events.SOCKET_ACCEPT, "checkAccept(String,int)", host, Integer.valueOf(port));
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkAccess(Thread t) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREAD) ) {
			setLocked(true);
			processEvent(Events.ACCESS_THREAD, "checkAccess(Thread)", t);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkAccess(ThreadGroup g) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREADGROUP) ) {
			setLocked(true);
			processEvent(Events.ACCESS_THREADGROUP, "checkAccess(ThreadGroup)", g);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT_WITH_CONTEXT) ) {
			setLocked(true);
			processEvent(Events.SOCKET_CONNECT_WITH_CONTEXT, "checkConnect(String,int,Object)", host, Integer.valueOf(port), context);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT) ) {
			setLocked(true);
			processEvent(Events.SOCKET_CONNECT,"checkConnect(String,int)", host, Integer.valueOf(port));
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkCreateClassLoader() {
		if( !isLocked() && isEventEnabled(Events.CLASSLOADER_CREATE) ) {
			setLocked(true);
			processEvent(Events.CLASSLOADER_CREATE,"checkCreateClassLoader()");
			setLocked(false);
		}
	}	

	@Override
	public synchronized void checkDelete(String file) {
		if( !isLocked() &&  isEventEnabled(Events.FILE_DELETE) ) {
			setLocked(true);
			processEvent(Events.FILE_DELETE,"checkDelete(String)", file);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkExec(String cmd) {
		if( !isLocked() && isEventEnabled(Events.FILE_EXECUTE) ) {
			setLocked(true);
			processEvent(Events.FILE_EXECUTE,"checkExec(String)", cmd);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkExit(int status) {
		if( !isLocked() && isEventEnabled(Events.EXIT) ) {
			setLocked(true);
			processEvent(Events.EXIT,"checkExit(int)", Integer.valueOf(status));
			setLocked(false);
		}		
	}

	@Override
	public synchronized void checkLink(String lib) {
		if( !isLocked() && isEventEnabled(Events.LINK) ) {
			setLocked(true);
			processEvent(Events.LINK,"checkLink(String)", lib);
			setLocked(false);
		}		
	}

	@Override
	public synchronized void checkListen(int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_LISTEN) ) {
			setLocked(true);
			processEvent(Events.SOCKET_LISTEN,"checkListen(int)", Integer.valueOf(port));
			setLocked(false);
		}	
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST_WITH_TTL) ) {
			setLocked(true);
			processEvent(Events.SOCKET_MULTICAST_WITH_TTL,"checkMulticast(InetAddress,byte)", maddr, Byte.valueOf(ttl));
			setLocked(false);
		}	
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST) ) {
			setLocked(true);
			processEvent(Events.SOCKET_MULTICAST,"checkMulticast(InetAddress)", maddr);
			setLocked(false);
			}	
		}

	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_ACCESS) ) {
			setLocked(true);
			processEvent(Events.PACKAGE_ACCESS,"checkPackageAccess(String)", pkg);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_DEFINE) ) {
			setLocked(true);
			processEvent(Events.PACKAGE_DEFINE,"checkPackageDefinition(String)", pkg);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION_WITH_CONTEXT) ) {
			setLocked(true);
			processEvent(Events.PERMISSION_WITH_CONTEXT,"checkPermission(Permission,Object)", perm, context);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPermission(Permission perm) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION) ) {
			setLocked(true);
			processEvent(Events.PERMISSION,"checkPermission(Permission)", perm);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPrintJobAccess() {
		if( !isLocked() && isEventEnabled(Events.PRINT) ) {
			setLocked(true);
			processEvent(Events.PRINT,"checkPrintJobAccess()");
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPropertiesAccess() {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_ANY) ) {
			setLocked(true);
			processEvent(Events.PROPERTIES_ANY,"checkPropertiesAccess()");
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_NAMED) ) {
			setLocked(true);
			processEvent(Events.PROPERTIES_NAMED,"checkPropertyAccess(String)",key);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkRead(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			processEvent(Events.FILE_READ_WITH_FILEDESCRIPTOR,"checkRead(FileDescriptor)",fd);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkRead(String file, Object context) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_CONTEXT) ) {
			setLocked(true);
			processEvent(Events.FILE_READ_WITH_CONTEXT,"checkRead(String,Object)", file, context);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkRead(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ) ) {
			setLocked(true);
			processEvent(Events.FILE_READ,"checkRead(String)", file);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkSecurityAccess(String target) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_SECURITY) ) {
			setLocked(true);
			processEvent(Events.ACCESS_SECURITY,"checkSecurityAccess(String)", target);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkSetFactory() {
		if( !isLocked() && isEventEnabled(Events.FACTORY) ) {
			setLocked(true);
			processEvent(Events.FACTORY,"checkSetFactory()");
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			processEvent(Events.FILE_WRITE_WITH_FILEDESCRIPTOR,"checkWrite(FileDescriptor)",fd);
		setLocked(false);
		}
	}

	@Override
	public synchronized void checkWrite(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE) ) {
			setLocked(true);
			processEvent(Events.FILE_WRITE,"checkWrite(String)",file);
		setLocked(false);
		}
	}

	@Override
	protected Class<?>[] getClassContext() {
		return super.getClassContext();
	}

	@Override
	public Object getSecurityContext() {
		return super.getSecurityContext();
	}

	@Override
	public ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	/**
	 * Process an event.  Required since callers
	 * may trigger additional nested security manager permission
	 * calls resulting in a stack overflow.
	 * @param type type of event being processed
	 * @param event actual event being processed
	 */
	private void processEvent( Events type, Object ...params ) {
		// Events event, Object[] obj1, String format, Object ...obj2
		try {
			if( filterEvent(type, params) == FilterActions.ALLOW ) {
				spoolEvent( type, params );
			}
		}catch(Throwable t) {
			t.printStackTrace();
			System.exit(20);
		}
	}
	
	
	/**
	 * Process event filters
	 * @param type type of event being processed
	 * @param event actual event being processed
	 * @return FilterActions.ALLOW or FilterActions.DENY.
	 */
	private FilterActions filterEvent(Events type, Object ...params) {
		return rulelist.filterEvents( type, params );
	}
	
	private void spoolEvent(Events event, Object ...params ) throws IOException, SQLException {
		
		int sz = params.length;
		int idx=0;
		StringBuffer buff = new StringBuffer();
		
		// Produce stacktrace if enabled.
		String stacktrace = "";
		StackTraceElement[] ste = null;
		if( callstackopt != Callstack.NONE ) {
			ste = Thread.currentThread().getStackTrace();
			stacktrace = generateCallStack(ste);
		}
	
		while ( idx < sz  ) {
			if( params[idx] instanceof Byte ) {
				buff.append(String.format("%02X",((Byte)params[idx]).byteValue()));
			} else if( params[idx] instanceof InetAddress ) {
				buff.append(fiutil.getFilteredString(((InetAddress)params[idx]).getHostAddress()));
			} else if( params[idx] instanceof FileDescriptor ) {
				// Skip descriptors, nothing interesting.
			} else if( params[idx] instanceof Thread ) {
				// Skip descriptors, nothing interesting.
			} else if( params[idx] instanceof ThreadGroup ) {
				// Skip descriptors, nothing interesting.
			} else if( params[idx] instanceof Permission ) {
				Permission p = (Permission)params[idx];
				buff.append("n=");
				buff.append(fiutil.getFilteredString(p.getName()));
				buff.append(",");
				buff.append("a=");
				buff.append(fiutil.getFilteredString(p.getActions()));
				buff.append(",");
				buff.append("cn=");
				buff.append(fiutil.getFilteredString(p.getClass().getName()));
			} else if( params[idx] instanceof String ) {
				buff.append(fiutil.getFilteredString((String)params[idx]));
			} else {
			  // Skip context types and other types of Objects.
			}	
			buff.append(',');
			idx++;
		};
		// trim trailing field separators (if any)
		boolean moresparators = true;
		while( moresparators ) {
			if( buff.toString().endsWith(",") ) {
				buff.setLength(buff.length()-1);
			} else {
				moresparators = false;
			}
		}
		
		dbutil.insertEvent(dbconn,
				           0,
				           System.currentTimeMillis(),
				           event.toString(),
				           id,
				           stacktrace,
				           buff.toString() ); 

		
	}
	
	private boolean isLocked() {
		return bLocked;
	}
	
	private void setLocked(boolean state) {
		bLocked = state;
	}
	
	
	/**
	 * Initialize the NullSecurityManager subclass via property settings.
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
    private void initializeFromProperties() throws ClassNotFoundException, NoSuchMethodException, SecurityException, 
    											   InstantiationException, IllegalAccessException, IllegalArgumentException, 
    											   InvocationTargetException, IOException {
    	
		// Load jvmxray.properties
		p = PropertyUtil.getJVMXRayProperties();
    	
    	// Get the trace level
    	String lvl = p.getProperty(CONF_PROP_STACKTRACE);
    	callstackopt = Callstack.valueOf(lvl);
    	
		id = p.getProperty(PropertyUtil.CONF_PROP_EVENT_SERV_IDENTITY);
    	
    	// Iterate over all the properties
    	for( int i=1; i < 500; i++ ) {
    		
    		// Common settings for all filters.
    		String fclass = p.getProperty("jvmxray.filter"+i+".class");
    		String events = p.getProperty("jvmxray.filter"+i+".events");
    		String defaults = p.getProperty("jvmxray.filter"+i+".default");
    		
    		// No more filters or missing filter.  Continue to look for
    		// next numbered fitler.
    		if( fclass == null || events == null || defaults == null )
    			continue;
    		
    		// Collect all properties specific to the filter.
    		Properties np = new Properties();
    		Enumeration<String> e = (Enumeration<String>) p.propertyNames();
    		while (e.hasMoreElements() ) {
    			String key = e.nextElement();
    			String value = p.getProperty(key);
    			String prefix = "jvmxray.filter"+i;
    			if( key.startsWith(prefix) ) {
    				np.put(key,value);
    			}
    		}
    	
             // Take any new events and add it to the list of supported events in
             // usrevents.
    		EnumSet<Events> gvents = EnumSet.noneOf(Events.class);
             String[] sevents = events.split(",");
             for( int i3=0; i3 < sevents.length; i3++ ) {
            	 String levent = sevents[i3].trim();
            	 if ( !usrevents.contains(levent) ) {
            		 gvents.add(Events.valueOf(levent));
            		 usrevents.add(Events.valueOf(levent));

            	 }
             }
            	 
        	 // Create instance of specified filter using reflection
    		 Class c = getClass().getClassLoader().loadClass(fclass);
             Constructor cd = c.getConstructor(EnumSet.class, FilterActions.class, Properties.class);
             FilterActions filteraction = FilterActions.valueOf(defaults);
             JVMXRayFilterRule fdr = (JVMXRayFilterRule)cd.newInstance(gvents,filteraction, np);
             
            		 
             // Add the rule to the list
         	 rulelist.add( fdr );
             
    	}
    	
    }
	
	/**
	 * Test if target type of event handling is enabled.
	 * @param event Event to test if enabled.
	 * @return True, the event is enabled.  False, the event is not enabled.
	 */
    private boolean isEventEnabled(Events event) {
    	
    	boolean iseventenabled = false;
    	for (Events c : usrevents) {
    		if ( event==c ) {
    			iseventenabled = true;
    		}
    	}
    	return iseventenabled;
    }
    
	/**
     * Generate a callstack based upon specification.
     * @param callstack Type of callstack to generate.
     */
    String generateCallStack(StackTraceElement[] stacktrace) {
    	
    	StringBuffer buff = new StringBuffer();
		URL location = null;
    	
    	switch ( callstackopt ) {
    		case LIMITED:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				buff.append(c.getName());
	    				buff.append("->");
      				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case SOURCEPATH:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				location = c.getResource('/' + c.getName().replace('.', '/') + ".class");
	    				buff.append(location.toString());
	    				buff.append("->");
    				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case FULL:
    			for (StackTraceElement e : stacktrace ) {
    				buff.append(e.getClassName());
    				buff.append('(');
    				buff.append(e.getMethodName());
    				buff.append(':');
    				buff.append(e.getLineNumber());
    				buff.append(')');
    				buff.append("->");
    			}
    			break;		
    		case NONE:
    			buff.append("<disabled>");
    			break;

    	}

    	// Chop off trailing ->
		if (buff.length()>0 && buff.toString().endsWith("->"))
			buff.setLength(buff.length()-2);
    	
    	return buff.toString();
    }
}
