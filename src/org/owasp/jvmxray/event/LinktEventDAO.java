package org.owasp.jvmxray.event;

public class LinktEventDAO extends ImmutableEvent implements ILinkEvent {


	LinktEventDAO(int pk, int state, long timestamp,  String tid, String identity, String stacktrace,
			 String lib) {
		super(pk, state, timestamp, tid, Events.LINK, identity, stacktrace, lib, "", "");
	}

	@Override
	public String getLib() {
		String[] p = getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("lib=");
		buff.append(getLib());

		
		return buff.toString();
		
	}

}
