package org.jvmxray.platform.agent.unitest;

import org.jvmxray.platform.shared.property.XRPropertyBase;

import java.nio.file.Path;

public class XRUnitTestProperties extends XRPropertyBase {

    public XRUnitTestProperties(Path jvmxrayHome) {
        super(jvmxrayHome, "propertytest", "propertytest.file");
    }
}
