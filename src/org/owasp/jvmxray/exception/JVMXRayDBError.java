package org.owasp.jvmxray.exception;

public class JVMXRayDBError extends JVMXRayRuntimeException {

	public JVMXRayDBError() {
		// TODO Auto-generated constructor stub
	}

	public JVMXRayDBError(String message) {
		super(message);
	}

	public JVMXRayDBError(Throwable cause) {
		super(cause);
	}

	public JVMXRayDBError(String message, Throwable cause) {
		super(message, cause);
	}

	public JVMXRayDBError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
