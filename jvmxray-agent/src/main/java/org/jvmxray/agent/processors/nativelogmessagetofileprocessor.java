package org.jvmxray.agent.processors;

import org.jvmxray.agent.exception.JVMXRayRuntimeException;
import org.jvmxray.agent.util.PropertyUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

//TODO Needs some improvement to log in new dir structure.  See SimpleLocalLogger.
public class nativelogmessagetofileprocessor extends BaseTask {

    private PrintWriter pw = null;

    public nativelogmessagetofileprocessor() {}

    public void init(PropertyUtil pu) throws Exception {
        super.init(pu);
        String dir = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
        String fn = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_EVENT_LOGFILE_FN, PropertyUtil.SYS_PROP_AGENT_EVENT_LOGFILE_DEFAULT);
        if( dir==null || fn==null ) {
            throw new JVMXRayRuntimeException("Bad configuration. dir="+dir+" fn="+fn);
        }
        FileWriter fw = new FileWriter(dir+fn, true);
        pw = new PrintWriter(fw);
    }

    public void shutdown() {
        pw.close();
    }

    public void queueObject(Object obj) {
        getQueue().add(obj);
    }

    public void processObject(Object obj) {
        pw.printf(obj.toString());
    }

}
