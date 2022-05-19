package org.jvmxray.task;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class NativeConsoleLoggingTask extends BaseTask {

    private Properties p;
    private Vector queue = new Vector();

    public NativeConsoleLoggingTask(Properties p) {
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
        try {
            // Thread safe shallow clone.
            Vector local = null;
            synchronized (queue) {
                local = (Vector) queue.clone();
                queue.clear();
            }
            Enumeration elements = local.elements();
            while( elements.hasMoreElements() ) {
                    String logEntry = (String)elements.nextElement();
                    System.out.println(logEntry);
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

}