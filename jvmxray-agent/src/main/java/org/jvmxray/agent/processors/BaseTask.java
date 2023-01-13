package org.jvmxray.agent.processors;

import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.util.PropertyUtil;

import java.util.Enumeration;
import java.util.TimerTask;
import java.util.Vector;

public abstract class BaseTask extends TimerTask implements IJVMXRayProcessor  {

    private Vector queue = new Vector();
    protected PropertyUtil pu;

    public void init(PropertyUtil pu ) throws Exception {
        this.pu = pu;
    }

    public void run() {
        processQueue();
    }

    /**
     * Queue of objects to process.  Usually, IEvents or sometimes
     * Strings in the case of simple logging.
     * @return
     */
    protected Vector getQueue() {
        return queue;
    }

    /**
     * Process the event queue.  Implementations provide the
     * implementation.
     */
    protected void processQueue() {
        try {
            // Thread safe shallow clone.
            Vector local = null;
            synchronized (queue) {
                local = (Vector) queue.clone();
                queue.clear();
            }
            Enumeration elements = local.elements();
            while( elements.hasMoreElements() ) {
                Object obj = elements.nextElement();
                this.processObject(obj);
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Shutdown forces queued objects to be processed immediately
     * rather than waiting for timer to elapse.
     */
    public void shutdown() {
        processQueue();
    }

    public String toString() {
        StringBuffer buff = new StringBuffer(1000);
        buff.append(this.getClass().getName());
        buff.append("[instance=");
        buff.append(this.hashCode());
        buff.append("]");
        return buff.toString();
    }

}
