package org.owasp.jvmxray.adaptors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.owasp.jvmxray.api.NullSecurityManager;



public class ConsoleAdaptor extends NullSecurityManager {
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // 2016-11-16 12:08:43 PST
	
	@Override
	public void fireEvent(Events event, String message) {
	
		TimeZone stz = TimeZone.getDefault(); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date());
		
		StringBuffer buff = new StringBuffer();
		buff.append( "CONSOLEADAPTOR " );
		buff.append( dt );
		buff.append( ' ' );
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( message.toString() );
		
		System.out.println(buff.toString());
	
	}

}
