package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayAccessThreadEvent extends JVMXRayBaseEvent {

	private static final String format = "t=%s";
	
	public JVMXRayAccessThreadEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.ACCESS_THREAD, stacktrace, callstackopt, parameters);
	}

	public Thread getThread() {
		Object[] obj = getParameters();
		Thread t = (Thread)obj[0];
		return t;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getThread()};
	}
}
