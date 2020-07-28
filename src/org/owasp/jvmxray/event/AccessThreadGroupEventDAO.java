package org.owasp.jvmxray.event;

public class AccessThreadGroupEventDAO extends BaseEvent implements IAccessThreadGroupEvent {
	
	public AccessThreadGroupEventDAO(int id, int state, long timestamp, String tid, String identity, String stacktrace,
		 String tg) {
		super(id, state, timestamp, tid, Events.ACCESS_THREADGROUP, identity, stacktrace, tg, "", "");
	}

	@Override
	public String getThreadGroupSignature() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("tg=");
		buff.append(getThreadGroupSignature());
		
		return buff.toString();
		
	}
	
}
