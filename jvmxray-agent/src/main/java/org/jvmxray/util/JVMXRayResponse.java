package org.jvmxray.util;

public class JVMXRayResponse {

	private String data;
	private int responsecode;

	public JVMXRayResponse(String data, int responsecode ) {
		this.data = data;
		this.responsecode = responsecode;
	}

	public String getResponseData() {
		return data;
	}
	
	public int getResponseCode() {
		return responsecode;
	}
}
