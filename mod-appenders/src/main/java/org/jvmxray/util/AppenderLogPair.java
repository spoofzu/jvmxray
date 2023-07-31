package org.jvmxray.util;

import java.nio.charset.Charset;

public class AppenderLogPair {

    private AppenderLogPair() {}

    public static final String value( String key, String value ) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("=");
        builder.append(AppenderLogPairEncoder.encode(value, Charset.forName("UTF-8")));
        return builder.toString();
    }
}
