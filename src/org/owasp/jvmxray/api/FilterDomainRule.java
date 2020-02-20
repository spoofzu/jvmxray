package org.owasp.jvmxray.api;

import org.owasp.jvmxray.api.NullSecurityManager.Events;
import org.owasp.jvmxray.api.NullSecurityManager.FilterActions;

/**
 * Base class to enable or disable various event types of interest.
 * @author Milton Smith
 *
 */
public abstract class FilterDomainRule {

	public FilterDomainRule() {}
	
	/*
	 * Determine an event match.
	 * @param event Current event being processed.  Will be one of the supported
	 * types as defined by, NullSecurityManager.Events.
	 * @param obj Variable arguments list depending upon the event type.
	 *    if event is, Events.PERMISSION,          obj is,     Permission || Permission, Object
	 *    if event is, Events.CLASSLOADER_CREATE,  obj is,     ""
	 *    if event is, Events.EXIT,                obj is,     int
	 *    if event is, Events.EXEC,                obj is,     String
	 *    if event is, Events.LINK,                obj is,     String
	 *    if event is, Events.FILE_READ,           obj is,     String || String, Object
	 *    if event is, Events.FILE_WRITE,          obj is,     String
	 *    if event is, Events.FILE_DELETE,         obj is,     String
	 *    if event is, Events.SOCKET_CONNECT,      obj is,     String, int || String, int, Object
	 *    if event is, Events.SOCKET_LISTEN,       obj is,     int
	 *    if event is, Events.SOCKET_ACCEPT,       obj is,     String, int
	 *    if event is, Events.SOCKET_MULTICAST,    obj is,     InetAddress, byte(optional)
	 *    if event is, Events.PROPERTIES_ANY,      obj is,     ""
	 *    if event is, Events.PRINT,               obj is,     ""
	 *    if event is, Events.PACKAGE_ACCESS,      obj is,     String
	 *    if event is, Events.PACKAGE_DEFINE,      obj is,     String
	 *    if event is, Events.FACTORY,             obj is,     ""
	 *    if event is, Events.ACCESS,              obj is,     String || Thread || ThreadGroup
	 * A salient point is that even in the case where Java does not pass arguments
	 * NullSecurityManager always passes an empty String for the first argument.
	 * This means all events have at least 1 parameter.    
	 *   
	 */
	public abstract FilterActions isMatch(Events event, Object ...obj);

}
