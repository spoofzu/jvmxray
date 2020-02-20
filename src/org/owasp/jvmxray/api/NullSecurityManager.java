package org.owasp.jvmxray.api;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.security.Permission;
import java.security.Policy;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;


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
	public static final String SECURITY_MANAGER_OPTION = "jvmxray.securitymanager";
	
	/**
	 * Property name of the events to capture.  Acceptable events are those specified by the enum Events.
	 * The default is all events.
	 */
	public static final String SECURITY_EVENTS = "jvmxray.events";
	
	/**
	 * Stacktrace detail property.  Settings are specified in NullSecurityManager.Callstack
	 * and described in the jvmxray.properties file.
	 */
	public static final String SECURITY_STACKTRACE = "jvmxray.event.stacktrace";
	
	/**
	 * Set of events to process.
	 */
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	/**
	 * Callstack options. 
	 */
	private Callstack callstackopt = Callstack.NONE;
	
	
	//private SecurityManager smd = null;
	
	private FilterDomainList rulelist = new FilterDomainList();
	
	private volatile boolean bSupressRecursion = false;
	
	private StackTraceElement[] stacktrace = null;

			
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
	public enum Events {
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
		if( isEventEnabled(Events.PERMISSION) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PERMISSION, new Object[] {perm,callstack}, "n=%s, a=%s, s=%s, stack=%s", perm.getName(),perm.getActions(),perm.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( isEventEnabled(Events.PERMISSION) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PERMISSION, new Object[] {perm,context,callstack}, "n=%s, a=%s, s=%s, ctx=%s, stack=%s", perm.getName(),perm.getActions(),perm.toString(),context.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkCreateClassLoader() {
		if( isEventEnabled(Events.CLASSLOADER_CREATE) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.CLASSLOADER_CREATE, new Object[] {callstack}, "stack=%s", callstack);
		}
	}

	@Override
	public synchronized void checkAccess(Thread t) {
		if( isEventEnabled(Events.ACCESS) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.ACCESS, new Object[] {t,callstack}, "t=%s, stack=%s", t.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkAccess(ThreadGroup g) {
		if( isEventEnabled(Events.ACCESS) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.ACCESS, new Object[] {g,callstack}, "tg=%s, stack=%s", g.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkExit(int status) {
		if( isEventEnabled(Events.EXIT) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.EXIT, new Object[] {status,callstack}, "s=%s, stack=%s", status, callstack);
		}
	}

	@Override
	public synchronized void checkExec(String cmd) {
		if( isEventEnabled(Events.FILE_EXECUTE) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.FILE_EXECUTE, new Object[] {cmd,callstack}, "cmd=%s, stack=%s", cmd, callstack);
		}
	}

	@Override
	public synchronized void checkLink(String lib) {
		if( isEventEnabled(Events.LINK) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.LINK, new Object[] {lib,callstack}, "lib=%s, stack=%s", lib, callstack);
		}
	}

	@Override
	public synchronized void checkRead(FileDescriptor fd) {
		// NOTE: Don't believe we can get the file name so this is not useful.
	}

	@Override
	public synchronized void checkRead(String file) {
		if( isEventEnabled(Events.FILE_READ) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.LINK, new Object[] {file,callstack}, "f=%s, stack=%s", file, callstack);
		}
	}

	@Override
	public synchronized void checkRead(String file, Object context) {
		if( isEventEnabled(Events.FILE_READ) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.FILE_READ, new Object[] {file,context,callstack}, "f=%s, c=%s, stack=%s", file, context.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
		// NOTE: Don't believe we can get the file name so this is not useful.
	}

	@Override
	public synchronized void checkWrite(String file) {
		if( isEventEnabled(Events.FILE_WRITE) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.FILE_WRITE, new Object[] {file,callstack}, "f=%s, stack=%s", file, callstack);
		}
	}

	@Override
	public synchronized void checkDelete(String file) {
		if( isEventEnabled(Events.FILE_DELETE) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.FILE_DELETE, new Object[] {file,callstack}, "f=%s, stack=%s", file, callstack);
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port) {
		if( isEventEnabled(Events.SOCKET_CONNECT) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_CONNECT, new Object[] {host,port,callstack}, "h=%s, p=%s, stack=%s", host, port, callstack);
		}
	}

	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( isEventEnabled(Events.SOCKET_CONNECT) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_CONNECT, new Object[] {port,context,callstack}, "h=%s, p=%s, ctx=%s, stack=%s", host, port, context.toString(), callstack);
		}
	}

	@Override
	public synchronized void checkListen(int port) {
		if( isEventEnabled(Events.SOCKET_LISTEN) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_LISTEN, new Object[] {port,callstack}, "p=%i, stack=%s", port, callstack);
		}
	}

	@Override
	public synchronized void checkAccept(String host, int port) {
		if( isEventEnabled(Events.SOCKET_ACCEPT) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_ACCEPT, new Object[] {host,port,callstack}, "h=%s, p=%s, stack=%s", host, port, callstack);
		}
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_MULTICAST, new Object[] {maddr,callstack}, "maddr=%s, stack=%s", maddr.toString(), callstack);
		}
	}

	@Override
	@Deprecated 
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.SOCKET_MULTICAST, new Object[] {maddr,ttl,callstack}, "maddr=%s, ttl=%s, stack=%s", maddr, Byte.toString(ttl), callstack);
		}
	
	}

	@Override
	public synchronized void checkPropertiesAccess() {
		if( isEventEnabled(Events.PROPERTIES_ANY) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PROPERTIES_ANY, new Object[] {callstack}, "stack=%s", callstack);
		}
	}

	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( isEventEnabled(Events.PROPERTIES_NAMED) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PROPERTIES_NAMED, new Object[] {key,callstack}, "key=%s, stack=%s", key, callstack);
		}
	}

	@Override
	public synchronized void checkPrintJobAccess() {
		if( isEventEnabled(Events.PRINT) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PRINT, new Object[] {callstack}, "stack=%s", callstack);
		}
	}

	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( isEventEnabled(Events.PACKAGE_ACCESS) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PACKAGE_ACCESS, new Object[] {pkg,callstack}, "pkg=%s, stack=%s", pkg, callstack);
		}
	}

	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( isEventEnabled(Events.PACKAGE_DEFINE) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.PACKAGE_DEFINE, new Object[] {pkg,callstack}, "pkg=%s, stack=%s", pkg, callstack);
		}
	}

	@Override
	public synchronized void checkSetFactory() {
		if( isEventEnabled(Events.FACTORY) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.FACTORY, new Object[] {callstack}, "stack=%s", callstack);
		}
	}

	@Override
	public synchronized void checkSecurityAccess(String target) {
		if( isEventEnabled(Events.ACCESS) ) {
			String callstack = "";
			if( callstackopt != Callstack.NONE ) {
				stacktrace = Thread.currentThread().getStackTrace();
			}
			callstack = generateCallStack(callstackopt);
			processEvent(Events.ACCESS, new Object[] {target,callstack}, "t=%s, stack=%s", target, callstack);
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
	private void processEvent(Events event, Object[] obj1, String format, Object ...obj2 ) {
		
		try {
			if( bSupressRecursion ) {
				return;
			}
			if( filterEvent(event, obj1, format, obj2) == FilterActions.ALLOW ) {
				bSupressRecursion = true;
				fireEvent( event, obj1, format, obj2 );
				bSupressRecursion = false;
			}
		}catch(Throwable t) {
			t.printStackTrace();
			bSupressRecursion = false;
		}
	}
	
	/**
	 * Fires an event.  Similar to NullSecurityManager.fireEvent(Event,String) with the
	 * exception that subclasses can improve the default message format
	 * provided by NullSecurityManager.  For example, PERMISSION events are formatted
	 * by default like this, n=%s, a=%s, s=%s", where the variables are assigned
	 * like this perm.getName(),perm.getActions()+",s="+perm.toString(), respectively.
	 * Designers can override this and alter the format based on message type as they
	 * choose.  Designers may wish to do this for any number of reasons like
	 * creating CSV file output, etc.
	 * @param event Type of event fired such as Events.PACKAGE_DEFINE
	 * @param obj1 Method arguments as passed by java.lang.SecurityManager.
	 * @param format Default format (e.g., String.format()) for specified message type.
	 * @param obj2 Variable number of arguments (e.g., String.format()) dependent upon event type.
	 */
	protected void fireEvent(Events event, Object[] obj1, String format, Object ...obj2) {
		
		 //    if event is, Events.PERMISSION,          obj is,     Permission || Permission, Object
		 //    if event is, Events.CLASSLOADER_CREATE,  obj is,     ""
		 //    if event is, Events.EXIT,                obj is,     int
		 //    if event is, Events.EXEC,                obj is,     String
		 //    if event is, Events.LINK,                obj is,     String
		 //    if event is, Events.FILE_READ,           obj is,     String || String, Object
		 //    if event is, Events.FILE_WRITE,          obj is,     String
		 //    if event is, Events.FILE_DELETE,         obj is,     String
		 //    if event is, Events.SOCKET_CONNECT,      obj is,     String, int || String, int, Object
		 //    if event is, Events.SOCKET_LISTEN,       obj is,     int
		 //    if event is, Events.SOCKET_ACCEPT,       obj is,     String, int
		 //    if event is, Events.SOCKET_MULTICAST,    obj is,     InetAddress, byte(optional)
		 //    if event is, Events.PROPERTIES_ANY,      obj is,     ""
		 //    if event is, Events.PRINT,               obj is,     ""
		 //    if event is, Events.PACKAGE_ACCESS,      obj is,     String
		 //    if event is, Events.PACKAGE_DEFINE,      obj is,     String
		 //    if event is, Events.FACTORY,             obj is,     ""
		 //    if event is, Events.ACCESS,              obj is,     String || Thread || ThreadGroup
		
		String message = String.format( format, obj2 );
		
		
		fireEvent( event, message );
		
	}
	
	/**
	 * Fire an event.  This is implemented by callers so that events
	 * can be handled by log systems, SIEMS, etc.  This framework 
	 * provides implementation for some popular systems like logback
	 * and Java logging.
	 * @param message Message associated with the event.
	 */
	protected abstract void fireEvent(Events event, String message);
	
	/**
	 * Process event filters
	 * @return FilterActions.ALLOW or FilterActions.DENY.
	 */
	private FilterActions filterEvent(Events event, Object ...obj) {
		
		return rulelist.filterEvents( event, obj);
		
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
     * Value of the property, NullSecurityManager.SECURITY_MANAGER_OPTION, is used.  If the
     * property is not provided no security manager is used.  No SecurityManager is the
     * default.  To specific the default security assign, "java.lang.SecurityManager" as
     * the property value.  If a specified SecurityManager cannot be loaded an error message
     * is printed, no default is assigned, and processing aborts.<br/>
     * CURRENTLY NOT FUNCTIONAL
     */
    private void assignSecurityManagerDefault() {


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
     * Generate a callstack based upon specification.
     * @param callstack Type of callstack to generate.
     */
    protected String generateCallStack(Callstack callstack) {
    	
    	StringBuffer buff = new StringBuffer();
    	
    	switch ( callstack ) {
 
    		case LIMITED:
    			Class[] clz = getClassContext();
    			for (Class c : clz ) {
    				buff.append(c.getName());
    				buff.append("->");
    			}
    			break;
    	
    		case FULL:
    			if (stacktrace==null) break;
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
    
    
	/**
	 * Initialize the NullSecurityManager subclass via property settings.
	 */
    private void initializeFromProperties() {
    
    	try {
    	
    		// Load jvmxray.properties
	    	Properties p = new Properties();
	    	InputStream in = null;
	    	try {
		    	 in = getClass().getResourceAsStream("/jvmxray.properties");
		    	 p.load(in);
	    	}finally {
	       	 if( in != null )
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    	
	    	// Get the trace level
	    	String lvl = p.getProperty(SECURITY_STACKTRACE);
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
	             FilterDomainRule fdr = (FilterDomainRule)cd.newInstance(gvents,filteraction, np);
	             
	            		 
	             // Add the rule to the list
	         	 rulelist.add( fdr );
	             
	    	}
	   
    	
    	} catch( Exception e ) {
    		e.printStackTrace();
    	}
    	
    }
    
    
}
