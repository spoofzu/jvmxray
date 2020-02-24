package org.owasp.jvmxray.api;

import java.net.InetAddress;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRaySocketMulticastWithTTLEvent extends JVMXRayBaseEvent {

	private static final String format =  "maddr=%s, ttl=%s";
	
	public JVMXRaySocketMulticastWithTTLEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.SOCKET_MULTICAST_WITH_TTL, stacktrace, callstackopt, parameters);
	}

	public InetAddress getAddress() {
		Object[] obj = getParameters();
		InetAddress h = (InetAddress)obj[0];
		return h;
	}
	
	public byte getTTL() {
		Object[] obj = getParameters();
		byte ttl = (byte)obj[0];
		return ttl;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getAddress().toString(),Integer.toHexString(getTTL())};
	}


}
