package org.jvmxray.agent.filters;

import java.util.EnumSet;
import java.util.Properties;

import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.util.EventUtil;

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
	private StackDebugLevel callstackopts;
	private String rulename;
	
	public NullFilter(String rulename, EnumSet<IEvent.Events> supported, FilterActions defaultfilter, Properties p, StackDebugLevel callstackopts) {
		this.rulename = rulename;
		this.events = supported;
		this.defaultfilter = defaultfilter;
		this.p = p;
		this.callstackopts = callstackopts;
	}

	@Override
	public String getRuleName() {
		return rulename;
	}

	@Override
	public StackDebugLevel getCallstackOptions() {
		return callstackopts;
	}
	
	@Override
	public FilterActions isMatch(EventDAO event) {
		FilterActions results = FilterActions.NEUTRAL;
		String let = event.getEt();
		IEvent.Events et = IEvent.Events.valueOf(let);
		//TODOMS: Need improved validation to reduce possibility of typing/user errors in configuration file.
		// NullFilter does not include criteria.  On match(event type), we apply the default action.
		if( events.contains(et)) {
			results = defaultfilter;
		}
		return results;
	}

}
