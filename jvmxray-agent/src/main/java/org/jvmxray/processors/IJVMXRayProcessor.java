package org.jvmxray.processors;

public interface IJVMXRayProcessor {

    public void processEvent(String event);

    public void startup();

    public void shutdown();

    public boolean isRunning();

}
