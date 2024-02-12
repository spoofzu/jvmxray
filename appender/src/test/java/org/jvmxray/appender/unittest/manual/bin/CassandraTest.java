package org.jvmxray.appender.unittest.manual.bin;

public class CassandraTest {

//    private static jvmxraysecuritymanager sm = null;
//    private Permission perm = new TestPermission();
//    private static volatile boolean bFlagged = false;
//
//    public static void init() {
//        try {
//            // Ensure we only get executed once.
//            if(bFlagged) return;
//            System.out.println("CassandraTest: Begin initialization.");
//            sm = new jvmxraysecuritymanager();
//            bFlagged = true; // On success.
//        } catch (Exception e) {
//            System.out.println("CassandraTest: Initialization failed.");
//            e.printStackTrace();
//            System.exit(10);
//        }
//    }
//
//    public static final void main(String[] args) {
//        CassandraTest inst;
//        try {
//            inst = new CassandraTest();
//            inst.init();
//            inst.checkPermission0();
//            inst.checkPermission1();
//            inst.checkRead0();
//
//            // Wait 4-sec, time for queue events to be processed
//            int max_wait = 4000;
//            long ot = System.currentTimeMillis();
//            long nt = ot;
//            while((nt-ot)<max_wait) {
//                Thread thread = Thread.currentThread();
//                thread.yield();
//                thread.sleep(100);
//                nt = System.currentTimeMillis();
//            }
//            System.out.println("CassandraTest.main(): Finished.");
//        }catch(Throwable t){
//            System.err.println("CassandraTest.main(): ");
//            t.printStackTrace();
//        }
//    }
//
//    public void checkPermission0() {
//        sm.checkPermission(perm);
//    }
//
//    public void checkPermission1() {
//        sm.checkPermission(perm, this);
//    }
//
//    public void checkRead0() throws IOException {
//        File file = File.createTempFile("simpleagenttest0-", ".tmp");
//        file.deleteOnExit();
//        FileOutputStream ostr = new FileOutputStream(file);
//        FileDescriptor fd = ostr.getFD();
//        sm.checkRead(fd);
//    }

}
