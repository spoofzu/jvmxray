package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Events;

public interface IJVMXRayEvent {
	
	public Events getType();
	
	public Object[] getParameters();
	
	public String getStringFormat();
	
	public Object[] getStringArgs();
	
	public StackTraceElement[] getStackTrace();
	
	public String toString();

}
