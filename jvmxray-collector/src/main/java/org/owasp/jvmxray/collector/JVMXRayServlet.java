package org.owasp.jvmxray.collector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.owasp.jvmxray.util.DBUtil;
import org.owasp.jvmxray.util.JSONUtil;
import org.owasp.jvmxray.util.PropertyUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.exception.JVMXRayUnimplementedException;
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

	// Database connection
	private Connection dbconn;

	// Database utilities
	private DBUtil dbutil;

	public JVMXRayServlet() { }
    
	// Required per Servlet Spec
	@Override
	public void service(HttpServletRequest req, HttpServletResponse res) {

		String httpMethod = "";
    	String httpQueryString = "";
    	long szContent = 0;

		try {
			// Start timer to measure performance.
			startTimer();
			// First time?  If so, initilize connection to database.
			synchronized (this) {
				if (dbconn == null) {
					initDB();
				}
			}
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
			String threadid = Thread.currentThread().getName()+":"+Thread.currentThread().getId();
		    logger.info("Path: " + httpMethod + " Query String" + httpQueryString);
		    // TODOMS: probably best to elimate GET method for data transfer
		    // to minimize logging concerns.
		    if (httpMethod.equals("GET")) {
				if (httpQueryString.startsWith("/api/status/")) {
					statusPage(req, res);
				} else if (httpQueryString.startsWith("/api/config/")) {
						configPage(res);
				} else if (httpQueryString.startsWith("/api/echo/")) {
					echoPage(res, content);
				} else if (httpQueryString.startsWith("/api/event/")) {
					eventPage(res, content);
				} else {
			    	res.setStatus(404);
			    	res.getWriter().println("<b>The Requested resource not found.  resource="+httpQueryString+"</b>");
				}
		    } else if ( httpMethod.equals("POST") ) {
				if (httpQueryString.startsWith("/api/status/")) {
					statusPage(req, res);
				} else if (httpQueryString.startsWith("/api/config/")) {
						configPage(res);
				} else if (httpQueryString.startsWith("/api/echo/")) {
					echoPage(res, content);
				} else if (httpQueryString.startsWith("/api/event/")) {
					eventPage(res, content);
				} else {
					res.setStatus(404);
					res.getWriter().println("<b>The Requested resource not found.  resource=" + httpQueryString + "</b>");
				}
//			}else if (httpMethod.equals("HEAD") ) {
//				if (httpQueryString.startsWith("/api/config/")) {
//					configPageLastUpdate(res);
//				} else {
//					res.setStatus(404);
//					res.getWriter().println("<b>The Requested resource not found.  resource=" + httpQueryString + "</b>");
//				}
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

	private void spoolEvent(IEvent event) throws IOException, SQLException {
		dbutil.insertEvent(dbconn,
				event );
	}

	private final void initDB() throws SQLException, IOException {
		PropertyUtil pu = PropertyUtil.getInstance();
		Properties p = pu.getServerProperties();

		dbutil = DBUtil.getInstance(p);
		dbconn = dbutil.createConnection();
		if( dbconn == null ) {
			throw new IOException("JVMXRayServet.initDB(): Unable to initialize database.");
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
		//TODOMS: investigate for using this to close db future connection.
	}

	private void configPage(HttpServletResponse res) throws Exception {
    	res.setContentType("text/plain");
		SimpleDateFormat format;
		String ret;
		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("PST"));
		ret = format.format(new Date()) + " PST";

		StringBuffer buff = new StringBuffer();
		InputStream in = null;
		try {
			//TODOMS: Need a property for this and grab from file on disk.
			String surl = PropertyUtil.SYS_PROP_CLIENT_DEFAULT;
			in = PropertyUtil.class.getResourceAsStream(surl);
			int bt;
			char ch;
			while( (bt = in.read()) != -1 ) {
				ch = (char)bt;
				buff.append(ch);
			}
		} finally {
			if( in != null )
				try {
					in.close();
					in = null;
				} catch (IOException e) {}
		}
		PrintWriter pw = res.getWriter();
		res.setStatus(200);
		pw.print(buff.toString());
	}

    private void echoPage(HttpServletResponse res, String content) throws Exception {
		res.setContentType("text/plain");
	    JSONUtil j = JSONUtil.getInstance();
	    IEvent event = null;
	    long ct = System.currentTimeMillis();
	    long et = 0;
	    StringBuffer buff = new StringBuffer();
	    String reason = "";
	    if (content != null && content.length() > 0 ) {
		    event = j.fromJSON(content);
		    et = ct - event.getTimeStamp();
	    } else {
	    	reason = "(content null or zero)";
	    	et = -1;
	    }
	    int sz = content.length();
	    buff.append("Response[");
	    buff.append(sz);
	    buff.append("bytes/");
	    buff.append(et);
	    buff.append("ms] Reason[");
	    buff.append(reason);
	    buff.append("]=");
		buff.append(content);
    	res.setStatus(200);
    	res.getWriter().println(buff.toString());
    }

	private void eventPage(HttpServletResponse res, String content) throws Exception {
		res.setContentType("text/plain");
		int response_code = 400;
		JSONUtil j = JSONUtil.getInstance();
		IEvent event = null;
		long et = 0;
		StringBuffer buff = new StringBuffer();
		// Write event to local sqllite db.
		if (content != null && content.length() > 0 ) {
			event = j.fromJSON(content);
			spoolEvent(event);
			response_code=200;
		} else {
			response_code=400;
		}
		res.setStatus(response_code);
		res.getWriter().println(buff.toString());
	}
    
    private void statusPage(HttpServletRequest req, HttpServletResponse res) throws Exception {
		res.setContentType("text/html");
	    SimpleDateFormat format;
	    String ret;
	    format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	    format.setTimeZone(TimeZone.getTimeZone("PST"));
	    ret = format.format(new Date()) + " PST";
	    
    	PrintWriter writer = res.getWriter();
		writer.println("<h1>JVMXRay: Client Transaction Performance</h1>");;
		writer.println("Last update(server time), "+ret);
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