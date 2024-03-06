package org.jvmxray.platform.shared.service;

public class XREvent implements XRIEvent {

    private TYPE type;

    public XREvent(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

}
