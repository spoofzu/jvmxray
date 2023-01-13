package org.jvmxray.collector.servlet;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.exception.JVMXRayDBException;
import org.jvmxray.collector.util.DBUtil;
import org.jvmxray.agent.util.JSONUtil;
import org.jvmxray.agent.util.PropertyUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jvmxray.agent.exception.JVMXRayUnimplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMVXRayServlet is used for handling events and managing deployed jvmxrayagents.
 * JVMXRayServlet supports the following REST end-points.
 * <pre>
 * End-point      Format       Description
 * /api/event/    REST/JSON    Accepts events from deployed JVMXRayAgents and persists them
 *                             to relational database.  Returns 200 status on success.
 * /api/echo/     REST/JSON    Accepts events from deployed JVMXRayAgents. Echo's JSON sent
 *                             to server back to the client.  Does not persist events.
 *                             Useful for diagnostics and performance. Returns 200 status
 *                             on success.
 * /api/status/   HTTP(S)/     Displays performance metrics from the web browser like, records
 *                HTML         processed, min transaction time, max transaction time, avg time,
 *                             min/max/and avg event payload sizes (in bytes).
 * </pre>
 * @author Milton Smith
 */
public class JVMXRayServlet extends HttpServlet {

    private static final long serialVersionUID = 7190747012329076227L;

    /** Get logger instance. */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.servlet.JVMXRayServer");
    // Path separator
    private static final String fileSeparator = File.separator;

    private static long trxCount = 0, trxStart = 0, trxEnd = 0;
    private static long trxMin = Integer.MAX_VALUE, trxMax = Integer.MIN_VALUE, trxAvg = 0;
    private static long payloadMin = Long.MAX_VALUE, payloadMax = Long.MIN_VALUE, payloadAvg = 0;
    private static long trxCombined = 0, payloadCombined = 0;

    // Database connection
    private Connection dbconn;
    // Database utilities
    private DBUtil dbutil;

    public JVMXRayServlet() { }

    /**
     * Required per servlet specification.
     * @param req   the {@link HttpServletRequest} object that
     *                  contains the request the client made of
     *                  the servlet
     *
     * @param res  the {@link HttpServletResponse} object that
     *                  contains the response the servlet returns
     *                  to the client
     */
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
            byte[] data = new byte[12040]; //TODO: 12k limitation on raw event objects.
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
                    configPage(req, res);
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
                    configPage(req, res);
                } else if (httpQueryString.startsWith("/api/echo/")) {
                    echoPage(res, content);
                } else if (httpQueryString.startsWith("/api/event/")) {
                    eventPage(res, content);
                } else {
                    res.setStatus(404);
                    res.getWriter().println("<b>The Requested resource not found.  resource=" + httpQueryString + "</b>");
                }
// Future: performant method to check if agent config has been updated.
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

    /**
     * Persist event to relational db.
     * @param event Event from Agent.
     * @throws IOException
     * @throws SQLException
     */
    private void spoolEvent(IEvent event) throws JVMXRayDBException, SQLException {
        //TODO: clean this up.  Eeeek
        EventDAO eDAO = new EventDAO(event.getMatchingRule(), event.getPk(), event.getSt(),
                                     event.getTs(), event.getTid(), event.getEt(),
                                     event.getAid(), event.getStackTrace(), event.getP1(),
                                     event.getP2(),event.getP3());
        dbutil.insertEvent(dbconn,
                eDAO );
    }

    /**
     * Initialize the database prior to use.
     * @throws SQLException Thrown on database operations (e.g., SELECT, UPDATE, INSERT, DELETE, CREATE, etc.)
     * @throws IOException
     * @throws ClassNotFoundException  Thrown on JDBC driver problems.  Missing, etc.
     */
    private final void initDB() throws SQLException, IOException, ClassNotFoundException {
        dbutil = DBUtil.getInstance();
        dbconn = dbutil.createConnection();
        if( dbconn == null ) {
            throw new IOException("JVMXRayServet.initDB(): Unable to initialize database.");
        }
    }

    /**
     * Start timer for status end-point.
     */
    private static synchronized void startTimer() {
        trxStart = System.currentTimeMillis();
        trxCount++;
    }

    /**
     * Stop timer for status end-point.
     */
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

    /**
     * Required per servlet specification.
     * @param config 			the <code>ServletConfig</code> object
     *					that contains configuration
     *					information for this servlet
     *
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // Servlet init() is called but ServletConfig is null on JVMXRay j2ee container.
        //
        // Following performs the server setup and initialization.
        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_SERVER_CONFIG_DEFAULT);
        String server_base = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_BASE_DIRECTORY);
        server_base = (server_base.endsWith(fileSeparator) ) ? server_base : server_base + fileSeparator;
        String agent_config_default_dir = server_base + "agent-config" + fileSeparator +
                "default" + fileSeparator;
        String server_logs_dir = server_base + "logs" + fileSeparator;
        // Create logs dir and parent dirs, if necessary.
        File fi = new File(server_logs_dir);
        if( !fi.exists() ) {
            fi.mkdirs();
            logger.info("Server logs directory created. dir="+fi);
        }
        // Create agent-config dir and parent dirs, if necessary.
        fi = new File(agent_config_default_dir);
        if( !fi.exists() ) {
            fi.mkdirs();
            logger.info("Default agent directory created. dir=" + agent_config_default_dir);
        }
        // Install default set of Agent properties to
        // {server-base}/agent-config/default/default.properties so that
        // all agents can download a default set of properties.
        // Admins must create other properties as desired.
        File dst  = new File(agent_config_default_dir, "default.properties");
        if( !dst.exists() ) {
            try {
               // URL dp = ClassLoader.getSystemResource(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
                URL dp = PropertyUtil.class.getResource(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
                File src = new File(dp.toURI());
                FileUtils.copyFile(src, dst);
                logger.info("Default agent properties installed.  src="+src+" dst="+dst);
            } catch(Exception e) {
                logger.warn("Problem creating default agent properties.  Check server configuration.",e);
            }
        }
    }

    /**
     * Required per servlet specification.  NOT IMPLEMENTED.
     * @return
     */
    @Override
    public ServletConfig getServletConfig() {
        // Not called by JVMXRayServletContainerOld
        throw new JVMXRayUnimplementedException("JVMXRayServlet.getServletConfig(): Not implemented.");
    }

    /**
     * Required per servlet specification.  NOT IMPLEMENTED.
     * @return
     */
    @Override
    public String getServletInfo() {
        // Not called by JVMXRayServletContainerOld
        throw new JVMXRayUnimplementedException("JVMXRayServlet.getServletConfig(): Not implemented.");
    }

    /**
     * Required per servlet specification.  NOT IMPLEMENTED.
     * @return
     */
    @Override
    public void destroy() {
        // Not called by JVMXRayServletContainerOld
        //TODOMS: investigate for using this to close db future connection.
    }

    /**
     * Agent configuration end-point.  Facilitates cloud-based Agent configuration.
     * FUTURE: Facilitates different types of property configuration data depending
     * on the class of agent.  For example, Production agents may have a performant
     * configuration whereas test configurations may be more comprehensive for
     * testing all the various event types.
     * @param res
     * @throws Exception
     */
    private void configPage(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setContentType("text/plain");
        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_SERVER_CONFIG_DEFAULT);
        String fi = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_BASE_DIRECTORY);
        //
        // Load agent specific properties usually looks something like this,
        //    /Users/milton/jvmxray/agent-config/{aid}.properties
        // If no properties agentPropFile exist then try the category,
        //    /Users/milton/jvmxray/agent-config/{category}/default.properties
        // If agent specific propertie or category properties don't exist
        // return a default set of properties at,
        //   /Users/milton/jvmxray/agent-config/default/
        //
        // NOTE: Regardless of which metadata the agent sends it's the server that controls
        //       which properties are returned to agents.
        if (fi !=null && fi.length()>0 ) {
            fi = fi.trim();
            fi = (fi.endsWith(fileSeparator)) ? fi + "agent-config" : fi + fileSeparator + "agent-config";
            fi+=fileSeparator;
        } else {
            res.setStatus(404);
            logger.error("Unable to load agent properties. Bad configuration.  f="+fi);
            return;
        }
        File targetPropFile = null;
        String aid = req.getParameter("aid");
        String cat = req.getParameter( "cat");
        File agentPropFile = null;
        String fn = null;
        // Try to load agent specific properties (by agent id, aid).
        if( aid != null && aid.length()>0 ) {
            fn = aid + ".properties";
            agentPropFile = new File(fi,fn);
            if( agentPropFile.exists() ) {
                targetPropFile = agentPropFile;
            }
        }
        // Try to load category properties.
        File categoryPropFile = null;
        if ( targetPropFile == null ) {
            if (cat != null && cat.length() > 0) {
                fi = cat + fileSeparator;
                fn = "default.properties";
                categoryPropFile = new File(fi, fn);
                if (categoryPropFile.exists()) {
                    targetPropFile = categoryPropFile;
                }
            }
        }
        // If we can't load agent specific/category props then try default properties
        File defaultPropFile = null;
        if( targetPropFile == null ) {
            fi += "default" + fileSeparator;
            fn = "default.properties";
            defaultPropFile = new File(fi,fn);
            if( defaultPropFile.exists() ) {
                targetPropFile = defaultPropFile;
            }
        }
        if( targetPropFile == null ) {
            res.setStatus(404);
            logger.error("No agent property files exists.  Check configuration.");
            return;
        }
        StringBuffer buff = new StringBuffer();
        InputStream in = new DataInputStream(new FileInputStream(targetPropFile));
        int bt;
        char ch;
        try {
            while( (bt = in.read()) != -1 ) {
                ch = (char)bt;
                buff.append(ch);
            }
        } finally {
            if( in != null ) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
        PrintWriter pw = res.getWriter();
        res.setStatus(200);
        pw.print(buff.toString());
    }

    /**
     * Support for the echo entry-point.
     * @param res
     * @param content
     * @throws Exception
     */
    private void echoPage(HttpServletResponse res, String content) throws Exception {
        res.setContentType("text/plain");
        long ct = System.currentTimeMillis();
        long et = 0;
        StringBuffer buff = new StringBuffer();
        String reason = "";
        if (content != null && content.length() > 0 ) {
            JSONUtil j = JSONUtil.getInstance();
            IEvent event = j.eventFromJSON(content);
            long ts = event.getTs();
            et = ct - ts;
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

    /**
     * Support for the event entry-point.
     * @param res
     * @param content
     * @throws Exception
     */
    private void eventPage(HttpServletResponse res, String content) throws Exception {
        res.setContentType("text/plain");
        int response_code = 400;
        JSONUtil j = JSONUtil.getInstance();
        IEvent event = null;
        long et = 0;
        StringBuffer buff = new StringBuffer();
        // Write event to db.
        if (content != null && content.length() > 0 ) {
            event = j.eventFromJSON(content);
            spoolEvent(event);
            response_code=200;
        } else {
            response_code=400;
        }
        res.setStatus(response_code);
        res.getWriter().println(buff.toString());
    }

    /**
     * Support for the status entry-point.
     * @param req
     * @param res
     * @throws Exception
     */
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