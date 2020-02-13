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
		
				// Collect all properties specific to the filter.
				Properties np = new Properties();
				Enumeration<String> e = (Enumeration<String>) p.propertyNames();
				boolean startswithpropertypresent = false;
				while (e.hasMoreElements() ) {
					String key = e.nextElement();
					String value = p.getProperty(key);
					if( key.contains("startswith") ) {
						if ( v1.toString().startsWith(value) ) {
							startswithpropertypresent = true;
							results = defaultfilter; 
							break;
						}
					} else if( key.contains("endswith") ) {
						if ( v1.toString().endsWith(value) ) {
							startswithpropertypresent = true;
							results = defaultfilter; 
							break;
						}
					} else if( key.contains("matches") ) {
						if ( v1.toString().matches(value) ) {
							startswithpropertypresent = true;
							results = defaultfilter; 
							break;
						}
					}
				}
				
				if ( !startswithpropertypresent ) {
					results = (defaultfilter == FilterActions.ALLOW) ? FilterActions.DENY : FilterActions.ALLOW;
				}
			}
	
		}
			
		return results;
	}

}
