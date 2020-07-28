package org.owasp.jvmxray.event;

public class ClassLoaderEventDAO extends BaseEvent implements IClassLoaderEvent {
	
	public ClassLoaderEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace) {
		super(pk, state, timestamp, tid, Events.CLASSLOADER_CREATE, identity, stacktrace, "", "", "");

	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		
		return buff.toString();
		
	}

}
