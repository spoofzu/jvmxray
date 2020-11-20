package org.owasp.jvmxray.event;

public interface IMappedEvent extends IEvent {

	public String getKey(); 
	
	public String getValue();
	
	public void setValue(String value);
	
	public long getTTL();
	
	public boolean isUpdated();
	
	
}
