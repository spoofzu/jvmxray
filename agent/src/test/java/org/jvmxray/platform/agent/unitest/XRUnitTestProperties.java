package org.jvmxray.platform.agent.unitest;

import org.jvmxray.platform.shared.property.XRPropertyBase;

import java.io.File;

public class XRUnitTestProperties extends XRPropertyBase {

    public XRUnitTestProperties(File jvmxrayHome) {
        super(jvmxrayHome, "propertytest", "propertytest.file");
    }
}
