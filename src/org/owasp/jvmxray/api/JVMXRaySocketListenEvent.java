package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRaySocketListenEvent extends JVMXRayBaseEvent {

private static final String format =  "p=%s";
	
	public JVMXRaySocketListenEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.SOCKET_LISTEN, stacktrace, callstackopt, parameters);
	}

	public int getPort() {
		Object[] obj = getParameters();
		int p = (int)obj[0];
		return p;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getPort()};
	}

}
