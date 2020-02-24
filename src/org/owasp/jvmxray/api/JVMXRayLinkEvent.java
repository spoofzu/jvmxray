package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayLinkEvent extends JVMXRayBaseEvent {

private static final String format =  "lib=%s, stack=%s";
	
	public JVMXRayLinkEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.LINK, stacktrace, callstackopt, parameters);
	}

	public String getLibraryName() {
		Object[] obj = getParameters();
		String name = (String)obj[0];
		return name;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getLibraryName(), getStackTrace()};
	}
}
