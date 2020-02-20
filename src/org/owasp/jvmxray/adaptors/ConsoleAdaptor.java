package org.owasp.jvmxray.adaptors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.owasp.jvmxray.api.NullSecurityManager;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

/**
 * Send events to the system console (System.out).
 * @author Milton Smith
 *
 */
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

//	public void fireEvent(Events event, Object[] obj1, String format, Object ...obj2) {
//		
//		// Accept the default message format or change it to meet your specifications.
//		// Example shows simple compressed CSV file format.  
//		//
//		// A more performant method is to precompute or hardcode the format based on
//		// event type.  Code provided is simply to demonstrate what is possible.  Of course,
//	    // this only improves the message so you would want to override
//      // fireEvents(Events,String) will need to be improved if CSV is desired.  For example,
//      // change the following default,
//      // CONSOLEADAPTOR 2020-02-20 12:43:22 PST
//      // to something like,
//      // 2020-02-20,12:43:22,PST
//      // alternatively change to UTC or timestamp format for simplicy when processing
//	    // with scripts, log analyzers, secuirty tools, etc.
//		//
//      //
//		StringBuffer buff = new StringBuffer();
//		int len = obj2.length;
//		for (int i=0; i<len; i++) {
//			buff.append("%s,");
//		}
//		if(len>0)
//			buff.setLength(buff.length()-1);
//		String message = String.format( buff.toString(), obj2 );
//		
//		fireEvent( event, message );
//		
//	}
	
}
