package org.owasp.jvmxray.driver;

import java.util.ArrayList;
import java.util.Iterator;

import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;

/**
 * Used by the framework to iterate over a list of FilterDomainRules.
 * @author Milton Smith
 *
 */
class JVMXRayFilterList {

	ArrayList<JVMXRayFilterRule> list = new ArrayList<JVMXRayFilterRule>();
	
	void add( JVMXRayFilterRule r ) {
		list.add( r );
	}
	
	Iterator<JVMXRayFilterRule> iterator() {
		return list.iterator();
	}
	
	FilterActions filterEvents( IEvent event ) {
		
		FilterActions result = FilterActions.DENY;
		
		Iterator<JVMXRayFilterRule> i = iterator();
		while( i.hasNext() ) {
			JVMXRayFilterRule r = i.next();
			FilterActions filterresult = r.isMatch( event );
			if( filterresult == FilterActions.ALLOW || filterresult == FilterActions.DENY ) {
				result = filterresult;
				break;
			} 
		}
		
		return result;
	}
}
