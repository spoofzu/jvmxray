package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayAccessSecurityEvent extends JVMXRayBaseEvent {

	private static final String format = "t=%s";
	
	public JVMXRayAccessSecurityEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.ACCESS_SECURITY, stacktrace, callstackopt, parameters);
	}

	public String getTarget() {
		Object[] obj = getParameters();
		String target = (String)obj[0];
		return target;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getTarget()};
	}
	
}
