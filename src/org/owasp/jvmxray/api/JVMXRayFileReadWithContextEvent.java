package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayFileReadWithContextEvent extends JVMXRayBaseEvent {
	
	private static final String format = "f=%s, c=%s";

	public JVMXRayFileReadWithContextEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.FILE_READ_WITH_CONTEXT, stacktrace, callstackopt, parameters);
	}

	public String getFileName() {
		Object[] obj = getParameters();
		String filename = (String)obj[0];
		return filename;
	}
	
	public Object getContext() {
		Object[] obj = getParameters();
		Object context = (Object)obj[1];
		return context;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getFileName(), getContext().toString()};
	}

}
