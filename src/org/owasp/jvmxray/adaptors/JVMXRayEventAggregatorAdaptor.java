package org.owasp.jvmxray.adaptors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.owasp.jvmxray.adaptors.util.JVMXRayBaseEventAggregator;
import org.owasp.jvmxray.adaptors.util.JVMXRaySortableListItem;
import org.owasp.jvmxray.util.IEvent;
import org.owasp.jvmxray.util.PropertyUtil;

public class JVMXRayEventAggregatorAdaptor extends JVMXRayBaseAdaptor {

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // 2016-11-16 12:08:43 UTC	
	private JVMXRayBaseEventAggregator aggevents = new JVMXRayBaseEventAggregator();
	private volatile boolean bInitialized = false;
	
	public JVMXRayEventAggregatorAdaptor() {
	}

	
	private void initialize() throws MalformedURLException, IOException {
		Properties p = PropertyUtil.getJVMXRayProperties();
		String finterval = p.getProperty(PropertyUtil.CONF_PROP_EVENT_AGG_FILE_INTERVAL, "30");
		Integer interval = Integer.valueOf(finterval);
			
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
			     try {
					updateEventsFile();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
		}, 250, interval*1000);		
	}
	
	@Override
	public void fireEvent(IEvent event) throws MalformedURLException, IOException {
	
		// Initialize once, set interval timer, define the workflow.
		if (!bInitialized) { 
			bInitialized = true;
			initialize();
		}
		
		synchronized(aggevents) { 
			System.out.println("event fired, "+event);
			aggevents.fireEvent(event);
		}
		
	}
	
	private void updateEventsFile() throws MalformedURLException, IOException {
		
		TimeZone stz = TimeZone.getTimeZone("UTC"); // Default timezone of the server.
		df.setTimeZone(stz); 
		String dt = df.format(new Date());
		
		Properties p = PropertyUtil.getJVMXRayProperties();
		String fname = p.getProperty(PropertyUtil.CONF_PROP_EVENT_AGG_FILE);
		File file = null;
		PrintWriter writer = null;
		
		try {
			
			//TODOMS: Improve to test if user has file permissions to create folders & files.
			
			file = new File(fname);
			System.out.println("writing events to file, "+fname);
			// create directory tree if not present
			//file.getParentFile().mkdirs();
			writer = new PrintWriter(file, "UTF-8");
			
			writer.println( "EVENTAGGREGATOR" );
			writer.println( dt );
			writer.println( "----"); 

			synchronized(aggevents) { 
				JVMXRaySortableListItem[] events = aggevents.getEvents();
				for( JVMXRaySortableListItem sortable : events ) {
					IEvent event = sortable.getEvent();
					
					System.out.println("writing event, "+event.toString());
					
					String et = event.getEventType();
					String ct = Integer.toString(sortable.getCount());
					String it = event.getIdentity();
					String me = event.getMemo();
					String st = event.getStackTrace();
					String line = String.format( "%s %s %s %s %s %s", dt, ct, it, et, me, st );
					writer.println( line );
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

	void init(Properties p) throws Exception {
		super.init(p);
	}
	
	public static final void main(String[] args) {
		try {
			JVMXRayEventAggregatorAdaptor b = new JVMXRayEventAggregatorAdaptor();
			Properties p = PropertyUtil.getJVMXRayProperties();
			b.init(p);
		} catch( Throwable t ) {
			t.printStackTrace();
			System.exit(10);
		}
	}
	
}
