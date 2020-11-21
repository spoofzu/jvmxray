package org.owasp.jvmxray.event;

public class SocketListenEventDAO extends BaseEvent implements ISocketListenEvent {

	SocketListenEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			int port) {
		super(pk, state, timestamp, tid, Events.SOCKET_LISTEN, identity, stacktrace, Integer.toString(port), "", "");
		
	}

	public int getPort() {
		String[] p = super.getParams();
		return Integer.parseInt(p[0]);
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("port=");
		buff.append(Integer.toString(getPort()));

		
		return buff.toString();
	}
	
}
