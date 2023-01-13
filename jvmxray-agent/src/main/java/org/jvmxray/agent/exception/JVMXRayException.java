package org.jvmxray.agent.exception;

public class JVMXRayException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2917074781191411062L;

	public JVMXRayException() {
		super();
	}

	public JVMXRayException(String message) {
		super(message);
	}

	public JVMXRayException(Throwable cause) {
		super(cause);
	}

	public JVMXRayException(String message, Throwable cause) {
		super(message, cause);
	}

	public JVMXRayException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
