package org.owasp.jvmxray.adaptors;

import java.util.logging.Logger;

import org.owasp.jvmxray.event.IEvent;

/**
 * Send events to Java logger.
 * @author Milton Smith
 *
 */
public class JVMXRayJavaLoggingAdaptor extends JVMXRayBaseAdaptor {

	private static final Logger logger = Logger.getLogger("org.owasp.jvmxray.adaptors.JavaLoggingAdaptor");

	@Override
	protected void fireEvent(IEvent event) {
	
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		
		logger.info(buff.toString());
		
	}

}
