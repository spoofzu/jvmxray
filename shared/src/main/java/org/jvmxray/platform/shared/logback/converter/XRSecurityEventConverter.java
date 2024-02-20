package org.jvmxray.platform.shared.logback.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;

public class XRSecurityEventConverter extends ClassicConverter {

    StackTraceElement priorSMCaller = null;

    @Override
    public String convert(ILoggingEvent event) {
        StackTraceElement[] stackTrace = event.getCallerData();
        if (stackTrace != null && stackTrace.length > 0) {
            for (StackTraceElement element : stackTrace) {
                if (isSecurityManagerCaller(element)) {
                    // Format and return the caller data
                    return formatCallerData(element);
                }
                priorSMCaller = element;
            }
        }
        return "Caller not found";
    }

    private boolean isSecurityManagerCaller(StackTraceElement element) {
        // Implement logic to determine if this stack trace element
        // is the caller of SecurityManager
        // For example, check the class name, method name, etc.

        return element.getClassName().contains("YourSecurityManagerClassName");
    }

    private String formatCallerData(StackTraceElement element) {
        // Format the stack trace element as needed
        return element.getClassName() + "." + element.getMethodName() +
                "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
    }

}
