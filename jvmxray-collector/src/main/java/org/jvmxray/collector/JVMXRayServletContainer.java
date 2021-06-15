package org.jvmxray.collector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Java web application server for testing JVMXRayServlet.
 * <pre>
 * WARNING: J2EE SERVER CODE PROVIDED TO FACILITATE DEVELOPMENT AND IMPROVE
 *          UNIT TEST CASES.  THE SERVER IS NOT INTENDED FOR PRODUCTION USE.
 *          THE J2EE IMPLEMENTATION IS PURPOSEFULLY MINIMAL AND SUPPORTS
 *          ONLY THE LIMIT FEATURES USED BY JVMXRAY.
 * </pre>
 *
 * @author Milton Smith
 */
public class JVMXRayServletContainer {

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.collector.JVMXRayServletContainer");
	private static JVMXRayServletContainer container;
	private static final int PORT = 9123;  //TODOMS: Add default value to configuration
	private boolean bContinue = true;
	private int THREAD_POOL_MAX = 10; //TODOMS: Add default value to configuration

	private JVMXRayServletContainer() {}

	/**
	 * Obtain JVMXRayServletContainer Singlton.
	 * @return Shared JVMXRayServletContainer instance.
	 */
	public static synchronized JVMXRayServletContainer getInstance() {
		JVMXRayServletContainer result = null;
		if( container == null ) {
			container = new JVMXRayServletContainer();
		}
		result = container;
		return result;
	}

	/**
	 * Primary entry point to start container.
	 */
	public void serverStart() {
		ServerSocket server = null;
		try {
			//TODOMS: Need to improve serversocket in case where server has multiple IP addresses/nic cards.
			server = new ServerSocket(PORT);
			server.setReuseAddress(true);
			logger.info("Servlet container initialized. "+server.toString());
			ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_MAX);
			//TODOMS: need smooth method to shutdown server.
			while( bContinue ) {
				Socket socket = server.accept();
				JVMXRayServerThread task = new JVMXRayServerThread(socket);
				executor.execute(task);
			}
			executor.shutdown();
	    }catch(Throwable t ) {
	    	logger.error("Unexpected error.  err="+t.getMessage(),t);
	    	System.exit(10);
	    }finally{
			try {
				if( server!=null) {
					server.close();
				}
			} catch (IOException e) {}
		}
	}

	public void finish() {
		bContinue = false;
	}

}
