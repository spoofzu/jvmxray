package org.jvmxray.agent.exception;

public class JVMXRayRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -875907252882576055L;

	public JVMXRayRuntimeException() {
		super();
	}

	public JVMXRayRuntimeException(String message) {
		super(message);
	}

	public JVMXRayRuntimeException(Throwable cause) {
		super(cause);
	}

	public JVMXRayRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public JVMXRayRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	
}
