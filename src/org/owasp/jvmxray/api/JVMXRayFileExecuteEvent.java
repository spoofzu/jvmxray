package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayFileExecuteEvent extends JVMXRayBaseEvent {

private static final String format = "cmd=%s";
	
	public JVMXRayFileExecuteEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.FILE_EXECUTE, stacktrace, callstackopt, parameters);
	}

	public String getCommand() {
		Object[] obj = getParameters();
		String command = (String)obj[0];
		return command;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getCommand()};
	}

}
