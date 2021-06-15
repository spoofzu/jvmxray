package org.jvmxray;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.jvmxray.driver.RedSecurityManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

/**
 * Quality tests for the various methods of NullSecurityManager.  The purpose is
 * to identify some simple functionality problems in NullSecurityManager.
 * @author Milton Smith
 *
 */
@Category(IntegrationTest.class)
public class RedSecurityManagerTest {

	private static RedSecurityManager sm = null;
	private Permission perm = new TestPermission();

	@BeforeClass
	public static void init() {
		try {
			// Force remote retrieval of configuration from JVMXRay server.  Uncomment to specify
			// full qualified remote server end-point to load jvmxrayclient.properties.  All configuration
			// settings will be loaded from this file.  Don't forget to start the server by running
			// JVMXRayStandaloneServer.  Alternatively, to limit unit testing to the agent code,
			// thus not requiring a running server, then leave the following line commented out
			// and the jvmxray.configuration file will be loaded from the local jar.
			//
			//System.setProperty("jvmxray.configuration","http://localhost:9123/api/config/");

			// Force debug logging in agent.  Agents must use light weight logger.
			// Can't use slf4j loggers like server.  Settings are NONE, DEDUG, and INFO.
			// Incorrect or unspecified then NONE is assigned default.
			//
			//
			System.setProperty("jvmxray.lightlogger","DEBUG");
			sm = new RedSecurityManager();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
	}

	@AfterClass
	public static void finish() {
		// Cached log interval is 2-sec so need to keep alive long enough to log.
		long starttime = System.currentTimeMillis();
		while( starttime+6000 > System.currentTimeMillis() ) {
			Thread.yield();
		}
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
		File file = File.createTempFile("redsecuritymanagertest0-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		sm.checkRead(fd);
	}

	@Test
	public void checkRead1() throws IOException {
		File file = File.createTempFile("redsecuritymanagertest2-", ".tmp");
		file.deleteOnExit();
		sm.checkRead(file.getName());
	}

	@Test
	public void checkRead3() throws IOException {
		File file = File.createTempFile("redsecuritymanagertest3-", ".tmp");
		file.deleteOnExit();
		sm.checkRead(file.getName(), sm.getSecurityContext());
	}

	@Test
	public void checkWrite0() throws IOException {
		File file = File.createTempFile("redsecuritymanagertest4-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		sm.checkWrite(fd);
	}

	@Test
	public void checkWrite1() throws IOException {
		File file = File.createTempFile("redsecuritymanagertest5-", ".tmp");
		file.deleteOnExit();
		sm.checkWrite(file.getName());
	}

	@Test
	public void checkDelete() throws IOException {
		File file = File.createTempFile("redsecuritymanagertest6-", ".tmp");
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
		sm.checkPropertyAccess("redsecuritymanagertest.test.propertykeyname");
	}

	@Test
	public void checkPrintJobAccess() {
		sm.checkPrintJobAccess();
	}

	@Test
	public void checkPackageAccess() {
		sm.checkPackageAccess("reddummypackageaccess.jar");
	}

	@Test
	public void checkPackageDefinition() {
		sm.checkPackageDefinition("reddummypackagedefinition.jar");
	}

	@Test
	public void checkSetFactory() {
		sm.checkSetFactory();
	}

	@Test
	public void checkSecurityAccess() {
		sm.checkSecurityAccess("redsecurityaccesstarget");
	}

}
