package org.owasp.jvmxray;

import java.util.EnumSet;
import java.util.logging.Logger;

public class JavaLoggingEventSink extends NullSecurityManager {

	private static final Logger logger = Logger.getLogger("org.owasp.security.logging.util.JavaLoggingEventSink");

	@Override
	protected void fireEvent(String message) {
	
		logger.info(message);
		
	}
	
	public EnumSet<Events> assignEvents() {
		
		// To process user events from command line simple call, return super.getEnabledEvents();
		
		EnumSet<Events> events = EnumSet.of(Events.PERMISSION);
		
		return events;
	}

}