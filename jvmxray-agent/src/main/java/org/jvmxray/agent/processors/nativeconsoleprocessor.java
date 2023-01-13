package org.jvmxray.agent.processors;

import org.jvmxray.agent.event.IEvent;

public class nativeconsoleprocessor extends BaseTask {

    public nativeconsoleprocessor() {
    }

    public void shutdown() {
        // N/A don't care about System.out
    }

    public void queueObject(Object obj) {
        getQueue().add(obj);
    }

    public void processObject(Object obj) {
        System.out.println(obj.toString());
    }

}
