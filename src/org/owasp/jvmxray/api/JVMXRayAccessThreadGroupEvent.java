package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayAccessThreadGroupEvent extends JVMXRayBaseEvent {

private static final String format = "tg=%s";
	
	public JVMXRayAccessThreadGroupEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.ACCESS_THREADGROUP, stacktrace, callstackopt, parameters);
	}

	public String getThreadGroup() {
		Object[] obj = getParameters();
		String tg = (String)obj[0];
		return tg;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getThreadGroup()};
	}

}
