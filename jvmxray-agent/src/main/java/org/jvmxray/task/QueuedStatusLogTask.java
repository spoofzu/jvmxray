package org.jvmxray.task;

import org.jvmxray.exception.JVMXRayRuntimeException;
import org.jvmxray.util.PropertyUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class QueuedStatusLogTask extends BaseTask {

    private Properties p;
    private Vector queue = new Vector();

    // End of line separator
    private static final String EOL = System.getProperty("line.separator");

    public QueuedStatusLogTask(Properties p) {
        this.p = p;
    }

    protected void queueMessage(String message) {
        queue.add(message);
    }

    public void logMessage(String message) {
        logMessage(message,null);
    }

    public void logMessage(String message, Throwable t) {
        StringBuffer buff = new StringBuffer();
        StringWriter sw = new StringWriter();
        buff.append(wrapWithEOL(message));
        PrintWriter ps = null;
        if (t!=null) {
            ps = new PrintWriter(sw);
            t.printStackTrace(ps);
            buff.append(wrapWithEOL(sw.toString()));
        }
        queueMessage(buff.toString());
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
            String fn = p.getProperty(PropertyUtil.SYS_PROP_CLIENT_STATUS_LOG_FN, PropertyUtil.SYS_PROP_AGENT_STATUS_DEFAULT_FN);
            if (dir != null && fn != null) {
                dir = dir.trim();
                fn = fn.trim();
            } else {
                throw new JVMXRayRuntimeException("QueuedStatusLogTask.execute(): Bad configuration.  dir="+dir+" fn="+fn);
            }
            FileWriter fw = new FileWriter(dir + fn, true);
            pw = new PrintWriter(fw);
            Enumeration elements = local.elements();
            while (elements.hasMoreElements()) {
                String logEntry = (String) elements.nextElement();
                pw.printf(logEntry);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    // Wraps date/time, etc around the message to be logged.
    private String wrapPrefix( String message ) {
        String nm = Thread.currentThread().getName();
        long id = Thread.currentThread().getId();
        String threadid = nm+"-"+id;
        StringBuffer buff = new StringBuffer();
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Date now = new Date();
        String fmtDate = df.format(now);
        buff.append(fmtDate);
        buff.append(',');
        buff.append(threadid);
        buff.append(',');
        buff.append(message);
        return wrapWithEOL(buff.toString());
    }

    private String wrapWithEOL(String message) {
        message = message.trim();
        if(!message.endsWith(EOL)) {
            message+=EOL;
        }
        return message;
    }

}
