package org.owasp.jvmxray.driver;


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

import org.owasp.jvmxray.event.EventFactory;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;
import org.owasp.jvmxray.exception.JVMXRayDBError;
import org.owasp.jvmxray.util.DBUtil;
import org.owasp.jvmxray.util.FileUtil;
import org.owasp.jvmxray.util.PropertyUtil;

public class NullSecurityManager  extends SecurityManager {
	
	/**
	 * Stacktrace detail property.  Settings are specified in NullSecurityManager.Callstack
	 * and described in the jvmxray.properties file.
	 */
	//public static final String CONF_PROP_STACKTRACE = "jvmxray.event.stacktrace";
	
	// Lock access to securitymanager methods while executing.  Must be locked as default until
	// dbconn has time to initialize.
	private volatile boolean bLocked = true;
	
	// jvmxray.properties
	protected Properties p = new Properties();
	
	// Hold list of filters to process.
	private JVMXRayFilterList rulelist = new JVMXRayFilterList();
	
	// Level of detail for callstack.  Disabled by default.
	//private Callstack callstackopt = Callstack.NONE;
	
	// Events to process.
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	// Server identity
	private String id;
	
	private Connection dbconn;
	
	private DBUtil dbutil;
	private FileUtil fiutil;
	
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
			fiutil = FileUtil.getInstance();
			dbutil = DBUtil.getInstance(p);
			try {
				dbconn = dbutil.createConnection();
				if( dbconn != null ) {
					setLocked(false);
				} else {
					JVMXRayDBError e = new JVMXRayDBError("JDBC connection failed to initialize.  dbconn=null");
					throw e;
				}
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
	public synchronized void checkSecurityAccess(String target) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_SECURITY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessSecurityEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								target // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkAccess(Thread t) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREAD) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessThreadEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								t.toString() // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkAccess(ThreadGroup tg) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREADGROUP) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessThreadGroupEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								tg.toString() // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkCreateClassLoader() {
		if( !isLocked() && isEventEnabled(Events.CLASSLOADER_CREATE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createCreateClassLoaderEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}	
	
	@Override
	public synchronized void checkExit(int status) {
		if( !isLocked() && isEventEnabled(Events.EXIT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createExitEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								status // exit code
								);
			processEvent(ev);
			setLocked(false);
		}		
	}
	
	@Override
	public synchronized void checkSetFactory() {
		if( !isLocked() && isEventEnabled(Events.FACTORY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFactoryEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkDelete(String file) {
		if( !isLocked() &&  isEventEnabled(Events.FILE_DELETE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileDeleteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file ); //  File to delete
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkExec(String cmd) {
		if( !isLocked() && isEventEnabled(Events.FILE_EXECUTE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileExecuteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								cmd ); //  Command to execute
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkRead(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file ); //  File to read
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkRead(String file, Object context) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file, // file to read
								context.toString() ); //  file context
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkRead(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadWithFileDescriptorEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkWrite(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileWriteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file); // File to write
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileWriteWithFileDescriptorEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkLink(String lib) {
		if( !isLocked() && isEventEnabled(Events.LINK) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createLinkEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								lib); // library name
			
			processEvent(ev);
			setLocked(false);
		}		
	}
	
	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_ACCESS) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPackageAccessEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								pkg); // package name
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_DEFINE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPackageDefineEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								pkg); // package name
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkPermission(Permission perm) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPermissionEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								perm.getName(),  // Permission name
								perm.getActions()); // Permission actions
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPermissionWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								perm.getName(),  // Permission name
								perm.getActions(), // Permission actions
								context.toString()); // Context info
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkPrintJobAccess() {
		if( !isLocked() && isEventEnabled(Events.PRINT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPrintEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later	
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkPropertiesAccess() {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_ANY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPropertiesAnyEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later	
			processEvent(ev);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_NAMED) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPropertiesNamedEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								key); // Property key
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkAccept(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_ACCEPT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketAcceptEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port ); // Port number
			processEvent(ev);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketConnectEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port ); // Port number
			processEvent(ev);
			setLocked(false);
		}
	}
	
	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketConnectWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port, // Port number
								context.toString()); // Context info.
			processEvent(ev);
			setLocked(false);
		}
	}

	@Override
	public synchronized void checkListen(int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_LISTEN) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketListenEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								port); // Port number
			processEvent(ev);
			setLocked(false);
		}	
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketMulticastEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later		
								maddr.toString()); // Multicast address
			processEvent(ev);
			setLocked(false);
		}	
	}
	
	@Override
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST_WITH_TTL) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketMulticastWithTTLEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								maddr.toString(), // Multicast address
								String.format("%02X ", ttl)); // Time to live
			processEvent(ev);
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

	private String getStackTrace(IEvent event) {
		String stacktrace = "";
		StackTraceElement[] ste = null;
		ste = Thread.currentThread().getStackTrace();
		stacktrace = generateCallStack(rulelist.getCallstackOptions(event), ste);
		return stacktrace;
	}
	
	/**
	 * Process an event.  Required since callers
	 * may trigger additional nested security manager permission
	 * calls resulting in a stack overflow.
	 * @param type type of event being processed
	 * @param event actual event being processed
	 */
	private void processEvent( IEvent event )  {
		// Events event, Object[] obj1, String format, Object ...obj2
		try {
			if( rulelist.filterEvents( event ) == FilterActions.ALLOW ) {
				event.setStackTrace(getStackTrace(event));
				spoolEvent( event );
			}
		}catch(Throwable t) {
			t.printStackTrace();
			System.exit(20);
		}
	}
	
	
	private void spoolEvent(IEvent event) throws IOException, SQLException {
		
		dbutil.insertEvent(dbconn,
				           event );

		
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
//    	String lvl = p.getProperty(CONF_PROP_STACKTRACE);
//    	callstackopt = Callstack.valueOf(lvl);
    	
    	// Get the assigned server identity or the default.
    	id = System.getProperty(PropertyUtil.SYS_PROP_EVENT_SERV_IDENTITY, PropertyUtil.SYS_PROP_EVENT_SERV_IDENTITY_DEFAULT);
    	
    	// Iterate over all the properties
    	for( int i=1; i < 500; i++ ) {
    		
    		// Common settings for all filters.
    		String fclass = p.getProperty("jvmxray.filter"+i+".class");
    		String events = p.getProperty("jvmxray.filter"+i+".events");
    		String strace = p.getProperty("jvmxray.filter"+i+".stacktrace");
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
             
             // Grab callstackopts for the filter
             Callstack opts = Callstack.NONE;
             opts = Callstack.valueOf(strace);
            	 
        	 // Create instance of specified filter using reflection
    		 Class c = getClass().getClassLoader().loadClass(fclass);
             Constructor cd = c.getConstructor(EnumSet.class, FilterActions.class, Properties.class, Callstack.class);
             FilterActions filteraction = FilterActions.valueOf(defaults);
             JVMXRayFilterRule fdr = (JVMXRayFilterRule)cd.newInstance(gvents,filteraction, np, opts);
             
            		 
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
     * Return a string to identify calling thread suitable for logging.
     * @return Identity of the current thread.
     */
    private String getThreadStamp() {
    	Thread t = Thread.currentThread();
    	return t.getName()+"-"+t.getId();
    }
    
	/**
     * Generate a callstack based upon specification.
     * @param callstack Type of callstack to generate.
     */
    String generateCallStack(Callstack callstackopt, StackTraceElement[] stacktrace) {
    	
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
