package org.owasp.jvmxray.event;

public class PropertiesNamedEventDAO extends ImmutableEvent implements IPropertiesNamedEvent {

	PropertiesNamedEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			 String key) {
		super(pk, state, timestamp, tid, Events.PROPERTIES_NAMED, identity, stacktrace, key, "", "" );
	}

	@Override
	public String getKey() {
		String[] p = getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("key=");
		buff.append(getKey());

		
		return buff.toString();
	}

}
