package org.owasp.jvmxray.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.exception.JVMXRayConnectionException;
import org.owasp.jvmxray.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JVMXRayClient {

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.webhook.JVMXRayClient");
	
	private URL url;
	private HttpURLConnection connection = null;
	private OutputStreamWriter writer = null;
	
	public JVMXRayClient( URL url ) throws JVMXRayConnectionException {
		if (url == null) {
			throw new JVMXRayConnectionException("Conneciton exception.  msg=URL null");
		}
		this.url = url;		
		
	}
	
	public void openConnection() throws JVMXRayConnectionException {
		try {	
			String protocol = url.getProtocol();
			if( protocol==null && protocol.length() > 0 ) {
				throw new JVMXRayConnectionException("Connection exception.  msg=URL protocol is null");
			}
			protocol = protocol.toUpperCase().trim();
		    // Only HTTP and HTTPS supported
			if( protocol.equals("HTTP") ) {
				connection = (HttpURLConnection)url.openConnection();
			}else if( protocol.equals("HTTPS") ) {
				connection = (HttpsURLConnection)url.openConnection();
			} else {
				throw new JVMXRayConnectionException("Unsupported Protocol.  protocol="+protocol );
			}
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "JVMXRayV1");
			//connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			//connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Accept", "text/html");
			
		} catch( Exception e ) {
			throw new JVMXRayConnectionException("Connection exception.  msg="+e.getMessage(), e );
		}	
	}
	
	public void closeConnection() {
		if ( writer == null ) return;
		try {
			writer.flush();
		} catch( Exception e ) {
		} finally {
			try {
				writer.close();
			} catch (Exception e) {}
		}
	}

	public JVMXRayResponse fireEvent( IEvent event ) throws JVMXRayConnectionException {
		JVMXRayResponse response = null;
		JSONUtil j = JSONUtil.getInstance();
		// If problem getting data, make 5 attempts before giving up.
		int MAX_TRIES = 5, tries = 0;
		JVMXRayConnectionException err = new JVMXRayConnectionException();
		int n=500;
		//
		// If failed to send to server, try again:
		// Tries: 0   Seconds to wait: 0
		//        1                    500
		//        2                    4000
		//        3                   13500
		//        4                   32000
		//
		while( tries < MAX_TRIES ) {
			openConnection();
			String data = j.toJSON(event);
			sendData( data );
			// Note: can only get response after closing connection. 
			closeConnection();
			try {
				response = getResponse();
			} catch( JVMXRayConnectionException e1) {
				err = e1;
				try {
					Thread.sleep(n);
				} catch (InterruptedException e2) {}
				logger.debug("Recoverable error reading from server.  Attempts="+tries, err);
				tries++;
				n=500*(tries^3);
			}
			// Continue on if we received a response.
			if (response != null ) {
				break;
			}
		}
		// Note: after 5 attempts, it's still possible for response to be null 
		// if there are server connection problems.  In this case, we throw the
		// last exception received with it's enclosing exception included.
		if ( response == null ) {
			throw err;
		}

		return response;
		
	}
	
	protected void sendData(String data) throws JVMXRayConnectionException {
		try {
			//int len = data.length();
			connection.connect();
			writer = new OutputStreamWriter( connection.getOutputStream() );
			char[] cdata = data.toCharArray();
			for( char c : cdata ) {
				writer.write( (int)c );
			}
		} catch( IOException e ) {
			throw new JVMXRayConnectionException("Connection exception.  Post data problem.  msg="+e.getMessage(), e);
		}
	}
	
	public JVMXRayResponse getResponse() throws JVMXRayConnectionException {
		StringBuilder response = new StringBuilder();
		int responsecode = 0;
		try {
			responsecode =  connection.getResponseCode();
			if( responsecode == HttpURLConnection.HTTP_OK ) {
				BufferedReader br = new BufferedReader(
				  new InputStreamReader(connection.getInputStream(), "UTF-8"));		
				String line = null;
				while ((line = br.readLine()) != null) {
				  response.append(line.trim());
				}
			}
		} catch( IOException e ) {
			throw new JVMXRayConnectionException("Connection exception.  Response problem.  msg="+e.getMessage(), e);
		}		
		return new JVMXRayResponse(response.toString(), responsecode);
	}
	
}
