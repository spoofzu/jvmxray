package org.owasp.jvmxray.api;

import java.net.URL;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

abstract class JVMXRayBaseEvent implements IJVMXRayEvent {

	private Events event;
	private Object[] parameters;
	private StackTraceElement[] stacktrace;
	private Callstack callstackopt;

	public JVMXRayBaseEvent( Events event, StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters ) {
		this.event = event;

		this.callstackopt = callstackopt;
		this.stacktrace = stacktrace;
		this.parameters = parameters;
		
	}
	
	public Events getType() {
		return event;
	}
	
	public Object[] getParameters() {
		return parameters;
	}
	
	public abstract String getStringFormat();
	
	public abstract Object[] getStringArgs();
	
	public StackTraceElement[] getStackTrace() {
		return stacktrace;
	}
	
	/**
     * Generate a callstack based upon specification.
     * @param callstack Type of callstack to generate.
     */
    String generateCallStack() {
    	
    	StringBuffer buff = new StringBuffer();
		URL location = null;
    	
    	switch ( callstackopt ) {
    		case LIMITED:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				buff.append(c.getName());
	    				buff.append("->");
      				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case SOURCEPATH:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				location = c.getResource('/' + c.getName().replace('.', '/') + ".class");
	    				buff.append(location.toString());
	    				buff.append("->");
    				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case FULL:
    			for (StackTraceElement e : stacktrace ) {
    				buff.append(e.getClassName());
    				buff.append('(');
    				buff.append(e.getMethodName());
    				buff.append(':');
    				buff.append(e.getLineNumber());
    				buff.append(')');
    				buff.append("->");
    			}
    			break;		
    		case NONE:
    			buff.append("<disabled>");
    			break;

    	}

    	// Chop off trailing ->
		if (buff.length()>0 && buff.toString().endsWith("->"))
			buff.setLength(buff.length()-2);
    	
    	return buff.toString();
    }
	
	public String toString() {
		
		StringBuffer buff = new StringBuffer();
		
		buff.append(this.getType());
		buff.append(',');
		buff.append(String.format(getStringFormat(),getStringArgs()));
		buff.append(',');
		buff.append("stack="+generateCallStack());
		

		return  buff.toString();
	}

}
