package org.owasp.jvmxray.util;

public interface IEvent {

	public int getState();
	
	public long getTimeStamp();
	
	public String getEventType();

    public String getIdentity();

    public String getStackTrace();

    public String getMemo();
    
    public String toString();
	
}
