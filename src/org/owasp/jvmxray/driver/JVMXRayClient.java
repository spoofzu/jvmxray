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

public abstract class JVMXRayClient {

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.webhook.JVMXRayClient");
	protected int MAX_TRIES = 5;
	private URL url;
	private HttpURLConnection connection = null;
	private OutputStreamWriter writer = null;
	
	public JVMXRayClient( URL url ) throws JVMXRayConnectionException {
		if (url == null) {
			throw new JVMXRayConnectionException("Conneciton exception.  msg=URL null");
		}
		this.url = url;		
		
	}

	public void fireEvent() throws JVMXRayConnectionException {
		try {
			IEvent event = getEvent();
			JVMXRayResponse response = _fireEvent(event);
			finishConnection( response );
		} catch( Exception e ) {
			throw new JVMXRayConnectionException("Connection exception.  msg="+e.getMessage(), e);
		}
	}

	public abstract void startConnection(HttpURLConnection connection) throws Exception;
	
	public abstract IEvent getEvent() throws Exception;
	
	public abstract void finishConnection(JVMXRayResponse response) throws Exception;
	
	public abstract int retries( int currentAttempt );
	
	private void openConnection() throws JVMXRayConnectionException {
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
			startConnection(connection);
		} catch( Exception e ) {
			throw new JVMXRayConnectionException("Connection exception.  msg="+e.getMessage(), e );
		}	
	}
	
	private void closeConnection() {
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
	
	private JVMXRayResponse _fireEvent( IEvent event ) throws JVMXRayConnectionException {
		JVMXRayResponse response = null;
		JSONUtil j = JSONUtil.getInstance();
		// If problem getting data, make 5 attempts before giving up.
		int tries = 0;
		JVMXRayConnectionException err = new JVMXRayConnectionException();
		int n=retries(tries);
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
				n=retries(tries);
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
	
	private void sendData(String data) throws JVMXRayConnectionException {
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
	
	private JVMXRayResponse getResponse() throws JVMXRayConnectionException {
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
