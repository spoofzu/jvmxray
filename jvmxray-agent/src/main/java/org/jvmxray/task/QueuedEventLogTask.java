package org.jvmxray.task;

import org.jvmxray.exception.JVMXRayRuntimeException;
import org.jvmxray.util.PropertyUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class QueuedEventLogTask extends BaseTask {

    private Properties p;
    private Vector queue = new Vector();

    public QueuedEventLogTask(Properties p) {
        this.p = p;
    }

    protected void queueMessage(String message) {
        queue.add(message);
    }

    public void logEvent(String message) {
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
            String dir = p.getProperty(PropertyUtil.SYS_PROP_CLIENT_BASE_DIR);
            String fn = p.getProperty(PropertyUtil.SYS_PROP_CLIENT_EVENT_LOG_FN, PropertyUtil.SYS_PROP_AGENT_EVENT_DEFAULT_FN);
            if( dir!=null && fn!=null ) {
                dir = dir.trim();
                fn = fn.trim();
            } else {
                throw new JVMXRayRuntimeException("QueuedEventLogTask.execute(): Bad configuration.  dir="+dir+" fn="+fn);
            }
            FileWriter fw = new FileWriter(dir+fn, true);
            pw = new PrintWriter(fw);
            Enumeration elements = local.elements();
            while( elements.hasMoreElements() ) {
                String logEntry = (String)elements.nextElement();
                pw.printf(logEntry);
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }finally{
            if( pw!=null) {
                pw.close();
            }
        }
    }

}
