package org.jvmxray.agent.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class AgentLogPairEncoder {

    private AgentLogPairEncoder() {}

    public static final String encode(String data, Charset charset) {
        if( data == null ) {
            return null;
        }
        return URLEncoder.encode(data,charset);
    }

    public static final String decode(String data, Charset charset) {
        if( data == null ) {
            return null;
        }
        return URLDecoder.decode(data, charset);
    }

}
