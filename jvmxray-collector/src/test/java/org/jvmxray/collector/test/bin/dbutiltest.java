package org.jvmxray.collector.test.bin;

import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.exception.JVMXRayDBException;
import org.jvmxray.agent.filters.StackDebugLevel;
import org.jvmxray.agent.util.EventUtil;
import org.jvmxray.collector.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manually run unit-test for DBUtil.
 */
public class dbutiltest {

    public dbutiltest() {}

    public void test() throws ClassNotFoundException, SQLException, JVMXRayDBException {
        DBUtil dbUtil = DBUtil.getInstance();
        EventUtil eventUtil = EventUtil.getInstance();
        Connection conn = null;
        try {
            // Retrive connection, create tables in preexisting JVMXRAY schema.
            conn = dbUtil.createConnection();
            // Create a sample event w/dummy data.
            int max = Integer.MAX_VALUE;
            int min = Integer.MIN_VALUE;;
            // Create some sample metadata to make event interesting.
            Thread t = Thread.currentThread();
            int iAid = (int)(Math.random()*(max-min+1)+min);
            // Assign variables
            String mr = "";                        // Matching rule.
            int id = -1; // N/A, not used on Insert
            int st = -1; // N/A, not currently used.
            long ts = System.currentTimeMillis();  // Timestamp
            String tid=t.getName()+"-"+t.getId();; // Thread information
            String et="FILE_READ";                 // Event fired
            String aid=Integer.toString(iAid);     // Unique Agent cloud ID
            String p1="/dir/dir/file.abc";         // Fake file name
            String p2="";                          // N/A, not used for FILE_READ
            String p3="";                          // N/A, not used for FILE_READ
            // Create sample stacktrace data
            StackTraceDAO sDAO = getStackTrace1(StackDebugLevel.FULL);
            // Create sample event
            EventDAO eDAO = new EventDAO(mr,id,st,ts,tid,et,aid,sDAO,p1,p2,p3);
            // Insert event.
            dbUtil.insertEvent(conn, eDAO);
        } finally {
            if( conn != null ) {
                conn.close();
            }
        }
    }

    // Create a few levels in the stacktraces
    private StackTraceDAO getStackTrace1(StackDebugLevel opts) {
        return getStackTrace2(opts);
    }
    // Create a few levels in the stacktraces
    private StackTraceDAO getStackTrace2(StackDebugLevel opts) {
        return getStackTrace3(opts);
    }
    // Create a few levels in the stacktraces
    private StackTraceDAO getStackTrace3(StackDebugLevel opts) {
        return getStackTrace4(opts);
    }
    // Create a few levels in the stacktraces
    private StackTraceDAO getStackTrace4(StackDebugLevel opts) {
        EventUtil eventUtil = EventUtil.getInstance();
        return eventUtil.createStackTraceDAO(opts);
    }


    // Entry point.
    //
    public static final void main(String[] argc) {
        try {
            System.out.println("dbutiltest.main(): test starting.");
            dbutiltest dbtest = new dbutiltest();
            dbtest.test();
        }catch(Throwable t) {
            System.err.println("dbutiltest.main(): test ***failed***.");
            t.printStackTrace();
            System.exit(10);
        }
        System.out.println("dbutiltest.main(): test completed.");
        //TODO: check for long running threads.  Should not need to force exit.
        System.exit(0);
    }

}
