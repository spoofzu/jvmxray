package org.owasp.jvmxray.event;

public class FileReadWithFileDescriptorEventDAO extends BaseEvent implements IFileReadWithFileDescriptorEvent {


	FileReadWithFileDescriptorEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace) {
		super(pk, state, timestamp, tid, Events.FILE_READ_WITH_FILEDESCRIPTOR, identity, stacktrace, null, null, null);
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());

	return buff.toString();
		
	}

}
