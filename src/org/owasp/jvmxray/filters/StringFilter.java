package org.owasp.jvmxray.filters;

import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import org.owasp.jvmxray.api.FilterDomainRule;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

/**
 * Filter to handle the first argument of each event type as a String.
 * Supports different matching criteria like startwith, endswith, and matches.
 * @author Milton Smith
 *
 */
public class StringFilter extends FilterDomainRule {

	private FilterActions defaultfilter;
	private EnumSet<Events> events;
	private Properties p;
	private Properties np;
	private boolean bCritieriaPresent = false;
	Enumeration<String> propertyNameEnum = null;
	
	public StringFilter(EnumSet<Events> events, FilterActions defaultfilter, Properties p) {
		
		// defaultfilter = FilterActions.ALLOW, prints all java packages.
		// defaultfilter = FilterActions.DENY, suppresses all java packages.
		
		this.events = events;
		this.defaultfilter = defaultfilter;
		this.p = p;
		
		// Remove non-criteria properties
		Properties np = new Properties();
		propertyNameEnum = (Enumeration<String>) p.propertyNames();
		while (propertyNameEnum.hasMoreElements() ) {
			String key = propertyNameEnum.nextElement();
			String value = p.getProperty(key);
			if (key.contains("startswith") ||
			    key.contains("endswith") ||
				key.contains("matches")) {
				np.setProperty(key,value);
				bCritieriaPresent = true;
			}
		}
		
	}

	@Override
	public FilterActions isMatch(Events event, Object ...obj) {
		
		FilterActions results = FilterActions.NEUTRAL;
		
		// Only handle specified events also ensure a parameter is
		// present, if none, then no matching work to do.
		if( events.contains( event ) && obj.length > 0 ) {
	
			if( obj[0] instanceof String) {
				
				String v1 = (String)obj[0];
		
				// Collect all properties specific to the filter.
				while (bCritieriaPresent && propertyNameEnum.hasMoreElements() ) {
					String key = propertyNameEnum.nextElement();
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
