package org.owasp.jvmxray.api;


import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayPropertiesAnyEvent extends JVMXRayBaseEvent {

	private static final String format = "";
	
	public JVMXRayPropertiesAnyEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.PROPERTIES_ANY, stacktrace, callstackopt, parameters);
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[0];
	}

}
