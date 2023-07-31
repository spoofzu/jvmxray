package org.jvmxray.agent.util;

import java.nio.charset.Charset;

public class AgentLogPair {

    private AgentLogPair() {}

    public static final String value( String key, String value ) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("=");
        builder.append(AgentLogPairEncoder.encode(value, Charset.forName("UTF-8")));
        return builder.toString();
    }
}
