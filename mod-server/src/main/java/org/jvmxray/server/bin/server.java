package org.jvmxray.server.bin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class server {

    // slf4j logger.
    private static Logger logger = LoggerFactory.getLogger("org.jvmxray.server.bin.server");

    private void init() {
        logger.info("JVMXRay Server, initializing.");
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread( ()->{
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        logger.info("JVMXRay Server, shutdown hook registered.");
    }

    private void nopLoop() throws InterruptedException {
        while(true) {
            Thread.yield();
            Thread.sleep(250);
        }
    }

    private void shutDown() {
        logger.info("JVMXRay Server, server exiting.");
        System.exit(0);
    }

    public static void main(String[] args) {
        // Loiter while logging framework receives/processes security events.
        try {
            server inst = new server();
            inst.init();
            inst.nopLoop();
        } catch(Throwable t) {
            logger.error("Uncaught exception, server exiting.  msg="+t.getMessage(),t);
            System.exit(10);
        }
    }

}