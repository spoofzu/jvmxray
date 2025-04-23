package org.jvmxray.platform.shared.log.logback.codec;

/**
 * Utility for organizing key/pairs when logback logging.
 *
 * @author Milton Smith
 */
public class XRLogKeypair {

    private String key;
    private String value;

    public XRLogKeypair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
    public String toString() {
        return key+"="+value;
    }

}
