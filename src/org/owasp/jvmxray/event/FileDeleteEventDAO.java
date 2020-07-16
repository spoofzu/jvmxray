package org.owasp.jvmxray.event;

public class FileDeleteEventDAO extends ImmutableEvent implements IFileDeleteEvent {

	public FileDeleteEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String file ) {
		super(pk, state, timestamp, tid, Events.FILE_DELETE, identity, stacktrace, file, "", "");
	}

	@Override
	public String getFile() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("file=");
		buff.append(getFile());
		
		return buff.toString();
		
	}

}
