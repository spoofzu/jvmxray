package org.jvmxray.collector.bin;

import org.jvmxray.collector.microcontainer.JVMXRayServletContainer;
import org.owasp.security.logging.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVMXRay HTTP REST server.
 */
public class jvmxrayserver {

    /** Get logger instance. */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.bin.jvmxrayserver");

    /**
     * CTOR
     */
    private jvmxrayserver() {}

    /**
     * Entry point.
     * @param args Not used.
     */
    public static void main(String[] args) {
        jvmxrayserver server = new jvmxrayserver();
        server.execute();
    }

    /**
     * Retrieve service instance and start it.
     */
    private void execute() {
        try {
            SecurityUtil.logShellEnvironmentVariables();
            SecurityUtil.logJavaSystemProperties();
            JVMXRayServletContainer service = JVMXRayServletContainer.getInstance();
            service.startService();
        }catch(Throwable t) {
            // Log error and force exit (in event threads are running).
            logger.error("Unexpected service exception.",t);
            System.exit(10);
        }
    }

}