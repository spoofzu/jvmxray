package org.owasp.jvmxray.event;

public interface IPermissionWithContextEvent extends IPermissionEvent {

	public String getContext();
	
}
