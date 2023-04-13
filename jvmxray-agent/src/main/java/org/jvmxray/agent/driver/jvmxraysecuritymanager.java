package org.jvmxray.agent.driver;

import org.jvmxray.agent.event.Eventmeta;
import org.jvmxray.agent.util.AgentIdentityUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 *  Note: java.lang.SecurityManager is being deprecated by the Java Team.  See notes
 *    associated with <a href="https://openjdk.org/jeps/411">JEP 411</a>.  Running
 *    on JRE 17+ produces a deprecation warning.
 * @see java.lang.SecurityManager
 * @author Milton Smith
 */
public class jvmxraysecuritymanager extends SecurityManager {

    // Any events sent to SM when ignoring are well, ignored.
    private volatile boolean isIgnoringEvents = true;
    // log4j2 logger.
    private static Logger clslogger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager");
    // Path separator
    private static final String fileSeparator = File.separator;
    // AgentIdentityUtil instance
    private AgentIdentityUtil AU = null;
    // Log message version.
    private static final String VERSION_TAG = "version";
    private static final String VERSION_ID = "0.1";

    // Stack meta enum
    private enum StackDebugLevel {
        NONE,
        SOURCEPATH,
        LIMITED,
        FULL
    }
    // Limit the depth of stack frames.  Truncate at 100.
    private static final int MAX_STACK_DEPTH = 100;

    // Early initializaiton.
    static {
        // Ensures the security framework receives security events.
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions p = new Permissions();
                p.add(new AllPermission());
                return p;
            }
        });
    }

    /**
     * CTOR
     */
    public jvmxraysecuritymanager() {
        try {
            init();
        }catch(Throwable t){
            clslogger.error("Uncaught initialization error. msg="+t.getMessage(),t);
        }
    }

    private void init() throws IOException {
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread( ()->{
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        // Retrieve optional Agent Identity Properties file location
        // as specified by operator.  Sample setting from JRE command line,
        // -Djvmxray.agent.id.file=/Users/milton/jvmxrayinstanceid.properties
        //
        // On MAC OS default is, /Users/<userid>/.  Where <userid> is the id
        // of the user assigned to the jvmxray process.
        String basedir = System.getProperty("user.home");
        basedir.trim();
        String fi = System.getProperty("jvmxray.agent.id.file");
        String fn="jvmxrayinstanceid.properties";
        File source = null;
        // Use location specified in properties to load id or create a
        // default agent identity file.
        if (fi != null && fi.length() > 0) {
            source = new File(fi);
        } else {
            String instancefp = basedir.trim();
            instancefp = (instancefp.endsWith(fileSeparator)) ?
                    instancefp+"jvmxray-agent" :
                    instancefp+fileSeparator+"jvmxray-agent";
            source = new File(instancefp,fn);
        }
        try {
            AU = AgentIdentityUtil.getInstance(source);
            clslogger.info("Loading existing Agent identity file. fn="+source);
            if( AU==null) {
                throw new IOException("Unknown error initializing identity file. au=null fn="+source);
            }
        }catch(IOException e) {
            clslogger.error("Error creating identity file. fn="+source+" msg="+e.getMessage(),e);
            throw e;
        }
        // If no identity file, create a default.
        if (!source.exists()) {
            AU.saveProperties();
            clslogger.info("Creating Agent identity file. fn="+source);
        }
        clslogger.info("jvmxray initialization complete.");
        setIgnoringEvents(false);
    }

    public void shutDown() {
        // Shutdown gracefully
        try {
            clslogger.warn("JVM shutdown detected. jvmxray finishing up.");
        }catch(Throwable t) {
            String msg = "JVM shutdown with errors.  msg="+t.getMessage();
            System.err.println(msg);
            t.printStackTrace();
        }
    }

    /**
     * See core JavaDocs.
     * @param target
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSecurityAccess(String target) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.accesssecurity");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "ACCESS_SECURITY";  // Event type
            String P1       = target;             // p1, optional meta
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
            // MDC value "key" is automatically cleared when the try block is exited
        }finally{
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param t
     * @throws SecurityException
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(Thread t) throws SecurityException {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.accessthread");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "ACCESS_THREAD";  // Event type
            String P1       = t.getName();      // p1, optional meta
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param tg
     * @throws SecurityException
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(ThreadGroup tg) throws SecurityException {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.accessthreadgroup");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "ACCESS_THREADGROUP";      // Event type
            String P1       = tg.getName();             // p1, optional meta
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkCreateClassLoader() {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.classloadercreate");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "CLASSLOADER_CREATE"; // Event type
            String P1       = "";
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param status
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExit(int status) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.exit");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "EXIT";               // Event type
            String P1 = Long.toString(status);     // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSetFactory() {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.factory");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FACTORY";               // Event type
            String P1 = "";
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkDelete(String file) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.filedelete");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FILE_DELETE"; // Event type
            String P1 = file;               // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param cmd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExec(String cmd) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.fileexecute");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FILE_EXECUTE"; // Event type
            String P1 = cmd;                 // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.fileread");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FILE_READ"; // Event type
            String P1 = file;             // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs
     * @param file
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file, Object context) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.filereadwithcontext");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "FILE_READ_WITH_CONTEXT"; // Event type
            String P1       = file;                     // p1, optional meta
            String P2       = context.toString();       // p2, optional meta
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(FileDescriptor fd) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.filereadwithfiledescriptor");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FILE_READ_WITH_FILEDESCRIPTOR"; // Event type
            String P1 = fd.toString(); // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if (logger.isDebugEnabled() || logger.isInfoEnabled()) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace(logger, EVENTTP, eventid, aid, cat, ste);
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(String file) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.filewrite");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "FILE_WRITE";          // Event type
            String P1       = file;                  // p1, optional meta
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(FileDescriptor fd) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.filewritewithfiledescriptor");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "FILE_WRITE_WITH_FILEDESCRIPTOR"; // Event type
            String P1 = fd.toString();                         // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param lib
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkLink(String lib) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.link");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "LINK";              // Event type
            String P1 = lib;                       // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        }finally{
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageAccess(String pkg) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.packageaccess");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "PACKAGE_ACCESS";                // Event type
            String P1 = pkg;                                   // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageDefinition(String pkg) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.packagedefine");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "PACKAGE_DEFINE";                 // Event type
            String P1       = pkg;                              // p1, optional meta
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    @Override
    public void checkPermission(Permission perm) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.permission");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "PERMISSION";                     // Event type
            String P1       = perm.getName();                   // p1, optional meta
            String P2       = perm.getActions();                // p2, optional meta
            String P3       = perm.getClass().getName();        // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
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
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.permissionwithcontext");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "PERMISSION_WITH_CONTEXT";        // Event type
            String P1       = perm.getName();                   // p1, optional meta
            String P2       = perm.getActions();                // p2, optional meta
            String P3       = context.toString();               // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPrintJobAccess() {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.print");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "PRINT";              // Event type
            String P1       = "";
            String P2       = "";
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertiesAccess() {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.propertiesany");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "PROPERTIES_ANY";         // Event type
            String P1 = "";
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param key
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertyAccess(String key) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.properertiesnamed");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "PROPERTIES_NAMED"; // Event type
            String P1 = key; // p1, optional meta
            String P2 = "";
            String P3 = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
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
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketaccept");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP = "SOCKET_ACCEPT"; // Event type
            String P1 = host;                   // p1, optional meta
            String P2 = Integer.toString(port); // p2, optional meta
            String P3 = "";                     // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
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
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketconnect");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "SOCKET_CONNECT";        // Event type
            String P1       = host;                    // p1, optional meta
            String P2       = Integer.toString(port);  // p2, optional meta
            String P3       = "";                      // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
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
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketconnectwithcontext");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "SOCKET_CONNECT_WITH_CONTEXT";    // Event type
            String P1       = host;                             // p1, optional meta
            String P2       = Integer.toString(port);           // p2, optional meta
            String P3       = context.toString();               // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkListen(int port) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if (isIgnoringEvents()) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketlisten");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "SOCKET_LISTEN";        // Event type
            String P1       = Integer.toString(port); // p1, optional meta
            String P2       = "";                     // p2, optional meta
            String P3       = "";                     // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param maddr
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkMulticast(InetAddress maddr) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketmulticast");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "SOCKET_MULTICAST";           // Event type
            String P1       = maddr.getCanonicalHostName(); // p1, optional meta
            String P2       = maddr.getHostAddress();       // p2, optional meta
            String P3       = "";
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally {
            setIgnoringEvents(false);
        }
    }

    /**
     * See core JavaDocs.
     * @param maddr
     * @param ttl
     * @see java.lang.SecurityManager
     */
    @Override
    @SuppressWarnings("deprecation")
    public void checkMulticast(InetAddress maddr, byte ttl) {
        // NOTE: No calls to super.method() w/o testing. Sometimes generates exceptions.
        if( isIgnoringEvents() ) return;
        setIgnoringEvents(true);
        // Tag events metadata version identifier.
        try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable(VERSION_TAG, VERSION_ID)) {
            Logger logger = LoggerFactory.getLogger("org.jvmxray.agent.driver.jvmxraysecuritymanager.events.socketmulticastwithttl");
            String eventid = AU.getVMID();
            String aid = AU.getStringProperty(AgentIdentityUtil.PROPERTY_AID);
            String cat = AU.getStringProperty(AgentIdentityUtil.PROPERTY_CATEGORY);
            String EVENTTP  = "SOCKET_MULTICAST_WITH_TTL";    // Event type
            String P1       = maddr.getCanonicalHostName();   // p1, optional meta
            String P2       = maddr.getHostAddress();         // p2, optional meta
            String P3       = String.valueOf(ttl);            // p3, optional meta
            logger.warn("{} | {} | {} | {} | {} | {} | {}", EVENTTP, eventid, aid, cat, P1, P2, P3);
            if( logger.isDebugEnabled() || logger.isInfoEnabled() ) {
                StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                logStackTrace( logger, EVENTTP, eventid, aid, cat, ste );
            }
        } finally{
            setIgnoringEvents(false);
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

    // Used to lock API when processing an event.  Some events trigger other events leading
    // to circular dependencies.
    private void setIgnoringEvents(boolean value) {
        isIgnoringEvents = value;
    }

    // Test to see if API locked.
    private boolean isIgnoringEvents() {
        return isIgnoringEvents;
    }

    public String getClassLocation(String className) throws ClassNotFoundException {
        if ( className == null ) {
            return "null classname";
        }
        Class<?> clazz = Class.forName(className);
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        if (cs == null ) {
            return "codesource null";
        }
        URL location = cs.getLocation();

        return location != null ? location.getPath() : "getlocation null";
    }

    private Eventmeta[] createStackTraceArray(Logger logger, StackTraceElement[] ste) {
        List<Eventmeta> eventmetaList = new ArrayList<>();
        boolean truncated = false;
        int iSz = ste.length;
        if (ste.length > MAX_STACK_DEPTH - 1) {
            iSz = MAX_STACK_DEPTH - 1;
            truncated = true;
        }
        for (int i = 0; i < iSz; i++) {
            StackTraceElement element = ste[i];
            Class eclass = null;
            String clsloadernm = (element.getClassLoaderName() == null) ? "primordial" : element.getClassLoaderName();
            String clsnm = element.getClassName();
            String methnm = "unassigned";
            int linenum = 0;
            String loc = "unassigned";
            String modulenm = "not available";
            String modulevr = "not available";
            boolean isnative = false;
            String ds = "unassigned";
            String filenm = element.getFileName();

            try {
                loc = getClassLocation(clsnm);
            }catch(ClassNotFoundException e ) {}

            if (logger.isDebugEnabled()) {
                isnative = element.isNativeMethod();
                methnm = element.getMethodName();
                modulenm = (element.getModuleName()!=null) ? element.getModuleName() : "not available";
                modulevr = (element.getModuleVersion()!=null) ? element.getModuleVersion() : "not available";
                linenum = element.getLineNumber();
                ds = (element.toString() == null) ? "unavailable" : element.toString();
            }
            Eventmeta eventmeta = new Eventmeta(clsloadernm, filenm, clsnm, methnm, linenum, loc, modulenm, modulevr, isnative, ds);
            eventmetaList.add(eventmeta);
        }
        if (truncated) {
            int truncatedcnt = ste.length - iSz;
            String msg = String.format("truncated frames, %d of %d frames dropped.", truncatedcnt, ste.length);
            Eventmeta eventmeta = new Eventmeta("unassigned", "unassigned", "unassigned", "unassigned", 0, "unassigned", "unassigned", "unassigned", false, msg);
            eventmetaList.add(eventmeta);
        }
        return eventmetaList.toArray(new Eventmeta[0]);
    }

    /**
     * Logs a stackframe associated with an event as a series of log messages where each message
     * tagged with the parent events event id and the depth of each frame is indicated by a
     * positve int.
     * indicated with
     * @param logger
     * @param EVENTTP
     * @param eventId
     * @param aid
     * @param category
     * @param ste
     */
    private void logStackTrace(Logger logger, String EVENTTP, String eventId, String aid, String category, StackTraceElement[] ste) {
        Eventmeta[] eventMetaArray = createStackTraceArray(logger, ste);
        IntStream.range(0, eventMetaArray.length).forEach(i -> {
            // Note: createStackTraceArray() controls the depth of meta provided.
            String logMessage = String.format("STACKFRM | %d | %s | %s | %s| %s | %s | %s | %s | %s | %s | %s | %d | %s | %b",
                    i,
                    EVENTTP,
                    eventId,
                    aid,
                    category,
                    eventMetaArray[i].getClsLoaderNm(),
                    eventMetaArray[i].getClsNm(),
                    eventMetaArray[i].getMethNm(),
                    eventMetaArray[i].getModuleNm(),
                    eventMetaArray[i].getModuleVr(),
                    eventMetaArray[i].getFileNm(),
                    eventMetaArray[i].getLineNum(),
                    eventMetaArray[i].getLoc(),
                    eventMetaArray[i].isIsNative()
            );
            if (logger.isDebugEnabled()) {
                logger.debug(logMessage);
            } else if (logger.isInfoEnabled()) {
                logger.info(logMessage);
            }
        });
    }


}
