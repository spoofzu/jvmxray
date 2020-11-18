package org.owasp.jvmxray.server.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.servlet.ServletConfig;

import org.owasp.jvmxray.util.HttpRAWParse;
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
public class JVMXRayServletContainer extends Thread {

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.webhook.JVMXRayServletContainer");
	
	private static JVMXRayServletContainer container;
	
	private JVMXRayServletContainer() {}
	
	public static synchronized JVMXRayServletContainer getInstance() {
		JVMXRayServletContainer result = null;
		if( container == null ) {
			container = new JVMXRayServletContainer();
		}
		result = container;
		return result;
	}
	
	// Note: the servlet container runs in it's own thread.  Required so calls from unit testing 
	// framework (junit) are not blocked when server starts and dispatches worker threads to handle
	// servlet requests.
	// 
	public void run() {
		try {
	    	int PORT = 9123;
			ServerSocket server = new ServerSocket(PORT, 10, InetAddress.getByName("127.0.0.1"));
		
			logger.info("*** JVMXRayServletContainer started on port "+PORT+" ***");
		
			/**
			 * Listen for new client connections.
			 */
		    Socket socket = null;
			while(true) {
			    try {
					server.setSoTimeout(20000);
			    	socket = server.accept();
			    	// Create a new thread to service each client connection.
			    	final Socket threadsocket = socket;
			    	Thread worker = new Thread() {	
				    	public void run() {
				    		try {
							    // Setup for the servlet request/response
							    InputStream cin = threadsocket.getInputStream();
							    OutputStream cout = threadsocket.getOutputStream();
							    StringWriter bout = new StringWriter();
								HttpRAWParse rp = new HttpRAWParse(cin);
								rp.parseRequest();
								String content = rp.getContent();
								content = (content == null ) ? "" : content;
								String httpMethod = rp.getMethod();
								String httpQueryString = rp.getQueryString();
								ByteArrayInputStream fin = new ByteArrayInputStream(content.getBytes());
								JVMXRayServletInputStream in = new JVMXRayServletInputStream(fin);
							    
								// Create a servlet thread for the client.
								// Note: many servlet features not implemented. Calling an unimplemented method throws an exception.
								// Note: keep in mine the streams used by JVMXRayServlet are copies.
							    JVMXRayServlet xrayServlet = new JVMXRayServlet();
							    xrayServlet.init((ServletConfig)null); //TODOMS: ServletConfig
							    JVMXRayServletRequest req = new JVMXRayServletRequest( in, content, httpMethod, httpQueryString ); 
							    JVMXRayServletResponse res = new JVMXRayServletResponse( bout ); 
							    
							    // Call service to process the client connection.
							    xrayServlet.service(req, res); 
							    res.getWriter().flush();
							    
							    // Finish up and send data from buffers to client
							    String EOL = "\r\n";
							    String status = HttpRAWParse.getHttpReply(res.getStatus());
							    String date = HttpRAWParse.getDateHeader();
							    String body = bout.toString();
							    
							    // Construct header to send back to client.
							    int contentsz = body.getBytes("UTF-8").length;
							    String h1 = "HTTP/1.1 " + status + EOL;
							    String h2 = "Content-Type: text/html; charset=UTF-8" + EOL;
							    String h3 = "Date: " + date + EOL;
							    String h4 = "Connection: keep-alive" + EOL;  //TODOMS: experiment with keep-alive/close.
							    String h5 = "Content-Length: " + contentsz + EOL;
							    String h6 = EOL;
							    String h7 = body + EOL;
							    
							    String response = h1 + h2 + h3 + h3 + h4 + h5 + h6 + h7;
							    
							    // Insert HTTP header into client response stream.
							    cout.write(response.getBytes("UTF-8"));
							    
							    cout.flush();
							    cout.close();
				    		} catch( Throwable t ) {
						    	logger.error("Unhandled server connection exception.",t);
				    		} finally {
			    				try {
									if( threadsocket != null ) {
										threadsocket.close();
									}
								} catch (IOException e) {}
				    		}
				    	}
				    };
				    // Start a new client connection thread.
				    worker.start();
			    	
			    } catch( SocketTimeoutException e ) {
			    	logger.error("Recoverable SocketTimeoutException.  Closing socket and wait for a new connection.",e);
			    	continue;
			    } catch( Throwable t ) {
			    	logger.error("Unhandled exception.  Closing socket and wait for a new connection.",t);
			    	break;
			    } 
			}
			
	    }catch(Throwable t ) {
	    	logger.error("*** JVMXRayServletContainer exited. Unexpected error.  err="+t.getMessage()+"***",t);
	    } finally {
	    	logger.info("*** JVMXRayServletContainer exited. ***");
	    }
		
	}


	
	
}
