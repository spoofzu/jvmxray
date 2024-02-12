package org.jvmxray.platform.shared.property;

import java.io.File;

public class XRInjectorProperties extends XRPropertyBase {

    public XRInjectorProperties(File jvmxrayHome) {
        super(jvmxrayHome, "injector", "injector.properties");
    }


}
