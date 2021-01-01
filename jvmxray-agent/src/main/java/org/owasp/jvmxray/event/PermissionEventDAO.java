package org.owasp.jvmxray.event;

public class PermissionEventDAO extends BaseEvent implements IPermissionEvent {


	PermissionEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String name, String actions ) {
		super(pk, state, timestamp, tid, Events.PERMISSION, identity, stacktrace, name, actions, "");
	}

	@Override
	public String getPermissionName() {
		String[] p = super.getParams();
		return p[0];
	}

	@Override
	public String getPermissionActions() {
		String[] p = super.getParams();
		return p[1];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("n=");
		buff.append(getPermissionName());
		buff.append(",");
		
		buff.append("a=");
		buff.append(getPermissionActions());

		
		return buff.toString();
		
	}
	
}
