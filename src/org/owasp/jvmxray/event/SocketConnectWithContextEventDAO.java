package org.owasp.jvmxray.event;

public class SocketConnectWithContextEventDAO extends BaseEvent implements ISocketConnectWithContextEvent {

	SocketConnectWithContextEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace, String host, int port, String context ) {
		super(pk, state, timestamp, tid, Events.SOCKET_CONNECT_WITH_CONTEXT, identity, stacktrace, host, Integer.toString(port), context);

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
	
	@Override
	public String getContext() {
		String[] p = super.getParams();
		return p[2];
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
		buff.append(",");
		
		buff.append("ctx=");
		buff.append(getContext());
		
		return buff.toString();
	}

}
