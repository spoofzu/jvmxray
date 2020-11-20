package org.owasp.jvmxray.event;

public interface IFileReadWithContextEvent extends IEvent {

	public String getFile();
	
	public String getContext();
}
