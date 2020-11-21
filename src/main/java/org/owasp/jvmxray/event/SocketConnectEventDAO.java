package org.owasp.jvmxray.event;

public class SocketConnectEventDAO extends BaseEvent implements ISocketConnectEvent {


	SocketConnectEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String host, int port) {
		super(pk, state, timestamp, tid, Events.SOCKET_CONNECT, identity, stacktrace, host, Integer.toString(port), "");
	}

	@Override
	public String getHost() {
		String[] p = super.getParams();
		return p[0];
	}

	@Override
	public int getPort() {
		String[] p = super.getParams();
		return Integer.parseInt(p[1]);
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("host=");
		buff.append(getHost());
		buff.append(",");
		
		buff.append("port=");
		buff.append(getPort());

		
		return buff.toString();
	}


}
