package org.owasp.jvmxray.api;

import java.io.FileDescriptor;

import org.owasp.jvmxray.api.NullSecurityManager.Callstack;
import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayFileWriteWithFileDescriptorEvent extends JVMXRayBaseEvent {

private static final String format = "f=%s";
	
	public JVMXRayFileWriteWithFileDescriptorEvent(StackTraceElement[] stacktrace, Callstack callstackopt, Object[] parameters) {
		super(Events.FILE_WRITE_WITH_FILEDESCRIPTOR, stacktrace, callstackopt, parameters);
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
