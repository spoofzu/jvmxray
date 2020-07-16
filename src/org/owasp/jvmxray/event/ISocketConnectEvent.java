package org.owasp.jvmxray.event;

public interface ISocketConnectEvent extends IEvent {

	public String getHost();
	
	public int getPort();

}

