package org.jvmxray;

import java.security.BasicPermission;
import java.util.Random;

import org.junit.experimental.categories.Category;
import org.jvmxray.collector.JVMXRayServletContainer;
import org.jvmxray.driver.RedSecurityManager;

/**
 * Send significant number of permission events to the server and
 * measure the response.
 * @author Milton Smith
 *
 */
//@Category(IntegrationTest.class)
public class NullSecurityManagerPerfIntegrationTest {

	private static RedSecurityManager nullsecuritymgr = new RedSecurityManager();
	private static JVMXRayServletContainer server;

	/**
	 * Create a new sample permission for testing.
	 */
	public class TestPermission extends BasicPermission {
		private static final long serialVersionUID = 5932223970203786295L;
		private String action = "";
		public TestPermission(String name) {
			super(name);
			Random r = new Random(System.currentTimeMillis());
			action = name + "-action-"+r.nextLong();
		}
		public String getActions() {
			return action;
		}
	}


	/**
	 * Initialize a server for testing.
	 */
	public static void setup() {	
		try {
			server = JVMXRayServletContainer.getInstance();
			server.serverStart();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
		nullsecuritymgr = new RedSecurityManager();
	}
	
	// Terminate server upon conclusion of test.
	public static void finish() {	
		server.finish();
	}
	
	/** Call NullSecurityManager.checkPermission(Permission) N times.
	 * Wait 5 mins to allow for time to check /api/status/
	 * TestPermissions.
	 */
	public void performanceTest() {
		int i=0;
		for( i=0; i<500; i++ ) {
			Random r = new Random(System.currentTimeMillis());
			StringBuffer buff = new StringBuffer();
			buff.append(r.nextInt(91)+65);
			TestPermission p = new TestPermission(buff.toString());
			nullsecuritymgr.checkPermission(p);
			// Allow system some time for housekeeping tasks.
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			Thread.yield();
		}
		// Leave server up for 2-min after test so we can run /api/status/
		long currentTime = System.currentTimeMillis();
		long stopTime = currentTime + (1000*60*2);  // 2-min.
		while( currentTime < stopTime ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			Thread.yield();
			currentTime = System.currentTimeMillis();
		}

	}
	
	/**
	 * Start test.  Call http://localhost:9123/api/status/ in your browser to see
	 * performance data.  Kill the app when your done or 2min, whichever comes first.
	 * @param argv Command line parameters.
	 */
	public static void main( String[] argv) {
		NullSecurityManagerPerfIntegrationTest test = new NullSecurityManagerPerfIntegrationTest();
		try {
			test.setup();
			test.performanceTest();
		} finally {
			test.finish();
		}
	}



}
