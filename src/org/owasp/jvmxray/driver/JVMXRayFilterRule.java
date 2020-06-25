package org.owasp.jvmxray.driver;

import org.owasp.jvmxray.driver.NullSecurityManager.Events;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;

/**
 * Base class to enable or disable various event types of interest.
 * @author Milton Smith
 *
 */
public abstract class JVMXRayFilterRule {

	public JVMXRayFilterRule() {}
	
	public abstract FilterActions isMatch(Events type, Object ...params);

}
