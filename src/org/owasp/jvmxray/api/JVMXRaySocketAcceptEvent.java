package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRaySocketAcceptEvent extends JVMXRayBaseEvent {

	private static final String format = "h=%s, p=%s";
	
	public JVMXRaySocketAcceptEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.PERMISSION, stacktrace, callstackopt, parameters);
	}

	public String getHost() {
		Object[] obj = getParameters();
		String h = (String)obj[0];
		return h;
	}
	
	public int getPort() {
		Object[] obj = getParameters();
		int p = (int)obj[1];
		return p;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getHost(),getPort()};
	}

}
