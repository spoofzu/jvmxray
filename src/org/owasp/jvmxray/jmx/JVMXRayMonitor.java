package org.owasp.jvmxray.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.owasp.jvmxray.api.IJVMXRayEvent;

public class JVMXRayMonitor implements JVMXRayMonitorMBean {
	
	
	ArrayList<SortableListItem> eventsAgg = new ArrayList<SortableListItem>();

	public JVMXRayMonitor() {}

	@Override
	public String[] getJVMXRayEvents() {
		
		String[] result = new String[eventsAgg.size()];
		int idx = 0;
		Iterator<SortableListItem> i = eventsAgg.iterator();
		
		while( i.hasNext() ) {
			SortableListItem item = i.next();
			result[idx]= item.toString();
			idx++;
		}

		
		return result;
		
	}
	
	public void fireEvent(IJVMXRayEvent event) {
		
		SortableListItem newitem = new SortableListItem(event,0);
		
		if( !eventsAgg.contains(newitem) ) {
			eventsAgg.add(newitem);
		// Increment count of existing event
		} else {
			SortableListItem olditem = (SortableListItem) eventsAgg.get(eventsAgg.indexOf(newitem));
			olditem.setCount(olditem.getCount()+1);
		}
		Collections.sort(eventsAgg);

	}

	
}
