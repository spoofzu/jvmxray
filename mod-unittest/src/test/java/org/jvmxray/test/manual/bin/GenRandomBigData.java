package org.jvmxray.test.manual.bin;

import org.jvmxray.agent.driver.jvmxraysecuritymanager;
import org.jvmxray.test.TestPermission;
import org.jvmxray.test.statistics.StatisticsReport;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Set;

public class GenRandomBigData {

    private static final int RECORDS_MAX = 5000;
    //private static final int RECORDS_MAX = 1000000;

    String[] JLIB = {
            "ant-1.9.0.jar", "commons-cli-1.2.jar", "httpclient-4.0.1.jar", "jmeter-2.9.jar", "log4j-1.2.16.jar",
            "poi-3.11-beta2.jar", "struts-core-1.3.10.jar", "tomcat-juli-7.0.23.jar", "velocity-1.7.jar",
            "ant-1.9.1.jar", "commons-cli-1.3.1.jar", "httpclient-4.0.3.jar", "jmeter-2.9.jar", "log4j-1.2.17.jar",
            "poi-3.12-beta1.jar", "struts-core-1.3.10.1.jar", "tomcat-juli-7.0.27.jar", "velocity-1.7-dep.jar",
            "ant-1.9.2.jar", "commons-codec-1.10.jar", "httpclient-4.5.5.jar", "jmeter-2.9.jar", "log4j-1.2.9.jar",
            "poi-3.13.jar", "struts-core-1.3.8.jar", "tomcat-juli-7.0.29.jar", "velocity-1.7.jar",
            "ant-1.9.3.jar", "commons-collections-3.2.2.jar", "httpcore-4.0.1.jar", "jmeter-3.0.jar", "logback-classic-1.0.13.jar",
            "poi-3.14.jar", "struts-core-1.3.8.jar", "tomcat-util-7.0.32.jar", "velocity-tools-2.0.jar",
            "ant-1.9.4.jar", "commons-collections4-4.0.jar", "httpcore-4.4.11.jar", "jmeter-components-3.0.jar", "logback-core-1.0.13.jar",
            "poi-3.15.jar", "struts-core-1.3.9.jar", "websocket-api-9.3.6.v20151106.jar", "velocity-tools-2.0-dep.jar",
            "ant-1.9.5.jar", "commons-configuration2-2.2.jar", "httpcore-nio-4.4.11.jar", "jmeter-core-3.0.jar", "logstash-logback-encoder-4.11.jar",
            "poi-3.16.jar", "struts-core-1.3.9.jar", "websocket-client-9.3.6.v20151106.jar", "ws-commons-util-1.0.2.jar",
            "ant-1.9.6.jar", "commons-io-2.5.jar", "jackson-core-2.9.0.jar", "jmeter-functions-3.0.jar", "postgresql-42.2.5.jar",
            "poi-3.17.jar", "struts-core-1.3.9.jar", "websocket-common-9.3.6.v20151106.jar", "wsdl4j-1.6.3.jar",
            "ant-1.9.7.jar", "commons-lang3-3.7.jar", "jackson-databind-2.9.0.jar" };

    String[] WLIB = {
            "advapi32.dll", "api-ms-win-core-console-l1-1-0.dll", "api-ms-win-core-datetime-l1-1-0.dll", "api-ms-win-core-debug-l1-1-0.dll", "api-ms-win-core-errorhandling-l1-1-0.dll",
            "api-ms-win-core-file-l1-1-0.dll", "api-ms-win-core-file-l2-1-0.dll", "api-ms-win-core-file-l2-1-1.dll", "api-ms-win-core-handle-l1-1-0.dll", "api-ms-win-core-heap-l1-1-0.dll",
            "api-ms-win-core-interlocked-l1-1-0.dll", "api-ms-win-core-libraryloader-l1-1-0.dll", "api-ms-win-core-localization-l1-2-0.dll", "api-ms-win-core-memory-l1-1-0.dll", "api-ms-win-core-namedpipe-l1-1-0.dll",
            "api-ms-win-core-processenvironment-l1-1-0.dll", "api-ms-win-core-processthreads-l1-1-0.dll", "api-ms-win-core-processthreads-l1-1-1.dll", "api-ms-win-core-profile-l1-1-0.dll", "api-ms-win-core-rtlsupport-l1-1-0.dll",
            "api-ms-win-core-string-l1-1-0.dll", "api-ms-win-core-synch-l1-1-0.dll", "api-ms-win-core-synch-l1-2-0.dll", "api-ms-win-core-sysinfo-l1-1-0.dll", "api-ms-win-core-timezone-l1-1-0.dll",
            "api-ms-win-core-util-l1-1-0.dll", "api-ms-win-crt-conio-l1-1-0.dll", "api-ms-win-crt-convert-l1-1-0.dll", "api-ms-win-crt-environment-l1-1-0.dll", "api-ms-win-crt-filesystem-l1-1-0.dll",
            "api-ms-win-crt-heap-l1-1-0.dll", "api-ms-win-crt-locale-l1-1-0.dll", "api-ms-win-crt-math-l1-1-0.dll", "api-ms-win-crt-multibyte-l1-1-0.dll", "api-ms-win-crt-private-l1-1-0.dll",
            "api-ms-win-crt-process-l1-1-0.dll", "api-ms-win-crt-runtime-l1-1-0.dll", "api-ms-win-crt-stdio-l1-1-0.dll", "api-ms-win-crt-string-l1-1-0.dll", "api-ms-win-crt-time-l1-1-0.dll",
            "api-ms-win-crt-utility-l1-1-0.dll", "bcrypt.dll", "comdlg32.dll", "crypt32.dll", "gdi32.dll",
            "kernel32.dll", "mpr.dll", "msasn1.dll", "msvcrt.dll", "ntdll.dll",
            "ole32.dll", "oleaut32.dll", "rpcrt4.dll", "secur32.dll", "shell32.dll",
            "shlwapi.dll", "sspicli.dll", "user32.dll", "userenv.dll", "winhttp.dll",
            "wininet.dll"};

    String[] DIRECTORIES = {
            "C:\\Windows", "C:\\Program Files", "C:\\Users", "C:\\ProgramData", "C:\\Documents and Settings",
            "C:\\Program Files (x86)", "C:\\Windows\\System32", "C:\\Windows\\SysWOW64", "C:\\Windows\\Temp", "C:\\Windows\\Microsoft.NET",
            "C:\\Python27", "C:\\Python27\\Scripts", "/usr/local", "/usr/bin", "/usr/share",
            "/usr/lib", "/usr/include", "/usr/src", "/var/log", "/var/tmp",
            "/bin", "/sbin", "/etc", "/usr/sbin", "/opt",
            "/home", "/root", "/var", "/tmp", "/lib",
            "/media", "/mnt", "/boot", "/sys", "/proc",
            "/usr/local/bin", "/usr/local/sbin", "/usr/local/lib", "/usr/local/include", "/usr/local/src",
            "/usr/bin/X11", "/usr/bin/X11R6", "/usr/games", "/usr/lib/X11", "/usr/lib/X11R6",
            "/usr/share/X11", "/usr/share/fonts", "/usr/share/locale", "/usr/share/man", "/usr/share/misc",
            "/usr/share/zoneinfo", "/usr/src/linux", "/var/cache", "/var/games", "/var/lib",
            "/var/local", "/var/lock", "/var/mail", "/var/opt", "/var/spool",
            "/var/www", "/dev", "/proc/bus", "/proc/fs", "/proc/irq",
            "/proc/sys", "/proc/net", "/run", "/sys/block", "/sys/class",
            "/sys/devices", "/sys/fs", "/sys/kernel", "/sys/module", "/sys/power",
            "/sys/bus", "/sys/debug", "/sys/firmware", "/sys/hypervisor", "/sys/kernel/debug",
            "/sys/kernel/security", "/sys/virtual", "/media/cdrom", "/media/floppy", "/mnt/cdrom",
            "/mnt/floppy", "/boot/grub", "/etc/alternatives", "/etc/cron.d", "/etc/cron.daily",
            "/etc/cron.hourly", "/etc/cron.monthly", "/etc/cron.weekly", "/etc/init.d", "/etc/modprobe.d",
            "/etc/modules", "/etc/pam.d", "/etc/rc.d", "/etc/selinux", "/etc/skel",
            "/etc/sysconfig", "/etc/yum.repos.d", "/root/.ssh", "/var/cache/ldconfig", "/var/lib/dhcp",
            "/var/lib/misc", "/var/lib/ntp", "/var/lib/rpm", "/var/lib/systemd", "/var/lock/subsys",
            "/var/log/audit", "/var/log/httpd", "/var/log/mail", "/var/log/samba", "/var/log/wtmp",
            "/var/spool/cron", "/var/spool/mail", "/var/spool/mqueue"
    };

    String[] FQDNS = {
            "www.orange-elephant.net", "www.water-pizza.org", "www.green-umbrella.biz", "www.cherry-pizza.biz", "www.banana-zebra.info",
            "www.lion-tree.com", "www.moon-banana.io", "www.red-moon.com", "www.zebra-moon.biz", "www.sun-tree.com",
            "www.neptune-zebra.org", "www.ice-water.biz", "www.yellow-zebra.com", "www.cherry-umbrella.tv", "www.tree-jupiter.biz",
            "www.elephant-sun.com", "www.apple-elephant.info", "www.jupiter-lion.io", "www.red-elephant.com", "www.sun-lion.com",
            "www.orange-lion.biz", "www.banana-orange.tv", "www.green-lion.com", "www.zebra-jupiter.net", "www.water-orange.info",
            "www.moon-pizza.com", "www.sun-apple.tv", "www.red-pizza.org", "www.lion-pizza.biz", "www.neptune-banana.net",
            "www.ice-elephant.com", "www.cherry-jupiter.info", "www.yellow-elephant.biz", "www.tree-zebra.com", "www.banana-cherry.net",
            "www.apple-water.tv", "www.jupiter-elephant.org", "www.red-jupiter.com", "www.sun-tree.biz", "www.orange-water.io",
            "www.lion-sun.com", "www.moon-water.biz", "www.elephant-moon.net", "www.pizza-jupiter.com", "www.neptune-orange.org",
            "www.umbrella-elephant.com", "www.zebra-tree.info", "www.water-banana.com", "www.green-banana.tv", "www.cherry-elephant.biz",
            "www.apple-moon.com", "www.jupiter-zebra.net", "www.red-zebra.com", "www.sun-cherry.biz", "www.orange-pizza.info",
            "www.lion-orange.io", "www.moon-cherry.net", "www.elephant-sun.com", "www.pizza-water.com", "www.neptune-lion.tv",
            "www.umbrella-jupiter.info", "www.zebra-banana.com", "www.water-elephant.net", "www.green-moon.com", "www.cherry-tree.biz",
            "www.apple-orange.org", "www.jupiter-tree.com", "www.red-banana.tv", "www.sun-zebra.net", "www.orange-jupiter.com",
            "www.lion-pizza.io", "www.moon-zebra.org", "www.elephant-water.com", "www.pizza-orange.biz", "www.neptune-sun.net",
            "www.umbrella-moon.com", "www.zebra-orange.tv", "www.water-cherry.org", "www.green-pizza.com", "www.cherry-sun.biz",
            "www.apple-banana.net", "www.jupiter-pizza.org", "www.red-tree.com", "www.sun-water.net", "www.orange-banana.io",
            "www.lion-apple.info", "www.moon-orange.net", "www.elephant-jupiter.biz", "www.pizza-lion.com", "www.neptune-moon.tv",
            "www.umbrella-banana.org" };

    private final StatisticsReport report = new StatisticsReport();
    private static final jvmxraysecuritymanager SM = new jvmxraysecuritymanager();

    public void process() throws Exception {
        long count = 1;
        while(count<=RECORDS_MAX) {
            int r = rndRange(0,25);
            report.addRnd(r);
            execSecurityManagerFeature(r);
            count++;
        }
        String result = report.finish();
        System.out.println();
        System.out.println(result);
    }

    // Java application entry point.
    public static final void main(String[] argc) {
        GenRandomBigData instance = new GenRandomBigData();
        try {
            instance.process();
        }catch(Throwable t) {
            System.err.println("GenRandomBigData: general exception.");
            t.printStackTrace();
        }
    }

    public void execSecurityManagerFeature(int idx) throws IOException {
            switch(idx) {
                case 0:
                    report.add("checkPermission0()");
                    checkPermission0();
                    break;
                case 1:
                    report.add("checkPermission1()");
                    checkPermission1();
                    break;
                case 2:
                    report.add("checkCreateClassLoader0()");
                    checkCreateClassLoader0();
                    break;
                case 3:
                    report.add("checkAccess0()");
                    checkAccess0();
                    break;
                case 4:
                    report.add("checkAccess1()");
                    checkAccess1();
                    break;
                case 5:
                    report.add("checkExit0()");
                    checkExit0();
                    break;
                case 6:
                    report.add("checkExec0()");
                    checkExec0();
                    break;
                case 7:
                    report.add("checkLink0()");
                    checkLink0();
                    break;
                case 8:
                    report.add("checkRead0()");
                    checkRead0();
                    break;
                case 9:
                    report.add("checkRead1()");
                    checkRead1();
                    break;
                case 10:
                    report.add("checkRead3()");
                    checkRead3();
                    break;
                case 11:
                    report.add("checkWrite0()");
                    checkWrite0();
                    break;
                case 12:
                    report.add("checkWrite1()");
                    checkWrite1();
                    break;
                case 13:
                    report.add("checkDelete()");
                    checkDelete();
                    break;
                case 14:
                    report.add("checkConnect0()");
                    checkConnect0();
                    break;
                case 15:
                    report.add("checkConnect1()");
                    checkConnect1();
                    break;
                case 16:
                    report.add("checkListen()");
                    checkListen();
                    break;
                case 17:
                    report.add("checkAccept()");
                    checkAccept();
                    break;
                case 18:
                    report.add("checkMulticast0()");
                    checkMulticast0();
                    break;
                case 19:
                    report.add("checkMulticast1()");
                    checkMulticast1();
                    break;
                case 20:
                    report.add("checkPropertiesAccess()");
                    checkPropertiesAccess();
                    break;
                case 21:
                    report.add("checkPropertyAccess()");
                    checkPropertyAccess();
                    break;
                case 22:
                    report.add("checkPrintJobAccess()");
                    checkPrintJobAccess();
                    break;
                case 23:
                    report.add("checkPackageAccess()");
                    checkPackageAccess();
                    break;
                case 24:
                    report.add("checkPackageDefinition()");
                    checkPackageDefinition();
                    break;
                case 25:
                    report.add("checkSetFactory()");
                    checkSetFactory();
                    break;
                case 26:
                    report.add("checkSecurityAccess()");
                    checkSecurityAccess();
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }
    }

    public void checkPermission0() {
        SM.checkPermission(new TestPermission("permissionCP0"));
    }

    public void checkPermission1() {
        SM.checkPermission(new TestPermission("permissionCP1") , this);
    }

    public void checkCreateClassLoader0() {
        SM.checkCreateClassLoader();
    }

    public void checkAccess0() {
        SM.checkAccess(Thread.currentThread());
    }

    public void checkAccess1() {
        SM.checkAccess(Thread.currentThread().getThreadGroup());
    }

    public void checkExit0() {
        SM.checkExit(rndRange(0,100));
    }

    public void checkExec0() {
        String dir = DIRECTORIES[rndRange(0,DIRECTORIES.length-1)];
        SM.checkExec("cd "+dir);
    }

    public void checkLink0() {
        String lib = WLIB[rndRange(0,WLIB.length-1)];
        SM.checkLink(lib);
    }

    public void checkRead0() throws IOException {
        File file = File.createTempFile("checkRead0-test-", ".tmp");
        file.deleteOnExit();
        FileOutputStream ostr = new FileOutputStream(file);
        FileDescriptor fd = ostr.getFD();
        SM.checkRead(fd);
    }

    public void checkRead1() throws IOException {
        File file = File.createTempFile("checkRead1-test-", ".tmp");
        file.deleteOnExit();
        SM.checkRead(file.getName());
    }

    public void checkRead3() throws IOException {
        File file = File.createTempFile("checkRead3-test-", ".tmp");
        file.deleteOnExit();
        SM.checkRead(file.getName(), SM.getSecurityContext());
    }

    public void checkWrite0() throws IOException {
        File file = File.createTempFile("checkWrite0-test-", ".tmp");
        file.deleteOnExit();
        FileOutputStream ostr = new FileOutputStream(file);
        FileDescriptor fd = ostr.getFD();
        SM.checkWrite(fd);
    }

    public void checkWrite1() throws IOException {
        File file = File.createTempFile("checkWrite1-test-", ".tmp");
        file.deleteOnExit();
        SM.checkWrite(file.getName());
    }

    public void checkDelete() throws IOException {
        File file = File.createTempFile("checkDelete-test-", ".tmp");
        file.deleteOnExit();
        SM.checkDelete(file.getName());
    }

    public void checkConnect0() {
        int port = rndRange(1000,3000);
        String host = FQDNS[rndRange(0,FQDNS.length-1)];
        SM.checkConnect(host, port);
    }

    public void checkConnect1() {
        int port = rndRange(1000,3000);
        String host = FQDNS[rndRange(0,FQDNS.length-1)];
        SM.checkConnect(host, port, SM.getSecurityContext());
    }

    public void checkListen() {
        int port = rndRange(1000,3000);
        SM.checkListen(port);
    }

    public void checkAccept() {
        int port = rndRange(1000,3000);
        String host = FQDNS[rndRange(0,FQDNS.length-1)];
        SM.checkAccept(host, port);
    }

    public void checkMulticast0() throws UnknownHostException {
        SM.checkMulticast(InetAddress.getLocalHost());
    }

    public void checkMulticast1() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        int r = rndRange(0,127);
        byte ttl = (byte)r; // Note: bytes are signed, -128 to 127
        SM.checkMulticast(localhost, ttl);
    }

    public void checkPropertiesAccess() {
        SM.checkPropertiesAccess();
    }

    /**
     * Walk system properties.  Select a random keyname
     * add 'sample-'+propertyname and call
     * <code>sm.checkPropertyAccess()</code>.
     */
    private void checkPropertyAccess() {
        Properties p = System.getProperties();
        int idx = rndRange(1,p.size());
        int count=1;
        final Set<String> keys = p.stringPropertyNames();
        for( final String key : keys ) {
            if (count >= idx) {
                SM.checkPropertyAccess("jvmxray-" + key);
                break;
            }
            count++;
        }
    }

    public void checkPrintJobAccess() {
        SM.checkPrintJobAccess();
    }

    public void checkPackageAccess() {
        String jar = JLIB[rndRange(0,JLIB.length-1)];
        SM.checkPackageAccess(jar);
    }

    public void checkPackageDefinition() {
        String jar = JLIB[rndRange(0,JLIB.length-1)];
        SM.checkPackageDefinition(jar);
    }

    public void checkSetFactory() {
        SM.checkSetFactory();
    }

    public void checkSecurityAccess() {
        SM.checkSecurityAccess("securityaccesstarget");
    }

    private int rndRange(final int lower, final int upper) {
        SecureRandom r = new SecureRandom();
        int value = r.nextInt(upper - lower + 1) + lower;
        return value;
    }

}
