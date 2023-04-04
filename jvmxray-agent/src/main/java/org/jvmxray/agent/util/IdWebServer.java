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
    public static String supportedWebServerType(String rawCmdLine) {
        String[] cmdarray = rawCmdLine.split(" ");
        String cmd = cmdarray[0];
        cmd = cmd.trim().toLowerCase();
        String trimCmdLine = rawCmdLine.trim().toLowerCase();
        // Element zero must contain the java executable.
        if( cmd.contains("java")) {
            if( trimCmdLine.contains("catalina")) {
                System.out.println("catalina found=["+rawCmdLine+"]");
                return "catalina";
            }
            if( trimCmdLine.contains("spring-boot")) {
                // disregard maven process that started springboot.
                if(trimCmdLine.contains("-dmaven.home")) {
                    return null;
                }
                System.out.println("spring-boot found=["+rawCmdLine+"]");
                return "spring-boot";
            }
        }
        return null;
    }
}
