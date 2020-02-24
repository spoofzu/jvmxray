package org.owasp.jvmxray.adaptors;

import java.util.logging.Logger;

import org.owasp.jvmxray.api.IJVMXRayEvent;
import org.owasp.jvmxray.api.NullSecurityManager;

/**
 * Send events to Java logger.
 * @author Milton Smith
 *
 */
public class JavaLoggingAdaptor extends NullSecurityManager {

	private static final Logger logger = Logger.getLogger("org.owasp.jvmxray.adaptors.JavaLoggingAdaptor");

	@Override
	protected void fireEvent(IJVMXRayEvent event) {
	
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( event.toString() );
		
		logger.info(buff.toString());
		
	}

}