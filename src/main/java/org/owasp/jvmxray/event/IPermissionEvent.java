package org.owasp.jvmxray.event;

public interface IPermissionEvent extends IEvent {

	public String getPermissionName();
	
	public String getPermissionActions();
	
}
