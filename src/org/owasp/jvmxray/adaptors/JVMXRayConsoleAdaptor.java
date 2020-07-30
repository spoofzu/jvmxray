package org.owasp.jvmxray.adaptors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.util.PropertyUtil;

public class JVMXRayConsoleAdaptor extends JVMXRayBaseAdaptor {

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // 2016-11-16 12:08:43 UTC
	
	@Override
	public void fireEvent(IEvent event) {
	
		long ts = event.getTimeStamp();
		TimeZone stz = TimeZone.getTimeZone("UTC"); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date(ts));
		String name = "CONSOLEADAPTOR";
		
		String line = String.format( "%s %s, %s", name, dt, event.toString() );
		System.out.println(line);
	
	}
	
	void init(Properties p) throws Exception {
		super.init(p);
	}
	
	public static final void main(String[] args) {
		try {
			JVMXRayConsoleAdaptor b = new JVMXRayConsoleAdaptor();
			Properties p = PropertyUtil.getInstance().getJVMXRayProperties();
			b.init(p);
		} catch( Throwable t ) {
			t.printStackTrace();
			System.exit(10);
		}
	}

}
