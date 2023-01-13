package org.jvmxray.agent.simplelogger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Very simple logger.  Log messages to console as well as
 * file in the agent's base path.  This logger does not cache
 * and is not thread safe.
 */
public class SimpleLocalLogger  {

    // Path separator
    private static final String fileSeparator = File.separator;
    private static final String EOL = System.lineSeparator();
    private static SimpleLocalLogger logger = null;
    private PrintWriter pw = null;
    private int fileidx = 0;
    private String basedir = "";
    private long records = 0;
    private long RECORDS_MAX = 10000000; //10m
    boolean bAvailable = true;

    private SimpleLocalLogger() {}

    public static final SimpleLocalLogger getInstance() {
        if( logger == null ) {
            logger = new SimpleLocalLogger();
        }
        return logger;
    }

    public void init(String basedir) throws IOException {
        // Create the directory structure if necessary.
        if( basedir == null || basedir.length()<1) {
            throw new IOException("No basedir specified.  Can't create status log.");
        }
        basedir.trim();
        this.basedir = (basedir.endsWith(fileSeparator)) ?
                     basedir+"jvmxray-agent" :
                     basedir+fileSeparator+"jvmxray-agent";
        this.basedir += fileSeparator+"logs";
        cutNewLog(this.basedir);
    }

    private void cutNewLog(String basedir) throws IOException {
        if( pw!=null) {
            pw.flush();
            pw.close();
        }
        // Create parent dirs if needed
        File fi = new File(basedir);
        if( !fi.exists() ) {
            fi.mkdirs();
        }
        String fn = getNextLogFileName(basedir);
        fi = new File(basedir,fn);
        FileWriter fw = new FileWriter(fi, false);
        pw = new PrintWriter(fw);
        logMessage("JVMXRay Agent status log opened.");
    }

    public void shutDown() {
        pw.flush();
        pw.close();
    }

    public void logMessage(String message) {
        logMessage(System.out, message);
    }

    private void logMessage(PrintStream out, String message) {
        if( message == null || message.length()<1 ) return;
        boolean bAvailable = true;
        // Logging is best attempt.  If we have troubles creating
        // log file (IOExceptions) then we do best effort.  Passing
        // through the IOException's would force callers to catch
        // exceptions on logging messages and makes code more
        // cumbersome.
        if( records >= RECORDS_MAX ) {
            try {
                if( !bAvailable ) {
                    cutNewLog(basedir);
                }
            } catch (IOException e) {
                bAvailable = false;
                e.printStackTrace();
            }
            records=0;
        }
        String msg = wrapPrefix(message);
        out.println(msg);
        if( bAvailable ) {
            pw.println(msg);
        }
        records++;
    }

    public void logMessage(String message, Throwable t) {
        if( t == null) logMessage(message);
        StringWriter sw = new StringWriter();
        PrintWriter tpw = new PrintWriter(sw);
        t.printStackTrace(tpw);
        logMessage(System.err, message);
        System.err.println(sw);
        if( bAvailable ) {
            pw.println(sw);
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
        return buff.toString();
    }

    private String wrapWithEOL(String message) {
        message = message.trim();
        if(!message.endsWith(EOL)) {
            message+=EOL;
        }
        return message;
    }

    private String getNextLogFileName(String basedir) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-d-EEE-HHmmss-z");
        Date now = new Date();
        String ts = df.format(now);
        String fn="jvmxrayagent-status-"+ts+"-"+ fileidx +".txt";
        fileidx++;
        return fileSeparator + fn;
    }

}
