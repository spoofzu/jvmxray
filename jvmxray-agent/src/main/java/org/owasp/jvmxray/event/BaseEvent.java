package org.owasp.jvmxray.event;

public abstract class BaseEvent implements IEvent {

	private int pk;
	private int st;
	private long ts;
	private String it;
	private String tr;
	private Events et;
	private String tid;
	private String p1;
	private String p2;
	private String p3;
	
	BaseEvent(int pk, int state, long timestamp, String tid, Events type, String identity, String stacktrace, String p1, String p2, String p3) {
		this.pk = pk;
		this.st = state;
		this.ts = timestamp;
		this.tid = tid;
		this.et = type;
		this.it = identity;
		this.tr = stacktrace;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
	
    public int getPK() {
    	return pk;
    }

	public int getState() {
		return st;
	}
	
	public long getTimeStamp() {
		return ts;
	}
	
	public String getThreadId() {
		return tid;
	}
	
	public Events getEventType() {
		return et;
	}

    public String getIdentity() {
    	return it;
    }

    public String getStackTrace() {
    	return tr;
    }
    
    public void setStackTrace( String st) {
    	tr = st;
    }
    
    public String[] getParams() {
    	return new String[] {p1, p2, p3};
    }
    
    public String toString() {
    	
    	StringBuffer buff = new StringBuffer();
    	
    	buff.append("cn=");
    	buff.append(this.getClass().getName());
    	buff.append(",");
    	
    	buff.append("pk=");
    	buff.append(getPK());
    	buff.append(",");
    	
    	buff.append("state=");
    	buff.append(getState());
    	buff.append(",");
    	
    	buff.append("ts=");
    	buff.append(getTimeStamp());
    	buff.append(",");
    	
    	buff.append("tid=");
    	buff.append(getThreadId());
    	buff.append(",");
    	
    	buff.append("et=");
    	buff.append(getEventType());
    	buff.append(",");
    	
    	buff.append("sid=");
    	buff.append(getIdentity());
    	buff.append(",");
    	
    	buff.append("cs=");
    	buff.append(getStackTrace());
    	buff.append(",");
    	
    	String[] p = getParams();
    	
    	buff.append("p1=");
    	buff.append(p[0]);
    	buff.append(",");
    	
    	buff.append("p2=");
    	buff.append(p[1]);
    	buff.append(",");
    	
    	buff.append("p3=");
    	buff.append(p[2]);
    	
    	return buff.toString();
    }

}


