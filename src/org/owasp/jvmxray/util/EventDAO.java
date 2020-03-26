package org.owasp.jvmxray.util;

public class EventDAO {

	int id;
	private int st;
	private long ts;
	private String et;
	private String it;
	private String tr;
	private String me;
	
	EventDAO(int id, int state, long timestamp, String eventtype, String identity, String stacktrace, String memo) {
		this.id = id;
		this.st = state;
		this.ts = timestamp;
		this.et = eventtype;
		this.it = identity;
		this.tr = stacktrace;
		this.me = memo;
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


}


