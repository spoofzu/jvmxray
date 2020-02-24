package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayClassLoaderCreateEvent extends JVMXRayBaseEvent {

private static final String format = "";
	
	public JVMXRayClassLoaderCreateEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.CLASSLOADER_CREATE, stacktrace, callstackopt, parameters);
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[0];
	}
}
