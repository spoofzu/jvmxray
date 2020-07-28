package org.owasp.jvmxray.event;

public class FileExecuteEventDAO extends BaseEvent implements IFileExecuteEvent {

	FileExecuteEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String command) {
		super(pk, state, timestamp, tid, Events.FILE_EXECUTE, identity, stacktrace, command, "", "");
	}

	@Override
	public String getCommand() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("cmd=");
		buff.append(getCommand());
		
		return buff.toString();
		
	}
	
}
