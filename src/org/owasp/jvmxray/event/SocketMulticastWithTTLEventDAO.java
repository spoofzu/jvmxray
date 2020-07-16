package org.owasp.jvmxray.event;

public class SocketMulticastWithTTLEventDAO extends ImmutableEvent implements ISocketMulticastWithTTLEvent {

	SocketMulticastWithTTLEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace, String addr, String ttl) {
		super(pk, state, timestamp, tid, Events.SOCKET_MULTICAST_WITH_TTL, identity, stacktrace, addr, ttl, "" );
	}

	@Override
	public String getAddress() {
		String[] p = super.getParams();
		return p[0];
	}

	@Override
	public String getTTL() {
		String[] p = super.getParams();
		return p[1];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("ttl=");
		buff.append(getTTL());
		
		return buff.toString();
	}
	
}
