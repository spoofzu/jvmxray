package org.jvmxray.task;

import org.jvmxray.exception.JVMXRayRuntimeException;
import org.jvmxray.util.PropertyUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class NativeTextFileLoggingTask extends BaseTask {

    private Properties p;
    private Vector queue = new Vector();

    public NativeTextFileLoggingTask(Properties p) {
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
            String dir = p.getProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
            String fn = p.getProperty(PropertyUtil.SYS_PROP_AGENT_EVENT_LOGFILE_FN, PropertyUtil.SYS_PROP_AGENT_EVENT_LOGFILE_DEFAULT);
            if( dir!=null && fn!=null ) {
                dir = dir.trim();
                fn = fn.trim();
            } else {
                throw new JVMXRayRuntimeException("NativeTextFileLoggingTask.execute(): Bad configuration.  dir="+dir+" fn="+fn);
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
