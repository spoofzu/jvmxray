package org.jvmxray.platform.shared.property;

import java.nio.file.Path;

public class XRAgentProperties extends XRPropertyBase implements XRIProperties {

    public XRAgentProperties(Path jvmxrayHome) {
        super(jvmxrayHome, "agent", "agent.properties");
    }

}