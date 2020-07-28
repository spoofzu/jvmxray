package org.owasp.jvmxray.driver;

import java.util.ArrayList;
import java.util.Iterator;

import org.owasp.jvmxray.driver.NullSecurityManager.Callstack;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.event.IEvent;


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
	
	JVMXRayFilterRule getFilterRule( IEvent event ) {
		JVMXRayFilterRule result = null;
		Iterator<JVMXRayFilterRule> i = iterator();
		while( i.hasNext() ) {
			JVMXRayFilterRule r = i.next();
			FilterActions fr = r.isMatch( event );
			if( fr == FilterActions.ALLOW || fr == FilterActions.DENY ) {
				result = r;
				break;
			} 
		}
		return result;
	}
	
	FilterActions filterEvents( IEvent event ) {
		JVMXRayFilterRule fr = getFilterRule(event);
		return fr.isMatch( event );
	}
	
	Callstack getCallstackOptions( IEvent event ) {
		JVMXRayFilterRule fr = getFilterRule(event);
		return fr.getCallstackOptions();
	}
	
}
