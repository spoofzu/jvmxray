package org.jvmxray;

import org.jvmxray.util.EventUtil;

public class EventParseTest {

    String[] unit_test_cases = new String[] {
        "1,-1,1622675911733,main-1,PROPERTIES_ANY,8e024c669b8aa469-37e64854-175d95a6ab1-8000,,,,",
        "1,-1,1622675911733,main-1,ACCESS_THREAD,8e024c669b8aa469-37e64854-175d95a6ab1-8000,,main,,main",
        "-1,-1,1622675911734,main-1,ACCESS_THREADGROUP,8e024c669b8aa469-37e64854-175d95a6ab1-8000,,main,,main",
        "-1,-1,1622675911753,main-1,FILE_READ,8e024c669b8aa469-37e64854-175d95a6ab1-8000,,nullsecuritymanagertest2-3256419829939161348.tmp,,nullsecuritymanagertest2-3256419829939161348.tmp",
    };

    //@Test
    public void test1() {
        for (int i=0; i<unit_test_cases.length; i++ ) {
            System.out.println("Test case "+i+": ");
            System.out.println("Original --> "+unit_test_cases[i]);
            System.out.println("Parsed");
            EventUtil ep = EventUtil.getInstance();
            int pk = ep.getPK(unit_test_cases[i]);
            int st = ep.getState(unit_test_cases[i]);
            long ts = ep.getTimeStamp(unit_test_cases[i]);
            String ti = ep.getThreadId(unit_test_cases[i]);
            String et = ep.getEventType(unit_test_cases[i]);
            String id = ep.getIdentity(unit_test_cases[i]);
            String cs = ep.getStackTrace(unit_test_cases[i]);
            String p1 = ep.getParam1(unit_test_cases[i]);
            String p2 = ep.getParam1(unit_test_cases[i]);
            String p3 = ep.getParam1(unit_test_cases[i]);
            System.out.println("  pk[0]=" + pk);
            System.out.println("  st[1]=" + st);
            System.out.println("  ts[2]=" + ts);
            System.out.println("  ti[3]=" + ti);
            System.out.println("  et[4]=" + et);
            System.out.println("  id[5]=" + id);
            System.out.println("  cs[6]=" + cs);
            System.out.println("  p1[7]=" + p1);
            System.out.println("  p2[8]=" + p2);
            System.out.println("  p3[9]=" + p3);
            System.out.println("");
            System.out.println("");
        }
    }
}
