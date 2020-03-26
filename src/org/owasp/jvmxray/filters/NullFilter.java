package org.owasp.jvmxray.filters;

import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

import java.util.EnumSet;
import java.util.Properties;

import org.owasp.jvmxray.api.JVMXRayFilterRule;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

/**
 * The NullFilter performs no function except to enable or disable a specified
 * classes of events.  
 * @author Milton Smith
 *
 */
public class NullFilter extends JVMXRayFilterRule {

	private FilterActions defaultfilter;
	private EnumSet<Events> events;
	private Properties p;
	
	public NullFilter(EnumSet<Events> supported, FilterActions defaultfilter, Properties p) {
		
		// defaultfilter = FilterActions.ALLOW, prints all java packages.
		// defaultfilter = FilterActions.DENY, suppresses all java packages.
		
		this.events = supported;
		this.defaultfilter = defaultfilter;
		this.p = p;
		
	}

	@Override
	public FilterActions isMatch(Events type, Object ...params) {
			
		FilterActions results = FilterActions.NEUTRAL;
		
		if( events.contains( type )) {
			
			results = defaultfilter;
			
		}
		
		return results;
	}

}
