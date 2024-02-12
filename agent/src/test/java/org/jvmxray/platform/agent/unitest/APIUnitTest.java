package org.jvmxray.platform.agent.unitest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.jvmxray.platform.agent.engine.XRLoggingSecurityManager;
import org.jvmxray.platform.shared.property.XRPropertyFactory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

/**
 * Simple Agent API unit test.  Test will run as a Maven test
 * at project build time.  Alternatively, may be run on the
 * commandline.
 * @author Milton Smith
 */
public class APIUnitTest {

    private static XRLoggingSecurityManager sm  = new XRLoggingSecurityManager();
    private Permission perm = new TestPermission("testpermission");

    public static final void main(String[] args) {
        try {
            APIUnitTest.setUpOnce();
            APIUnitTest agentTest = (new APIUnitTest());
            agentTest.checkPermission0();
            agentTest.checkPermission1();
            agentTest.checkCreateClassLoader0();
            agentTest.checkAccess0();
            agentTest.checkAccess1();
            agentTest.checkExit0();
            agentTest.checkExec0();
            agentTest.checkLink0();
            agentTest.checkRead0();
            agentTest.checkRead1();
            agentTest.checkRead3();
            agentTest.checkWrite0();
            agentTest.checkWrite1();
            agentTest.checkDelete();
            agentTest.checkConnect0();
            agentTest.checkConnect1();
            agentTest.checkListen();
            agentTest.checkAccept();
            agentTest.checkMulticast0();
            agentTest.checkMulticast1();
            agentTest.checkPropertiesAccess();
            agentTest.checkPropertyAccess();
            agentTest.checkPrintJobAccess();
            agentTest.checkPackageAccess();
            agentTest.checkPackageDefinition();
            agentTest.checkSetFactory();
            agentTest.checkSecurityAccess();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void setUpOnce() throws Exception {
        // Init jvmxray.
        XRPropertyFactory.init();
    }

    @Test
    public void checkPermission0() {
        sm.checkPermission(perm);
    }
    @Test
    public void checkPermission1() {
        sm.checkPermission(perm, this);
    }
    @Test
    public void checkCreateClassLoader0() {
        sm.checkCreateClassLoader();
    }
    @Test
    public void checkAccess0() {
        sm.checkAccess(Thread.currentThread());
    }
    @Test
    public void checkAccess1() {
        sm.checkAccess(Thread.currentThread().getThreadGroup());
    }
    @Test
    public void checkExit0() {
        sm.checkExit(100);
    }
    @Test
    public void checkExec0() {
        sm.checkExec("cd .");
    }
    @Test
    public void checkLink0() {
        sm.checkLink("mylib.dll");
    }
    @Test
    public void checkRead0() throws IOException {
        File file = File.createTempFile("simpleagenttest0-", ".tmp");
        file.deleteOnExit();
        FileOutputStream ostr = new FileOutputStream(file);
        FileDescriptor fd = ostr.getFD();
        sm.checkRead(fd);
    }
    @Test
    public void checkRead1() throws IOException {
        File file = File.createTempFile("simpleagenttest2-", ".tmp");
        file.deleteOnExit();
        sm.checkRead(file.getName());
    }
    @Test
    public void checkRead3() throws IOException {
        File file = File.createTempFile("simpleagenttest3-", ".tmp");
        file.deleteOnExit();
        sm.checkRead(file.getName(), sm.getSecurityContext());
    }
    @Test
    public void checkWrite0() throws IOException {
        File file = File.createTempFile("simpleagenttest4-", ".tmp");
        file.deleteOnExit();
        FileOutputStream ostr = new FileOutputStream(file);
        FileDescriptor fd = ostr.getFD();
        sm.checkWrite(fd);
    }
    @Test
    public void checkWrite1() throws IOException {
        File file = File.createTempFile("simpleagenttest5-", ".tmp");
        file.deleteOnExit();
        sm.checkWrite(file.getName());
    }
    @Test
    public void checkDelete() throws IOException {
        File file = File.createTempFile("simpleagenttest6-", ".tmp");
        file.deleteOnExit();
        sm.checkDelete(file.getName());
    }
    @Test
    public void checkConnect0() {
        sm.checkConnect("localhost", 8081);
    }
    @Test
    public void checkConnect1() {
        sm.checkConnect("localhost", 8081, sm.getSecurityContext());
    }
    @Test
    public void checkListen() {
        sm.checkListen(123);
    }
    @Test
    public void checkAccept() {
        sm.checkAccept("localhost", 1234);
    }
    @Test
    public void checkMulticast0() throws UnknownHostException {
        sm.checkMulticast(InetAddress.getLocalHost());
    }
    @Test
    public void checkMulticast1() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        byte ttl = 05;
        sm.checkMulticast(localhost, ttl);
    }
    @Test
    public void checkPropertiesAccess() {
        sm.checkPropertiesAccess();
    }
    @Test
    public void checkPropertyAccess() {
        sm.checkPropertyAccess("simpleagenttest.test.propertykeyname");
    }
    @Test
    public void checkPrintJobAccess() {
        sm.checkPrintJobAccess();
    }
    @Test
    public void checkPackageAccess() {
        sm.checkPackageAccess("dummypackageaccess.jar");
    }
    @Test
    public void checkPackageDefinition() {
        sm.checkPackageDefinition("dummypackagedefinition.jar");
    }
    @Test
    public void checkSetFactory() {
        sm.checkSetFactory();
    }
    @Test
    public void checkSecurityAccess() {
        sm.checkSecurityAccess("securityaccesstarget");
    }

    public class TestPermission extends Permission {
        public TestPermission(String name) {
            super(name);
        }
        @Override
        public boolean implies(Permission permission) {
            return false;
        }
        @Override
        public boolean equals(Object obj) {
            return false;
        }
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public String getActions() {
            return null;
        }
    }

}
