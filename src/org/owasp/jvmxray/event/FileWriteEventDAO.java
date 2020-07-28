package org.owasp.jvmxray.event;

public class FileWriteEventDAO extends BaseEvent implements IFileWriteEvent {

	private String file;

	FileWriteEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			 String file) {
		super(pk, state, timestamp, tid, Events.FILE_WRITE, identity, stacktrace, file, "", "");
		this.file = file;
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
