package org.owasp.jvmxray.adaptors;


import org.owasp.jvmxray.api.IJVMXRayEvent;
import org.owasp.jvmxray.api.NullSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send events to logback logger.
 * @author Milton Smith
 *
 */
public class JVMXRayLogbackAdapter extends NullSecurityManager {
	
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.adaptors.LogbackAdapter");
	
	@Override
	public void fireEvent(IJVMXRayEvent event) {
		
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( event.toString() );
		
		logger.info(buff.toString());
		
	}
	
}
