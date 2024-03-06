package org.jvmxray.platform.shared.service;

import org.jvmxray.platform.shared.property.XRIProperties;

public interface XRIService {

    String getName();

    XRIProperties getProperties();

    void init(XRServiceProperties properties);

    void run();

    void shutDown();

    void onEvent(XRIEvent event);

    boolean equals(Object value);

}
