package org.jvmxray.agent.processors;

import org.jvmxray.agent.util.PropertyUtil;

/**
 * Interface for Agent event processors.
 */
public interface IJVMXRayProcessor  {

    public void init(PropertyUtil pu) throws Exception;

                     /**
     * Add event to the queue for later processsing.  Useful for
     * agent performance.  Agents don't need to block on slow
     * file write or socket transfers.
     * @param obj
     */
    public void queueObject(Object obj);

    /**
     * Unload event queue and process events.  Depends upon
     * implementation.  Implementations write events to files
     * or send to rest services, etc.
     * @param obj
     */
    public void processObject(Object obj);

    /**
     * Generally called when the processor is no longer needed.
     * Implemention depends upon the implementor.  For instance,
     * stream classes will usually flush buffers and close
     * streams prior to exiting.  Note: if you call shutdown
     * it's not guarnteed that you can use the processor.  It's
     * recommended you destroy the object and create a new
     * processor instance.
     */
    public void shutdown();


}
