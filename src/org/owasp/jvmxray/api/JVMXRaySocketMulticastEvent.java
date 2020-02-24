package org.owasp.jvmxray.api;

import java.net.InetAddress;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRaySocketMulticastEvent extends JVMXRayBaseEvent {

	private static final String format =  "maddr=%s";
	
	public JVMXRaySocketMulticastEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.SOCKET_MULTICAST, stacktrace, callstackopt, parameters);
	}

	public InetAddress getAddress() {
		Object[] obj = getParameters();
		InetAddress h = (InetAddress)obj[0];
		return h;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getAddress().toString()};
	}

}
