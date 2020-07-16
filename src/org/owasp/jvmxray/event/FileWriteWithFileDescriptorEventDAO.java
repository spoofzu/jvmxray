package org.owasp.jvmxray.event;

public class FileWriteWithFileDescriptorEventDAO extends ImmutableEvent implements IFileWriteWithFileDescriptorEvent {

	FileWriteWithFileDescriptorEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace ) {
		super(pk, state, timestamp, tid, Events.FILE_WRITE_WITH_FILEDESCRIPTOR, identity, stacktrace, "", "", "");
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		
		return buff.toString();
		
	}

}
