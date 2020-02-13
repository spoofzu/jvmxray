package org.owasp.jvmxray.filters;

import org.owasp.jvmxray.api.NullSecurityManager.Events;
import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

public abstract class FilterDomainRule {

	public FilterDomainRule() {	}
	
	public abstract FilterActions isMatch(Events event, Object ...obj);

}
