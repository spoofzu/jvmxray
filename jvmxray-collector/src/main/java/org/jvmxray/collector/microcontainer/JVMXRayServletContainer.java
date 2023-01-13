package org.jvmxray.collector.microcontainer;

import org.jvmxray.agent.util.PropertyUtil;
import org.owasp.security.logging.util.DefaultIntervalLoggerModel;
import org.owasp.security.logging.util.DefaultIntervalProperty;
import org.owasp.security.logging.util.IntervalLoggerController;
import org.owasp.security.logging.util.SecurityLoggingFactory;
import org.owasp.security.logging.util.IntervalProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletInputStream;

/**
 * Specialized servlet micro-container for easy IDE project testing. It's not
 * recommended for production or any serious work.
 * @author Milton Smith
 */
public class JVMXRayServletContainer {

    /** Get logger instance. */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.micrcontainer.JVMXRayServletContainer");
    /** Server instance. */
    private static JVMXRayServletContainer container;
    private int iAvailableProcessors = Runtime.getRuntime().availableProcessors();
    //TODO: May need tuning.
    private static final int CPU_THREADS_MAX = 15;
    //TODO: May need tuning.
    private static final int CPU_THREADS_MIN = 1;
    //TODO: May need tuning.
    private static final int CPU_THREADS_CORE = 1;
    //TODO: May need tuning.
    private static final int CPU_THREADS_TTL = 30000;


    /**
     * CTOR
     */
    private JVMXRayServletContainer() {}

    /**
     * Only a single instance of the server allowed.
     * @return Return existing instance or create a new instance.
     */
    public static synchronized JVMXRayServletContainer getInstance() {
        JVMXRayServletContainer result = null;
        if( container == null ) {
            container = new JVMXRayServletContainer();
        }
        result = container;
        return result;
    }

    /**
     *
     * @throws UnknownHostException Thrown on host resolver problem.  Check host as specified by
     * PropertyUtil.SYS_PROP_SERVER_IP_HOST.
     */
    public void startService() throws IOException {
        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_SERVER_CONFIG_DEFAULT);
        String sHost = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_IP_HOST, "");
        if( sHost == null || sHost.equals("") ) {
            logger.warn("Invalid/missing server host name.  Using localhost as default.");
            sHost = "localhost";
        }
        int iDefaultPort = 9123;
        int iPort = 9123;
        try {
            iPort = pu.getIntProperty(PropertyUtil.SYS_PROP_SERVER_IP_PORT, iDefaultPort);
        }catch(NumberFormatException e) {
            logger.warn("Invalid server port.  Using default "+iDefaultPort+".  err="+e.getMessage()+" raw value="+
                    pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_IP_PORT));
            iPort = 9123;
        }
        // Set ceiling on number of threads.
        int iMax = iAvailableProcessors * CPU_THREADS_MAX;
        int iCore = iAvailableProcessors * CPU_THREADS_CORE;
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
        ThreadPoolExecutor tpeServicePool =  new ThreadPoolExecutor(iCore,
                                                                    iMax,
                                                                    CPU_THREADS_TTL,
                                                                    TimeUnit.MILLISECONDS,
                                                                    queue);
        boolean bContinue = true;
        InetAddress inetHost = InetAddress.getByName(sHost);
        ServerSocket server = new ServerSocket(iPort, iMax, inetHost);
        // Setup interval logger.
        IntervalLoggerController wd = SecurityLoggingFactory.getControllerInstance();
        // Add our service metrics to the interval logger.
        DefaultIntervalLoggerModel model = new DefaultIntervalLoggerModel();
        model.addProperty( new DefaultIntervalProperty("ActiveThreads") {
                                public void refresh() {
                                   value = Integer.toString(tpeServicePool.getActiveCount());
                               }
                           }
        );
        model.addProperty( new DefaultIntervalProperty("PoolSize") {
                               public void refresh() {
                                   value = Integer.toString(tpeServicePool.getPoolSize());
                               }
                           }
        );
        model.addProperty( new DefaultIntervalProperty("LargestPoolSize") {
                               public void refresh() {
                                   value = Integer.toString(tpeServicePool.getLargestPoolSize());
                               }
                           }
        );
        model.addProperty( new DefaultIntervalProperty("CompletedTaskCount") {
                               public void refresh() {
                                   value = Long.toString(tpeServicePool.getCompletedTaskCount());
                               }
                           }
        );
        // Remove default properties we don't need.
        IntervalProperty[] properties = model.getProperties();
        for ( IntervalProperty i : properties ) {
            String propertyName = i.getName();
            if( propertyName.equals("ThreadNew") ||
                propertyName.equals("ThreadRunnable") ||
                propertyName.equals("ThreadBlocked") ||
                propertyName.equals("ThreadWaiting") ||
                propertyName.equals("ThreadTerminated")) {
                    model.removeProperty(i);

            }
        }
        wd.setStatusMessageModel(model);
        // Update interval to every 15 seconds
        wd.start(15000);
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread tSDHook = new Thread( ()->{
            logger.warn("Shutdown request received.  Performing clean-up activities.");
            if( tpeServicePool != null ) tpeServicePool.shutdown();
            if( wd != null ) wd.stop();
        });
        Runtime.getRuntime().addShutdownHook(tSDHook);
        logger.info("REST services initialized. "+server.toString());
        while( bContinue ) {
            Socket socket = server.accept();
            tpeServicePool.execute(new JVMXRayServerThread(socket));
        }
    }
}
