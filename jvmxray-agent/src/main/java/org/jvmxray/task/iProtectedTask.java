package org.jvmxray.task;


public interface iProtectedTask {
	
	public void setNextNode( iProtectedTask t ) ;
	
	public iProtectedTask getNextNode();
	
	public void setPreviousNode( iProtectedTask t );
	
	public iProtectedTask getPreviousNode();
	
	
	public int retryDelay( int attempt );
	
	public boolean preProcess() throws Exception;
	
	public boolean execute() throws Exception;
	
	public boolean rollback(Exception e) throws Exception;
	
	public boolean rollback() throws Exception;
	
	public boolean postProcess() throws Exception;
	
}
