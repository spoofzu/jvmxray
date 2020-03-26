package org.owasp.jvmxray.util;

import java.util.Properties;

public class FileUtil {
	
	private static FileUtil fu = null;
	private static Properties p = null;
	
	public static final String EOL = System.getProperty("line.separator");
	// allow a few selective special characters.
	public static final String ALLOWED_CHAR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890()=.:/ ";
	public static final char REPLACEMENT_CHAR = '@';
	
	private FileUtil() {}
	
	public static final synchronized FileUtil getInstance(Properties p) {
		if ( fu == null ) {
			fu = new FileUtil();
		}
		FileUtil.p = p;
		return fu;
	}
	
	
	/**
	 * Filter string value.  
	 * @param value String to filter.  Any character outside the set 
	 * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890()=.:/ " is replaced with
	 * an @ symbol.  Removes common reserved characters from input/output data.
	 * @return Filtered String.
	 */
	public final String getFilteredString(String value) {
		StringBuffer buff = new StringBuffer();
		if (value != null ) {
			if ( value.length() > 0 ) {
				for ( char c : value.toCharArray()) {
					int idx = ALLOWED_CHAR.indexOf(c);
					if( idx > -1 ) {
						buff.append(c);
					} else {
						buff.append(REPLACEMENT_CHAR);
					}
				}
			}
		}
		return buff.toString();
	}
	
}
