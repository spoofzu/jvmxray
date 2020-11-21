package org.owasp.jvmxray.filters;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import org.owasp.jvmxray.driver.JVMXRayFilterRule;
import org.owasp.jvmxray.driver.NullSecurityManager.Callstack;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.event.IEvent;

/**
 * Filter to handle the first argument of each event type as a String.
 * Supports different matching criteria like startwith, endswith, and matches.
 * @author Milton Smith
 *
 */
public class StringFilter extends JVMXRayFilterRule {

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
	public FilterActions isMatch(IEvent event) {
		
		FilterActions results = FilterActions.NEUTRAL;
		
		// Get searchable fields for the record type.
		Object[] obj = event.getParams();
		
		// Only handle specified events also ensure a parameter is
		// present, if none, then no matching work to do.
		if( events.contains( event.getEventType() ) && obj.length > 0 ) {
				
			// Skip if no criteria specified.
			if (bCritieriaPresent) {
			
				boolean bCriteriaMatch = false;
				Enumeration<String> keys = (Enumeration<String>) np.propertyNames();
		
				// Collect all properties specific to the filter.
				while (bCritieriaPresent && keys.hasMoreElements() ) {
					String key = keys.nextElement();
					String value = np.getProperty(key);
					// Break on first positive match.  Iterate over the
					// numbered criteria, which may be out of order.
					for( int idx=1; idx < 500 ; idx++ ) {
						// Property name format if field search is specified, keyname.keyname.match_criteria.field_search_criteria
						String sf = "0";
						String[] lvls = key.split(".");
						if( lvls.length == 3 ) {
	 						int keyidx = key.lastIndexOf('.');
							sf = key.substring(keyidx,key.length());
						}
						
						int fidx = 0;
						try {
							fidx = Integer.valueOf(sf);
						} catch (NumberFormatException e){
							String msg = "Non-numeric search field specified. key="+key+" type="+event.getEventType().toString()+" error="+e.getMessage();
							RuntimeException e1 = new RuntimeException(msg);
							throw e1;
						}
						// Test to ensure field index is appropriate for this record type.
						if( fidx > obj.length || fidx < 0 ) {
							String msg = "Invalid search index for record type. Valid range is, 0-"+obj.length+". key="+key+" type="+event.getEventType().toString();
							RuntimeException e = new RuntimeException(msg);
							throw e;
						}
						
						// Check to make sure index specified by user is searchable (String type).
						if( obj[fidx] instanceof String) {
							String search_field = (String)obj[fidx];
							if( key.contains("startswith"+idx) ) {
								if ( search_field.toString().startsWith(value) ) {
									bCriteriaMatch = true;
									results = defaultfilter; 
									break;
								}
							} else if( key.contains("endswith"+idx) ) {
								if ( search_field.toString().endsWith(value) ) {
									bCriteriaMatch = true;
									results = defaultfilter; 
									break;
								}
							} else if( key.contains("matches"+idx) ) {
								if ( search_field.toString().matches(value) ) {
									bCriteriaMatch = true;
									results = defaultfilter; 
									break;
								}
							}
						}
					}
				}
				// If user selects ALLOW as default we DENY.  If they select DENY then we allow.  This allows
				// criteria to either include or exclude non-matches.
				if (!bCriteriaMatch ) {
					results = ( defaultfilter == FilterActions.ALLOW ) ? FilterActions.DENY : FilterActions.ALLOW;
				}
			
			// Handles the case where no criteria is present so we process ALLOW or DENY
			// at the event level.
			} else  {
				results = defaultfilter;
			} 
	
		}
			
		return results;
	}

}
