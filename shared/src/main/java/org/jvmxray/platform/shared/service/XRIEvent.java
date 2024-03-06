package org.jvmxray.platform.shared.service;

public interface XRIEvent {

    enum TYPE {
        PAUSE,
        RESUME
    }

    TYPE getType();

}
