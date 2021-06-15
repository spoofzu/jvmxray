package org.jvmxray.driver;

import org.jvmxray.filters.IJVMXRayFilterRule;
import org.jvmxray.filters.JVMXRayFilterList;
import org.jvmxray.task.FilterActions;
import org.jvmxray.task.QueuedEventNetworkTask;
import org.jvmxray.task.QueuedStatusLogTask;
import org.jvmxray.event.IEvent;
import org.jvmxray.util.EventUtil;
import org.jvmxray.util.PropertyUtil;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.dgc.VMID;
import java.security.Permission;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;

/**
 * WhiteSecurityManager conforms to the java.lang.SecurityManager specifications.  This class
 * is loaded dynamically in your Java application through command line options.  The class
 * monitors access to protected resources used during runtime and sends them to a remote
 * server for futher processing.
 * @see java.lang.SecurityManager
 * @author Milton Smith
 */
public class WhiteSecurityManager extends SecurityManager {

    // jvmxrayclient.properties
    protected Properties p = new Properties();
    // Hold list of filters to process.
    private JVMXRayFilterList rulelist = new JVMXRayFilterList();
    // Server identity
    private String id;
    private volatile boolean locked = false;
    // Buffer of status Strings.  Used for logging diagnostics messages.
    private QueuedStatusLogTask statuslogger;
    // Buffer of status Strings.  Used for logging events.
    private QueuedEventNetworkTask eventlogger;
    // Events to process.
    private EnumSet<IEvent.Events> usrevents = EnumSet.noneOf(IEvent.Events.class);
    // Event utilities
    private static final EventUtil eu = EventUtil.getInstance();

    public WhiteSecurityManager() {
        try {
            initializeFromProperties();  // Initialize property settings, resolve configuration variables, etc.
            initialize(); // Initialize diagnostic logging.
            statuslogger.logMessage("WhiteSecurityManager(CTOR): Initialization complete.");
        }catch(Throwable t){
            statuslogger.logMessage("WhiteSecurityManager(CTOR): Error initializing.", t);
        }
    }

    /**
     * Implementation initialization.  Any failure in initialization can
     * crash the app server.  Recommendation fail gracefully and allow
     * continued operation of the server.
     * @return
     * @throws Exception
     */
    private void initialize() throws Exception {
        // Initialize the status logger.
        statuslogger = new QueuedStatusLogTask(p);
        // Initialize the network event logger.
        eventlogger = new QueuedEventNetworkTask(p);
        // Status/diagnostic messages are logged in a queue as Strings.  Background thread
        // pulls events from queue and logs them at regular intervals.
        Timer timer = new Timer();
        //TODOMS: Need PERIOD to be a configuration property.
        long PERIOD = 1000 * 2;   // Write log entries every PERIOD-seconds
        // Events are logged in a queue as Strings.  Background thread
        // pulls events from queue and logs them at regular intervals.
        timer.scheduleAtFixedRate(statuslogger, 0, PERIOD);
        // Events are logged in a queue as Strings.  Background thread
        // pulls events from queue and logs them at regular intervals.
        timer.scheduleAtFixedRate(eventlogger, 0, PERIOD);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "ACCESS_SECURITY",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    target,
                    "",
                    "");
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "ACCESS_THREAD",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    t.getName(),
                    "",
                    "");
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "ACCESS_THREADGROUP",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    tg.getName(),
                    "",
                    "");
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "CLASSLOADER_CREATE",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    "",
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "EXIT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    Long.toString(status),
                    "",
                    "");
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FACTORY",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    "",
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_DELETE",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    file,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_EXECUTE",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    cmd,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_READ",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    file,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_READ_WITH_CONTEXT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    file,
                    context.toString(),
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_READ_WITH_FILEDESCRIPTOR",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    fd.toString(),
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_WRITE",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    file,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "FILE_WRITE_WITH_FILEDESCRIPTOR",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    fd.toString(),
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "LINK",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    lib,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PACKAGE_ACCESS",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    pkg,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PACKAGE_DEFINE",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    pkg,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PERMISSION",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    perm.getClass().getName(),
                    perm.getName(),
                    perm.getActions()
            );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PERMISSION_WITH_CONTEXT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    perm.getName(),
                    perm.getActions(),
                    context.toString()
            );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PRINT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    "",
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PROPERTIES_ANY",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    "",
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "PROPERTIES_NAMED",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    key,
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_ACCEPT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    host,
                    Integer.toString(port),
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_CONNECT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    host,
                    Integer.toString(port),
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_CONNECT_WITH_CONTEXT",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    host,
                    Integer.toString(port),
                    context.toString() );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_LISTEN",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    Integer.toString(port),
                    "",
                    "" );
            processEvent(logEvent);
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
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_MULTICAST",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    maddr.getHostName(),
                    maddr.getHostAddress(),
                    maddr.getCanonicalHostName());
            processEvent(logEvent);
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
    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
        if( isLocked() ) return;
        setLocked(true);
        try {
            String logEvent = fromLogEvent(
                    "-1",
                    "-1",
                    Long.toString(System.currentTimeMillis()),
                    getThreadStamp(),
                    "SOCKET_MULTICAST_WITH_TTL",
                    id,
                    "", // Empty for now, filled in later, if enabled.
                    maddr.getHostName(),
                    maddr.getHostAddress(),
                    String.valueOf(ttl) );
            processEvent(logEvent);
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

    // Gather up additional event metadata and process.
    private void processEvent(String event)  {
        try {
            Callstack opts = null;
            FilterActions fa = null;
            // If fr is null, no matching rule in configuration file.
            IJVMXRayFilterRule fr = rulelist.getFilterRuleByEvent(event);
            if(fr!=null) {
                opts = fr.getCallstackOptions();
                fa = fr.isMatch(event);
            }else{
                opts = Callstack.NONE;     // Default, no callstack
                fa = FilterActions.ALLOW;  // Default, handle event
            }
            if( fa.equals(FilterActions.ALLOW) ) {
                // Assign a stacktrace for the event if enabled for the
                // event type.  If anything but Callstack.NONE we
                // include appropriate callstack report.
                if(!opts.equals(Callstack.NONE)) {
                    event = getStackTrace(opts, event);
                }
                eventlogger.sendEvent(event);
            }
            // Log any unhandled exceptions and continue (if possible).
        } catch(Throwable t) {
            statuslogger.logMessage("RedSecurityManager.processEvent():  Unhandled exception. msg="+t.getMessage(), t);
        }
    }

    /**
     * Initialize the NullSecurityManager subclass via property settings from jvmxrayclient.properties.
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    private void initializeFromProperties() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, IOException {

        // Load jvmxrayclient.properties and cache it.
        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_CLIENT_DEFAULT);
        p = pu.getProperties();

        // Get the assigned server identity or generate a new identity and save it.
        try {
            // Throws exception if can't load file from configuration.
            id = pu.getServerId();
        } catch( Exception e ) {
            // Server identity from command line, if assigned by user.
            String i1 = System.getProperty(PropertyUtil.SYS_PROP_EVENT_SERV_IDENTITY);
            String i3=""; String lid="";
            // If no id assigned by user, generate a new one.
            if( i1 == null ) {
                String i2 = new VMID().toString();
                i3 = PropertyUtil.formatVMID(i2);
                lid=i3;
                // Id assigned by user, save it.
            } else {
                lid=i1;
            }
            // If exception occurs, they bubble up.
            pu.saveServerId(lid);
            id = lid;
        }
        // Iterate over the first 500 properties, current limit.
        for( int i1=1; i1 < 500; i1++ ) {
            // Common settings for all filters.
            String fclass = p.getProperty("jvmxray.filter"+i1+".class");
            String events = p.getProperty("jvmxray.filter"+i1+".events");
            String strace = p.getProperty("jvmxray.filter"+i1+".stacktrace");
            String defaults = p.getProperty("jvmxray.filter"+i1+".default");
            // No more filters or missing filter.  Continue to look for
            // next numbered filter.
            if( fclass == null || events == null || strace == null || defaults == null ) {
                continue;
            }
            // Trim leading/trailing and store.
            fclass.trim();
            events.trim();
            strace.trim();
            strace.trim();
            defaults.trim();
            p.setProperty("jvmxray.filter"+i1+".class", fclass);
            p.setProperty("jvmxray.filter"+i1+".events", events);
            p.setProperty("jvmxray.filter"+i1+".stacktrace",strace);
            p.setProperty("jvmxray.filter"+i1+".default",defaults);
            // Collect all properties specific to the filter so we can include
            // with the rule.
            Properties np = new Properties();
            Enumeration<String> e = (Enumeration<String>) p.propertyNames();
            while (e.hasMoreElements() ) {
                String key = e.nextElement();
                String value = p.getProperty(key);
                String prefix = "jvmxray.filter"+i1;
                if( key.startsWith(prefix) ) {
                    np.put(key,value);
                }
            }
            // Take any new events and add it to the list of supported events in
            // usrevents.
            EnumSet<IEvent.Events> gvents = EnumSet.noneOf(IEvent.Events.class);
            String[] sevents = events.split(",");
            for( int i2=0; i2 < sevents.length; i2++ ) {
                String levent = sevents[i2].trim();
                // List of Event types for current filter being processesed
                gvents.add(IEvent.Events.valueOf(levent));
                if ( !usrevents.contains(levent) ) {
                    // List of all unique Events described per configuration
                    usrevents.add(IEvent.Events.valueOf(levent));
                }
            }
            // Grab callstackopts for the filter
            Callstack opts = Callstack.valueOf(strace);
            // Create instance of specified filter using reflection
            Class c = getClass().getClassLoader().loadClass(fclass);
            Constructor cd = c.getConstructor(EnumSet.class, FilterActions.class, Properties.class, Callstack.class);
            FilterActions filteraction = FilterActions.valueOf(defaults);
            IJVMXRayFilterRule fr = (IJVMXRayFilterRule)cd.newInstance(gvents,filteraction, np, opts);
            // Add the rule to the list
            rulelist.add(fr);
        }
    }

    private final String fromLogEvent( String pk, String state, String ts, String thread_id,
                                       String et, String instance_id, String cs,
                                       String param1, String param2, String param3 ) {
        String event = eu.getEscapedEventString(pk, state, ts, thread_id, et, instance_id,
                cs, param1, param2, param3);
        event+=eu.EOL;
        return event;
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
     * Generate stacktrace for event.  Optional, assigned if configured.
     * @param event Active event.
     * @return  Active event with stack information at a level specified by the
     * eventtypes callstack options as configured.
     */
    private String getStackTrace(Callstack opts, String event) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        EventUtil ep = EventUtil.getInstance();
        String pk = Integer.toString(ep.getPK(event));
        String st = Long.toString(ep.getState(event));
        String ts = Long.toString(ep.getTimeStamp(event));
        String ti = ep.getThreadId(event);
        String et = ep.getEventType(event);
        String id = ep.getIdentity(event);
        String cs = getStackTrace(opts, stack);
        String p1 = ep.getParam1(event);
        String p2 = ep.getParam2(event);
        String p3 = ep.getParam3(event);
        String eventwithcs = fromLogEvent(pk,st,ts,ti,et,id,cs,p1,p2,p3);
        return eventwithcs;
    }

    /**
     * Generate a stacktrace.  Optional, assigned if configured.
     * @param opts Stacktrace options, LIMITED, SOURCEPATH, FULL.
     * @param stacktrace Array of stack trace elements.
     */
    private String getStackTrace(Callstack opts, StackTraceElement[] stacktrace) {
        StringBuffer buff = new StringBuffer();
        URL location = null;

        switch ( opts ) {
            case LIMITED:
                for (StackTraceElement e : stacktrace ) {
                    try {
                        Class c = Class.forName(e.getClassName());
                        buff.append(c.getName());
                        buff.append("->");
                    } catch( ClassNotFoundException e1) {
                        statuslogger.logMessage("RedSecurityManager.generateCallStack(): msg="+e1.getMessage(), e1);
                    }
                }
                break;
            case SOURCEPATH:
                for (StackTraceElement e : stacktrace ) {
                    try {
                        Class c = Class.forName(e.getClassName());
                        location = c.getResource('/' + c.getName().replace('.', '/') + ".class");
                        buff.append(location.toString());
                        buff.append("->");
                    } catch( ClassNotFoundException e1) {
                        statuslogger.logMessage("RedSecurityManager.generateCallStack(): msg="+e1.getMessage(), e1);
                    }
                }
                break;
            case FULL:
                for (StackTraceElement e : stacktrace ) {
                    buff.append(e.getClassName());
                    buff.append('(');
                    buff.append(e.getMethodName());
                    buff.append(':');
                    buff.append(e.getLineNumber());
                    buff.append(')');
                    buff.append("->");
                }
                break;
            case NONE:
                buff.append("DISABLED");
                break;
        }

        // Chop off trailing ->
        if (buff.length()>0 && buff.toString().endsWith("->"))
            buff.setLength(buff.length()-2);

        return buff.toString();
    }

    private void setLocked(boolean value) {
        locked = value;
    }

    private boolean isLocked() {
        return locked;
    }

}
