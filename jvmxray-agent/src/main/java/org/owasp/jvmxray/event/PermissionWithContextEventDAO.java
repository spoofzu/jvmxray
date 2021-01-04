package org.owasp.jvmxray.event;

public class PermissionWithContextEventDAO extends BaseEvent implements IPermissionWithContextEvent {

	PermissionWithContextEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace, String name, String actions, String context) {
		super(pk, state, timestamp, tid, Events.PERMISSION_WITH_CONTEXT, identity, stacktrace, name, actions, context);
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
	
	@Override
	public String getContext() {
		String[] p = super.getParams();
		return p[2];
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
		buff.append(",");
		
		buff.append("ctx=");
		buff.append(getContext());

		
		return buff.toString();
	}
		

}
