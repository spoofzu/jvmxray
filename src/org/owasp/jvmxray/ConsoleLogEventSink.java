package org.owasp.jvmxray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;


public class ConsoleLogEventSink extends NullSecurityManager {
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); //2016-11-16 12:08:43
	
	@Override
	public void fireEvent(String message) {
	
		TimeZone stz = TimeZone.getDefault(); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date());
		System.out.println(dt+" "+message);
	
	}

	public EnumSet<Events> assignEvents() {
	
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
		
    	String pv = System.getProperty(SECURITY_EVENTS, "zzzz");
    	if( pv == "zzzz" ) { // If no events specified, capture all events.
    		System.setProperty(SECURITY_EVENTS, buff.toString());
    	} 
		
		return super.assignEvents();
	}

}
