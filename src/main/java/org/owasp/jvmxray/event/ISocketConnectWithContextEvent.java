package org.owasp.jvmxray.event;

public interface ISocketConnectWithContextEvent extends ISocketConnectEvent {

	public String getHost();

	public int getPort();
	
	public String getContext();
	
}
