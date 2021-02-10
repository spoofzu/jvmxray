package org.owasp.jvmxray.util;

/**
 * Used for agent logging since use of 3rd party libraries, like logback, cause
 * problems since Java core may not be fully initialized prior to the first
 * SecurityManager call.  To keep agents as stable as possible use of any
 * 3rd party libraries in the agent is tightly curtailed.  For other areas
 * outside the agent, OWASP security logging is used which makes use of
 * full featured slf4j compliant loggers.  To assign an agent log level
 * the following in your unit tests by adding,<br/>
 *   System.setProperty("jvmxray.lightlogger","DEBUG");<br/>
 * To assign debug agent logging from the command line do the following,<br/>
 *   -Djvmxray.lightlogger=DEBUG<br/>
 * Valid log levels are, NONE, DEBUG, INFO.<br/>
 * NONE is the default and performs no logging.  DEBUG creates detailed
 * output suitable for development testing and remediation.  INFO is a
 * light level of logging for most important activities.
 */
public class LiteLogger {

    private static final String SYS_PROP_LITELOGGER_LEVEL="jvmxray.lightlogger";
    private static final String DEFAULT = "xx.kl3hh44sfdsf43hj2523kj8fdnn2jdjdfh";
    private static LiteLogger loggerinstance;
    private Level eLevel = Level.NONE;

    private enum Level {
        NONE,
        DEBUG,
        INFO;
    }

    private LiteLogger() {
        String rawsetting = System.getProperty(SYS_PROP_LITELOGGER_LEVEL, DEFAULT);
        if (rawsetting!=null) {
            rawsetting = rawsetting.trim();
            rawsetting.toUpperCase();
        }
        if( rawsetting == null || rawsetting.equals(DEFAULT) ) {
            this.eLevel = Level.NONE;
        } else if( rawsetting.equals("DEBUG")) {
            this.eLevel = Level.DEBUG;
        } else if( rawsetting.equals("INFO")) {
            this.eLevel = Level.INFO;
        } else {
            System.out.println("LiteLogger.LiteLogger(): Unknown log level.  Correct settings: NONE, DEBUG, INFO.  Default assigned is NONE");
            this.eLevel = Level.NONE;
        }
    }

    public final boolean isNone() {
        return this.eLevel == Level.NONE;
    }

    public final boolean isDebug() {
        return (this.eLevel == Level.DEBUG);
    }

    public final boolean isInfo() {
        return (this.eLevel == Level.DEBUG) || (this.eLevel == Level.INFO);
    }

    public static final synchronized LiteLogger getLoggerinstance() {
        if (loggerinstance ==null) {
            loggerinstance = new LiteLogger();
        }
        return loggerinstance;
    }

    public void debug(String message) {
        if (isDebug()) {
            System.out.println(message);
            System.out.flush();
        }
    }

    public void info(String message) {
        if (isInfo()) {
            System.out.println(message);
            System.out.flush();
        }
    }
}
