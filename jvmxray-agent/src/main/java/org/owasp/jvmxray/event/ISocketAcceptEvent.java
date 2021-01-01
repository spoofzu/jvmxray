package org.owasp.jvmxray.event;

public interface ISocketAcceptEvent extends IEvent {

	public String getHost();
	
	public int getPort();
	
}
