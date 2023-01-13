package org.jvmxray.agent.exception;

public class JVMXRayServiceError extends JVMXRayException {

    public JVMXRayServiceError() {
        super();
    }

    public JVMXRayServiceError(String message) {
        super(message);
    }

    public JVMXRayServiceError(Throwable cause) {
        super(cause);
    }

    public JVMXRayServiceError(String message, Throwable cause) {
        super(message, cause);
    }

    public JVMXRayServiceError(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

