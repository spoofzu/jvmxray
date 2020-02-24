package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayExitEvent extends JVMXRayBaseEvent {

private static final String format = "s=%s";
	
	public JVMXRayExitEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.EXIT, stacktrace, callstackopt, parameters);
	}

	public int getStatusCode() {
		Object[] obj = getParameters();
		int status = (int)obj[0];
		return status;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getStatusCode()};
	}
}
