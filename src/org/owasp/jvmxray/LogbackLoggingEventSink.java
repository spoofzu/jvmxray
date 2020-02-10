package org.owasp.jvmxray;

import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackLoggingEventSink extends NullSecurityManager {
	
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.security.logging.util.LogbackLoggingEventSink");
	
	@Override
	public void fireEvent(String message) {
		
		logger.info(message);
		
	}

	public EnumSet<Events> assignEvents() {
	
		// Handy event reference.
		//
		// CLASSLOADER_CREATE The CLASSLOADER_CREATE event captures privilege requests to create a new ClassLoader.
		// EXEC  	  	      The EXEC event captures privilege requests when creating new subprocesses. 
		// EXIT               The EXIT event captures privilege requests to halt the Java Virtual Machine with specified status code.
		// FACTORY            The FACTORY event captures privilege requests to set the socket factory used by ServerSocket or Socket, or the stream handler factory used by URL.
		// FILE_DELETE        The FILE_DELETE event captures privilege requests to delete the specified file.
		// FILE_EXECUTE       The FILE_EXECUTE event captures privilege requests to execute specified subprocesses. 
		// FILE_READ  	      The FILE_READ event captures privilege requests to read a specified file or file descriptor.
		// FILE_WRITE  	      The FILE_WRITE event captures privilege requests to write to the specified file or file descriptor.
		// LINK               The LINK event captures privilege requests to dynamically link the specified library.
		// PACKAGE_ACCESS     The PACKAGE event captures privilege requests to access specified package.  
		// PACKAGE_DEFINE     The PACKAGE event captures privilege requests to define classes in the specified package.
		// PERMISSION         The PERMISSION event captures privilege requests to privileged resources.  Optional resource context information may be included.
		// PRINT              The PRINT event captures privilege requests by application to print.
		// PROPERTIES_ANY     The PROPERTIES_ANY event captures privilege requests to access System Properties.
		// PROPERTIES_NAMED   The PROPERTIES_NAMED event captures privilege requests to access named System Properties.
		// SOCKET_ACCEPT      The SOCKET_ACCEPT event captures privilege requests to accept socket connections at the specified host and port.
		// SOCKET_CONNECT     The SOCKET_CONNECT event captures privilege requests to open socket connections to the specified host and port.
		// SOCKET_LISTEN      The SOCKET_LISTEN event captures privilege requests to halt the Java Virtual Machine with specified status code.
		// SOCKET_MULTICAST   The SOCKET_MULTICAST event captures privilege requests to listen to socket connections on a specified port.    
		
		// Hard code your properties.
		//
		// EnumSet<Events> events = EnumSet.of(
		// Events.PERMISSION, Events.CLASSLOADER_CREATE
		// );
		//return EnumSet.allOf(Events.class);
		
		// Or enter them as properties and let super class do the work.
		//
		StringBuffer buff = new StringBuffer();
		buff.append("CLASSLOADER_CREATE, EXEC, EXIT, FACTORY,");
		buff.append("FILE_DELETE, FILE_EXECUTE, FILE_READ, FILE_WRITE,");
		buff.append("LINK, PACKAGE_ACCESS, PACKAGE_DEFINE, PERMISSION,");
		buff.append("PRINT, PROPERTIES_ANY, PROPERTIES_NAMED, SOCKET_ACCEPT,");
		buff.append("SOCKET_CONNECT, SOCKET_LISTEN, SOCKET_MULTICAST");
		System.setProperty(SECURITY_EVENTS, buff.toString());
		
		return super.assignEvents();
	}

}
