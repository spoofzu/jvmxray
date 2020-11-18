package org.owasp.jvmxray.driver;

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

	public boolean executeChainedTask( iProtectedTask task ){
		//TODOMS: Similar to code in JVMXRayClient._fireEvent() and filter processing.  Investigate opportunity for code reuse.
		boolean result = false;
		iProtectedTask t = task;
		if (t!= null ) {
			try {
				int currAttempt = 1;
				int maxattempt = 5;
				int delayBeforeNextAttempt = t.retryDelay(currAttempt);
				while ( t!=null && t.preProcess() ) {
					if( currAttempt < maxattempt ) {
						try {
							Thread.sleep(delayBeforeNextAttempt);
						} catch (InterruptedException e) {}
						Thread.yield();
						try {
							if ( t.execute() ) {
								result = true;
							} else {
								break;
							}
						} catch (ServiceConfigurationError e) {
							currAttempt++;
							delayBeforeNextAttempt = t.retryDelay(currAttempt);
							continue;
						} 
						if ( t.postProcess() ) {
							t = t.getNextNode();
							currAttempt = 1;
							continue;
						} else {
							break;
						}
					} 
				}
			} catch( Exception e ) {
				try {
					if( t!=null) {
						t.rollback(e);
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
