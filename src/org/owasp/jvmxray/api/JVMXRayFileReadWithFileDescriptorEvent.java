package org.owasp.jvmxray.api;

import java.io.FileDescriptor;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayFileReadWithFileDescriptorEvent extends JVMXRayBaseEvent {

	private static final String format = "fd=%s";

	public JVMXRayFileReadWithFileDescriptorEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.FILE_READ_WITH_FILEDESCRIPTOR, stacktrace, callstackopt, parameters);
	}

	public FileDescriptor getFileDescriptor() {
		Object[] obj = getParameters();
		FileDescriptor fd = (FileDescriptor)obj[0];
		return fd;
	}
	
	public String getStringFormat() {
		return format;
	}
	
	public Object[] getStringArgs() {
		return new Object[] {getFileDescriptor().toString()};
	}


}
