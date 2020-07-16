package org.owasp.jvmxray.event;

public class CreateFileReadWithContextEventDAO extends ImmutableEvent implements IFileReadWithContextEvent {

	CreateFileReadWithContextEventDAO(int pk, int state, long timestamp, String tid, Events event, String identity,
			String stacktrace, String file, String context) {
		super(pk, state, timestamp, tid, event, identity, stacktrace, file, context, null);
	}

	@Override
	public String getFile() {
		String[] p = super.getParams();
		return p[0];
	}

	@Override
	public String getContext() {
		String[] p = super.getParams();
		return p[1];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("file=");
		buff.append(getFile());
		buff.append(",");
		
		buff.append("ctx=");
		buff.append(getContext());
		
		return buff.toString();
		
	}

}
