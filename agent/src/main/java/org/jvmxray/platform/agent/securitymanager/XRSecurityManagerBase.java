package org.jvmxray.platform.agent.securitymanager;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;

public abstract class XRSecurityManagerBase extends SecurityManager {

    private ThreadLocal<Boolean> isHandlingEvent = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Ensures the security framework receives security events.
    static {
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions p = new Permissions();
                p.add(new AllPermission());
                return p;
            }
        });
    }

    public XRSecurityManagerBase() {
        super();
        // Register shutdownhook.  Process service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread(() -> {
            this.shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
    }

    protected abstract void shutDown();

    protected abstract void handleEvent( String loggerName, String p1, String p2, String p3 );

    private void handleEvent0(String loggerName, String p1, String p2, String p3 ) {
        // If current thread re-enters we return to caller.  Happends due to the fact
        // that handling a security event generates downstream events and we only
        // wish to store primary events of the caller.
        if (isHandlingEvent.get()) {
            return;
        }
        try {
            isHandlingEvent.set(Boolean.TRUE);
            handleEvent(loggerName, p1, p2, p3);
        } finally {
            // Reset the flag as the event handling is done
            isHandlingEvent.set(Boolean.FALSE);
        }
    }

    /**
     * See core JavaDocs.
     * @param target
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSecurityAccess(String target) {;
        String loggerName = "org.jvmxray.events.access.target";
        handleEvent0(loggerName, target, "", "");
    }

    /**
     * See core JavaDocs.
     * @param t
     * @throws SecurityException
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(Thread t) throws SecurityException {
        String loggerName = "org.jvmxray.events.access.thread";
        handleEvent0(loggerName, t.getName(), "", "" );
    }

    /**
     * See core JavaDocs.
     * @param tg
     * @throws SecurityException
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccess(ThreadGroup tg) throws SecurityException {
        String loggerName = "org.jvmxray.events.access.threadgroup";
        handleEvent0(loggerName, tg.getName(), "", "" );
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkCreateClassLoader() {
        String loggerName = "org.jvmxray.events.classloader.create";
        handleEvent0(loggerName, "", "", "" );
    }

    /**
     * See core JavaDocs.
     * @param status
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExit(int status) {
        String loggerName = "org.jvmxray.events.system.exit";
        handleEvent0(loggerName, Long.toString(status), "", "" );
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkSetFactory() {
        String loggerName = "org.jvmxray.agent.system.factory";
        handleEvent0(loggerName, "", "", "" );
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkDelete(String file) {
        String loggerName = "org.jvmxray.events.io.filedelete";
        handleEvent0(loggerName, file, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param cmd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkExec(String cmd) {
        String loggerName = "org.jvmxray.events.system.execute";
        handleEvent0(loggerName, cmd, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file) {
        String loggerName = "org.jvmxray.events.io.fileread";
        handleEvent0(loggerName, file, "", "" );
    }

    /**
     * See core JavaDocs
     * @param file
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(String file, Object context) {
        String loggerName = "org.jvmxray.events.io.filereadwithcontext";
        handleEvent0(loggerName, file, context.toString(), "" );
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkRead(FileDescriptor fd) {
        String loggerName = "org.jvmxray.events.io.filereadwithfiledescriptor";
        handleEvent0(loggerName, fd.toString(), "", "" );
    }

    /**
     * See core JavaDocs.
     * @param file
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(String file) {
        String loggerName = "org.jvmxray.events.io.filewrite";
        handleEvent0(loggerName, file, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param fd
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkWrite(FileDescriptor fd) {
        String loggerName = "org.jvmxray.events.io.filewritewithfiledescriptor";
        handleEvent0(loggerName, fd.toString(), "", "" );
    }

    /**
     * See core JavaDocs.
     * @param lib
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkLink(String lib) {
        String loggerName = "org.jvmxray.events.system.link";
        handleEvent0(loggerName, lib, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageAccess(String pkg) {
        String loggerName = "org.jvmxray.events.classloader.packageaccess";
        handleEvent0(loggerName, pkg, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param pkg
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPackageDefinition(String pkg) {
        String loggerName = "org.jvmxray.events.classloader.packagedefine";
        handleEvent0(loggerName, pkg, "", "" );
    }

    @Override
    public void checkPermission(Permission p) {
        String loggerName = "org.jvmxray.events.permission.check";
        handleEvent0(loggerName, p.getName(), p.getActions(), p.getClass().getName() );
    }

    /**
     * See core JavaDocs.
     * @param p
     * @param context
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPermission(Permission p, Object context) {
        String loggerName = "org.jvmxray.events.permission.checkwithcontext";
        handleEvent0(loggerName, p.getName(), p.getActions(), context.toString() );
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPrintJobAccess() {
        String loggerName = "org.jvmxray.events.system.print";
        handleEvent0(loggerName, "", "", "" );
    }

    /**
     * See core JavaDocs.
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertiesAccess() {
        String loggerName = "org.jvmxray.events.system.propertiesany";
        handleEvent0(loggerName, "", "", "" );
    }

    /**
     * See core JavaDocs.
     * @param key
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkPropertyAccess(String key) {
        String loggerName = "org.jvmxray.events.system.properertiesnamed";
        handleEvent0(loggerName, key, "", "" );
    }

    /**
     * See core JavaDocs.
     * @param host
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkAccept(String host, int port) {
        String loggerName = "org.jvmxray.events.socket.accept";
        handleEvent0(loggerName, host, Integer.toString(port), "" );
    }

    /**
     * See core JavaDocs.
     * @param host
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkConnect(String host, int port) {
        String loggerName = "org.jvmxray.events.socket.connect";
        handleEvent0(loggerName, host, Integer.toString(port), "" );
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
        String loggerName = "org.jvmxray.events.socket.connectwithcontext";
        handleEvent0(loggerName, host, Integer.toString(port), context.toString() );
    }

    /**
     * See core JavaDocs.
     * @param port
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkListen(int port) {
        String loggerName = "org.jvmxray.events.socket.listen";
        handleEvent0(loggerName, Integer.toString(port), "", "" );
    }

    /**
     * See core JavaDocs.
     * @param maddr
     * @see java.lang.SecurityManager
     */
    @Override
    public void checkMulticast(InetAddress maddr) {
        String loggerName = "org.jvmxray.events.socket.multicast";
        handleEvent0(loggerName, maddr.getCanonicalHostName(), maddr.getHostAddress(), "" );
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
        String loggerName = "org.jvmxray.events.socket.multicastwithttl";
        handleEvent0(loggerName, maddr.getCanonicalHostName(), maddr.getHostAddress(), String.valueOf(ttl) );
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

}
