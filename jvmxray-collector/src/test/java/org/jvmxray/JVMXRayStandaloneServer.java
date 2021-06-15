package org.jvmxray;

import org.jvmxray.collector.JVMXRayServletContainer;


/**
 *  Run JVMXRay Server from the command line.  Listens for incoming Agent
 *  connections and logs events.
 *  <pre>
 *  WARNING: J2EE SERVER CODE PROVIDED TO FACILITATE DEVELOPMENT AND IMPROVE
 *           UNIT TEST CASES.  THE SERVER IS NOT INTENDED FOR PRODUCTION USE.
 *           THE J2EE IMPLEMENTATION IS PURPOSEFULLY MINIMAL AND SUPPORTS
 *           ONLY THE LIMIT FEATURES USED BY JVMXRAY.
 *  </pre>
 *
 *  @author Milton Smith
 */
public class JVMXRayStandaloneServer {

    /**
     * CTOR
     */
    public JVMXRayStandaloneServer() {
    }

    /**
     * Primary entry-point
     * @param args Command line arguments.  NOT USED
     */
    public static void main(String[] args) {
        JVMXRayStandaloneServer server = new JVMXRayStandaloneServer();
        server.startServer();
    }

    // Server entry-point
    private void startServer() {
        try {
            JVMXRayServletContainer server = JVMXRayServletContainer.getInstance();
            server.serverStart();
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

}
