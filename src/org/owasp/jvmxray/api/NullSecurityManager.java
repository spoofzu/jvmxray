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

import org.owasp.jvmxray.filters.FilterDomainList;
import org.owasp.jvmxray.filters.FilterDomainRule;


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

	// Debugging info, https://docs.oracle.com/javase/7/docs/technotes/guides/security/troubleshooting-security.html
	
	/**
	 * System property name of the security manager to use, <code>nullsecuritymanager.securitymanager</code>
	 * If the property is unspecified then no security manager is used, this is the default.  If a 
	 * specified security manager is provided, NullSecurityManager will pass-thru it's calls.  To use
	 * Java's default SecurityManager the default security properties specify it's fully qualified
	 * class name, <code>java.lang.securitymanager</code>.
	 */
	public static final String SECURITY_MANAGER_OPTION = "jvmxray.securitymanager";
	
	/**
	 * Property name of the events to capture.
	 */
	public static final String SECURITY_EVENTS = "jvmxray.events";
	
	/**
	 * Set of events to process.
	 */
	EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	private SecurityManager smd = null;
	
	private FilterDomainList rulelist = new FilterDomainList();

			
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
	
	public enum FilterActions {
		ALLOW,
		NEUTRAL,
		DENY
	}
	
	protected NullSecurityManager() {
		super();
		assignSecurityManagerDefault();
		initializeFromProperties();
		//usrevents = assignEvents();
		// Turn off default access control checks as specified by java policy file defaults.
		//Policy.setPolicy(new NullPolicy());
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
	public synchronized void checkPermission(Permission perm) {
		if( isEventEnabled(Events.PERMISSION) )
			fireSafeEvent(Events.PERMISSION, "p="+perm.getName(), perm);
	}

	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( isEventEnabled(Events.PERMISSION) )
			fireSafeEvent(Events.PERMISSION, "p="+perm.getName(), perm, context);
	}

	@Override
	public synchronized void checkCreateClassLoader() {
		if( isEventEnabled(Events.CLASSLOADER_CREATE) )
			fireSafeEvent(Events.CLASSLOADER_CREATE, "");
	}

	@Override
	public synchronized void checkAccess(Thread t) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent(Events.ACCESS, "t="+t.toString(), t);
	}

	@Override
	public synchronized void checkAccess(ThreadGroup g) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent(Events.ACCESS, "tg="+g.toString(), g);
	}

	@Override
	public synchronized void checkExit(int status) {
		if( isEventEnabled(Events.EXIT) )
			fireSafeEvent(Events.EXIT, "s="+status, status);
	}

	@Override
	public synchronized void checkExec(String cmd) {
		if( isEventEnabled(Events.FILE_EXECUTE) )
			fireSafeEvent(Events.FILE_EXECUTE, "cmd="+cmd, cmd);
	}

	@Override
	public synchronized void checkLink(String lib) {
		if( isEventEnabled(Events.LINK) )
			fireSafeEvent(Events.LINK,"lib="+lib, lib);
	}

	@Override
	public synchronized void checkRead(FileDescriptor fd) {
// NOTE: Don't believe we can get the file name so this is not useful.
//		if( isEnabled() && isEventEnabled(Events.FILE_READ) )
//			fireSafeEvent(Events.FILE_READ, "fd="+fd.toString(), fd);
	}

	@Override
	public synchronized void checkRead(String file) {
		if( isEventEnabled(Events.FILE_READ) )
			fireSafeEvent(Events.FILE_READ, "f="+file, file);
	}

	@Override
	public synchronized void checkRead(String file, Object context) {
		if( isEventEnabled(Events.FILE_READ) )
			fireSafeEvent(Events.FILE_READ, "f="+file+" c="+context.toString(), file, context);
	}

	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
// NOTE: Don't believe we can get the file name so this is not useful.
//		if( isEnabled() && isEventEnabled(Events.FILE_WRITE) )
//			fireSafeEvent(Events.FILE_WRITE, "fd="+fd.toString(), fd);
	}

	@Override
	public synchronized void checkWrite(String file) {
		if( isEventEnabled(Events.FILE_WRITE) )
			fireSafeEvent(Events.FILE_WRITE, "f="+file, file);
	}

	@Override
	public synchronized void checkDelete(String file) {
		if( isEventEnabled(Events.FILE_DELETE) )
			fireSafeEvent(Events.FILE_DELETE, "f="+file, file);
	}

	@Override
	public synchronized void checkConnect(String host, int port) {
		if( isEventEnabled(Events.SOCKET_CONNECT) )
			fireSafeEvent(Events.SOCKET_CONNECT, host, port);
	}

	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( isEventEnabled(Events.SOCKET_CONNECT) )
			fireSafeEvent(Events.SOCKET_CONNECT, "h="+host+" p="+port, host, port, context);
	}

	@Override
	public synchronized void checkListen(int port) {
		if( isEventEnabled(Events.SOCKET_LISTEN) )
			fireSafeEvent(Events.SOCKET_LISTEN, "p="+port, port);
	}

	@Override
	public synchronized void checkAccept(String host, int port) {
		if( isEventEnabled(Events.SOCKET_ACCEPT) )
			fireSafeEvent(Events.SOCKET_ACCEPT, "h="+host+" p="+port, host, port);
	}

	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) )
			fireSafeEvent(Events.SOCKET_MULTICAST, "addr="+maddr.toString(), maddr);
	}

	@Override
	@Deprecated 
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( isEventEnabled(Events.SOCKET_MULTICAST) )
			fireSafeEvent(Events.SOCKET_MULTICAST, "addr="+maddr.toString()+" ttl="+Integer.toHexString(ttl), maddr, ttl);
	}

	@Override
	public synchronized void checkPropertiesAccess() {
		if( isEventEnabled(Events.PROPERTIES_ANY) )
			fireSafeEvent(Events.PROPERTIES_ANY, "");
	}

	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( isEventEnabled(Events.PROPERTIES_NAMED) )
			fireSafeEvent(Events.PROPERTIES_NAMED, "key="+key, key);
	}

	@Override
	public synchronized void checkPrintJobAccess() {
		if( isEventEnabled(Events.PRINT) )
			fireSafeEvent(Events.PRINT, "");
	}

	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( isEventEnabled(Events.PACKAGE_ACCESS) )
			fireSafeEvent(Events.PACKAGE_ACCESS, "pkg="+pkg, pkg);
	}

	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( isEventEnabled(Events.PACKAGE_DEFINE) )
			fireSafeEvent(Events.PACKAGE_DEFINE, "pkg="+pkg, pkg);
	}

	@Override
	public synchronized void checkSetFactory() {
		if( isEventEnabled(Events.FACTORY) )
			fireSafeEvent(Events.FACTORY, "");
	}

	@Override
	public synchronized void checkSecurityAccess(String target) {
		if( isEventEnabled(Events.ACCESS) )
			fireSafeEvent(Events.ACCESS, "pkg="+target, target);
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
	private void fireSafeEvent(Events event, String message, Object ...obj ) {
		
		SafeExecute s = new SafeExecute() {
			public void work() {
				if( filterEvent(event, obj) == FilterActions.ALLOW )
					fireEvent( event, message );
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
	protected abstract void fireEvent(Events event, String message);
	
	/**
	 * xxx
	 * @param FilterActions FilterActions.ALLOW, framework calls fireEvent().
	 * FilterActions.DENY, fireEvent() will not be called.
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
		    		String error = "Chaining security managers is not yet supported.";
		    		RuntimeException e1 = new RuntimeException(error);
		    		throw e1;
		    		
//			    	try {
//			    		Class<?> smc = Class.forName(pv);
//			    		Object sm = smc.getDeclaredConstructor().newInstance();
//			    		if (sm instanceof java.lang.SecurityManager) {
//			    			smd = (SecurityManager)sm;
//			    		}
//			    	} catch(Exception e2 ) {
//			    		System.err.println("SecurityManager implementation not loaded. msg="+e2.getMessage()+" class="+pv);
//			    		e2.printStackTrace();
//			    		System.exit(20);
//			    		
//			    	}
		    	}
			}
		};
		s.execute(this);

    }
    
	
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
