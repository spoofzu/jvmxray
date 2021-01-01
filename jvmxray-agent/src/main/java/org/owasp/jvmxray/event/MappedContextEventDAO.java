package org.owasp.jvmxray.event;

import org.owasp.jvmxray.event.IEvent.Events;

public class MappedContextEventDAO extends BaseEvent implements IMappedContextEvent {

	MappedContextEventDAO(int pk, int state, long timestamp, String tid, String identity,
			String stacktrace, String key, String value) {
		super(pk, state, timestamp, tid, Events.MAPPED_CONTEXT, identity, stacktrace, key, value, "" );
	}
	
	@Override
	public String getKey() {
		String[] p = super.getParams();
		return p[0];
	}
	
	@Override
	public String getValue() {
		String[] p = super.getParams();
		return p[1];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("key=");
		buff.append(getKey());
		buff.append(",");
		
		buff.append("value=");
		buff.append(getValue());

		
		return buff.toString();
		
	}

}
