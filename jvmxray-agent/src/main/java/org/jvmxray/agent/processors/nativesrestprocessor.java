package org.jvmxray.agent.processors;

import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.exception.JVMXRayConnectionException;
import org.jvmxray.agent.exception.JVMXRayRuntimeException;
import org.jvmxray.agent.util.JVMXRayClient;
import org.jvmxray.agent.util.PropertyUtil;

import java.net.MalformedURLException;
import java.net.URL;

public class nativesrestprocessor extends BaseTask {

    private URL webhookurl = null;

    public nativesrestprocessor() {}

    public void init( PropertyUtil pu ) throws Exception {
        super.init(pu);
        String wh = pu.getStringProperty(PropertyUtil.SYS_PROP_REST_WEBHOOK_EVENT);
        if( wh==null ) {
            throw new JVMXRayRuntimeException("Bad configuration.  wh="+wh);
        }
        try {
            webhookurl = new URL(wh);
        }catch(MalformedURLException e) {
            throw new JVMXRayRuntimeException("Malformed URL.  wh="+wh);
        }
    }

    public void shutdown() {
        super.shutdown();
    }

    public void queueObject(Object obj) {
        getQueue().add(obj);
    }

    public void processObject(Object obj) {
        try {
            JVMXRayClient client = new JVMXRayClient(webhookurl) {
                // TODO: Improvement, make number of retries a xray property setting.
                protected int MAX_TRIES = 5;
                public IEvent getEvent() throws JVMXRayConnectionException {
                    IEvent event = (IEvent)obj;
                    return event;
                }
            };
            client.fireEvent();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
