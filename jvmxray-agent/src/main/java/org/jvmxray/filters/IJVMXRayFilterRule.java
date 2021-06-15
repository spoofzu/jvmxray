package org.jvmxray.filters;

import org.jvmxray.driver.Callstack;
import org.jvmxray.task.FilterActions;

public interface IJVMXRayFilterRule {

    public FilterActions isMatch(String event);

    public Callstack getCallstackOptions();

}
