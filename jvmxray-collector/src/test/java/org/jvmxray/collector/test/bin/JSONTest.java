package org.jvmxray.collector.test.bin;

import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.filters.StackDebugLevel;
import org.jvmxray.agent.util.EventUtil;
import org.jvmxray.agent.util.JSONUtil;

public class JSONTest {

    public void test() throws Exception {
        // Create a sample event w/dummy data.
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE;
        // Create some sample metadata to make event interesting.
        Thread t = Thread.currentThread();
        int iAid = (int) (Math.random() * (max - min + 1) + min);
        // Assign variables
        String mr = "";   // Matching rule.
        int id = -1; // N/A, not used on Insert
        int st = -1; // N/A, not currently used.
        long ts = System.currentTimeMillis();  // Timestamp
        String tid = t.getName() + "-" + t.getId();
        ; // Thread information
        String et = "FILE_READ";                 // Event fired
        String aid = Integer.toString(iAid);     // Unique Agent cloud ID
        String p1 = "/dir/dir/file.abc";         // Fake file name
        String p2 = "";                          // N/A, not used for FILE_READ
        String p3 = "";                          // N/A, not used for FILE_READ
        // Create sample stacktrace data
        EventUtil eu = EventUtil.getInstance();
        StackTraceDAO sDAO = eu.createStackTraceDAO(StackDebugLevel.FULL);
        // Create sample event
        EventDAO eDAO = new EventDAO(mr, id, st, ts, tid, et, aid, sDAO, p1, p2, p3);
        // Print raw event, no JSON encoding
        System.out.println("***RAW Event Object***");
        System.out.println(eDAO);
        JSONUtil ju = JSONUtil.getInstance();
        // Print JSON encoded/escaped event.
        String esJSON = ju.eventToJSON(eDAO);
        System.out.println("***JSON Encoded/Escaped Event Object***");
        System.out.println(esJSON);
        System.out.println("***Compare deserialized object to RAW data.***");
        IEvent decodeDAO = ju.eventFromJSON(esJSON);
        //TODO problem here in the comparison.
        boolean result = decodeDAO.equals(eDAO);
        if (result) {
            System.out.println("Equal, success");
        } else {
            System.out.println("Not equal, serialization problem.");
        }
        System.out.println(decodeDAO);
    }

    public static final void main(String[] args) {
        try {
            JSONTest inst = new JSONTest();
            inst.test();
        }catch(Throwable t) {
            t.printStackTrace();
            System.exit(10);
        }
    }
}
