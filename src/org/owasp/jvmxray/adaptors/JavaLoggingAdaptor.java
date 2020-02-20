package org.owasp.jvmxray.adaptors;

import java.util.logging.Logger;

import org.owasp.jvmxray.api.NullSecurityManager;

/**
 * Send events to Java logger.
 * @author Milton Smith
 *
 */
public class JavaLoggingAdaptor extends NullSecurityManager {

	private static final Logger logger = Logger.getLogger("org.owasp.jvmxray.adaptors.JavaLoggingAdaptor");

	@Override
	protected void fireEvent(Events event, String message) {
	
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( message );
		
		logger.info(buff.toString());
		
	}

}