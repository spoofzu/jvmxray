package org.owasp.jvmxray.event;

public class FileReadWithContextEventDAO extends ImmutableEvent implements IFileReadWithContextEvent {

	FileReadWithContextEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String file, String context ) {
		super(pk, state, timestamp, tid, Events.FILE_READ_WITH_CONTEXT, identity, stacktrace, file, context, "");
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
