package org.owasp.jvmxray.adaptors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.owasp.jvmxray.adaptors.uti.JVMXRayBaseEventAggregator;
import org.owasp.jvmxray.adaptors.uti.JVMXRaySortableListItem;
import org.owasp.jvmxray.api.IJVMXRayEvent;
import org.owasp.jvmxray.api.NullSecurityManager;

public class JVMXRayEventAggregatorAdaptor extends NullSecurityManager {

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // 2016-11-16 12:08:43 UTC	
	private JVMXRayBaseEventAggregator aggevents = new JVMXRayBaseEventAggregator();
	private volatile boolean bInitialized = false;
	
	public JVMXRayEventAggregatorAdaptor() {
		// TODO Auto-generated constructor stub
	}

	
	private void initialize() {
	
		String finterval = jvmxrayProperties.getProperty(NullSecurityManager.CONF_PROP_EVENT_AGG_FILE_INTERVAL, "30");
		Integer interval = Integer.valueOf(finterval);
			
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
			     updateEventsFile();
			  }
		}, 250, interval*1000);
		
	}
	
	@Override
	public void fireEvent(IJVMXRayEvent event) {
	
		// Initialize once, set interval timer, define the workflow.
		if (!bInitialized) { 
			bInitialized = true;
			initialize();
		}
		
		synchronized(aggevents) { 
			aggevents.fireEvent(event);
		}
		
	}
	
	private void updateEventsFile() {
		
		TimeZone stz = TimeZone.getTimeZone("UTC"); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date());
		
		String fname = jvmxrayProperties.getProperty(NullSecurityManager.CONF_PROP_EVENT_AGG_FILE);
		File file = null;
		PrintWriter writer = null;
		
		try {
			
			//TODOMS: Improve to test if user has file permissions to create folders & files.
			
			file = new File(fname);
			// create directory tree if not present
			//file.getParentFile().mkdirs();
			writer = new PrintWriter(file, "UTF-8");
			
			writer.println( "EVENTAGGREGATOR" );
			writer.println( dt );
			writer.println( "----"); 

			synchronized(aggevents) { 
				JVMXRaySortableListItem[] events = aggevents.getEvents();
				for( JVMXRaySortableListItem event : events ) {
					writer.println( event.toString() );
				}
			}
			
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			if( writer != null ) {
				writer.flush();
				writer.close();
			}
		}
		
	}
	
}
