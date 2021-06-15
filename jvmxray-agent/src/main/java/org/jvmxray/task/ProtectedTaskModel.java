package org.jvmxray.task;

import java.util.ServiceConfigurationError;

public class ProtectedTaskModel {
	
	private static ProtectedTaskModel pm;

	private ProtectedTaskModel() {}
	
	public static final ProtectedTaskModel getInstance() {
		
		if( pm == null ) {
			pm = new ProtectedTaskModel();;
		}
		
		return pm;
	}

	public boolean executeChainedTask( iProtectedTask task ) {
		//TODOMS: Similar to code in JVMXRayClient._fireEvent() and filter processing.  Investigate opportunity for code reuse.
		boolean result = false;
		iProtectedTask ptask = task;
		if (ptask!= null ) {
			try {
				int currAttempt = 1;
				int maxattempt = 5;
				int delayBeforeNextAttempt = ptask.retryDelay(currAttempt);
				while ( ptask.preProcess() ) {
					if( currAttempt < maxattempt ) {
						try {
							if ( ptask.execute() ) {
								result = true;
							} else {
								break;
							}
						} catch (ServiceConfigurationError e) {
							currAttempt++;
							delayBeforeNextAttempt = ptask.retryDelay(currAttempt);
							continue;
						} 
						if ( ptask.postProcess() ) {
							try {
								Thread.sleep(delayBeforeNextAttempt);
							} catch (InterruptedException e) {}
							Thread.yield();
							ptask = ptask.getNextNode();
							currAttempt = 1;
							if( ptask != null ) {
								continue;
							} else {
								break;
							}
						} else {
							break;
						}
					} 
				}
			} catch( Exception e ) {
				try {
					if( ptask!=null) {
						ptask.rollback(e);
					}
					e.printStackTrace();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} 
		}
		return result;
	}
	
}
