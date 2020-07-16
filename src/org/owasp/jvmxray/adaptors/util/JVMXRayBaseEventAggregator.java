package org.owasp.jvmxray.adaptors.util;

import java.util.ArrayList;
import java.util.Collections;

import org.owasp.jvmxray.event.IEvent;

public class JVMXRayBaseEventAggregator {

	ArrayList<JVMXRaySortableListItem> eventsAgg = new ArrayList<JVMXRaySortableListItem>();

	public JVMXRaySortableListItem[] getEvents() {

		
		return eventsAgg.toArray(new JVMXRaySortableListItem[0]);
		
	}
	
	public void fireEvent(IEvent event) {
		
		JVMXRaySortableListItem newitem = new JVMXRaySortableListItem(event,0);
		
		if( !eventsAgg.contains(newitem) ) {
			eventsAgg.add(newitem);
		// Increment count of existing event
		} else {
			JVMXRaySortableListItem olditem = (JVMXRaySortableListItem) eventsAgg.get(eventsAgg.indexOf(newitem));
			olditem.setCount(olditem.getCount()+1);
		}
		Collections.sort(eventsAgg);

	}
	
}
