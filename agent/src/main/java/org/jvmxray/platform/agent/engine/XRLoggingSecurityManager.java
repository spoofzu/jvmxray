package org.jvmxray.platform.agent.engine;

import org.jvmxray.platform.shared.event.XREventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XRLoggingSecurityManager extends XRSecurityManagerBase {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.agent.engine.XRLoggingSecurityManager");

    public XRLoggingSecurityManager() {
        super();
    }

    @Override
    protected void shutDown() {
    }

    // logback logs using the caller thread.
    @Override
    protected void handleEvent(String loggerName, String p1, String p2, String p3 ) {
        XREventLogger.logEvent(loggerName,p1,p2,p3);
    }


}
