package org.owasp.jvmxray.sample;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.owasp.jvmxray.api.NullSecurityManager.Events;

public class JVMXRayMonitor implements IJVMXRayMonitorMBean{
	
	HashMap fileCountMap = new HashMap();
	HashMap permissionCountMap = new HashMap();

	public JVMXRayMonitor() {
	}

	@Override
	public String[] getAggFileAccess() {
		
		Iterator i = fileCountMap.keySet().iterator();
		String[] result = new String[fileCountMap.size()];
		int idx=0;
		
		while( i.hasNext() ) {
			String key = (String) i.next();
			Integer max_count = (Integer) fileCountMap.get(key);
			String row = key+"="+max_count;
			result[idx] = row;
			idx++;
		}
		
		return result;
		
	}
	
	@Override
	public String[] getAggPermissionAccess() {
		
		Iterator i = permissionCountMap.keySet().iterator();
		String[] result = new String[permissionCountMap.size()];
		int idx=0;
		
		while( i.hasNext() ) {
			String key = (String) i.next();
			Integer max_count = (Integer) permissionCountMap.get(key);
			String row = key+"="+max_count;
			result[idx] = row;
			idx++;
		}
		
		return result;
		
	}

	void setFileEvent(Events event, String FQDN) {
		String key = event+FQDN;
		if( fileCountMap.containsKey(key) ) {
			Integer i = (Integer) fileCountMap.get(key);
			fileCountMap.put(key,Integer.valueOf(i.intValue()+1));
		}else {
			fileCountMap.put(key,Integer.valueOf(1));
		}
	}
	
	void setPermissionEvent(Events event, String permissionName) {
		String key = event+permissionName;
		if( permissionCountMap.containsKey(key) ) {
			Integer i = (Integer) permissionCountMap.get(key);
			permissionCountMap.put(key,Integer.valueOf(i.intValue()+1));
		}else {
			permissionCountMap.put(key,Integer.valueOf(1));
		}
	}
	
}
