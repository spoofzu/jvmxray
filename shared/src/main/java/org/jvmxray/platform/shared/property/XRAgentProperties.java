package org.jvmxray.platform.shared.property;

import java.io.File;

public class XRAgentProperties extends XRPropertyBase {

    public XRAgentProperties(File jvmxrayHome) {
        super(jvmxrayHome, "agent", "agent.properties");
    }

}
