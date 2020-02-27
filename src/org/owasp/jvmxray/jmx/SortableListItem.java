package org.owasp.jvmxray.jmx;


import org.owasp.jvmxray.api.IJVMXRayEvent;

public class SortableListItem implements Comparable<SortableListItem>  {

	private IJVMXRayEvent event;
	private int count;
	
	public SortableListItem(IJVMXRayEvent event, int count) {
		this.event = event;
		this.count = count;
	}
	
	@Override
	public int compareTo(SortableListItem o) {
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
		String eventname = event.getType().toString();
		Object[] obj = event.getParameters();
		String meta = "";		
		if( obj!=null)
			meta = event.getParameters()[0].toString();
		return eventname + meta;
	}
	
	public boolean equals(Object obj) {
		boolean result = false;
		
		if( obj != null && obj instanceof SortableListItem ) {
			SortableListItem target = (SortableListItem)obj;
			result = this.getIdentity().equals(target.getIdentity());
		}
		
		return result;
	}
	
	public String toString() {
		Object[] obj = event.getParameters();
		String meta = "";
		
		if( obj!=null)
			meta = event.getParameters()[0].toString();
		
		StringBuffer buff = new StringBuffer();
		buff.append(event.getType());
		buff.append(' ');
		buff.append(getCount());
		buff.append(' ');
		buff.append(meta);

		return buff.toString();
	}

}
