package org.owasp.jvmxray.driver;


import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.dgc.VMID;
import java.security.Permission;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import org.owasp.jvmxray.event.EventFactory;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;
import org.owasp.jvmxray.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;


/**
 * 
 * NullSecurityManager conforms to the java.lang.SecurityManager specifications.  This class intercepts
 * access to protected resources, builds an event, and sends the event to a server for processing.
 * 
 * @author Milton Smith
 *
 */
public class NullSecurityManager  extends SecurityManager {
	
	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.driver.NullSecurityManager");
	
	// Initialize logback
	static {
	    // assume SLF4J is bound to logback in the current environment
	    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    
	    JoranConfigurator configurator = new JoranConfigurator();
	    configurator.setContext(context);
	    // Call context.reset() to clear any previous configuration, e.g. default 
	    // configuration. For multi-step configuration, omit calling context.reset().
	    context.reset(); 
	    try {
	    	configurator.doConfigure(new File("./logback.xml"));
		} catch (JoranException e) {
			e.printStackTrace();
		}
	    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	    logger.debug("Logback initalized.");

	}
	
	// Lock access to NullSecurityManager methods while executing.  Blocked by default until
	// NullSecurityManager is properly initialized.
	private volatile boolean bLocked = true;
	
	// jvmxray.properties
	protected Properties p = new Properties();
	
	// Hold list of filters to process.
	private JVMXRayFilterList rulelist = new JVMXRayFilterList();
	
	// Events to process.
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	// Server identity
	private String id;

	// JSON end-point for event data.
	private URL webhookurl;
	
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
	
	
	/**
	 * CTOR
	 */
	public NullSecurityManager ()  {
		
		try {
			// Initialize JVMXRay properties.  If successful, release the lock and
			// accept calls from policy management framework.
			initializeFromProperties();
			bLocked = false;		
		} catch (Exception e) {
			logger.error( "NullSecurityManager can't read properties.  Exiting.", e);
			System.exit(30);
		}
		
	}
	
	/**
	 * @see java.lang.SecurityManager.checkSecurityAccess(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkAccess(Thread)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkAccess(ThreadGroup)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkCreateClassLoader()
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkExit(int)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkSetFactory()
	 */
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

	/**
	 * @see java.lang.SecurityManager.checkDelete(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkExec(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkRead(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkRead(String, Object)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkRead(FileDescriptor)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkWrite(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkWrite(FileDescriptor)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkLink(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPackageAccess(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPackageDefinition(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPermission(Permission)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPermission(Permission, Object)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPrintJobAccess()
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkPropertiesAccess()
	 */
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

	/**
	 * @see java.lang.SecurityManager.checkPropertyAccess(String)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkAccept(String, int)
	 */
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

	/**
	 * @see java.lang.SecurityManager.checkConnect(String, int)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkConnect(String, int, Object)
	 */
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

	/**
	 * @see java.lang.SecurityManager.checkListen(int)
	 */
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

	/**
	 * @see java.lang.SecurityManager.checkMulticast(InetAddress)
	 */
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
	
	/**
	 * @see java.lang.SecurityManager.checkMulticast(InetAddress, byte)
	 */
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

	/**
	 * @see java.lang.SecurityManager.getClassContext()
	 */
	@Override
	protected Class<?>[] getClassContext() {
		return super.getClassContext();
	}

	/**
	 * @see java.lang.SecurityManager.getSecurityContext()
	 */
	@Override
	public Object getSecurityContext() {
		return super.getSecurityContext();
	}

	/**
	 * @see java.lang.SecurityManager.getThreadGroup()
	 */
	@Override
	public ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	/**
	 * Generate a stack track.  Required depending on JVMXRay property settings.
	 * @param event
	 * @return  Stack trace
	 */
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
		try {
			if( rulelist.filterEvents( event ) == FilterActions.ALLOW ) {
				event.setStackTrace(getStackTrace(event));
				JVMXRayClient c = new JVMXRayClient(webhookurl);
				JVMXRayResponse response = c.fireEvent(event);
				logger.debug("Response code: "+response.getResponseCode()+" data: "+response.getResponseData());
			}
		}catch(Throwable t) {
			// TODOMS: Debatable if we should exit.  May be desirable to log 
			// and continue in the event the server condition is recoverable.  For example, a network
			// outage that lasts a few mins.  Need to think about this more.
			logger.error("Unrecoverable error reading from server.  Max retries exceeded.", t);
			System.exit(20);
		}
	}
	
	private boolean isLocked() {
		return bLocked;
	}
	
	private void setLocked(boolean state) {
		bLocked = state;
	}
	
	
	/**
	 * Initialize the NullSecurityManager subclass via property settings from jvmxray.properties.
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
    	
    	PropertyUtil pu = PropertyUtil.getInstance();
    	
		// Load jvmxray.properties
		p = pu.getJVMXRayProperties();
    	
    	// Get the assigned server identity or generate a new identity and save it.
		try {
			// Throws exceptions if can't load file with id.
			id = pu.getServerId();
		} catch( Exception e ) {
			
			// Server identity from command line, if assigned by user.
        	String i1 = System.getProperty(PropertyUtil.SYS_PROP_EVENT_SERV_IDENTITY);
        
        	String i3=""; String lid="";
        	// If no id assigned by user, generate a new one.
        	if( i1 == null ) { 
        		String i2 = new VMID().toString();
        		i3 = PropertyUtil.formatVMID(i2);
        		lid=i3;
        	// Id assigned by user, save it.
        	} else {
        		lid=i1;
        	}
		
			// If exception occurs, they bubble up.
			pu.saveServerId(lid);
			id = lid;
		}
		
		// Set the webhook url to send event data.
		try {
			String surl = p.getProperty(PropertyUtil.CONF_PROP_WEBHOOK_URL);
			webhookurl = new URL(surl);
		} catch( Exception e ) {
			
		}
    	
    	// Iterate over all the properties
    	for( int i=1; i < 500; i++ ) {
    		
    		// Common settings for all filters.
    		String fclass = p.getProperty("jvmxray.filter"+i+".class");
    		String events = p.getProperty("jvmxray.filter"+i+".events");
    		String strace = p.getProperty("jvmxray.filter"+i+".stacktrace");
    		String defaults = p.getProperty("jvmxray.filter"+i+".default");
    		
    		// No more filters or missing filter.  Continue to look for
    		// next numbered filter.
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
     * @param callstackopt Type of callstack to generate.
     * @param stacktrace StackTraceElement[] array of stack trace data.
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
    			buff.append("disabled");
    			break;

    	}

    	// Chop off trailing ->
		if (buff.length()>0 && buff.toString().endsWith("->"))
			buff.setLength(buff.length()-2);
    	
    	return buff.toString();
    }
}
