package org.owasp.jvmxray.adaptors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.owasp.jvmxray.api.IJVMXRayEvent;
import org.owasp.jvmxray.api.NullSecurityManager;

/**
 * Send events to the system console (System.out).
 * @author Milton Smith
 *
 */
public class JVMXRayConsoleAdaptor extends NullSecurityManager {
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // 2016-11-16 12:08:43 UTC
	
	@Override
	public void fireEvent(IJVMXRayEvent event) {
	
		TimeZone stz = TimeZone.getTimeZone("UTC"); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date());
		
		StringBuffer buff = new StringBuffer();
		buff.append( "CONSOLEADAPTOR " );
		buff.append( dt );
		buff.append( ' ' );
		buff.append( event.toString() );
		
		System.out.println(buff.toString());
	
	}

	
}
