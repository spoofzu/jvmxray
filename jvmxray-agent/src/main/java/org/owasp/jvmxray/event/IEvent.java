package org.owasp.jvmxray.event;

public interface IEvent {

	public int PK_UNUSED = -1;
	public int STATE_UNUSED = -1;
	
	/**
	 * Event types supported the <code>NullSecurityManager</code>.  
	 */
	public enum Events {
		ACCESS_SECURITY,
		ACCESS_THREAD,
		ACCESS_THREADGROUP,
		CLASSLOADER_CREATE,
		EXIT,
		FACTORY,
		FILE_DELETE,
		FILE_EXECUTE,
		FILE_READ,
		FILE_READ_WITH_CONTEXT,
		FILE_READ_WITH_FILEDESCRIPTOR,
		FILE_WRITE,
		FILE_WRITE_WITH_FILEDESCRIPTOR,
		LINK,
		PACKAGE_ACCESS,
		PACKAGE_DEFINE,
		PERMISSION,
		PERMISSION_WITH_CONTEXT,
		PRINT,
		PROPERTIES_ANY,
		PROPERTIES_NAMED,
		SOCKET_ACCEPT,
		SOCKET_CONNECT,
		SOCKET_CONNECT_WITH_CONTEXT,
		SOCKET_LISTEN,
		SOCKET_MULTICAST,
		SOCKET_MULTICAST_WITH_TTL,
		MAPPED_CONTEXT
	}
	
	public int getPK();

	public int getState();
	
	public long getTimeStamp();
	
	public String getThreadId();
	
	public Events getEventType();

    public String getIdentity();

    public String getStackTrace();
    
    public void setStackTrace(String st);
    
    public String[] getParams();

	
}
