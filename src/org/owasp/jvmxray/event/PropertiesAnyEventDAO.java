package org.owasp.jvmxray.event;

public class PropertiesAnyEventDAO extends ImmutableEvent implements IPropertiesAnyEvent {

	PropertiesAnyEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace ) {
		super(pk, state, timestamp, tid, Events.PROPERTIES_ANY, identity, stacktrace, "", "", "" );
	}

	public String[] getParams() {
		return new String[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());

		
		return buff.toString();
	}

}
