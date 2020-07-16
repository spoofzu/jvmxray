package org.owasp.jvmxray.event;


public class SocketAcceptEventDAO extends ImmutableEvent implements ISocketAcceptEvent {
	
	SocketAcceptEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			 String host, int port) {

		super(pk, state, timestamp, tid, Events.SOCKET_ACCEPT, identity, stacktrace, host, Integer.toString(port), "" );
		
	}

	public String getHost() {
		String[] p = super.getParams();
		return p[0];
	}
	
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
