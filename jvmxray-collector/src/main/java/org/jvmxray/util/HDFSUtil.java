package org.jvmxray.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread safe HDFS logging utility.
 */
public class HDFSUtil {

    private final ReentrantLock relock = new ReentrantLock();
    private static HDFSUtil hdfsutil = null;
    private PropertyUtil proputil;
    Configuration hdfsconf = null;
    FileSystem hdfs = null;
    String prophdfsurl = "";
    String propfilename = "";
    String propfanout = "";
    BufferedWriter bwOut = null;
    boolean bFileOpen = false;

    private HDFSUtil() {}

    private HDFSUtil(PropertyUtil p) {
        this.proputil = p;
    }

    public static synchronized final HDFSUtil getInstance( PropertyUtil proputil ) throws URISyntaxException, IOException {
        if( hdfsutil == null ) {
            hdfsutil = new HDFSUtil(proputil);
            Properties p = proputil.getProperties();
            hdfsutil.prophdfsurl = p.getProperty(PropertyUtil.CONF_PROP_SERVER_HDFS_SERVER);
            hdfsutil.hdfsconf = new Configuration();
            hdfsutil.hdfs = FileSystem.get( new URI(hdfsutil.prophdfsurl), hdfsutil.hdfsconf );
            hdfsutil.propfilename = p.getProperty(PropertyUtil.CONF_PROP_SERVER_HDFS_FILENAME);
            hdfsutil.propfanout = p.getProperty(PropertyUtil.CONF_PROP_SERVER_HDFS_FANOUT);
        }
        return hdfsutil;
    }

    public void openActiveFile() throws IOException {
        relock.lock(); // VM scope lock
        try {
            if( bFileOpen ) return;
            String fqn = prophdfsurl + propfilename;
            Path pfile = new Path(fqn);
            ;
            if (hdfs.exists(pfile)) {
                String sbkfile = new SimpleDateFormat(propfilename + "-backup-yyyy-MM-dd-'T'-HH-mm-S-Z").format(new Date());
                Path pbkfile = new Path(prophdfsurl + sbkfile);
                hdfs.rename(pfile, pbkfile);
            }
            OutputStream os = hdfs.create(pfile);
            bwOut = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bFileOpen = true;
            writeCSVHeader();
        }finally {
            relock.unlock();
        }
        return;
    }

    private void writeCSVHeader() throws IOException {
        bwOut.write("STATE,TIMESTAMP,THREADID,EVENTTYPE,IDENTITY,STACKTRACE,PARAM1,PARAM2,PARAM3");
    }

    public void writeEvent( String event ) throws IOException {
        StringBuilder builder = new StringBuilder();
        EventUtil eu = EventUtil.getInstance();
        builder.append(eu.getState(event));
        builder.append(',');
        builder.append(eu.getThreadId(event));
        builder.append(',');
        builder.append(eu.getEventType(event));
        builder.append(',');
        builder.append(eu.getIdentity(event));
        builder.append(',');
        builder.append(eu.getStackTrace(event));
        builder.append(',');
        builder.append(eu.getParam1(event));
        builder.append(',');
        builder.append(eu.getParam2(event));
        builder.append(',');
        builder.append(eu.getParam3(event));
        builder.append(',');
        bwOut.write(builder.toString());
    }

    public void closeActiveFile() throws IOException {
        relock.lock(); // VM scope lock
        try {
            if (relock.getHoldCount() < 2) {
                bwOut.flush();
                bwOut.close();
                bwOut=null;
                bFileOpen = false;
            }
        }finally {
            relock.unlock();
        }
    }

}
