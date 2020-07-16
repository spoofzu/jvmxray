package org.owasp.jvmxray.event;

public class AccessSecurityEventDAO extends ImmutableEvent implements IAccessSecurityEvent {
	
	public AccessSecurityEventDAO(int id, int state, long timestamp, String tid, String identity, String stacktrace, String target) {	
		super(id, state, timestamp, tid, Events.ACCESS_SECURITY, identity, stacktrace, target, "", "");
	}
	
	public String getTarget() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("target=");
		buff.append(getTarget());
		
		return buff.toString();
		
	}
	
}
