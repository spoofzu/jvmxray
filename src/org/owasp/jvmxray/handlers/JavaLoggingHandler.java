package org.owasp.jvmxray.handlers;

import java.util.EnumSet;
import java.util.logging.Logger;

import org.owasp.jvmxray.api.NullSecurityManager;
import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

public class JavaLoggingHandler extends NullSecurityManager {

	private static final Logger logger = Logger.getLogger("org.owasp.jvmxray.handlers.JavaLoggignHandler");

	@Override
	protected void fireEvent(String message) {
	
		logger.info(message);
		
	}
	
	@Override
	public FilterActions filterEvent(String message ) {
		//TODO: Need to determine a practical set of filtering as a default.
		return FilterActions.ALLOW;
	}
	
	public EnumSet<Events> assignEvents() {   
		
		// For testing you can hardcode your event types.
		//
		// EnumSet<Events> events = EnumSet.of(
		// Events.PERMISSION, Events.CLASSLOADER_CREATE
		// );
		//return EnumSet.allOf(Events.class);
		//
		// Normally, they should be assigned as properties so they can be set on
		// the command line or properties at startup.  Here we set them and let
		// the superclass assign them.
		//
		StringBuffer buff = new StringBuffer();
		buff.append("CLASSLOADER_CREATE, EXEC, EXIT, FACTORY,");
		buff.append("FILE_DELETE, FILE_EXECUTE, FILE_READ, FILE_WRITE,");
		buff.append("LINK, PACKAGE_ACCESS, PACKAGE_DEFINE, PERMISSION,");
		buff.append("PRINT, PROPERTIES_ANY, PROPERTIES_NAMED, SOCKET_ACCEPT,");
		buff.append("SOCKET_CONNECT, SOCKET_LISTEN, SOCKET_MULTICAST");
		
    	String pv = System.getProperty(SECURITY_EVENTS, "zzzz");
    	if( pv == "zzzz" ) { // If no events specified, capture all events.
    		System.setProperty(SECURITY_EVENTS, buff.toString());
    	} 
		
		return super.assignEvents();
	}

}