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
	
	private static NullSecurityManager mgr = null;
	private Permission perm = new TestPermission();
	
	public class TestPermission extends BasicPermission {
		TestPermission() {
			super("testpermission","testaction");
		}
	}
	
	@BeforeClass
	public static void init() {	
		mgr = new NullSecurityManager();
	}
	
	@Test
	public void checkPermission0() {
		mgr.checkPermission(perm);
	}

	@Test
	public void checkPermission1() {
		mgr.checkPermission(perm, this);
	}

	@Test
	public void checkCreateClassLoader0() {
		mgr.checkCreateClassLoader();
	}

	@Test
	public void checkAccess0() {
		mgr.checkAccess(Thread.currentThread());
	}

	@Test
	public void checkAccess1() {
		mgr.checkAccess(Thread.currentThread().getThreadGroup());
	}

	@Test
	public void checkExit0() {
		mgr.checkExit(100);
	}

	@Test
	public void checkExec0() {
		mgr.checkExec("cd .");
	}

	@Test
	public void checkLink0() {
		mgr.checkLink("mylib.dll");
	}

	@Test
	public void checkRead0() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest0-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		mgr.checkRead(fd);
	}

	@Test
	public void checkRead1() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest2-", ".tmp");
		file.deleteOnExit();
		mgr.checkRead(file.getName());
	}

	@Test
	public void checkRead3() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest3-", ".tmp");
		file.deleteOnExit();
		mgr.checkRead(file.getName(), new Object());
	}

	@Test
	public void checkWrite0() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest4-", ".tmp");
		file.deleteOnExit();
		FileOutputStream ostr = new FileOutputStream(file);
		FileDescriptor fd = ostr.getFD();
		mgr.checkWrite(fd);
	}

	@Test
	public void checkWrite1() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest5-", ".tmp");
		file.deleteOnExit();
		mgr.checkWrite(file.getName());
	}

	@Test
	public void checkDelete() throws IOException {
		File file = File.createTempFile("nullsecuritymanagertest6-", ".tmp");
		file.deleteOnExit();
		mgr.checkDelete(file.getName());
	}

	@Test
	public void checkConnect0() {
		mgr.checkConnect("localhost", 8081);
	}

	@Test
	public void checkConnect1() {
		mgr.checkConnect("localhost", 8081, new Object());
	}

	@Test
	public void checkListen() {
		mgr.checkListen(123);
	}

	@Test
	public void checkAccept() {
		mgr.checkAccept("localhost", 1234);
	}

	@Test
	public void checkMulticast0() throws UnknownHostException {
		mgr.checkMulticast(InetAddress.getLocalHost());
	}

	@Test
	public void checkMulticast1() throws UnknownHostException {
		InetAddress localhost = InetAddress.getLocalHost();
		byte ttl = 05;
		mgr.checkMulticast(localhost, ttl);
	}

	@Test
	public void checkPropertiesAccess() {
		mgr.checkPropertiesAccess();
	}

	@Test
	public void checkPropertyAccess() {
		mgr.checkPropertyAccess("nullsecuritymanagertest.test.propertykeyname");
	}

	@Test
	public void checkPrintJobAccess() {
		mgr.checkPrintJobAccess();
	}

	@Test
	public void checkPackageAccess() {
		mgr.checkPackageAccess("dummypackageaccess.jar");
	}

	@Test
	public void checkPackageDefinition() {
		mgr.checkPackageDefinition("dummypackagedefinition.jar");
	}

	@Test
	public void checkSetFactory() {
		mgr.checkSetFactory();
	}

	@Test
	public void checkSecurityAccess() {
		mgr.checkSecurityAccess("securityaccesstarget");
	}

	
	
}
