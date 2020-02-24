package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayFileWriteEvent extends JVMXRayBaseEvent {

private static final String format = "f=%s";
	
	public JVMXRayFileWriteEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.FILE_WRITE, stacktrace, callstackopt, parameters);
	}

	public String getFileName() {
		Object[] obj = getParameters();
		String filename = (String)obj[0];
		return filename;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getFileName()};
	}

}
