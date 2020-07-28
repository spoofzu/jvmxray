package org.owasp.jvmxray.filters;

import java.util.EnumSet;
import java.util.Properties;

import org.owasp.jvmxray.driver.JVMXRayFilterRule;
import org.owasp.jvmxray.driver.NullSecurityManager.Callstack;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;

/**
 * The NullFilter performs no function except to enable or disable a specified
 * classes of events.  
 * @author Milton Smith
 *
 */
public class NullFilter extends JVMXRayFilterRule {

	private FilterActions defaultfilter;
	private EnumSet<IEvent.Events> events;
	private Properties p;
	private Callstack callstackopts;
	
	public NullFilter(EnumSet<IEvent.Events> supported, FilterActions defaultfilter, Properties p, Callstack callstackopts) {
		
		// defaultfilter = FilterActions.ALLOW, prints all java packages.
		// defaultfilter = FilterActions.DENY, suppresses all java packages.
		
		this.events = supported;
		this.defaultfilter = defaultfilter;
		this.p = p;
		this.callstackopts = callstackopts;
		
	}

	@Override
	public Callstack getCallstackOptions() {
		return callstackopts;
	}
	
	@Override
	public FilterActions isMatch(IEvent event) {
			
		FilterActions results = FilterActions.NEUTRAL;
		
		if( events.contains( event.getEventType() )) {
			
			results = defaultfilter;
			
		}
		
		return results;
	}

}
