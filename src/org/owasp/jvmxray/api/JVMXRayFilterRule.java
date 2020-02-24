package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Events;
import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

/**
 * Base class to enable or disable various event types of interest.
 * @author Milton Smith
 *
 */
public abstract class JVMXRayFilterRule {

	public JVMXRayFilterRule() {}
	
	public abstract FilterActions isMatch(Events type, IJVMXRayEvent event);

}
