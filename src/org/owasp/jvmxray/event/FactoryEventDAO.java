package org.owasp.jvmxray.event;

public class FactoryEventDAO extends BaseEvent implements IFactoryEvent {

	FactoryEventDAO(int pk, int state, long timestamp, String identity, String tid, String stacktrace) {
		super(pk, state, timestamp, tid, Events.FACTORY, identity, stacktrace, "", "", "");
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		
		return buff.toString();
		
	}

}
