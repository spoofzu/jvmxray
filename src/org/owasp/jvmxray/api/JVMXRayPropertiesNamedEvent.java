package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayPropertiesNamedEvent extends JVMXRayBaseEvent {

	private static final String format = "kn=%s";
	
	public JVMXRayPropertiesNamedEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.PROPERTIES_NAMED, stacktrace, callstackopt, parameters);
	}

	public String getKeyName() {
		Object[] obj = getParameters();
		String kn = (String)obj[0];
		return kn;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getKeyName()};
	}

}
