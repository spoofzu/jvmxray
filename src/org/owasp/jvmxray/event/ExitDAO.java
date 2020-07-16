package org.owasp.jvmxray.event;

public class ExitDAO extends ImmutableEvent implements IExitEvent {

	private int status;
	
	ExitDAO(int pk, int state, long timestamp, String identity, String tid, String stacktrace,
			int status) {
		super(pk, state, timestamp, tid, Events.EXIT, identity, stacktrace, Integer.toString(status), "", "");
		this.status = status;
	}

	@Override
	public int getStatus() {
		String[] p = super.getParams();
		return Integer.parseInt(p[0]);
	}

	public String[] getParams() {
		return new String[] { Integer.toString(getStatus()) };
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("status=");
		buff.append(getStatus());

		
		return buff.toString();
		
	}
	
}
