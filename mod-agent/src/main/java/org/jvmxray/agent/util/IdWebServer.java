package org.jvmxray.agent.util;

/**
 * Helper class used to identify supported web serer types.  Most systems
 * have way to many processes running so finding web servers is like a
 * needle in haystack.  The helper finds Java processes and then
 * each supported subtype.  Keep in mind, identifying processes by
 * PID or PID file is unambigous whereas identifying web servers by this
 * method is a best effort.
 * @author Milton Smith
 */
public class IdWebServer {

    public static boolean isJavaProcess(String rawCmd) {
        String[] cmdarray = rawCmd.split(" ");
        if( cmdarray == null && cmdarray.length<2 ) {
            return false;
        }
        String cmd = cmdarray[0].trim().toLowerCase();
        return cmd.contains("java");
    }

    public static String getSupportedWebServerType(String rawCmd) {
        String trimCmdLine = rawCmd.trim().toLowerCase();
        // Element zero must contain the java executable.
        if( isJavaProcess(rawCmd) ) {
            if( trimCmdLine.contains("catalina")) {
                System.out.println("catalina found=["+rawCmd+"]");
                return "catalina";
            }
            if( trimCmdLine.contains("spring-boot")) {
                // disregard maven process that started springboot.
                if(trimCmdLine.contains("-dmaven.home")) {
                    return null;
                }
                System.out.println("spring-boot found=["+rawCmd+"]");
                return "spring-boot(experimental)";
            }
        }
        return null;
    }
}
