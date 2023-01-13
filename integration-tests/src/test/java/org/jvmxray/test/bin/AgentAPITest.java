package org.jvmxray.test.bin;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.jvmxray.test.IntegrationTest;
import org.jvmxray.test.TestPermission;
import org.jvmxray.agent.driver.jvmxrayagent;

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
@Category(IntegrationTest.class)
public class AgentAPITest {

    private static jvmxrayagent sm = null;
    private Permission perm = new TestPermission();
    private static volatile boolean bFlagged = false;

    @BeforeClass
    public static synchronized void init() {
        try {
            // Ensure we only get executed once.
            if(bFlagged) return;
            // ** RETAINED FOR ARCHIVAL PURPOSES BUT DON'T RECOMMEND USING.  -Milton
            //System.setProperty("jvmxrayserver.configuration","http://localhost:9123/api/event/");
            System.out.println("AgentAPITest: Begin initialization.");
            sm = new jvmxrayagent();
        } catch (Exception e) {
            System.out.println("AgentAPITest: Initialization failed.");
            e.printStackTrace();
            System.exit(10);
        } finally {
            bFlagged = true;
        }
    }

    // CLI entry point.  Run from the command line, optional.
    //
    public static final void main(String[] argc) {
        AgentAPITest agent = null;
        try {
            agent = new AgentAPITest();
            agent.init();
            agent.checkPermission0();
            agent.checkPermission1();
            agent.checkCreateClassLoader0();
            agent.checkAccess0();
            agent.checkAccess1();
            agent.checkExit0();
            agent.checkExec0();
            agent.checkLink0();
            agent.checkRead0();
            agent.checkRead1();
            agent.checkRead3();
            agent.checkWrite0();
            agent.checkWrite1();
            agent.checkDelete();
            agent.checkConnect0();
            agent.checkConnect1();
            agent.checkListen();
            agent.checkAccept();
            agent.checkMulticast0();
            agent.checkMulticast1();
            agent.checkPropertiesAccess();
            agent.checkPropertyAccess();
            agent.checkPrintJobAccess();
            agent.checkPackageAccess();
            agent.checkPackageDefinition();
            agent.checkSetFactory();
            agent.checkSecurityAccess();
            agent.shutDown();
            // Wait 4-sec, time for queue events to be processed
            int max_wait = 4000;
            long ot = System.currentTimeMillis();
            long nt = ot;
            while((nt-ot)<max_wait) {
                Thread thread = Thread.currentThread();
                thread.yield();
                thread.sleep(100);
                nt = System.currentTimeMillis();
            }
            System.out.println("AgentAPITest: Finished.");
        }catch(Throwable t1) {
            System.out.println("AgentAPITest.main(): Unexpected error.  msg="+t1.getMessage());
            t1.printStackTrace();
            try {
                if (agent != null) {
                    agent.shutDown();
                }
            } catch (Throwable t2) {
                t2.printStackTrace();
            }
        }

    }

    public void shutDown() {
        sm.shutDown();
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

}
