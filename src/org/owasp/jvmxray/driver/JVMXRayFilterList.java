package org.owasp.jvmxray.driver;

import java.util.ArrayList;
import java.util.Iterator;

import org.owasp.jvmxray.driver.NullSecurityManager.Events;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;

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
	
	FilterActions filterEvents( Events type, Object ...params ) {
		
		FilterActions result = FilterActions.DENY;
		
		Iterator<JVMXRayFilterRule> i = iterator();
		while( i.hasNext() ) {
			JVMXRayFilterRule r = i.next();
			FilterActions filterresult = r.isMatch( type, params  );
			if( filterresult == FilterActions.ALLOW || filterresult == FilterActions.DENY ) {
				result = filterresult;
				break;
			} 
		}
		
		return result;
	}
}
