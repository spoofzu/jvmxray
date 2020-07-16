package org.owasp.jvmxray.event;

public interface ISocketMulticastWithTTLEvent extends ISocketMulticastEvent {

	public String getTTL();
	
}
