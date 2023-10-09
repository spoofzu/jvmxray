package org.jvmxray.server.bin;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVMXRay logservice stub.  Small Java wrapper to initialize a logback logservice to accept
 * logging events from agents and persist them to centralized storage for later
 * processing/reporting.
 * @author Milton Smith
 */
public class logservice {

    private static final String OPT_PORT_SHORT = "p";
    private static final String OPT_PORT_LONG = "port";
    private int iPort = 9876;

    // slf4j logger.
    private static Logger logger = LoggerFactory.getLogger("org.jvmxray.logservice.bin.logservice");

    private void init(LoggerContext lc, int port) {
        logger.info("JVMXRay logservice, initializing.");
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread( ()->{
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        SimpleSocketServer server = new SimpleSocketServer(lc, port);
        server.start();
        logger.info("JVMXRay logservice, running.");
    }

    private void nopLoop() throws InterruptedException {
        while(true) {
            Thread.yield();
            Thread.sleep(250);
        }
    }

    private void shutDown() {
        logger.info("JVMXRay Server, processing completed.");
        System.exit(0);
    }

    private void defineCmdOptions(String[] args, Options options) {
        // PORT Option
        Option helpOption = Option.builder(OPT_PORT_SHORT)
                .longOpt(OPT_PORT_LONG)
                .desc("Server port number to listen for Agents events.")
                .hasArg()
                .argName("PORT NUMBER")
                .build();
        options.addOption(helpOption);
    }

    public static void main(String[] args) {
        // Loiter while logging framework receives/processes security events.
        try {
            logservice obj = new logservice();

            // Define command line options and parameters.
            Options options = new Options();
            obj.defineCmdOptions(args, options);
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            String sPid = cmd.getOptionValue(OPT_PORT_SHORT);
            int port = obj.iPort;
            try {
                port = Integer.parseInt(sPid);
            } catch (NumberFormatException e) {
                logger.error("Error port must be an integer. Defaulting to, "+port);
            }
            logger.info("Server port assigned. iPort=" + port);

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            obj.init(lc, port);
            obj.nopLoop();

        } catch(Throwable t) {
            logger.error("Uncaught exception, logservice exiting.  msg="+t.getMessage(),t);
            System.exit(10);
        }
    }

}