package org.owasp.jvmxray.event;

public class AccessThreadtEventDAO extends BaseEvent implements IAccessThreadEvent {
	
	public AccessThreadtEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace, String threadid) {
		super(pk, state, timestamp, tid, Events.ACCESS_THREAD, identity, stacktrace, threadid, "", "");
	}

	@Override
	public String getThreadId() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("tid=");
		buff.append(getThreadId());
		
		return buff.toString();
		
	}
	
}
