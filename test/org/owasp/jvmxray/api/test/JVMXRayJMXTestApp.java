package org.owasp.jvmxray.api.test;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import org.owasp.jvmxray.adaptors.JMXAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

public class JVMXRayJMXTestApp {

	private static final Logger logger = LoggerFactory.getLogger("org.owasp.jvmxray.sample.JVMXRayJMXTestApp");
	
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
	
//		System.setProperty("com.sun.management.jmxremote","");
//		System.setProperty("com.sun.management.jmxremote.port","1617");
//		System.setProperty("com.sun.management.jmxremote.authentication","false");
//		System.setProperty("com.sun.management.jmxremote.ssl","false");
		
		JMXAdaptor h = new JMXAdaptor();
		System.setSecurityManager(h);
		
	}
	
	public void init() {
		
		// Give a second to stabilize then trigger a few activities
		// to create some noise for our JMX bean.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		try {
			Socket socket = new Socket("localhost",7);
			socket.close();
		} catch (IOException e) {}

		try {
			URLConnection connection = new URL("https://www.github.com/").openConnection();
			connection.getContent();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public static void main(String[] args) {
		JVMXRayJMXTestApp app = new JVMXRayJMXTestApp();
		app.init();

		Scanner keyboard = new Scanner(System.in);
        boolean exit = false;
        while (!exit) {
            System.out.println("Press ENTER key to exit...");
            String input = keyboard.nextLine();
            if(input != null) {
                System.out.println("Finished");
                exit = true;
            }
        }
        keyboard.close();
	}
	
}
