package org.owasp.jvmxray.handlers;


import org.owasp.jvmxray.api.NullSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackHandler extends NullSecurityManager {
	
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.handlers.LogbackHandler");
	
	@Override
	public void fireEvent(Events event, String message) {
		
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( message );
		
		logger.info(buff.toString());
		
	}
	
}
