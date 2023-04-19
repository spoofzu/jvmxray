package org.jvmxray.logging.contrib;

public class LogPair {

    private LogPair() {}

    public static final String value( String key, String value ) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("={");
        builder.append(value);
        builder.append('}');
        return builder.toString();
    }
}
