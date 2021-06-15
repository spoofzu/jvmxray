package org.jvmxray.task;

import org.jvmxray.util.JVMXRayClient;
import org.jvmxray.exception.JVMXRayConnectionException;
import org.jvmxray.exception.JVMXRayRuntimeException;
import org.jvmxray.util.PropertyUtil;

import java.io.PrintWriter;;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class QueuedEventNetworkTask extends BaseTask {

    private Vector queue = new Vector();
    private Properties p;

    public QueuedEventNetworkTask(Properties p) {
        this.p = p;
    }

    @Override
    protected void queueMessage(String message) {
        queue.add(message);
    }

    public void sendEvent(String message) {
        queueMessage(message);
    }

    @Override
    public void execute() {
        PrintWriter pw = null;
        try {
            // Thread safe shallow clone.
            Vector local = null;
            synchronized (queue) {
                local = (Vector) queue.clone();
                queue.clear();
            }
            String wh = p.getProperty(PropertyUtil.CONF_PROP_WEBHOOK_EVENT_END_POINT);
            if( wh!=null ) {
                wh = wh.trim();
            } else {
                throw new JVMXRayRuntimeException("QueuedEventNetworkTask.execute(): Bad configuration.  wh="+wh);
            }
            URL webhookurl = null;
            try {
                webhookurl = new URL(wh);
            }catch(MalformedURLException e) {
                throw new JVMXRayRuntimeException("QueuedEventNetworkTask.execute(): Malformed URL.  wh="+wh);
            }
            // Iterate over the event buffer
            Enumeration elements = local.elements();
            while( elements.hasMoreElements() ) {
                String event = (String)elements.nextElement();
                // Prepare to transfer event to the server
                JVMXRayClient client = new JVMXRayClient(webhookurl) {
                    // Reassign max tries or use 5 for default.
                    protected int MAX_TRIES = 5;
                    @Override
                    public String getEvent() throws JVMXRayConnectionException {
                        return event;
                    }
                };
                // Transfer event to the server.
                client.fireEvent();
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }
}
