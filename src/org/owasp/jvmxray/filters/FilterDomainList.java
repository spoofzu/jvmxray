package org.owasp.jvmxray.filters;

import java.util.ArrayList;
import java.util.Iterator;

import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class FilterDomainList {

	ArrayList<FilterDomainRule> list = new ArrayList<FilterDomainRule>();
	
	public void add( FilterDomainRule r ) {
		list.add( r );
	}
	
	public Iterator<FilterDomainRule> iterator() {
		return list.iterator();
	}
	
	public FilterActions filterEvents( Events event, Object ...obj ) {
		
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
