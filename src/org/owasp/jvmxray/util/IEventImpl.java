package org.owasp.jvmxray.util;

import org.owasp.jvmxray.util.IEvent;

public class IEventImpl implements IEvent {

	private int st;
	private long ts;
	private String et;
	private String tr;
	private String me;
	private String it;

	public IEventImpl(EventDAO event) {
		st = event.getState();
		ts = event.getTimeStamp();
		et = event.getEventType();
		it = event.getIdentity();
		tr = event.getStackTrace();
		me = event.getMemo();
	}
	
	public int getState() {
		return st;
	}
	
	public long getTimeStamp() {
		return ts;
	}
	
	public String getEventType() {
		return et;
	}

    public String getIdentity() {
    	return it;
    }

    public String getStackTrace() {
    	return tr;
    }

    public String getMemo() {
    	return me;
    }

    public String toString() {
    	StringBuffer buff = new StringBuffer();
    	buff.append(getTimeStamp());
    	buff.append(",");
    	buff.append(getIdentity());
    	buff.append(",");
    	buff.append(getEventType());
    	buff.append(",");
    	buff.append(getMemo());
    	buff.append(",");
    	buff.append(getStackTrace());
    	return buff.toString();
    }
    
}
