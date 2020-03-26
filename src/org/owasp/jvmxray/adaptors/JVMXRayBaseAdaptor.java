package org.owasp.jvmxray.adaptors;


import java.sql.Connection;
import java.util.Properties;

import org.owasp.jvmxray.util.DBUtil;
import org.owasp.jvmxray.util.EventDAO;
import org.owasp.jvmxray.util.IEvent;
import org.owasp.jvmxray.util.IEventImpl;
import org.owasp.jvmxray.util.PropertyUtil;

abstract public class JVMXRayBaseAdaptor {
	
	private DBUtil dbutil = null;

	public JVMXRayBaseAdaptor() {}

	void init(Properties p) throws Exception {
		dbutil = DBUtil.getInstance(p);
		Connection dbconn = dbutil.createConnection();
		
		do {
			EventDAO originaledao = dbutil.getNextEvent(dbconn, null);
			while( originaledao != null ) {
				fireEvent(new IEventImpl(originaledao));
				EventDAO newedao = dbutil.getNextEvent(dbconn, originaledao);
				dbutil.deleteEvent(dbconn,originaledao);
				originaledao = newedao;
			}
			// Once we processed all events wait N milliseconds and try again.
			Thread.sleep(150);
			Thread.yield();
		} while( isTailingEvents() );
		
	}
	
	/**
	 * Fire an event.  This is implemented by callers so that events
	 * can be handled by log systems, SIEMS, etc.  This framework 
	 * provides implementation for some popular systems like logback
	 * and Java logging.
	 * @param event actual event being processed
	 */
	abstract void fireEvent(IEvent event) throws Exception;
	
	/**
	 * Continue listing for more events?
	 * @return true, continue listening.  false, event processing
	 * concludes and the adaptor terminates successfully.
	 */
	protected boolean isTailingEvents() {
		return true;
	}
	
	
}
