package org.owasp.jvmxray.event;

public class SocketMulticastEventDAO extends BaseEvent implements ISocketMulticastEvent {

	SocketMulticastEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String addr) {
		super(pk, state, timestamp, tid, Events.SOCKET_MULTICAST, identity, stacktrace, addr, "", "");
	}

	@Override
	public String getAddress() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("addr=");
		buff.append(getAddress());

		
		return buff.toString();
	}
	
}
