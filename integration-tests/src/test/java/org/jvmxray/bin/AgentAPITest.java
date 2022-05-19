package org.jvmxray.bin;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.jvmxray.IntegrationTest;
import org.jvmxray.TestPermission;
import org.jvmxray.driver.jvmxrayagent;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

//TODOMS:  Need more comprehensive unit test. This is only a
//         simple API test.
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
            // Force remote retrieval of configuration from JVMXRay server.  Uncomment to specify
            // full qualified remote server end-point to load jvmxrayclient.properties.  All configuration
            // settings will be loaded from this file.  Don't forget to start the server by running
            // JVMXRayStandaloneServer.  Alternatively, to limit unit testing to the agent code,
            // thus not requiring a running server, then leave the following line commented out
            // and the jvmxrayserver.configuration file will be loaded from the local jar.
            //
            //System.setProperty("jvmxrayserver.configuration","http://localhost:9123/api/config/");

            // Force debug logging in agent.  Settings are NONE, DEDUG, and INFO.
            // Incorrect or unspecified then NONE is assigned default.
            //
            System.setProperty("jvmxrayserver.lightlogger","DEBUG");
            sm = new jvmxrayagent();
            System.out.println("AgentAPITest: Initialized.");
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
            agent.finish();
        }catch(Throwable t1) {
            System.out.println("AgentAPITest.main(): Unexpected error.  msg="+t1.getMessage());
            t1.printStackTrace();
            try {
                if (agent != null) {
                    agent.finish();
                }
            } catch (Throwable t2) {
                t2.printStackTrace();
            }
        }
    }

    @AfterClass
    public static void finish() {
        // Cached log interval is 2-sec so need to keep alive long enough to log anything in cache.
        long starttime = System.currentTimeMillis();
        long finishtime= starttime + 6000;
        while( finishtime > starttime ) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {}
            starttime = System.currentTimeMillis();
            Thread.yield();
        }
        System.out.println("AgentAPITest: Finished.");
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
