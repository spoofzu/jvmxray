package org.jvmxray.platform.shared.logback.codec;

/**
 * Utility for organizing key/pairs when logback logging.
 *
 * @author Milton Smith
 */
public class XRLogKeypair {

    private XRLogKeypair() {}

    /**
     * Create logback key/pair.
     * @param key Key name
     * @param value Value for specified key.
     * @return String representation of key/value pair suitable for logback logging.
     */
    public static final String value( String key, String value ) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("=");
       // builder.append(AgentLogPairEncoder.encode(value, Charset.forName("UTF-8")));
        builder.append(value);
        return builder.toString();
    }
}
