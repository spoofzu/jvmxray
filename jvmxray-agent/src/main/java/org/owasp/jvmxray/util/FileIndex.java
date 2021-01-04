package org.owasp.jvmxray.util;

import java.io.File;

public class FileIndex {
	File file;
	long sts;
	long ets;
	public FileIndex(File file, long start_timestamp, long end_timestamp) {
		this.file = file;
		this.sts = start_timestamp;
		this.ets = end_timestamp;
	}
	
	public boolean containsTimeStamp(long ts) {
		boolean b1 = sts <= ts;
		boolean b2 = ts <= ets;
		return b1 && b2;
	}
	public File getFile() {
		return file;
	}
}
