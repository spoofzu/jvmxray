package org.owasp.jvmxray.driver;

import org.owasp.jvmxray.driver.NullSecurityManager.Callstack;
import org.owasp.jvmxray.driver.NullSecurityManager.FilterActions;
import org.owasp.jvmxray.event.IEvent;

/**
 * Base class to enable or disable various event types of interest.
 * @author Milton Smith
 *
 */
public abstract class JVMXRayFilterRule {

	public JVMXRayFilterRule() {}
	
	public abstract FilterActions isMatch(IEvent event);
	
	public abstract Callstack getCallstackOptions();

}
