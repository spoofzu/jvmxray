package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

public class XRServicePlatformProperties extends XRPropertyBase implements XRIProperties {

    public XRServicePlatformProperties(Path jvmxrayHome) {
        super(jvmxrayHome, "services", "svcplatform.properties");
    }

}
