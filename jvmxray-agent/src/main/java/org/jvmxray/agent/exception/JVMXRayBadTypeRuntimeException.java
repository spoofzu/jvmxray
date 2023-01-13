package org.jvmxray.agent.exception;

public class JVMXRayBadTypeRuntimeException extends JVMXRayRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1612784108931308039L;

	public JVMXRayBadTypeRuntimeException() {
		super();
	}

	public JVMXRayBadTypeRuntimeException(String message) {
		super(message);
	}

	public JVMXRayBadTypeRuntimeException(Throwable cause) {
		super(cause);
	}

	public JVMXRayBadTypeRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public JVMXRayBadTypeRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
