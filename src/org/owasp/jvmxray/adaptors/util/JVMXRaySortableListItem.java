package org.owasp.jvmxray.adaptors.util;

import org.owasp.jvmxray.util.IEvent;

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
		String eventname = event.getEventType();
		String meta = event.getMemo();
		return eventname + meta;
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
		buff.append(event.getMemo());

		return buff.toString();
	}

}
