package org.owasp.jvmxray.api;

import java.security.Permission;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayPermissionWithContextEvent extends JVMXRayBaseEvent {

	private static final String format = "n=%s, a=%s, cn=%s, ctx=%s";
	
	public JVMXRayPermissionWithContextEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.PERMISSION_WITH_CONTEXT,stacktrace, callstackopt, parameters);
	}

	public Permission getPermission() {
		Object[] obj = getParameters();
		Permission p = (Permission)obj[0];
		return p;
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
		Permission p = getPermission();
		return new Object[] {p.getName(),p.getActions(),p.getClass().getName(),getContext().toString()};
	}

}
