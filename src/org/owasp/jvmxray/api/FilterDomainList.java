package org.owasp.jvmxray.api;

import java.util.ArrayList;
import java.util.Iterator;

import org.owasp.jvmxray.api.NullSecurityManager.Events;
import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

/**
 * Used by the framework to iterate over a list of FilterDomainRules.
 * @author Milton Smith
 *
 */
class FilterDomainList {

	ArrayList<FilterDomainRule> list = new ArrayList<FilterDomainRule>();
	
	void add( FilterDomainRule r ) {
		list.add( r );
	}
	
	Iterator<FilterDomainRule> iterator() {
		return list.iterator();
	}
	
	FilterActions filterEvents( Events event, Object ...obj ) {
		
		FilterActions result = FilterActions.DENY;
		
		Iterator<FilterDomainRule> i = iterator();
		while( i.hasNext() ) {
			FilterDomainRule r = i.next();
			FilterActions current = r.isMatch( event, obj );
			if( current == FilterActions.ALLOW || current == FilterActions.DENY ) {
				result = current;
				break;
			} 
		}
		
		return result;
	}
}
