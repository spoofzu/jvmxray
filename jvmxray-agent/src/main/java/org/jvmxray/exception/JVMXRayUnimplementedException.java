package org.jvmxray.exception;

public class JVMXRayUnimplementedException extends JVMXRayRuntimeException {

	public JVMXRayUnimplementedException() {
		super();
	}

	public JVMXRayUnimplementedException(String message) {
		super(message);
	}

	public JVMXRayUnimplementedException(Throwable cause) {
		super(cause);
	}

	public JVMXRayUnimplementedException(String message, Throwable cause) {
		super(message, cause);
	}

	public JVMXRayUnimplementedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
