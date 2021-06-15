package org.jvmxray.collector;

import org.jvmxray.util.HttpRAWParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import java.io.*;
import java.net.Socket;

/**
 * Worker threads to receive and process inbound Agent connections.
 * <pre>
 * WARNING: J2EE SERVER CODE PROVIDED TO FACILITATE DEVELOPMENT AND IMPROVE
 *          UNIT TEST CASES.  THE SERVER IS NOT INTENDED FOR PRODUCTION USE.
 *          THE J2EE IMPLEMENTATION IS PURPOSEFULLY MINIMAL AND SUPPORTS
 *          ONLY THE LIMIT FEATURES USED BY JVMXRAY.
 * </pre>
 *
 * @author Milton Smith
 */
public class JVMXRayServerThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.collector.JVMXRayServletThread");
    private final Socket socket;

    /**
     * CTOR
     * @param socket Agent socket passed by JVMXRayServletContainer
     */
    public JVMXRayServerThread( final Socket socket ) {
        this.socket = socket;
    }

    /**
     * Thread entry point.
     */
    public void run() {
        try {
            logger.info("Agent connection accepted. host/ip="+socket.getInetAddress().getHostAddress());

            // Setup for the servlet request/response
            InputStream cin = socket.getInputStream();
            OutputStream cout = socket.getOutputStream();
            StringWriter bout = new StringWriter();
            HttpRAWParse rp = new HttpRAWParse(cin);
            rp.parseRequest();
            String content = rp.getContent();
            content = (content == null ) ? "" : content;
            String httpMethod = rp.getMethod();
            String httpQueryString = rp.getQueryString();
            ByteArrayInputStream fin = new ByteArrayInputStream(content.getBytes());
            JVMXRayServletInputStream in = new JVMXRayServletInputStream(fin);

            // Note: many servlet features not implemented. Calling an unimplemented method throws an exception.
            // Note: keep in mine the streams used by JVMXRayServlet are copies.
            JVMXRayServlet xrayServlet = new JVMXRayServlet();
            xrayServlet.init((ServletConfig)null); //NOTE: ServletConfig is unassigned but not used.
            JVMXRayServletRequest req = new JVMXRayServletRequest( socket, in, content, httpMethod, httpQueryString );
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
            String h2 = "Server: JVMXRay v0.0.1 " + EOL;
            String h3 = "Content-Type: "+res.getContentType()+"; charset=UTF-8" + EOL;
            String h4 = "Date: " + date + EOL;
            String h5 = "Connection: keep-alive" + EOL;  //TODOMS: experiment with keep-alive/close.
            String h6 = "Content-Length: " + contentsz + EOL;
            String h7 = EOL;
            String h8 = body + EOL;
            String response = h1 + h2 /* + h3 */ +  h4 + h5 + h6 + h7 + h8;

            // Insert HTTP header into client response stream.
            cout.write(response.getBytes("UTF-8"));

            cout.flush();
            cout.close();
        } catch( Throwable t ) {
            logger.error("Unhandled server connection exception.",t);
        } finally {
            logger.info("Agent connection closed. host/ip="+socket.getInetAddress().getHostAddress());
            try {
                if( socket != null ) {
                    socket.close();
                }
            } catch (IOException e) {}
        }
    }
}
