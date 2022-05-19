package org.jvmxray.processors;

import org.jvmxray.task.NativeServerTransportTask;

import java.util.Properties;
import java.util.Timer;

public class nativeserverxportprocessor implements IJVMXRayProcessor {

    private final Properties p;
    // Buffer of status Strings.  Used for logging events.
    private NativeServerTransportTask eventlogger;

    public nativeserverxportprocessor(Properties p, Timer timer ) {
        this.p = p;
        // Initialize the network event logger.
        eventlogger = new NativeServerTransportTask(p);
        // Events are logged in a queue as Strings.  Background thread
        // pulls events from queue and logs them at regular intervals.
        timer.scheduleAtFixedRate(eventlogger, 0, 2000);
    }

    @Override
    public void processEvent(String event) {
        eventlogger.sendEvent(event);
    }

    @Override
    public void startup() {
        eventlogger.startup();
    }

    @Override
    public void shutdown() {
        eventlogger.shutdown();
    }

    @Override
    public boolean isRunning() {
        return eventlogger.isRunning();
    }
}
