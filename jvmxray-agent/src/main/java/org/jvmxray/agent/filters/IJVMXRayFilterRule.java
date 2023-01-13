package org.jvmxray.agent.filters;

import org.jvmxray.agent.event.EventDAO;

public interface IJVMXRayFilterRule {

    public FilterActions isMatch(EventDAO event);

    public StackDebugLevel getCallstackOptions();

    public String getRuleName();

}
