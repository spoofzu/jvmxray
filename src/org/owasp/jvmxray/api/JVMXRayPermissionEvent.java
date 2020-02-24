package org.owasp.jvmxray.api;

import java.security.Permission;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayPermissionEvent extends JVMXRayBaseEvent {

	private static final String format = "n=%s, a=%s, cn=%s";
	
	public JVMXRayPermissionEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.PERMISSION, stacktrace, callstackopt, parameters);
	}

	public Permission getPermission() {
		Object[] obj = getParameters();
		Permission p = (Permission)obj[0];
		return p;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		Permission p = getPermission();
		return new Object[] {p.getName(),p.getActions(),p.getClass().getName()};
	}

}
