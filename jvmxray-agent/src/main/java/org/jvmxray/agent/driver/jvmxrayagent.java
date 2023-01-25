package org.jvmxray.agent.driver;

import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.filters.StackDebugLevel;
import org.jvmxray.agent.processors.BaseTask;
import org.jvmxray.agent.processors.IJVMXRayProcessor;
import org.jvmxray.agent.simplelogger.SimpleLocalLogger;
import org.jvmxray.agent.util.AgentIdentityUtil;
import org.jvmxray.agent.util.PropertyUtil;
import org.jvmxray.agent.filters.IJVMXRayFilterRule;
import org.jvmxray.agent.filters.JVMXRayFilterList;
import org.jvmxray.agent.filters.FilterActions;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.util.EventUtil;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.security.Permission;
import java.util.*;

/**
 * jvmxrayagent conforms to the java.lang.SecurityManager specifications.  The agent
 * is dynamically loaded within a running Java application Virtual Machine through
 * command line options like this,<br/>
 * <code>
 *     -Djava.security.manager=org.jvmxray.driver.jvmxrayagent"
 * </code>
 * The agent monitors access to protected resources used during runtime, turns them
 * into events for further processing.  Depending upon configuration options events
 * and metadata is sent to various destinations like, files, JSON end-points,
 * pub/sub services, or other custom end-points you define.  Limit the flow
 * of messages user configurable filters are provided like NullFilter and StringFilter
 * for carving down message traffic to only those messages of interest to you.
 * <p/>
 * To extend and customize Agent behavior developers can can do the following.
 * + Develop an event processor, event processors are listeners that receive each
 *   agent events as they are fired.  You can pass the events through ot the framework
 *   for normal processing or add your own innovative features.
 * + Filters, filters reduce data to a subset of meaningful events.  Two filters are
 *   provided StringFilter and NullFilter.  Developers can extend these existing filters
 *   or write new filters.
 * @see java.lang.SecurityManager
 * @author Milton Smith
 */
public class jvmxrayagent extends SecurityManager {

    // End of line separator
    private static final String EOL = System.lineSeparator();
    // Path separator
    private static final String fileSeparator = File.separator;
    // Hold list of filters to process.
    private JVMXRayFilterList rulelist = new JVMXRayFilterList();
    // Agent properties
    PropertyUtil pu = null;
    // Agent application identity
    private String aid;
    // Agent category
    private String category;
    // Any events sent to SM when locked are ignored (but not blocked).
    private volatile boolean locked = true;
    // Events to process.
    private EnumSet<IEvent.Events> usrevents = EnumSet.noneOf(IEvent.Events.class);
    // Event utilities
    private static final EventUtil eu = EventUtil.getInstance();
    // Background timer for event processing.
    private Timer tmrEventProcessorPulse = new Timer("tmrEventProcessorPulse");
    // List of event processors, as defined in configuration.
    private ArrayList<IJVMXRayProcessor> processors = new ArrayList<IJVMXRayProcessor>();
    // Lightweight logging for Agent.
    private SimpleLocalLogger statuslogger = SimpleLocalLogger.getInstance();

    /**
     * CTOR
     */
    public jvmxrayagent() {
        try {
            initializeFromProperties();  // Initialize property settings, resolve configuration variables, etc.
            // Event processing queues flush every 2-seconds.  Activate the timer tasks.
            processors.forEach(processor ->{
                tmrEventProcessorPulse.scheduleAtFixedRate((BaseTask)processor, 0, 2000);
            });
            // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
            Thread tSDHook = new Thread( ()->{
                shutDown();
            });
            Runtime.getRuntime().addShutdownHook(tSDHook);
        }catch(Throwable t){
            System.err.println("jvmxrayagent(CTOR): Error initializing. msg="+t.getMessage());
            t.printStackTrace();
        }
    }

    public void shutDown() {
        // Force any events to be processes.
        processors.forEach( ijvmxRayProcessor -> {ijvmxRayProcessor.shutdown();}  );
        //TODO Figure out why agent threads loiter on exit.
        //System.out.println(printLoiteringThreadMap());
        System.out.flush();
        System.err.flush();
        // Cancel various time
        if( tmrEventProcessorPulse != null ) tmrEventProcessorPulse.cancel();
        // Cancel property refreshing
        pu.finishRefreshingCache();
        // Flush and close status log.
        statuslogger.shutDown();
    }

    private String printLoiteringThreadMap() {
        StringBuffer buff = new StringBuffer(1000);
        Map map = Thread.getAllStackTraces();
        map.forEach((k,v)->{
            Thread t = (Thread)k;
            StackTraceElement[] elements = (StackTraceElement[])v;
            buff.append(t.getName()+"["+t.getId()+" ");
            buff.append(t.getState());
            buff.append("]");
            String OINDENT = "   ";
            String NINDENT = OINDENT;
            for( int i=0; i<elements.length; i++) {
                buff.append(EOL);
                buff.append(NINDENT);
                buff.append("|");
                buff.append(EOL);
                buff.append(NINDENT);
                buff.append("--> ");
                StackTraceElement element = elements[i];
                buff.append(element.toString());
                NINDENT += OINDENT;
            }
            buff.append(EOL);
        });
        return buff.toString();
    }

    /**
     * See core JavaDocs.
     * @param target
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSecurityAccess(String target) {
        //NOTE: No explict calls to super.method(), generates exceptions.
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
               "",                         // Matching rule (when event fired).
                   -1,                         // Primary key
                   -1,                         // State
                   System.currentTimeMillis(), // Timestamp
                   getThreadStamp(),           // Thread stamp
                   "ACCESS_SECURITY",          // Event type
                    aid,                       // Application id
                   null,                       // Stack trace data, optional
                   target,                     // p1, optional meta
                   "",                         // p2, optional meta
                   ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param t
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(Thread t) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State, optional
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "ACCESS_THREAD",            // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    t.getName(),                // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param tg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(ThreadGroup tg) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "ACCESS_THREADGROUP",       // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    tg.getName(),               // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally {
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkCreateClassLoader() {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "CLASSLOADER_CREATE",       // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    "",                         // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param status
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExit(int status) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "EXIT",                     // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    Long.toString(status),      // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSetFactory() {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "FACTORY",                  // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    "",                         // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkDelete(String file) { ;
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "FILE_DELETE",              // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    file,                       // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param cmd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExec(String cmd) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "FILE_EXECUTE",             // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    cmd,                        // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "FILE_READ",                // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    file,                       // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs
     * @param file
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file, Object context) { ;
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "FILE_READ_WITH_CONTEXT",   // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    file,                       // p1, optional meta
                    context.toString(),         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(FileDescriptor fd) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                          // Matching rule (when event fired).
                    -1,                              // Primary key
                    -1,                              // State
                    System.currentTimeMillis(),      // Timestamp
                    getThreadStamp(),                // Thread stamp
                    "FILE_READ_WITH_FILEDESCRIPTOR", // Event type
                    aid,                       // Application id
                    null,                            // Stack trace data, optional
                    "",                              // p1, optional meta
                    "",                              // p2, optional meta
                    ""                               // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(String file) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                         // Matching rule (when event fired).
                    -1,                             // Primary key
                    -1,                             // State
                    System.currentTimeMillis(),     // Timestamp
                    getThreadStamp(),               // Thread stamp
                    "FILE_WRITE",                   // Event type
                    aid,                       // Application id
                    null,                           // Stack trace data, optional
                    file,                           // p1, optional meta
                    "",                             // p2, optional meta
                    ""                              // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(FileDescriptor fd) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                           // Matching rule (when event fired).
                    -1,                               // Primary key
                    -1,                               // State
                    System.currentTimeMillis(),       // Timestamp
                    getThreadStamp(),                 // Thread stamp
                    "FILE_WRITE_WITH_FILEDESCRIPTOR", // Event type
                    aid,                       // Application id
                    null,                             // Stack trace data, optional
                    fd.toString(),                    // p1, optional meta
                    "",                               // p2, optional meta
                    ""                                // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param lib
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkLink(String lib) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "LINK",                     // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    lib,                        // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageAccess(String pkg) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PACKAGE_ACCESS",           // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    pkg,                        // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageDefinition(String pkg) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PACKAGE_DEFINE",           // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    pkg,                        // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param perm
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPermission(Permission perm) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PERMISSION",               // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    perm.getName(),             // p1, optional meta
                    perm.getActions(),          // p2, optional meta
                    perm.getClass().getName()   // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param perm
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPermission(Permission perm, Object context) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PERMISSION_WITH_CONTEXT",  // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    perm.getName(),             // p1, optional meta
                    perm.getActions(),          // p2, optional meta
                    context.toString()          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPrintJobAccess() {
        if( isLocked() ) return;
        setLocked(true);
        try {
        EventDAO eDAO = new EventDAO(
                "",                     // Matching rule (when event fired).
                -1,                         // Primary key
                -1,                         // State
                System.currentTimeMillis(), // Timestamp
                getThreadStamp(),           // Thread stamp
                "PRINT",                    // Event type
                aid,                       // Application id
                null,                       // Stack trace data, optional
                "",                         // p1, optional meta
                "",                         // p2, optional meta
                ""                          // p3, optional meta
        );
        processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertiesAccess() {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PROPERTIES_ANY",           // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    "",                         // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param key
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertyAccess(String key) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "PROPERTIES_NAMED",         // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optionall
                    key,                        // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param host
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccept(String host, int port) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "SOCKET_ACCEPT",            // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    host,                       // p1, optional meta
                    Integer.toString(port),     // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param host
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkConnect(String host, int port) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "SOCKET_CONNECT",           // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    host,                       // p1, optional meta
                    Integer.toString(port),     // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param host
     * @param port
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkConnect(String host, int port, Object context) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                        // Matching rule (when event fired).
                    -1,                            // Primary key
                    -1,                            // State
                    System.currentTimeMillis(),    // Timestamp
                    getThreadStamp(),              // Thread stamp
                    "SOCKET_CONNECT_WITH_CONTEXT", // Event type
                    aid,                       // Application id
                    null,                          // Stack trace data, optional
                    host,                          // p1, optional meta
                    Integer.toString(port),        // p2, optional meta
                    ""                             // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkListen(int port) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                     // Matching rule (when event fired).
                    -1,                         // Primary key
                    -1,                         // State
                    System.currentTimeMillis(), // Timestamp
                    getThreadStamp(),           // Thread stamp
                    "SOCKET_LISTEN",            // Event type
                    aid,                       // Application id
                    null,                       // Stack trace data, optional
                    Integer.toString(port),     // p1, optional meta
                    "",                         // p2, optional meta
                    ""                          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param maddr
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkMulticast(InetAddress maddr) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                      // Matching rule (when event fired).
                    -1,                          // Primary key
                    -1,                          // State
                    System.currentTimeMillis(),  // Timestamp
                    getThreadStamp(),            // Thread stamp
                    "SOCKET_MULTICAST",          // Event type
                    aid,                       // Application id
                    null,                        // Stack trace data, optional
                    maddr.getHostName(),         // p1, optional meta
                    maddr.getHostAddress(),      // p2, optional meta
                    maddr.getCanonicalHostName() // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param maddr
     * @param ttl
     * @see java.lang.SecurityManager
     */
    //TODO Clean-up deprecation item.
    @Override
    @SuppressWarnings("deprecation")
    public void checkMulticast(InetAddress maddr, byte ttl) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            EventDAO eDAO = new EventDAO(
                    "",                      // Matching rule (when event fired).
                    -1,                          // Primary key
                    -1,                          // State
                    System.currentTimeMillis(),  // Timestamp
                    getThreadStamp(),            // Thread stamp
                    "SOCKET_MULTICAST_WITH_TTL", // Event type
                    aid,                       // Application id
                    null,                        // Stack trace data, optional
                    maddr.getHostName(),         // p1, optional meta
                    maddr.getHostAddress(),      // p2, optional meta
                    String.valueOf(ttl)          // p3, optional meta
            );
            processEvent(eDAO);
        }finally{
            setLocked(false);
        }
    }

    /**
     * See core javaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    protected Class<?>[] getClassContext() {
        return super.getClassContext();
    }

    /**
     * See core javaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public Object getSecurityContext() {
        return super.getSecurityContext();
    }

    /**
     * See core javaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public ThreadGroup getThreadGroup() {
        return super.getThreadGroup();
    }

    // Events generated by overloaded SecurityManager methods, check*(), call this
    // method for processing (e.g., send to server, log to disk, etc).
    private void processEvent(EventDAO eDAO)  {
        try {
            StackDebugLevel opts = null;
            FilterActions fa = null;
            // If fr is null, no matching rule in configuration file.
            IJVMXRayFilterRule fr = rulelist.getFilterRuleByEvent(eDAO);
            if(fr!=null) {
                opts = fr.getCallstackOptions();
                fa = fr.isMatch(eDAO);
                eDAO.setMatchRule(fr.getRuleName());
            }else{
                opts = StackDebugLevel.NONE;     // Default, no callstack
                fa = FilterActions.ALLOW;  // Default, handle event
            }
            if( fa.equals(FilterActions.ALLOW) ) {
                // Assign a stacktrace (e.g., event.setStackTrace(IStackTrace[])) for the
                // event if enabled for the event type.  Performance note: stack frames are
                // generated for all StackDebugLevels except StackDebugLevel.NONE.  Carrying
                // some CPU performance impact.  However, depending upon StackDebugLevel
                // operators can tune network impacts since more/less data is sent over
                // the network.
                if(!opts.equals(StackDebugLevel.NONE)) {
                    setStackTrace(eDAO,opts);
                }
                //TODO Remove after debugging.
                statuslogger.logMessage("jvmxrayagent.processEvent(): matched event="+eDAO);
                // Passes event to each event processor as defined in agent configuration like
                // nativesrestprocessor and nativelogmessagetofileprocessor, or develop
                // your own processors to send events to different destinations.
                fireEventProcessors(eDAO);
            } else {
                //TODO Remove after debugging.  Careful about enabling this, many disgarded events.
                //statuslogger.logMessage("jvmxrayagent.processEvent(): event disgarded="+eDAO);
            }
            // Log any unhandled exceptions and continue (if possible).
        } catch(Throwable t) {
            statuslogger.logMessage("jvmxrayagent.processEvent():  Unhandled exception. msg="+t.getMessage(), t);
        }
    }

    // When an event is fired, it's passed to each event processor specified in
    // configuration.  Event processors not called in any particular order.
    private void fireEventProcessors(IEvent event) {
        Iterator i = processors.listIterator();
        while( i.hasNext() ) {
            IJVMXRayProcessor processor = (IJVMXRayProcessor)i.next();
            processor.queueObject(event);
        }
    }

    // Initialize the NullSecurityManager subclass via property settings from jvmxrayagent.properties.
    private void initializeFromProperties() throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, IOException {

        try {
            // Get our status logging initialized early to aid in
            // problem determination.
            String basedir = System.getProperty("user.home");
            statuslogger.init(basedir);

            // Retrieve optional Agent Identity Properties file location
            // as specified by operator.  Sample setting from JRE command line,
            // -Djvmxray.agent.id.file=/Users/milton/jvmxrayinstanceid.properties
            //
            // On MAC OS default is, /Users/<userid>/.  Where <userid> is the id
            // of the user assigned to the jvmxray process.
            String fi = System.getProperty(PropertyUtil.SYS_PROP_AGENT_IDENTITY_FILE);
            String fn="jvmxrayinstanceid.properties";
            File source = null;
            // Use location specified or create a default agent
            // identity file.
            if (fi != null && fi.length() > 0) {
                source = new File(fi);
            } else {
                String instancefp = basedir.trim();
                instancefp = (instancefp.endsWith(fileSeparator)) ?
                        instancefp+"jvmxray-agent" :
                        instancefp+fileSeparator+"jvmxray-agent";
                source = new File(instancefp,fn);
            }
            AgentIdentityUtil au = AgentIdentityUtil.getInstance(source);
            // If no identity file, create a default.
            if (!source.exists()) {
                au.saveProperties();
                statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): Creating Agent identity file.  source="+source.toString());
            }else{
                statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): Loading existing Agent identity file.  source="+source.toString());
            }
            // Agent needs an initial url for the server when run for first time.
            // Operator must start the agent once.  It will fail.  Next,
            // edit the bootstrapurl property in the identity file, created by
            // default on initial startup.
            //TODO: Need a better way bootstrap configuration.
            String sBootStrapUrl = au.getStringProperty("bootstrapurl");
            aid = au.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            category = au.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            if( sBootStrapUrl == null || sBootStrapUrl.length()<1) {
                String msg = "jvmxrayagent.initializeFromProperties(): Missing bootstrapurl.  Can't locate server.  src="+source;
                IOException e = new IOException(msg);
                statuslogger.logMessage(msg, e);
                throw e;
            }
            // Load jvmxrayagent.properties.  Use bootstrap to find server.  If that fails, try
            // to load default properties from the classpath.
            try {
                pu = PropertyUtil.getInstance(sBootStrapUrl, aid, category);
                pu.refreshProperties();
            }catch(Exception e) {
                statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): Failed to load Agent properties from server.  Trying default properties.");
                pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_AGENT_CONFIG_DEFAULT);
                statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): WARNING, using default properties.  Check configuration.");
            }
            String targetpropfile = pu.getStringProperty("jvmxray.property.target");
            targetpropfile = ( targetpropfile==null) ? "Unassigned target" : targetpropfile;
            statuslogger.logMessage("Agent properties loaded from, jvmxray.property.target="+targetpropfile);
            //TODO: Set a practical limit.
            for (int i2 = 1; i2 < 500; i2++) {
                String key = "jvmxray.agent.event.processor.classname" + i2;
                String cclass = pu.getStringProperty(key);
                if (cclass == null) {
                    continue;
                }
                cclass.trim();
                pu.setStringProperty(key, cclass);
                Class c = getClass().getClassLoader().loadClass(cclass);
                Constructor ctor = c.getConstructor();
                //TODO: All IJVMXRayProcessors must extend from BaseTask at the moment.
                IJVMXRayProcessor processor = (IJVMXRayProcessor) ctor.newInstance();
                try {
                    processor.init(pu);
                    processors.add(processor);
                    statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): event processor loaded, "+processor.toString());
                }catch(Exception e) {
                    String cn = processor.getClass().getName();
                    statuslogger.logMessage("jvmxrayagent.initializeFromProperties(): Failed to load or initialize event processor.  cls="+cn,e);
                }
            }

            //TODO: Set a practical limit.
            for (int i1 = 1; i1 < 500; i1++) {
                // Common settings for all filters.
                String rulename = "jvmxray.agent.filter" + i1;
                String fclass = pu.getStringProperty(rulename + ".class");
                String events = pu.getStringProperty(rulename + ".events");
                String strace = pu.getStringProperty(rulename + ".stacktrace");
                String defaults = pu.getStringProperty(rulename + ".default");
                // If any filter property is missed, we reject the entire filter.
                if (fclass == null || events == null || strace == null || defaults == null) {
                    continue;
                }
                // Collect all properties specific to the filter so we can include
                // with the rule.
                Properties np = new Properties();
                Enumeration<String> e = (Enumeration<String>) pu.getPropertyNames();
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    String value = pu.getStringProperty(key);
                    if (key.startsWith(rulename)) {
                        np.put(key, value);
                    }
                }
                // Take any new events and add it to the list of supported events in
                // usrevents.
                EnumSet<IEvent.Events> gvents = EnumSet.noneOf(IEvent.Events.class);
                String[] sevents = events.split(",");
                for (int i2 = 0; i2 < sevents.length; i2++) {
                    String levent = sevents[i2].trim();
                    // List of Event types for current filter being processesed
                    gvents.add(IEvent.Events.valueOf(levent));
                    if (!usrevents.contains(levent)) {
                        // List of all unique Events described per configuration
                        usrevents.add(IEvent.Events.valueOf(levent));
                    }
                }
                // Grab callstackopts for the filter
                StackDebugLevel opts = StackDebugLevel.valueOf(strace);
                // Create instance of specified filter using reflection
                Class c = getClass().getClassLoader().loadClass(fclass);
                //TODO Clean-up item.  Should be calling an interface method rather than passing params in constructors.
                Constructor cd = c.getConstructor(String.class, EnumSet.class, FilterActions.class, Properties.class, StackDebugLevel.class);
                FilterActions filteraction = FilterActions.valueOf(defaults);
                IJVMXRayFilterRule fr = (IJVMXRayFilterRule) cd.newInstance(rulename, gvents, filteraction, np, opts);
                // Add the rule to the list
                rulelist.add(fr);
            }
        }finally {
            // If we fail then we unlock.  No point in locking the web app if we can't init.
            setLocked(false);
        }
    }

    /**
     * Return a string to identify calling thread suitable for logging.
     * @return Identity of the current thread.
     */
    private String getThreadStamp() {
        Thread t = Thread.currentThread();
        return t.getName()+"-"+t.getId();
    }

    /**
     * Set stacktrace info by reference.
     * @param eDAO EventDAO.  EventDAO stacktrace data will be overwritten
     *             depending upon configuration options.
     * @param opts Stacktrace options, NONE, LIMITED, SOURCEPATH, FULL.
     */
    private void setStackTrace(EventDAO eDAO, StackDebugLevel opts) {
        if ( opts == StackDebugLevel.NONE ) return;
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        //TODO needs to be fixed.
        StackTraceDAO sDAO = eu.createStackTraceDAO(opts);
        eDAO.setStackTrace(sDAO);
    }

    // Used to lock API when processing an event.  Some events trigger other events leading
    // to circular dependencies.
    private void setLocked(boolean value) {
        locked = value;
    }

    // Test to see if API locked.
    private boolean isLocked() {
        return locked;
    }

}
