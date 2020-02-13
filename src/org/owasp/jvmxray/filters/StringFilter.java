package org.owasp.jvmxray.filters;

import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import org.owasp.jvmxray.api.NullSecurityManager.Events;

/**
 * Filter to handle the first argument of each event type as a String.
 * @author Milton Smith
 *
 */
public class StringFilter extends FilterDomainRule {

	private FilterActions defaultfilter;
	private EnumSet<Events> events;
	private Properties p;
	
	public StringFilter(EnumSet<Events> events, FilterActions defaultfilter, Properties p) {
		
		// defaultfilter = FilterActions.ALLOW, prints all java packages.
		// defaultfilter = FilterActions.DENY, suppresses all java packages.
		
		this.events = events;
		this.defaultfilter = defaultfilter;
		this.p = p;
		
	}

	@Override
	public FilterActions isMatch(Events event, Object ...obj) {
		
		FilterActions results = FilterActions.NEUTRAL;
		
		// Only handle specified events also ensure a parameter is
		// present, if none, then no matching work to do.
		if( events.contains( event ) && obj.length > 0 ) {
	
			if( obj[0] instanceof String) {
				
				String v1 = (String)obj[0];
		
				// Remove non-criterial properties
				boolean bCritieriaPresent = false;
				Properties np = new Properties();
				Enumeration<String> e = (Enumeration<String>) p.propertyNames();
				while (e.hasMoreElements() ) {
					String key = e.nextElement();
					String value = p.getProperty(key);
					if (key.contains("startswith") ||
					    key.contains("endswith") ||
						key.contains("matches")) {
						np.setProperty(key,value);
						bCritieriaPresent = true;
					}
				}
				
				// Collect all properties specific to the filter.
				e = (Enumeration<String>) np.propertyNames();
				while (bCritieriaPresent && e.hasMoreElements() ) {
					String key = e.nextElement();
					String value = np.getProperty(key);
					// Break on first positive match.  Iterate over the
					// numbered criteria, which may be out of order.
					for( int idx=1; idx < 500 ; idx++ ) {
						if( key.contains("startswith"+idx) ) {
							if ( v1.toString().startsWith(value) ) {
								results = defaultfilter; 
								break;
							}
						} else if( key.contains("endswith"+idx) ) {
							if ( v1.toString().endsWith(value) ) {
								results = defaultfilter; 
								break;
							}
						} else if( key.contains("matches"+idx) ) {
							if ( v1.toString().matches(value) ) {
								results = defaultfilter; 
								break;
							}
						}
					}
				}
				
				// Handles the case where no criteria is present so we process ALLOW or DENY
				// at the event level.
				if ( !bCritieriaPresent ) {
					results = defaultfilter;
				} 
			}
	
		}
			
		return results;
	}

}
