package org.owasp.jvmxray.api;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.URL;
import java.security.Permission;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;


/**
 * 
 * <code>NullSecurityManager</code> integrates with the Java security policy management
 * and provides visibility into privileges requested by applications.  Use the
 * <code>NullSecurityManager</code> to identify protected resources used by your application
 * during runtime.  The <code>NullSecurityManager</code> is not used directly but instead
 * subclassed by adaptors.  A few adaptors are provided like <code>ConsoleAdaptor</code>,
 * <code>JavaLoggingAdaptor</code>, and <code>LogbackAdaptor</code>
 * The <code>NullSecurityManager</code> does not offer protection for resources and does not
 * respect Java policy settings.  The benefit of this system is for monitoring application
 * access to protected resources during operation.
 * <p/>
 * <b>SPECIAL NOTE</b>
 * Depending upon the design of your application and which events you choose to log, use 
 * of this software can negatively impact your applications performance.  It's
 * recommended you profile your application in a test environment to understand 
 * any potential performance impacts prior to deployment.
 *
 * Following is a list of event types, description of the type, as well as type meta data
 * <pre>
 * EVENT TYPE                       DESCRIPTION(see java.lang.SecurityManager for more information)
 * -------------------------------  ---------------------------------------------------------------------------------------------------------------------------------------
 * ACCESS_SECURITY   				Event fired when permission with the specified permission target name should be granted or denied.
 * ACCESS_THREAD     				Event fired when calling thread attempts to modify thread argument.
 * ACCESS_THREADGROUP 				Event fired when calling thread attempts to modify the thread group argument.
 * CLASSLOADER_CREATE 				Event fired upon creation of a new class loader.
 * EXIT               				Event fired upon request to halt Java Virtual Machine with specified status code.
 * FACTORY                      	Event fired upon requests to set the socket factory used by ServerSocket or Socket, or the stream handler factory used by URL.
 * FILE_DELETE      		    	Event fired upon request to delete specified file.
 * FILE_EXECUTE      				Event fired upon request to execute specified subprocesses. 
 * FILE_READ  	     				Event fired upon request to read a specified file.
 * FILE_READ_WITH_CONTEXT           Event fired upon request to read a specified file within a security context.
 * FILE_READ_WITH_FILEDESCRIPTOR	Event fired upon request to read from a file descriptor.
 * FILE_WRITE  	      				Event fired upon request to write a specified file.
 * FILE_WRITE_WITH_FILEDESCRIPTOR	Event fired upon request to write to a file descriptor
 * LINK               				Event fired upon request to dynamically link a specified library.
 * PACKAGE_ACCESS     				Event fired upon request to access a package.  
 * PACKAGE_DEFINE     				Event fired upon request to define classes in the specified package.
 * PERMISSION         				Event fired upon request to privileged resource request.
 * PERMISSION_WITH_CONTEXT			Event fired upon request to privileged resource request within a security context.
 * PRINT              				Event fired upon request to initiate a print job.
 * PROPERTIES_ANY     				Event fired upon request to access System Properties.
 * PROPERTIES_NAMED   				Event fired upon request to access named System Property.
 * SOCKET_ACCEPT      				Event fired upon socket accepted state.
 * SOCKET_CONNECT     				Event fired upon socket connected state.
 * SOCKET_CONNECT_WITH_CONTEXT		Event fired upon socket connected state within a security context.
 * SOCKET_LISTEN      				Event fired upon socket listen state
 * SOCKET_MULTICAST   				Event fired for multi-cast socket (join/leave/send/or receive) events.
 * SOCKET_MULTICAST_WITH_TTL		Event fired for multi-cast socket (join/leave/send/or receive) events will TTL.
 * 
 * EVENT TYPE                       META DATA DESCRIPTION
 * -------------------------------  ---------------------------------------------------------------------------------------------------------------------------------------
 * ACCESS_SECURITY   				Target Permission Name, Call Stack (if present)
 * ACCESS_THREAD     				Thread Info, Call Stack (if present)
 * ACCESS_THREADGROUP 				ThreadGroup Info, Call Stack (if present)
 * CLASSLOADER_CREATE 				Call Stack (if present)
 * EXIT               				Exit Code, Call Stack (if present)
 * FACTORY                      	Call Stack (if present)
 * FILE_DELETE      		    	Fully Qualified File Name, Call Stack (if present)
 * FILE_EXECUTE      				Command, Call Stack (if present)
 * FILE_READ  	     				Fully Qualified File Name, Call Stack (if present)
 * FILE_READ_WITH_CONTEXT           Fully Qualified File Name, Context Info, Call Stack (if present)
 * FILE_READ_WITH_FILEDESCRIPTOR	File Descriptor Info, Call Stack (if present)
 * FILE_WRITE  	      				Fully Qualified File Name, Call Stack (if present)
 * FILE_WRITE_WITH_FILEDESCRIPTOR	File Descriptor Info, Call Stack (if present)
 * LINK               				Library Name, Call Stack (if present)
 * PACKAGE_ACCESS     				Package Name, Call Stack (if present)  
 * PACKAGE_DEFINE     				Package Name, Call Stack (if present) 
 * PERMISSION         				Permission Name, Permission Actions, Permission Class Name, Call Stack (if present)
 * PERMISSION_WITH_CONTEXT			Permission Name, Permission Actions, Permission Class Name, Context, Call Stack (if present)
 * PRINT              				Call Stack (if present)
 * PROPERTIES_ANY     				Call Stack (if present)
 * PROPERTIES_NAMED   				Property Name, Call Stack (if present)
 * SOCKET_ACCEPT      				Host Name or IP, Port Number, Call Stack (if present)
 * SOCKET_CONNECT     				Host Name or IP, Port Number, Call Stack (if present)
 * SOCKET_CONNECT_WITH_CONTEXT		Host Name or IP, Port Number, Context, Call Stack (if present)
 * SOCKET_LISTEN      				Port Number, Call Stack (if present)
 * SOCKET_MULTICAST   				IP, Call Stack (if present)    
 * SOCKET_MULTICAST_WITH_TTL		IP, TTL, Call Stack (if present)  
 * 
 * </pre>
 * 
 * @author Milton Smith
 *
 */
public abstract class NullSecurityManager extends SecurityManager {

	// Useful INFO
	// Debugging, https://docs.oracle.com/javase/7/docs/technotes/guides/security/troubleshooting-security.html
	// Permissions, https://docs.oracle.com/en/java/javase/12/security/permissions-jdk1.html#GUID-8B521D4F-1502-42EA-BA70-8E3322A163B5
	
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
	 * System property name that specifies the URL to load the jvmxray properties
	 */
	public static final String SYS_PROP_CONFIG_URL = "jvmxray.configuration";
	
	/**
	 * Stacktrace detail property.  Settings are specified in NullSecurityManager.Callstack
	 * and described in the jvmxray.properties file.
	 */
	public static final String CONF_PROP_STACKTRACE = "jvmxray.event.stacktrace";
	
	// Events to process.
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	// Level of detail for callstack.  Disabled by default.
	private Callstack callstackopt = Callstack.NONE;
	
	// Hold list of filters to process.
	private JVMXRayFilterList rulelist = new JVMXRayFilterList();
	
	// Locking flag for calls from JVM.
	private volatile static boolean bLocked = false;
	
	// Holds full stack traces, if enabled.
	private StackTraceElement[] stacktrace = null;

			
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
	
	protected NullSecurityManager() {
		super();
		assignSecurityManagerDefault();
		initializeFromProperties();
		// usrevents = assignEvents();
		// Turn off default access control checks as specified by java policy file defaults.
		// Policy.setPolicy(new NullPolicy());
	}
	
	@Override
	protected synchronized Class<?>[] getClassContext() {
		return super.getClassContext();
	}

	@Override
	public synchronized Object getSecurityContext() {
		return super.getSecurityContext();
	}

	@Override
	public synchronized void checkPermission(Permission perm) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PERMISSION, new JVMXRayPermissionEvent( stacktrace, callstackopt, new Object[] {perm} ));
			}finally{
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION_WITH_CONTEXT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PERMISSION_WITH_CONTEXT, new JVMXRayPermissionWithContextEvent( stacktrace, callstackopt, new Object[] {perm} ));
			}finally {
				setLocked(false);	
			}
		}
	}

	@Override
	public synchronized void checkCreateClassLoader() {
		if( !isLocked() && isEventEnabled(Events.CLASSLOADER_CREATE) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.CLASSLOADER_CREATE,new JVMXRayClassLoaderCreateEvent( stacktrace, callstackopt, new Object[0] ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkAccess(Thread t) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREAD) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.ACCESS_THREAD, new JVMXRayAccessThreadEvent( stacktrace, callstackopt, new Object[] {t} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkAccess(ThreadGroup g) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREADGROUP) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.ACCESS_THREADGROUP, new JVMXRayAccessThreadEvent( stacktrace, callstackopt, new Object[] {g} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkExit(int status) {
		if( !isLocked() && isEventEnabled(Events.EXIT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.EXIT, new JVMXRayExitEvent( stacktrace, callstackopt, new Object[] {status} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkExec(String cmd) {
		if( !isLocked() && isEventEnabled(Events.FILE_EXECUTE) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_EXECUTE, new JVMXRayFileExecuteEvent( stacktrace, callstackopt, new Object[] {cmd} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkLink(String lib) {
		if( !isLocked() && isEventEnabled(Events.LINK) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.LINK, new JVMXRayLinkEvent( stacktrace, callstackopt, new Object[] {lib} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkRead(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_READ_WITH_FILEDESCRIPTOR, new JVMXRayFileReadWithFileDescriptorEvent( stacktrace, callstackopt, new Object[] {fd} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkRead(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_READ, new JVMXRayFileReadEvent( stacktrace, callstackopt, new Object[] {file} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkRead(String file, Object context) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_CONTEXT) ) {
			try {
				setLocked(true);
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_READ_WITH_CONTEXT,new JVMXRayFileReadWithContextEvent( stacktrace, callstackopt, new Object[] {file} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_WRITE_WITH_FILEDESCRIPTOR, new JVMXRayFileWriteWithFileDescriptorEvent( stacktrace, callstackopt, new Object[] {fd} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkWrite(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_WRITE, new JVMXRayFileWriteEvent( stacktrace, callstackopt, new Object[] {file} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkDelete(String file) {
		if( !isLocked() &&  isEventEnabled(Events.FILE_DELETE) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FILE_DELETE, new JVMXRayFileDeleteEvent( stacktrace, callstackopt, new Object[] {file} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_CONNECT, new JVMXRaySocketConnectEvent( stacktrace, callstackopt, new Object[] {host,port} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT_WITH_CONTEXT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_CONNECT_WITH_CONTEXT, new JVMXRaySocketConnectWithContextEvent( stacktrace, callstackopt, new Object[] {host,port,context} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkListen(int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_LISTEN) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_LISTEN, new JVMXRaySocketListenEvent( stacktrace, callstackopt, new Object[] {port} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkAccept(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_ACCEPT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_ACCEPT, new JVMXRaySocketAcceptEvent( stacktrace, callstackopt, new Object[] {host,port} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_MULTICAST, new JVMXRaySocketMulticastEvent( stacktrace, callstackopt, new Object[] {maddr} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	@Deprecated 
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST_WITH_TTL) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.SOCKET_MULTICAST_WITH_TTL, new JVMXRaySocketMulticastWithTTLEvent( stacktrace, callstackopt, new Object[] {maddr, ttl} ));
			}finally {
				setLocked(false);
			}
		}
	
	}

	@Override
	public synchronized void checkPropertiesAccess() {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_ANY) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PROPERTIES_ANY, new JVMXRayPropertiesAnyEvent( stacktrace, callstackopt, new Object[0] ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_NAMED) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PROPERTIES_NAMED, new JVMXRayPropertiesNamedEvent( stacktrace, callstackopt, new Object[] { key } ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkPrintJobAccess() {
		if( !isLocked() && isEventEnabled(Events.PRINT) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PRINT, new JVMXRayPrintEvent( stacktrace, callstackopt, new Object[0] ));			
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_ACCESS) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PACKAGE_ACCESS, new JVMXRayPackageAccessEvent( stacktrace, callstackopt, new Object[] {pkg} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_DEFINE) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.PACKAGE_DEFINE, new JVMXRayPackageDefineEvent( stacktrace, callstackopt, new Object[] {pkg} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkSetFactory() {
		if( !isLocked() && isEventEnabled(Events.FACTORY) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.FACTORY, new JVMXRayFactoryEvent( stacktrace, callstackopt, new Object[0] ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized void checkSecurityAccess(String target) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_SECURITY) ) {
			setLocked(true);
			try {
				//String callstack = "";
				if( callstackopt != Callstack.NONE ) {
					stacktrace = Thread.currentThread().getStackTrace();
				}
				processEvent(Events.ACCESS_SECURITY, new JVMXRayAccessSecurityEvent( stacktrace, callstackopt, new Object[] {target} ));
			}finally {
				setLocked(false);
			}
		}
	}

	@Override
	public synchronized ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	/**
	 * Process an event.  Required since callers
	 * may trigger additional nested security manager permission
	 * calls resulting in a stack overflow. 
	 * @param message Message associated with the event.
	 */
	private void processEvent( Events type, IJVMXRayEvent event ) {
		// Events event, Object[] obj1, String format, Object ...obj2
		try {
			if( filterEvent(type, event) == FilterActions.ALLOW ) {
				fireEvent( event );
			}
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Fire an event.  This is implemented by callers so that events
	 * can be handled by log systems, SIEMS, etc.  This framework 
	 * provides implementation for some popular systems like logback
	 * and Java logging.
	 * @param message Message associated with the event.
	 */
	protected void fireEvent(IJVMXRayEvent handler) {
		// Defaut is to do nothing.
	}
	
	/**
	 * Process event filters
	 * @return FilterActions.ALLOW or FilterActions.DENY.
	 */
	private FilterActions filterEvent(Events type, IJVMXRayEvent event) {
		
		return rulelist.filterEvents( type, event );
		
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
     * Assigns a SecurityManager implementation to use with the NullSecurityManager.
     * Value of the property, NullSecurityManager.SYS_PROP_SECURITY_MANAGER, is used.  If the
     * property is not provided no security manager is used.  No SecurityManager is the
     * default.  To specific the default security assign, "java.lang.SecurityManager" as
     * the property value.  If a specified SecurityManager cannot be loaded an error message
     * is printed, no default is assigned, and processing aborts.<br/>
     * CURRENTLY NOT FUNCTIONAL
     */
    private void assignSecurityManagerDefault() {


    	String pv = System.getProperty(SYS_PROP_SECURITY_MANAGER, "zzzz");
    	if ( pv != "zzzz" ) {
    		
        	// TODOMS: Idea is to call methods of smd within methods of NullSecurityManager to chain
        	//         SecurityManager functionality.  Unfortunately, it's going to take more work
        	//         to determine if NullSecurityManager context information is appropriate.  For
        	//         example returning context as provided by NullSecurityManager.getClassContext()
        	//         and NullSecurityManager.getSecurityContext() or if smd.getClassContext() and
        	//         smd.getSecurityContext() is more appropriate or something else.  For now, I'm
        	//         disabling this feature.
        	//
    		String error = "Chaining security managers is unsupported.";
    		RuntimeException e1 = new RuntimeException(error);
    		throw e1;
    		
//	    	try {
//	    		Class<?> smc = Class.forName(pv);
//	    		Object sm = smc.getDeclaredConstructor().newInstance();
//	    		if (sm instanceof java.lang.SecurityManager) {
//	    			smd = (SecurityManager)sm;
//	    		}
//	    	} catch(Exception e2 ) {
//	    		System.err.println("SecurityManager implementation not loaded. msg="+e2.getMessage()+" class="+pv);
//	    		e2.printStackTrace();
//	    		System.exit(20);
//	    		
//	    	}
    	}

    }
   
    
    
	/**
	 * Initialize the NullSecurityManager subclass via property settings.
	 */
    private void initializeFromProperties() {
    
    	try {
    	
    		// Load jvmxray.properties
	    	Properties p = new Properties();
	    	InputStream in = null;
	    	try {    		
	        	// Load configuration properties from HTTPS URL.  If unassigned, load from /jvmxray.properties.
	        	String surl = System.getProperty(SYS_PROP_CONFIG_URL, "/jvmxray.properties");
	        	URL url = null;
	        	if( surl.equals("/jvmxray.properties")) {
			    	in = getClass().getResourceAsStream("/jvmxray.properties");
	        	} else {
	        		url = new URL(surl);
		   	     	HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
		   	     	in = new BufferedInputStream(con.getInputStream());
	        	}
	        	
		    	p.load(in);
	    		
	    	} finally {
	       	 if( in != null )
				try {
					in.close();
				} catch (IOException e) {
					throw e;
				}
	    	}
	    	
	    	// Get the trace level
	    	String lvl = p.getProperty(CONF_PROP_STACKTRACE);
	    	callstackopt = Callstack.valueOf(lvl);
	    	
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
	   
    	
    	} catch( Exception e ) {
    		e.printStackTrace();
    	}
    	
    }
    
    
    private boolean isLocked() {
    	return bLocked;
    }
    
    private void setLocked(boolean state) {
    	bLocked = state;
    }
    
}

