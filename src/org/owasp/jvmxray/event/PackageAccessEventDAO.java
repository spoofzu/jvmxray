package org.owasp.jvmxray.event;

public class PackageAccessEventDAO extends BaseEvent implements IPackageAccessEvent {

	PackageAccessEventDAO(int pk, int state, long timestamp, String tid, String identity, String stacktrace,
			String pkg) {
		super(pk, state, timestamp, tid, Events.PACKAGE_ACCESS, identity, stacktrace, pkg, "", "");
	}


	@Override
	public String getPackage() {
		String[] p = super.getParams();
		return p[0];
	}
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		buff.append(super.toString());
		buff.append(",");
		
		buff.append("pkg=");
		buff.append(getPackage());

		
		return buff.toString();
		
	}
	
}
