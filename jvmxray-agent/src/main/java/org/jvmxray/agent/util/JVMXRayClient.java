package org.jvmxray.agent.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.exception.JVMXRayConnectionException;
import org.jvmxray.agent.exception.JVMXRayException;

public abstract class JVMXRayClient {

	private static final String NIX_EOL = "\n";

	// Lightweight logger
	private LiteLogger ltlogger = LiteLogger.getLoggerinstance();

	protected int MAX_TRIES = 5;
	private URL url;
	private HttpURLConnection connection = null;
	private OutputStreamWriter writer = null;
	
	public JVMXRayClient( URL url ) throws JVMXRayConnectionException {
		if (url == null) {
			throw new JVMXRayConnectionException("Connection exception.  msg=URL null");
		}
		this.url = url;		
		
	}

	public void fireEvent() throws JVMXRayException {
		IEvent event = getEvent();
		JVMXRayResponse response = _fireEvent(event);
		finishConnection( response );
	}

	public void startConnection(HttpURLConnection connection) throws Exception {
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("User-Agent", "JVMXRayV1");
		//connection.setRequestProperty("Accept", "*/*");
		connection.setRequestProperty("Content-Type", "application/json; utf-8");
		//connection.setRequestProperty("Accept-Language", "en-US");
		connection.setRequestProperty("Accept", "text/html");
	}
	
	public abstract IEvent getEvent() throws JVMXRayException;
	
	public void finishConnection(JVMXRayResponse response) {
		int responsecode = response.getResponseCode();
		String responsedata = response.getResponseData();
		ltlogger.debug("JVMXRayClient.finshConnection(): status code="+responsecode+
				" server data["+responsedata.length()+"bytes]=" + responsedata);
	}
	
	public int retries( int currentAttempt ) {
		//
		// If failed to send to server, try again:
		// Tries: 0   Milliseconds to wait: 0
		//        1                         500
		//        2                         4000
		//        3                         13500
		//        4                         32000
		//
		return 500 * (currentAttempt ^ 3);
	}
	
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
	
	private JVMXRayResponse _fireEvent(IEvent event) throws JVMXRayConnectionException {
		JVMXRayResponse response = null;
		// If problem getting data, make 5 attempts before giving up.
		int tries = 0;
		int n=retries(tries);
		JVMXRayConnectionException lastException = null;
		JSONUtil ju = JSONUtil.getInstance();
		while( tries < MAX_TRIES ) {
			openConnection();
			String data = ju.eventToJSON(event);
			data += NIX_EOL;
			sendData( data );
			//Note: can only get response after closing connection.
			closeConnection();
			try {
				response = getResponse();
			} catch( JVMXRayConnectionException e1) {
				try {
					// Each failed attempt sleeps an increasing amount of time.
					Thread.sleep(n);
				} catch (InterruptedException e2) {}
				System.out.println("Error retrieving server response, recovering. Attempts="+tries);
				tries++;
				n=retries(tries);
				lastException = e1;
			}
			// Continue on if we received a response.
			if (response != null ) {
				break;
			}
		}
		// Note: after 5 attempts, it's still possible for response to be null 
		// if there are server connection problems.  In this case, we throw the
		// last exception received and move on.
		if ( response == null ) {
			throw lastException;
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
