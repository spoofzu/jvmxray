package org.owasp.jvmxray.api.test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.BasicPermission;
import java.security.Permission;

import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.jvmxray.driver.NullSecurityManager;

public class NullSecurityManagerTest {
	
	private static NullSecurityManager nullsecuritymgr = null;
	private Permission perm = new TestPermission();
	
	public class TestPermission extends BasicPermission {
		private static final long serialVersionUID = 5932223970203786295L;
		TestPermission() {
			super("testpermissionname1");
		}
		public String getActions() {
			// setting the action fm the two arg constructor does not work, return specific value.
			return "testaction1";
		}
	}
	
	@BeforeClass
	public static void init() {	
		nullsecuritymgr = new NullSecurityManager();
	}
	
	@Test
	public void checkPermission0() {
		nullsecuritymgr.checkPermission(perm);
	}

	@Test
	public void checkPermission1() {
		nullsecuritymgr.checkPermission(perm, this);
	}

	@Test
	public void checkCreateClassLoader0() {
		nullsecuritymgr.checkCreateClassLoader();
	}

	@Test
	public void checkAccess0() {
		nullsecuritymgr.checkAccess(Thread.currentThread());
	}

	@Test
	public void checkAccess1() {
		nullsecuritymgr.checkAccess(Thread.currentThread().getThreadGroup());
	}

	@Test
	public void checkExit0() {
		nullsecuritymgr.checkExit(100);
	}

	@Test
	public void checkExec0() {
		nullsecuritymgr.checkExec("cd .");
	}

	@Test
	public void checkLink0() {
		nullsecuritymgr.checkLink("mylib.dll");
	}

	@Test
	public void checkRead0() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest0-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		nullsecuritymgr.checkRead(fd);
	}

	@Test
	public void checkRead1() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest2-", ".tmp");
		file.deleteOnExit();
		nullsecuritymgr.checkRead(file.getName());
	}

	@Test
	public void checkRead3() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest3-", ".tmp");
		file.deleteOnExit();
		nullsecuritymgr.checkRead(file.getName(), nullsecuritymgr.getSecurityContext());
	}

	@Test
	public void checkWrite0() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest4-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		nullsecuritymgr.checkWrite(fd);
	}

	@Test
	public void checkWrite1() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest5-", ".tmp");
		file.deleteOnExit();
		nullsecuritymgr.checkWrite(file.getName());
	}

	@Test
	public void checkDelete() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest6-", ".tmp");
		file.deleteOnExit();
		nullsecuritymgr.checkDelete(file.getName());
	}

	@Test
	public void checkConnect0() {
		nullsecuritymgr.checkConnect("localhost", 8081);
	}

	@Test
	public void checkConnect1() {
		nullsecuritymgr.checkConnect("localhost", 8081, nullsecuritymgr.getSecurityContext());
	}

	@Test
	public void checkListen() {
		nullsecuritymgr.checkListen(123);
	}

	@Test
	public void checkAccept() {
		nullsecuritymgr.checkAccept("localhost", 1234);
	}

	@Test
	public void checkMulticast0() throws UnknownHostException {
		nullsecuritymgr.checkMulticast(InetAddress.getLocalHost());
	}

	@Test
	public void checkMulticast1() throws UnknownHostException {
		InetAddress localhost = InetAddress.getLocalHost();
		byte ttl = 05;
		nullsecuritymgr.checkMulticast(localhost, ttl);
	}

	@Test
	public void checkPropertiesAccess() {
		nullsecuritymgr.checkPropertiesAccess();
	}

	@Test
	public void checkPropertyAccess() {
		nullsecuritymgr.checkPropertyAccess("nullsecuritymanagertest.test.propertykeyname");
	}

	@Test
	public void checkPrintJobAccess() {
		nullsecuritymgr.checkPrintJobAccess();
	}

	@Test
	public void checkPackageAccess() {
		nullsecuritymgr.checkPackageAccess("dummypackageaccess.jar");
	}

	@Test
	public void checkPackageDefinition() {
		nullsecuritymgr.checkPackageDefinition("dummypackagedefinition.jar");
	}

	@Test
	public void checkSetFactory() {
		nullsecuritymgr.checkSetFactory();
	}

	@Test
	public void checkSecurityAccess() {
		nullsecuritymgr.checkSecurityAccess("securityaccesstarget");
	}

	
	
}
