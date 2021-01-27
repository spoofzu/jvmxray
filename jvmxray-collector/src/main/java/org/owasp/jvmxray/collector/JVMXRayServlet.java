package org.owasp.jvmxray.collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.exception.JVMXRayUnimplementedException;
import org.owasp.jvmxray.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test JVMXRay servlet.  JVMXRayServletContainer runs JVMXRayServlet and supports unit testing.
 * JVMXRayServletContain is not a fully functional J2EE servlet container.  Many features that
 * are not used by JVMXRayServlet have not be implemented.  At the present, JVMXRayServlet
 * supports the following REST end-points.
 * End-point      Format       Description
 * /api/echo/     REST/JSON    Accept events from NullSecurityManager and echo them back.
 * /api/status/   Web page 	   Displays performance metrics from the browser like, records
 *                             processed, min transaction time, max transaction time, avg time,
 *                             min/max/and avg event payload sizes (in bytes).
 * @author Milton Smith
 *
 */
public class JVMXRayServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7190747012329076227L;

	/** Get logger instance. */
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.server.test.JVMXRayServer");

    private static long trxCount = 0, trxStart = 0, trxEnd = 0;
    private static long trxMin = Integer.MAX_VALUE, trxMax = Integer.MIN_VALUE, trxAvg = 0;
    private static long payloadMin = Long.MAX_VALUE, payloadMax = Long.MIN_VALUE, payloadAvg = 0;
    private static long trxCombined = 0, payloadCombined = 0;
    
    public JVMXRayServlet() { }
    
	// Required per Servlet Spec
	@Override
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		String httpMethod = "";
    	String httpQueryString = "";
    	long szContent = 0;
    	
		try {
			
			// Start timer to measure performance.
			startTimer();

			// Place body content from a ServletInputStream to String
			String content = "";
			ServletInputStream in = req.getInputStream();
			ByteArrayOutputStream buff = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[1024];
			while ((nRead = req.getInputStream().read(data, 0, data.length)) != -1) {
				buff.write(data, 0, nRead);
		    }
			buff.flush();
		    byte[] byteArray = buff.toByteArray();   
		    content = new String(byteArray, StandardCharsets.UTF_8);
		    szContent = content.length();
			
		    //  Grab some request properties
		    httpMethod = req.getMethod();
			httpQueryString = req.getQueryString();
		    System.out.println("JVMXRayServlet.service() PATH: " + httpMethod + " " + httpQueryString);
		  
		    // TODOMS: probably best to elimate GET method for data transfer
		    // to minimize logging concerns.
		    if (httpMethod.equals("GET")) {
				if (httpQueryString.startsWith("/api/status/")) {
					statusPage(res);
				} else if (httpQueryString.startsWith("/api/config/")) {
						configPage(res);
				} else if (httpQueryString.startsWith("/api/echo/")) {
					echoPage(res, content);
				} else {
			    	res.setStatus(404);
			    	res.getWriter().println("<b>The Requested resource not found.  resource="+httpQueryString+"</b>");
				}
		    } else if ( httpMethod.equals("POST") ) {
				if (httpQueryString.startsWith("/api/status/")) {
					statusPage(res);
				} else if (httpQueryString.startsWith("/api/config/")) {
						configPage(res);
				} else if (httpQueryString.startsWith("/api/echo/")) {
					echoPage(res, content);
				} else {
					res.setStatus(404);
					res.getWriter().println("<b>The Requested resource not found.  resource=" + httpQueryString + "</b>");
				}
			}else if (httpMethod.equals("HEAD") ) {
				if (httpQueryString.startsWith("/api/config/")) {
					configPageLastUpdate(res);
				} else {
					res.setStatus(404);
					res.getWriter().println("<b>The Requested resource not found.  resource=" + httpQueryString + "</b>");
				}
		    } else { 	
		    	res.setStatus(404);
		    	res.getWriter().println("<b>No supported method.  method="+httpMethod+"</b>");
		    }
		    
		} catch (Exception e) {
		    logger.error( "httpMethod="+httpMethod+" httpQueryString="+httpQueryString, e);
		} finally {
			// Note: slight possibility szContent will not be assigned, depeneding upon where an exception occurs.  If so,
			// we simply accept that our content sizes may be approximate.
		    stopTimer(szContent);
		}
		
	}
	
	private static synchronized void startTimer() {
		trxStart = System.currentTimeMillis();
		trxCount++;
	}
	
	private static synchronized void stopTimer(long contentSize) {
	    trxEnd = System.currentTimeMillis();
	    long elapsed = trxEnd - trxStart;
	    trxCombined += elapsed;
	    trxAvg = trxCombined/trxCount;
	    trxMin = (elapsed < trxMin ) ? elapsed: trxMin;
	    trxMax = (elapsed > trxMax ) ? elapsed: trxMax;
	    
	    payloadCombined += contentSize;
	    payloadAvg = payloadCombined/trxCount;
	    payloadMin = ( contentSize < payloadMin ) ? contentSize : payloadMin;
	    payloadMax = ( contentSize > payloadMax ) ? contentSize : payloadMax;
	    
	}

	// Required per Servlet Spec
	@Override
	public void init(ServletConfig config) throws ServletException {
		// Servlet init() is called but config is null.
	}

	// Required per Servlet Spec
	@Override
	public ServletConfig getServletConfig() {
		// Not called by JVMXRayServletContainer
		throw new JVMXRayUnimplementedException("JVMXRayServlet.getServletConfig(): Not implemented.");	
	}
	
	// Required per Servlet Spec
	@Override
	public String getServletInfo() {
		// Not called by JVMXRayServletContainer
		throw new JVMXRayUnimplementedException("JVMXRayServlet.getServletConfig(): Not implemented.");	
	}

	// Required per Servlet Spec
	@Override
	public void destroy() {
		// Not called by JVMXRayServletContainer
	}

	private void configPage(HttpServletResponse res) throws Exception {
		SimpleDateFormat format;
		String ret;
		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("PST"));
		ret = format.format(new Date()) + " PST";

		PrintWriter writer = res.getWriter();
		writer.println("<h1>JVMXRay: Client Transaction Performance</h1>");;
		writer.println("Last update(server time) "+ret);
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Total Transactions Processed = "+trxCount);
		writer.println("<br/>");
		writer.println("Min Transaction Elapsed Time(ms) = "+trxMin );
		writer.println("<br/>");
		writer.println("Max Transaction Maximum Elapsed Time(ms) = "+trxMax );
		writer.println("<br/>");
		writer.println("Avg Transaction Performance(ms) = "+trxAvg );
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Min Payload Size(bytes) = "+payloadMin );
		writer.println("<br/>");
		writer.println("Max Payload Size(bytes) = "+payloadMax );
		writer.println("<br/>");
		writer.println("Avg Payload Size(bytes) = "+payloadAvg );
		writer.println("<br/>");

		writer.println("");
		res.setStatus(200);
	}

	private void configPageLastUpdate(HttpServletResponse res) throws Exception {
		SimpleDateFormat format;
		String ret;
		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("PST"));
		ret = format.format(new Date()) + " PST";

		PrintWriter writer = res.getWriter();
		writer.println("<h1>JVMXRay: Client Transaction Performance</h1>");;
		writer.println("Last update(server time) "+ret);
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Total Transactions Processed = "+trxCount);
		writer.println("<br/>");
		writer.println("Min Transaction Elapsed Time(ms) = "+trxMin );
		writer.println("<br/>");
		writer.println("Max Transaction Maximum Elapsed Time(ms) = "+trxMax );
		writer.println("<br/>");
		writer.println("Avg Transaction Performance(ms) = "+trxAvg );
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Min Payload Size(bytes) = "+payloadMin );
		writer.println("<br/>");
		writer.println("Max Payload Size(bytes) = "+payloadMax );
		writer.println("<br/>");
		writer.println("Avg Payload Size(bytes) = "+payloadAvg );
		writer.println("<br/>");

		writer.println("");
		res.setStatus(200);
	}

    private void echoPage(HttpServletResponse res, String content) throws Exception {
	    JSONUtil j = JSONUtil.getInstance();
	    IEvent event = null;
	    long ct = System.currentTimeMillis();
	    long et = 0;
	    StringBuffer responseBuffer = new StringBuffer();
	    String reason = "";
	    if (content != null && content.length() > 0 ) {
		    event = j.fromJSON(content);
		    et = ct - event.getTimeStamp();
	    } else {
	    	reason = "(content null or zero)";
	    	et = -1;
	    }
		responseBuffer.append("<h1>Echo:</h1>").
		append("<code>").append(content).append("</code>").
		append("Elapsed(ms)="+et+" "+reason);
		String c = responseBuffer.toString();
    	res.setStatus(200);
    	res.getWriter().println(c);
    }
    
    private void statusPage(HttpServletResponse res) throws Exception {
	    SimpleDateFormat format;
	    String ret;
	    format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	    format.setTimeZone(TimeZone.getTimeZone("PST"));
	    ret = format.format(new Date()) + " PST";
	    
    	PrintWriter writer = res.getWriter();
		writer.println("<h1>JVMXRay: Client Transaction Performance</h1>");;
		writer.println("Last update(server time) "+ret);
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Total Transactions Processed = "+trxCount);
		writer.println("<br/>");
		writer.println("Min Transaction Elapsed Time(ms) = "+trxMin );
		writer.println("<br/>");
		writer.println("Max Transaction Maximum Elapsed Time(ms) = "+trxMax );
		writer.println("<br/>");
		writer.println("Avg Transaction Performance(ms) = "+trxAvg );
		writer.println("<br/>");
		writer.println("<br/>");
		writer.println("Min Payload Size(bytes) = "+payloadMin );
		writer.println("<br/>");
		writer.println("Max Payload Size(bytes) = "+payloadMax );
		writer.println("<br/>");
		writer.println("Avg Payload Size(bytes) = "+payloadAvg );
		writer.println("<br/>");
		
		writer.println("");
    	res.setStatus(200);
    }
    
}