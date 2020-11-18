package org.owasp.jvmxray.event;

public class MappedEventDAO extends BaseEvent implements IMappedEvent {

	private long ttl = 0;
	
	MappedEventDAO(int pk, int state, long timestamp, String tid, String identity,
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
	
	@Override
	public void setValue( String value ) {
		String[] p = super.getParams();
		p[1] = value;
		ttl = 0;
	}
	
	@Override
	public long getTTL() {
		return ttl;
	}

	@Override
	public boolean isUpdated() {
		return false;
	}
	
	@Override
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
