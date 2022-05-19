package org.jvmxray.collector.bin;

import org.jvmxray.collector.cli.CommandProcessor;
import org.jvmxray.collector.cli.ICommandListener;
import org.jvmxray.collector.jms.JVMXRayJMSServerThread;
import org.jvmxray.collector.jms.JVMXRayThreadPoolExecutor;
import org.jvmxray.exception.JVMXRayServiceError;
import org.jvmxray.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.*;

public class jvmxrayserver implements RejectedExecutionHandler, ICommandListener {

    /**
     * Get logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.bin.jvmxrayserver");
    private static final String EOL = System.lineSeparator();
    private CommandProcessor cliprocessor = null;
    private PropertyUtil pu = null;
    private JVMXRayThreadPoolExecutor threadpool = null;
    private static final String CURSOR = ">";
    private static final int THREAD_POOL_MAX = 10;
    //private int THREAD_POOL_SLEEP = 60000;

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        //NoOP for now.
    }

    @Override
    public void cliKeyListener(char keytyped) {
        System.out.print(keytyped);
        System.out.flush();
    }

    //TODOMS: This need to be improved.  I assumed Java could receive
    //        events from CLI windows (like key presses) similar to
    //        the way AWT and Swing handle events.  Wrong!
    //        Lesson learned.  While this code works, it doesn't
    //        work exactly as intented.  When Java places CLI
    //        data into the System.in stream it does so after it
    //        receives a lf/crlf depending on OS.  It does not send
    //        each character as it's typed.
    //
    @Override
    public void cliCommandListener(String cmd, String[] args) {
        // Process cmds
        if (cmd!=null && cmd.length()>0) {
            switch (cmd) {
                case "pause":
                    try {
                        pause();
                    } catch (Throwable t) {
                        logger.error("Pause error. msg=" + t.getMessage(), t);
                    }
                    break;
                case "proceed":
                    try {
                        proceed();
                    } catch (Throwable t) {
                        logger.error("Proceed error. msg=" + t.getMessage(), t);
                    }
                    break;
                case "shutdown":
                case "quit":
                case "exit":
                    try {
                        shutdown();
                    } catch (Throwable t) {
                        logger.error("Shutdown error. msg=" + t.getMessage(), t);
                    }
                    break;
                case "shutdownnow":
                    try {
                        shutdownNow();
                    } catch (Throwable t) {
                        logger.error("ShutdownNow error. msg=" + t.getMessage(), t);
                    }
                    break;
                case "status":
                    printConsoleLine("Worker threads: "+printThreadPoolStatus());
                    break;
                case "help":
                    printConsoleLine("JVMXRayServer Help");
                    printConsoleLine("-----------------------------------------------------------------");
                    printConsoleLine("pause            Bring server offline safely.  Work in progress");
                    printConsoleLine("                 completes normally.  Don't start new work.");
                    printConsoleLine("proceed          Resume a paused server.  Server will accepts new");
                    printConsoleLine("                 Agent requests.");
                    printConsoleLine("shutdown         Bring server offline immediately.  Terminate");
                    printConsoleLine("                 any work in progress.  Alternates: quit, exit");
                    printConsoleLine("");
                    break;
                default:
                    break;
            }
        }
        System.out.print(CURSOR);
        System.out.flush();
    }

    private void printConsoleLine(String line) {
        System.out.println(line);
    }

    private void printConsoleLines(String[] lines) {
        Arrays.stream(lines).sequential().forEach( k -> {
            printConsoleLine(k);
        });
    }

    public void shutdown() throws JVMXRayServiceError {
        logger.info("Shutdown server requested by operator.");
        if(threadpool!=null) {
            threadpool.shutdown();
        }
        if( cliprocessor!=null) {
            cliprocessor.shutdownNow();
        }
    }

    public void shutdownNow() throws JVMXRayServiceError {
        logger.info("ShutdownNow server requested by operator.");
        if(threadpool!=null) {
            threadpool.shutdownNow();
        }
        if( cliprocessor!=null) {
            cliprocessor.shutdownNow();
        }
    }

    public void pause() throws JVMXRayServiceError {
        logger.info("Pause server requested by operator.");
        threadpool.pause();
    }

    public void proceed() throws JVMXRayServiceError {
        logger.info("Proceed server requested by operator.");
        threadpool.resume();
    }

    public boolean isRunning() {
        return (threadpool.getActiveCount()>0);
    }

    private String printThreadPoolStatus() {
        int intActiveCount = threadpool.getActiveCount();
        int intCorePoolSize = threadpool.getCorePoolSize();
        int intLargestPoolSize = threadpool.getLargestPoolSize();
        int intPoolSize = threadpool.getPoolSize();
        boolean bExecutorShutdown = threadpool.isShutdown();
        boolean bExecutorTerminated = threadpool.isTerminated();
        int intInActiveCount = intPoolSize - intActiveCount;
        StringBuffer buff = new StringBuffer();
        buff.append("ActiveCount=");
        buff.append(intActiveCount);
        buff.append(", ");
        buff.append("ZombieCount=");
        buff.append(intInActiveCount);
        buff.append(", ");
        buff.append("PoolSize=");
        buff.append(intPoolSize);
        buff.append(", ");
        buff.append("CorePoolSize=");
        buff.append(intCorePoolSize);
        buff.append(", ");
        buff.append("LargestPoolSize=");
        buff.append(intLargestPoolSize);
        buff.append(", ");
        buff.append("ExecutorShutdown=");
        buff.append(bExecutorShutdown?"true":"false");
        buff.append(", ");
        buff.append("bExecutorTerminated=");
        buff.append(bExecutorTerminated?"true":"false");
        buff.append(", ");
        return buff.toString();
    }

    /**
     * Main entry point.
     * @param args Command line args, if any.  Unused.
     */
    public static final void main(final String[] args) {
        try {
            // Initialize the server.
            jvmxrayserver container = new jvmxrayserver();
            container.init();
        }catch( Throwable t) {
            logger.error("Unhandled exception. msg="+t.getMessage(),t);
        }
    }

    /**
     * Initialize the server.
     */
    public void init() throws JVMXRayServiceError {
        // Grab server properties.
        pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_SERVER_CONFIG_DEFAULT);
        Properties props = pu.getProperties();
        // Get default thread pool sz
        int tmax = jvmxrayserver.THREAD_POOL_MAX;
        String tmp = props.getProperty(PropertyUtil.SYS_PROP_SERVER_THREAD_POOL_MAX, Integer.toString(tmax));
        if (tmp != null) {
            tmp = tmp.trim();
        }
        try {
            tmax = Integer.parseInt(tmp);
        } catch (NumberFormatException e) {
            logger.error("Configuration error.  property=" + PropertyUtil.SYS_PROP_SERVER_THREAD_POOL_MAX + " msg=" + e.getMessage());
        }
        // TODOMS: Need to make some of these configurable.
        int maximumPoolSize = tmax;
        int corePoolSize = (maximumPoolSize>4) ? (int)(maximumPoolSize *.25) : 1;
        long keepAliveTime = 60000L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        BlockingQueue workQueue = new LinkedBlockingQueue(5);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler handler = this;
        threadpool = new JVMXRayThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                                                   unit, workQueue, threadFactory, handler);
        // TODOMS: Need to rework JVMXRayJMSServerThread is designed.  Function is very similar to Agent
        // Command Processor architecture.  Need to improve the way server is processing events to
        // be more like the Agent.  While it's likely it's impractical to share all command processor
        // implementations between Agent and Server there's some value in sharing similar frameworks,
        // easier understand, less code overall, reduced bug fixing, etc.  And while some command
        // processor implementations can't be shared some will be supported.
        for (int i = 0; i < tmax; i++) {
            JVMXRayJMSServerThread task = new JVMXRayJMSServerThread(pu);
            threadpool.execute(task);
        }
        // Add CTRL-C shutdown hook.  May not be called if VM is exiting on an exception.
        // See addShutdownHook() Javadocs.
        //
        // TODOMS:  Need to rework all stream interaction to handle safely handle
        // shutdown cases.
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                try {
                    shutdown();
                } catch (Throwable t) {
                    logger.error("Unexpected error on shutdown.  msg=" + t.getMessage(), t);
                }
            }
        });
        // Add CLI command listener
        System.out.print(">");
        System.out.flush();
        cliprocessor = new CommandProcessor(System.in);
        cliprocessor.addCommandListener(this);
        cliprocessor.start();
    }

}
