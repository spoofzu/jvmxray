package org.jvmxray.filters;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import org.jvmxray.driver.Callstack;
import org.jvmxray.task.FilterActions;
import org.jvmxray.event.IEvent;
import org.jvmxray.util.EventUtil;

/**
 * Filter to handle the first argument of each event type as a String.
 * Supports different matching criteria like startwith, endswith, and matches.
 * Filter implements the string search capabilities as defined within the
 * event configuration.
 * @author Milton Smith
 *
 */
public class StringFilter implements IJVMXRayFilterRule {

	private FilterActions defaultfilter;
	private EnumSet<IEvent.Events> events;
	private Properties p;
	private Properties np;
	private boolean bCritieriaPresent = false;
	private Callstack callstackopts;
	
	public StringFilter(EnumSet<IEvent.Events> supported, FilterActions defaultfilter, Properties p, Callstack callstackopts) {
		
		// defaultfilter = FilterActions.ALLOW, prints all java packages.
		// defaultfilter = FilterActions.DENY, suppresses all java packages.
		
		this.events = supported;
		this.defaultfilter = defaultfilter;
		this.p = p;
		this.callstackopts = callstackopts;
		
		// Create new properties file with only the criteria specifications.
		np = new Properties();
		Enumeration<String> keys = (Enumeration<String>) p.propertyNames();
		while (keys.hasMoreElements() ) {
			String key = keys.nextElement();
			String value = p.getProperty(key);
			if (key.contains("startswith") ||
			    key.contains("endswith") ||
				key.contains("matches")) {
				np.setProperty(key,value);
				bCritieriaPresent = true;
			}
		}
		
	}
	
	public Callstack getCallstackOptions() {
		return callstackopts;
	}

	@Override
	public FilterActions isMatch(String event) {
		
		FilterActions results = FilterActions.NEUTRAL;
		
		// Get searchable fields for the record type.
		String param1 = EventUtil.getInstance().getParam1(event);
		String param2 = EventUtil.getInstance().getParam2(event);
		String param3 = EventUtil.getInstance().getParam3(event);
		Object[] obj = {param1,param2,param3};

		String ret = EventUtil.getInstance().getEventType(event);
		IEvent.Events et = IEvent.Events.valueOf(ret);

		// Only handle specified events also ensure a parameter is
		// present, if none, then no matching work to do.
		if( events.contains(et) && obj.length > 0 ) {
			// Skip if no criteria specified.
			if (bCritieriaPresent) {
//				boolean bCriteriaMatch = false;
				Enumeration<String> keys = (Enumeration<String>) np.propertyNames();
				// Collect all properties specific to the filter.
				while (bCritieriaPresent && keys.hasMoreElements() ) {
					String key = keys.nextElement();
					String value = np.getProperty(key);
					// Break on first positive match.  Iterate over the
					// numbered criteria, which may be out of order.
					for( int idx=1; idx < 500 ; idx++ ) {
						// Check if a field to search is specified in criteria.  Assume 0 (1st field),
						// if unspecified.
						String sf = "0";
						String[] lvls = key.split("\\.");
						if( lvls.length == 4 ) {
							sf = lvls[3];
						}
						int fidx = 0;
						try {
							fidx = Integer.valueOf(sf);
						} catch (NumberFormatException e){
							String msg = "Non-numeric search field specified. key="+key+" type="+ EventUtil.getInstance().getEventType(event)+" error="+e.getMessage();
							RuntimeException e1 = new RuntimeException(msg);
							throw e1;
						}
						// Test to ensure field index is appropriate for this record type.  Max, length==3
						if( !(fidx > -1 && fidx < obj.length) ) {
							String msg = "Invalid search index for record type. Valid range is, 0-"+obj.length+". key="+key+" type="+ EventUtil.getInstance().getEventType(event);
							RuntimeException e = new RuntimeException(msg);
							throw e;
						}
						
						// Check to make sure index specified by user is searchable (String type).
						if( obj[fidx] instanceof String) {
							String search_field = (String)obj[fidx];
							if( key.contains("startswith"+idx) ) {
								if ( search_field.startsWith(value) ) {
//									bCriteriaMatch = true;
									results = defaultfilter;
									break;
								}
							} else if( key.contains("endswith"+idx) ) {
								if ( search_field.endsWith(value) ) {
//									bCriteriaMatch = true;
									results = defaultfilter;
									break;
								}
							} else if( key.contains("matches"+idx) ) {
								if ( search_field.matches(value) ) {
//									bCriteriaMatch = true;
									results = defaultfilter;
									break;
								}
							}
						}
					}
				}
			// If no criteria is defined, we assume a match, and apply the default action.
			} else  {
				results = defaultfilter;
			} 
	
		}
			
		return results;
	}

}
