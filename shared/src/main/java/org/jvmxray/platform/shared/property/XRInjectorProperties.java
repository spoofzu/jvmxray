package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

public class XRInjectorProperties extends XRPropertyBase implements XRIProperties {

    public XRInjectorProperties(Path jvmxrayHome) {
        super(jvmxrayHome, "injector", "injector.properties");
    }


}
