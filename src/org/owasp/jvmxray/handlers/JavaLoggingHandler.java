package org.owasp.jvmxray.handlers;

import java.util.logging.Logger;

import org.owasp.jvmxray.api.NullSecurityManager;


public class JavaLoggingHandler extends NullSecurityManager {

	private static final Logger logger = Logger.getLogger("org.owasp.jvmxray.handlers.JavaLoggignHandler");

	@Override
	protected void fireEvent(Events event, String message) {
	
		StringBuffer buff = new StringBuffer();
		buff.append( event.toString() );
		buff.append( ' ' );
		buff.append( message );
		
		logger.info(buff.toString());
		
	}

}