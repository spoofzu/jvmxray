package org.owasp.jvmxray.collector;

import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Simple Java web application server for testing JVMXRayServlet.
 * This is not a fully functional J2EE Servlet container.  Many features
 * are not implemented.  JVMXRayServletContainer is only intended for
 * testing JVMXRayServlet.
 * 
 * @author Milton Smith
 *
 */
public class JVMXRayServletContainer {

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.collector.JVMXRayServletContainer");
	private static JVMXRayServletContainer container;
	private static final int PORT = 9123;
	private boolean bContinue = true;
	
	private JVMXRayServletContainer() {}

	public static synchronized JVMXRayServletContainer getInstance() {
		JVMXRayServletContainer result = null;
		if( container == null ) {
			container = new JVMXRayServletContainer();
		}
		result = container;
		return result;
	}

	public synchronized void listen() {
		try {
	    	int PORT = 9123;
//			ServerSocket server = new ServerSocket(PORT, 10, InetAddress.getByName("0.0.0.0"));
			ServerSocket server = new ServerSocket(PORT);
			logger.info("Servlet container initialized. "+server.toString());

			while( bContinue ) {
				 Socket socket = server.accept();
				 new JVMXRayServerThread(socket).start();
			}
			
	    }catch(Throwable t ) {
	    	logger.error("Unexpected error.  err="+t.getMessage(),t);
	    }
	}

	public synchronized void finish() {
		bContinue = false;
	}


	
	
}
