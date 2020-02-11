package org.owasp.jvmxray.api;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.EnumSet;

/**
 * 
 * <code>NullSecurityManager</code> integrates with the Java security policy management
 * and provides visibility into privileges requested by the application.  Use the
 * <code>NullSecurityManager</code> to identify resources by your application
 * during runtime.  The <code>NullSecurityManager</code> can be used in applications
 * you design or in legacy applications.
 * <p/>New Applications<br/>
 * To use <code>NullSecurityManager</code> with new applications you can assign the security
 * manager early at startup time by the following,<br/>
 * <code>
 * System.setSecurityManager(new LogbackHandler());
 * </code>
 * Substitute the event sink <code>LogbackHandler</code> with any available sink or
 * use any sink that extends <code>NullSecurityManager</code>.
 * <p/>Legacy Applications<br/>
 * To use <code>NullSecurityManager</code> with legacy applications assign an implementation
 * via the command line (or Java property setting) like the following,<br/>
 * <code>
 * -Djava.security.manager="org.owasp.jvmxray.handlers.LogbackHandler"
 * </code>
 * The <code>NullSecurityManager</code> does not offer protection for resources and does not
 * respect Java policy settings.  The benefit of this system is for monitoring application
 * activities leading to compromise.
 * <p/>SPECIAL NOTE<br/>
 * Depending on the design of your application, the implementation of <code>NullSecurityManager</code>
 * you use, and the events you choose to log, it may negatively impact performance.  It's
 * recommended you profile your application in a test environment to understand 
 * any potential performance impacts.
 * 
 * @author Milton Smith
 *
 */
public abstract class NullSecurityManager extends SecurityManager {

	/**
	 * System property name of the security manager to use, <code>nullsecuritymanager.securitymanager</code>
	 * If the property is unspecified then no security manager is used, this is the default.  If a 
	 * specified security manager is provided, NullSecurityManager will pass-thru it's calls.  To use
	 * Java's default SecurityManager the default security properties specify it's fully qualified
	 * class name, <code>java.lang.securitymanager</code>.
	 */
	public static final String SECURITY_MANAGER_OPTION = "nullsecuritymanager.securitymanager";
	
	/**
	 * Property name of the events to capture.
	 */
	public static final String SECURITY_EVENTS = "nullsecuritymanager.events";
	
	/**
	 * Set of events to process.
	 */
	EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	private SecurityManager smd = null;
			
	/**
	 * Event types supported the <code>NullSecurityManager</code>.  The list of protected resources
	 * may be far larger than those use directly by your application since resources used by
	 * infrastructure (e.g., web frameworks) are included as well.
	 * <code>
	 * ACCESS             The ACCESS event captures privilege requests to modify Thread or ThreadGroup arguments. 
	 * CLASSLOADER_CREATE The CLASSLOADER_CREATE event captures privilege requests to create a new ClassLoader.
	 * EXEC  	  	      The EXEC event captures privilege requests when creating new subprocesses. 
	 * EXIT               The EXIT event captures privilege requests to halt the Java Virtual Machine with specified status code.
	 * FACTORY            The FACTORY event captures privilege requests to set the socket factory used by ServerSocket or Socket, or the stream handler factory used by URL.
	 * FILE_DELETE        The FILE_DELETE event captures privilege requests to delete the specified file.
	 * FILE_EXECUTE       The FILE_EXECUTE event captures privilege requests to execute specified subprocesses. 
	 * FILE_READ  	      The FILE_READ event captures privilege requests to read a specified file or file descriptor.
	 * FILE_WRITE  	      The FILE_WRITE event captures privilege requests to write to the specified file or file descriptor.
	 * LINK               The LINK event captures privilege requests to dynamically link the specified library.
	 * PACKAGE_ACCESS     The PACKAGE event captures privilege requests to access specified package.  
	 * PACKAGE_DEFINE     The PACKAGE event captures privilege requests to define classes in the specified package.
	 * PERMISSION         The PERMISSION event captures privilege requests to privileged resources.  Optional resource context information may be included.
	 * PRINT              The PRINT event captures privilege requests by application to print.
	 * PROPERTIES_ANY     The PROPERTIES_ANY event captures privilege requests to access System Properties.
	 * PROPERTIES_NAMED   The PROPERTIES_NAMED event captures privilege requests to access named System Properties.
	 * SOCKET_ACCEPT      The SOCKET_ACCEPT event captures privilege requests to accept socket connections at the specified host and port.
	 * SOCKET_CONNECT     The SOCKET_CONNECT event captures privilege requests to open socket connections to the specified host and port.
	 * SOCKET_LISTEN      The SOCKET_LISTEN event captures privilege requests to halt the Java Virtual Machine with specified status code.
	 * SOCKET_MULTICAST   The SOCKET_MULTICAST event captures privilege requests to listen to socket connections on a specified port.    
	 * </code>
	 * Return the events of interest in the implementation when you override,
	 * <code>NullSecurityManager.getEnabledEvents()</code>
	 */
	protected enum Events {
		ACCESS,
		CLASSLOADER_CREATE,
		EXEC,
		EXIT,
		FACTORY,
		FILE_DELETE,
		FILE_EXECUTE,
		FILE_READ,
		FILE_WRITE,
		LINK,
		PACKAGE_ACCESS,
		PACKAGE_DEFINE,
		PERMISSION,
		PRINT,
		PROPERTIES_ANY,
		PROPERTIES_NAMED,
		SOCKET_ACCEPT,
		SOCKET_CONNECT,
		SOCKET_LISTEN,
		SOCKET_MULTICAST
	}
	
	public enum FilterActions {
		ALLOW,
		DENY
	}
	
	protected NullSecurityManager() {
		super();
		assignSecurityManagerDefault();
		usrevents = assignEvents();
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
	public void checkPermission(Permission perm) {
		if( isEventEnabled(Events.PERMISSION) )
			fireSafeEvent("PERMISSION, p="+perm.getName());
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
		if( isEventEnabled(Events.PERMISSION) )
			fireSafeEvent("PERMISSION_WITH_CONTEXT, p="+perm.getName()+" c="+context.toString());
	}

	@Override
	public void checkCreateClassLoader() {
		if( isEventEnabled(Events.CLASSLOADER_CREATE) )
			fireSafeEvent("CLASSLOADER_CREATE");
	}

	@Override
	public void checkAccess(Thread t) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent("ACCESS, t="+t.toString());
	}

	@Override
	public void checkAccess(ThreadGroup g) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent("ACCESS, tg="+g.toString());
	}

	@Override
	public void checkExit(int status) {
		if( isEventEnabled(Events.EXIT) )
			fireSafeEvent("EXIT, s="+status);
	}

	@Override
	public void checkExec(String cmd) {
		if( isEventEnabled(Events.FILE_EXECUTE) )
			fireSafeEvent("FILE_EXECUTE, cmd="+cmd);
	}

	@Override
	public void checkLink(String lib) {
		if( isEventEnabled(Events.LINK) )
			fireSafeEvent("LINK, lib="+lib);
	}

	@Override
	public void checkRead(FileDescriptor fd) {
		if( isEventEnabled(Events.FILE_READ) )
			fireSafeEvent("FILE_READ, fd="+fd.toString());
	}

	@Override
	public void checkRead(String file) {
		if( isEventEnabled(Events.FILE_READ) )
			fireSafeEvent("FILE_READ, f="+file);
	}

	@Override
	public void checkRead(String file, Object context) {
		if( isEventEnabled(Events.FILE_READ) )
			fireSafeEvent("FILE_READ, f="+file+" c="+context.toString());
	}

	@Override
	public void checkWrite(FileDescriptor fd) {
		if( isEventEnabled(Events.FILE_WRITE) )
			fireSafeEvent("FILE_WRITE, fd="+fd.toString());
	}

	@Override
	public void checkWrite(String file) {
		if( isEventEnabled(Events.FILE_WRITE) )
			fireSafeEvent("FILE_WRITE, f="+file);
	}

	@Override
	public void checkDelete(String file) {
		if( isEventEnabled(Events.FILE_DELETE) )
			fireSafeEvent("FILE_DELETE, f="+file);
	}

	@Override
	public void checkConnect(String host, int port) {
		if( isEventEnabled(Events.SOCKET_CONNECT) )
			fireSafeEvent("SOCKET_CONNECT, h="+host+" p="+port);
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		if( isEventEnabled(Events.SOCKET_CONNECT) )
			fireSafeEvent("SOCKET_CONNECT, h="+host+" p="+port+" c="+context.toString());
	}

	@Override
	public void checkListen(int port) {
		if( isEventEnabled(Events.SOCKET_LISTEN) )
			fireSafeEvent("SOCKET_LISTEN, p="+port);
	}

	@Override
	public void checkAccept(String host, int port) {
		if( isEventEnabled(Events.SOCKET_ACCEPT) )
			fireSafeEvent("SOCKET_ACCEPT, h="+host+" p="+port);
	}

	@Override
	public void checkMulticast(InetAddress maddr) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) )
			fireSafeEvent("SOCKET_MULTICAST, addr="+maddr.toString());
	}

	@Override
	@Deprecated 
	public void checkMulticast(InetAddress maddr, byte ttl) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) )
			fireSafeEvent("SOCKET_MULTICAST, addr="+maddr.toString()+" ttl="+Integer.toHexString(ttl));
	}

	@Override
	public void checkPropertiesAccess() {
		if( isEventEnabled(Events.PROPERTIES_ANY) )
			fireSafeEvent("PROPERTIES_ANY");
	}

	@Override
	public void checkPropertyAccess(String key) {
		if( isEventEnabled(Events.PROPERTIES_NAMED) )
			fireSafeEvent("PROPERTIES_NAMED, key="+key);
	}

	@Override
	public void checkPrintJobAccess() {
		if( isEventEnabled(Events.PRINT) )
			fireSafeEvent("PRINT");
	}

	@Override
	public void checkPackageAccess(String pkg) {
		if( isEventEnabled(Events.PACKAGE_ACCESS) )
			fireSafeEvent("PACKAGE_ACCESS, pkg="+pkg);
	}

	@Override
	public void checkPackageDefinition(String pkg) {
		if( isEventEnabled(Events.PACKAGE_DEFINE) )
			fireSafeEvent("PACKAGE_DEFINITION, pkg="+pkg);
	}

	@Override
	public void checkSetFactory() {
		if( isEventEnabled(Events.FACTORY) )
			fireSafeEvent("FACTORY");
	}

	@Override
	public void checkSecurityAccess(String target) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent("ACCESS, pkg="+target);
	}

	@Override
	public ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	/**
	 * Fire a thread safe event.  This is required since callers
	 * may trigger additional nested security manager permission
	 * calls resulting in a stack overflow.  We also implement
	 * the callers filter handling.
	 * @param message Message associated with the event.
	 */
	private void fireSafeEvent(String message) {
		
		SafeExecute s = new SafeExecute() {
			public void work() {
				if( filterEvent(message) == FilterActions.ALLOW )
					fireEvent( message );
			}
		};
		s.execute(this);
	}
	
	/**
	 * Fire an event.  This is implemented by callers so that events
	 * can be handled by log systems, SIEMS, etc.  This framework 
	 * provides implementation for some popular systems like logback
	 * and Java logging.
	 * @param message Message associated with the event.
	 */
	protected abstract void fireEvent(String message);
	
	/**
	 * Filter events.  The result of the user definable filter operation
	 * determines if fireEvent() is called.
	 * @param FilterActions FilterActions.ALLOW, framework calls fireEvent().
	 * FilterActions.DENY, fireEvent() will not be called.
	 */
	protected abstract FilterActions filterEvent(String message);
	
	/**
	 * Test if target type of event handling is enabled.
	 * @param event Event to test if enabled.
	 * @return True, the event is enabled.  False, the event is not enabled.
	 */
    private boolean isEventEnabled(Events event) {
    	
    	boolean isenabled = false;
    	for (Events c : usrevents) {
    		if ( event==c ) {
    			isenabled = true;
    		}
    	}
    	return isenabled;
    }
    
    /**
     * Assigns a SecurityManager implementation to use with the NullSecurityManager.
     * Value of the property, NullSecurityManager.SECURITY_MANAGER_OPTION, is used.  If the
     * property is not provided no security manager is used.  No SecurityManager is the
     * default.  To specific the default security assign, "java.lang.SecurityManager" as
     * the property value.  If a specified SecurityManager cannot be loaded an error message
     * is printed, no default is assigned, and processing aborts.
     */
    private void assignSecurityManagerDefault() {

		SafeExecute s = new SafeExecute() {
			public void work() {
		    	String pv = System.getProperty(SECURITY_MANAGER_OPTION, "zzzz");
		    	if ( pv != "zzzz" ) {
		    		
		        	// TODOMS: Idea is to call methods of smd within methods of NullSecurityManager to chain
		        	//         SecurityManager functionality.  Unfortunately, it's going to take more work
		        	//         to determine if NullSecurityManager context information is appropriate.  For
		        	//         example returning context as provided by NullSecurityManager.getClassContext()
		        	//         and NullSecurityManager.getSecurityContext() or if smd.getClassContext() and
		        	//         smd.getSecurityContext() is more appropriate or something else.  For now, I'm
		        	//         disabling this feature.
		        	//
		    		fireSafeEvent("User supported security managers not supported at the moment." );
		    		System.exit(10);
		    		
			    	try {
			    		Class<?> smc = Class.forName(pv);
			    		Object sm = smc.getDeclaredConstructor().newInstance();
			    		if (sm instanceof java.lang.SecurityManager) {
			    			smd = (SecurityManager)sm;
			    		}
			    	} catch(Exception e ) {
			    		fireSafeEvent("SecurityManager implementation not loaded. msg="+e.getMessage()+" class="+pv);
			    		e.printStackTrace();
			    		System.exit(20);
			    		
			    	}
		    	}
			}
		};
		s.execute(this);

    }
    
    /**
     * Default behavior is to assign events from property values (via command line).
     * May be overridden by implementers of NullSecurityManager to enable events of
     * interest.
     * @return An EnumSet of events to fire.
     */
    protected EnumSet<Events> assignEvents() {
    	
    	EnumSet<Events> tevents = EnumSet.noneOf(Events.class);
    	
		SafeExecute s = new SafeExecute() {
			public void work() {
		    	String se = System.getProperty(SECURITY_EVENTS, "zzzz");
		    	if( !se.equals("zzzz") ) {
		    		String[] tokens = se.toUpperCase().split(",");
		    		for( String event: tokens ) {
		    			tevents.add(Events.valueOf(event.trim()));
		
		    		}
		    	}
			}
		};
		s.execute(this);
    
    	return tevents;
    	
    }
    
    
	
}
