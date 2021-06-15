package org.jvmxray.task;

/**
 * Implemented by filters but generally, <br/>
 * ALLOW events captured on match.
 * NEUTRAL, events pass through to next filter.
 * DENY, events removed on match.
 */
public enum FilterActions {
    ALLOW,
    NEUTRAL,
    DENY
}
