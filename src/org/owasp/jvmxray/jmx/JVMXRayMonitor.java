package org.owasp.jvmxray.jmx;

import org.owasp.jvmxray.adaptors.uti.JVMXRayBaseEventAggregator;
import org.owasp.jvmxray.adaptors.uti.JVMXRaySortableListItem;

public class JVMXRayMonitor extends JVMXRayBaseEventAggregator implements JVMXRayMonitorMBean {

	public JVMXRayMonitor() {}

	@Override
	public String[] getJVMXRayEvents() {
		
		JVMXRaySortableListItem[] events = super.getEvents();
		String[] result = new String[events.length];
		int idx=0;
		for( JVMXRaySortableListItem event : events) {
			result[idx] = event.toString();
			idx++;
		}
		return result;
		
	}
	

	
}
