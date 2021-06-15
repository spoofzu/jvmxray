package org.jvmxray.filters;

import java.util.EnumSet;
import java.util.Properties;

import org.jvmxray.driver.Callstack;
import org.jvmxray.task.FilterActions;
import org.jvmxray.event.IEvent;
import org.jvmxray.util.EventUtil;

/**
 * The NullFilter performs no function except to enable or disable a specified
 * classes of events.  
 * @author Milton Smith
 *
 */
public class NullFilter implements IJVMXRayFilterRule {

	private FilterActions defaultfilter;
	private EnumSet<IEvent.Events> events;
	private Properties p;
	private Callstack callstackopts;
	
	public NullFilter(EnumSet<IEvent.Events> supported, FilterActions defaultfilter, Properties p, Callstack callstackopts) {
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
	public FilterActions isMatch(String event) {
		FilterActions results = FilterActions.NEUTRAL;
		String let = EventUtil.getInstance().getEventType(event);
		IEvent.Events et = IEvent.Events.valueOf(let);
		//TODOMS: Need improved validation to reduce possibility of typing/user errors in configuration file.
		// NullFilter does not include criteria.  On match(event type), we apply the default action.
		if( events.contains(et)) {
			results = defaultfilter;
		}
		return results;
	}

}
