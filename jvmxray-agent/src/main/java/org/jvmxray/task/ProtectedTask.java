package org.jvmxray.task;

public class ProtectedTask implements iProtectedTask {

	private iProtectedTask nt = null;
	private iProtectedTask pt = null;
	private String description;
	
	public ProtectedTask( String description ) {
		this.description = description;
	}
	
	public void setNextNode( iProtectedTask t ) {
		 this.nt = t;
	}
	
	public iProtectedTask getNextNode() {
		return nt;
	}
	
	public void setPreviousNode( iProtectedTask t ) {
		 this.nt = t;
	}
	
	public iProtectedTask getPreviousNode() {
		return nt;
	}
	
	@Override
	public boolean execute() throws Exception {
		return false;
	}

	@Override
	public boolean rollback(Exception e) throws Exception {
		return false;
	}
	
	@Override
	public boolean rollback() throws Exception {
		return false;
	}


	@Override
	public boolean preProcess() throws Exception {
		return false;
	}

	@Override
	public boolean postProcess() throws Exception {
		return false;
	}

	@Override
	public int retryDelay(int attempt) {
		return 500*(attempt^3);
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(this.getClass().getName());
		buff.append('[');
		buff.append(this.hashCode());
		buff.append("]");
		iProtectedTask child = this.getNextNode();
		iProtectedTask parent = this.getPreviousNode();
		if (parent!=null || child!=null) {
			buff.append("-->");
		}
		if( parent != null ) {
			buff.append(parent.getClass().getName());
			buff.append("P[");
			buff.append(parent.hashCode());
			buff.append("]");
			buff.append("...");
		} 
		if( child != null ) {
			buff.append(child.getClass().getName());
			buff.append("C[");
			buff.append(child.hashCode());
			buff.append("]");
		}
		return buff.toString();
	}

}
