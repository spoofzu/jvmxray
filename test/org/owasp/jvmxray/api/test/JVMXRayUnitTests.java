package org.owasp.jvmxray.api.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.owasp.jvmxray.adaptors.JVMXRayConsoleAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * 
 * A simple unit test to assess basic functions of the api.  
 * @author Milton Smith
 *
 */
public class JVMXRayUnitTests {
	
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.api.test.JVMXRayUnitTests");
	
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final PrintStream originalStream = System.out;

//  Usually prints a few lines like to the console  even through streams are redirected.  This is because
//  calls to System.setOut() generates permission checks when we reassign the streams.
//
//	CONSOLEADAPTOR 2020-02-25 16:05:37 PST PACKAGE_ACCESS, pkg=java.lang,stack=<disabled>
//	CONSOLEADAPTOR 2020-02-25 16:05:38 PST PERMISSION, n=setIO, a=, cn=java.lang.RuntimePermission,stack=<disabled>
//	CONSOLEADAPTOR 2020-02-25 16:05:38 PST PERMISSION, n=setIO, a=, cn=java.lang.RuntimePermission,stack=<disabled>
//	CONSOLEADAPTOR 2020-02-25 16:05:38 PST PERMISSION, n=setIO, a=, cn=java.lang.RuntimePermission,stack=<disabled>

	
	static {
		
		// Initialize logback logging
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		ContextInitializer ci = new ContextInitializer(lc);
		lc.reset();
		try {
			ci.autoConfig(); 
		} catch (JoranException e) {
			e.printStackTrace();
		}
		
		// Console adaptor for easy testing.
		JVMXRayConsoleAdaptor h = new JVMXRayConsoleAdaptor();
		System.setSecurityManager(h);
		
	}
	
	// Comment out save and restore stream to see lots of messages in your console window.	
	@Before
	public void saveStream() {
	   System.setOut(new PrintStream(out));
	}
	
	@After
	public void restoreStream() {
	   System.setOut(originalStream);
	}
	
	@Test
	public void testSetSystemProperty() {
		
		System.setProperty("notepad","imlovenit");		
		assertTrue(stream().contains("PERMISSION"));
		assertTrue(stream().contains("notepad"));
		assertTrue(stream().contains("a=write"));
		
	}

// TODOMS: Not sure why this does not work.  Seems like it should but no event fired.
//
//	@Test
//	public void testWriteToFile() {
//		
//		File tmp = null;
//		try{
//			tmp = File.createTempFile("jvmxray-temp2", null); 
//		}catch(Exception e) {}
//		
//		PrintWriter writer;
//		try {
//			writer = new PrintWriter(tmp, "UTF-8");
//			writer.println("She sells sea shells by the seashore.");
//			writer.close();
//		} catch (FileNotFoundException | UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		assertTrue(stream().contains("FILE_WRITE"));
//		assertTrue(stream().contains("jvmxray-temp2"));
//	}

	@Test
	public void testExec() {

		Process p = null;
		try {
			p = Runtime.getRuntime().exec("cd ."+File.separatorChar);
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue(stream().contains("FILE_EXECUTE"));
		assertTrue(stream().contains("cd"));
	}

	
	@Test
	public void testSocketConnection() {
		
		try {
			Socket socket = new Socket("localhost",7);
			socket.close();
		} catch (IOException e) {}
		assertTrue(stream().contains("SOCKET_CONNECT"));
		assertTrue(stream().contains("h=localhost"));
		assertTrue(stream().contains("p=7"));
	}
	
	@Test
	public void testURL() {
		try {
			URLConnection connection = new URL("https://www.github.com/").openConnection();
			connection.getContent();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue(stream().contains("SOCKET_CONNECT"));
		assertTrue(stream().contains("h=github.com"));
		assertTrue(stream().contains("p=443"));
	}
	
	public String stream() {
		return out.toString();
	}
}
