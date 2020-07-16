package org.owasp.jvmxray.adaptors.util;

import org.owasp.jvmxray.event.IEvent;

public class JVMXRaySortableListItem implements Comparable<JVMXRaySortableListItem>  {

	private IEvent event;
	private int count;
	
	public JVMXRaySortableListItem(IEvent event, int count) {
		this.event = event;
		this.count = count;
	}
	
	public IEvent getEvent() {
		return event;
	}
	
	@Override
	public int compareTo(JVMXRaySortableListItem o) {
		return Integer.valueOf(o.getCount()).compareTo(Integer.valueOf(this.getCount()));
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public String getIdentity() {
		// Provides the event without the count as an identity for list item
		// comparison, PERMISSION,n=setContextClassLoader, a=, cn=java.lang.RuntimePermission
		String eventname = event.getEventType().toString();
		String p0 = event.getParams()[0];
		String p1 = event.getParams()[1];
		String p2 = event.getParams()[2];
		StringBuffer meta = new StringBuffer();
		if ( p0 != null && p0.length()>0 ) {
			meta.append(p0);
		}
		if ( p1 != null && p1.length()>0 ) {
			meta.append(p1);
		}
		if ( p2 != null && p2.length()>0 ) {
			meta.append(p2);
		}
		return eventname + meta.toString();
	}
	
	public boolean equals(Object obj) {
		boolean result = false;
		
		if( obj != null && obj instanceof JVMXRaySortableListItem ) {
			JVMXRaySortableListItem target = (JVMXRaySortableListItem)obj;
			result = this.getIdentity().equals(target.getIdentity());
		}
		
		return result;
	}
	
	public String toString() {

		StringBuffer buff = new StringBuffer();
		buff.append(event.getEventType());
		buff.append(' ');
		buff.append(getCount());
		buff.append(' ');
		
		String p0 = event.getParams()[0];
		String p1 = event.getParams()[1];
		String p2 = event.getParams()[2];
		StringBuffer meta = new StringBuffer();
		if ( p0 != null && p0.length()>0 ) {
			meta.append(p0);
		}
		if ( p1 != null && p1.length()>0 ) {
			meta.append(p1);
		}
		if ( p2 != null && p2.length()>0 ) {
			meta.append(p2);
		}
		
		buff.append(meta.toString());

		return buff.toString();
	}

}
