package org.owasp.jvmxray.filters;

import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class FileDomainRule extends FilterDomainRule {

	private FilterActions defaultfilter;
	
	public FileDomainRule(FilterActions defaultfilter) {
		
		// defaultfilter = FilterActions.ALLOW, prints specified files.
		// defaultfilter = FilterActions.DENY, suppresses specified files, prints everything else.
		
		this.defaultfilter = defaultfilter;
	}

	@Override
	public FilterActions isMatch(Events event, Object ...obj) {
		
		FilterActions results = FilterActions.NEUTRAL;
		
		if( event == Events.FILE_READ || event == Events.FILE_WRITE || event == Events.FILE_EXECUTE || event == Events.FILE_DELETE) {
//			String filename = (String) obj[0];
//			if ( filename.endsWith(".exe") || // match on exe files and bin files. 
//				 filename.endsWith(".bin")
//			    ) {
//				results = defaultfilter; 
//			}
			results = defaultfilter;
		}
		
		
		return results;
	}

}
