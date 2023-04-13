package org.jvmxray.agent.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A very simple logger for use prior to the loading an initialization
 * of a more robust logging framework like log4j2, logback, etc.
 * This is not a general use logging mechanism since it closes the
 * log after each message but it's a safe design for limited case
 * where some messages need to be logged prior to full logging
 * framework initialization.
 * @author Milton Smith
 */
public class SimpleLogging {

    private static final String DATE_FORMAT = "yyyy.MM.dd 'on' HH:mm:ss.SSS z";
    private static SimpleLogging instance = null;
    private File logfile = null;
    private boolean bConsoleLogging = false;

    private SimpleLogging() {}

    public synchronized static final SimpleLogging getInstance() {
        if( instance == null ) {
            instance = new SimpleLogging();
        }
        return instance;
    }

    public void defineLog(File debuglog, boolean isAlsoLoggingToConsole) throws IOException {
        bConsoleLogging = isAlsoLoggingToConsole;
        logfile = debuglog;
        String sdir = debuglog.getParent();
        File dir = new File(sdir);
        if( !dir.exists() ) {
            dir.mkdirs();
        }
    }

    public void defineLog(String debuglog, boolean isAlsoLoggingToConsole) throws IOException {
        if( debuglog == null || debuglog.length()<1 ) {
            throw new IOException("SimpleLogging: bad file name.  debuglog="+debuglog);
        } else {
            System.out.println("SimpleLogging: simple log defined.  debuglog="+debuglog);
        }
        defineLog(debuglog, isAlsoLoggingToConsole);
    }

    public void log(String message) {
        PrintWriter debugwriter = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logfile,true);
            debugwriter = new PrintWriter(fos);
            String fullmsg = getTimeStamp()+" "+message;
            debugwriter.println(fullmsg);
            if( bConsoleLogging ) {
                System.out.println(fullmsg);
            }
        }catch(Exception e) {
        }finally{
            if(debugwriter!=null) {
                debugwriter.flush();
                debugwriter.close();
            }
            if(fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
        }
    }

    private String getTimeStamp() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        Date date = new Date();
        String fmtDate = formatter.format(date);
        return fmtDate;
    }

}
