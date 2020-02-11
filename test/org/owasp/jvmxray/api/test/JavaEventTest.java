package org.owasp.jvmxray.api.test;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;
import org.owasp.jvmxray.handlers.ConsoleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

public class JavaEventTest {
	
	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.api.test.JavaEventTest");
	
	// Using @Begin annotation would be customary but we need to ensure
	// logging is initialized prior to it's use.
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
		
		// Choose the event sink.
		//LogbackHanlder h = new LogbackHanlder();
		ConsoleHandler h = new ConsoleHandler();
		System.setSecurityManager(h);
		
	}
	
	@Test
	public void testFileManagement() {
		
		try{
			//create a temp file
	    	File temp = File.createTempFile("temp-file-name", ".tmp"); 
	    	boolean r = temp.canRead();
	    	boolean w = temp.canWrite();
	    	boolean e = temp.canExecute();  // NOTE triggers execute event check.
	    	StringBuffer buff = new StringBuffer();
	    	buff.append("File="+temp.getName()+"file permissions=");
	    	if (r) buff.append("R");
	    	if (w) buff.append("W");
	    	if (e) buff.append("X");
	    	logger.info(buff.toString());
	    }catch(IOException e){
	    	e.printStackTrace();
	    }
		logger.info("complete - file management");
		
	}
	
	@Test
	public void testSystemProperties() {
		System.setProperty("notepad","imlovenit");
		logger.info("complete - property test");
	}
	
	@Test
	public void testSystemExec() {
		
		try {
			Process p = Runtime.getRuntime().exec("cd ."+File.separatorChar);
			logger.info("process exit code="+p.exitValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("complete - execution test");
	}
	
	@Test
	public void testSocketConnection() {
		
		try {
			// We only create and close the socket.  Not necessary to send/read data.
			Socket echoSocket = new Socket("localhost",7);
			echoSocket.close();
		} catch (IOException e) {}
		
		logger.info("complete - socket connection test");
	}
	
	@Test
	public void testURL() {
		
		try {
			// We only need to retrieve a connection.  Not necessary to send/read data.
			URLConnection connection = new URL("https://www.oracle.com/").openConnection();
			connection.getContent();
		} catch (IOException e) {}
		
		logger.info("complete - url connection test");
	}
	
	@Test
	public void testJDBC() {
		

		//tbd dbunit
		
	}
	
}
