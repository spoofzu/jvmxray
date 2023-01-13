package org.jvmxray.agent.event;

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
	
	public int getPk();

	public int getSt();
	
	public long getTs();
	
	public String getTid();
	
	public String getEt();

    public String getAid();

    public IStackTrace getStackTrace();
    
    public void setStackTrace(IStackTrace str);
    
    public String getP1();

	public String getP2();

	public String getP3();

	public String toString();

	public String getMatchingRule();

	public boolean equals(Object obj);
}
