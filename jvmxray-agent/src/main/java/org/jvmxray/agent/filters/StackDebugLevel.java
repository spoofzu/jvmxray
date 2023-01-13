package org.jvmxray.agent.filters;

/**
 * StackDebugLevel options for captured events. <br/>
 * NONE Do not include stacktrace, default.
 * LIMITED, Include limited information.  Class call stack, without method or line number references.
 * FULL, Include full stack trace information.
 */
public enum StackDebugLevel {
    NONE,
    SOURCEPATH,
    LIMITED,
    FULL
}
