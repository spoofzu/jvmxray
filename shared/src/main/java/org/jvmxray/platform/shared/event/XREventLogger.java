package org.jvmxray.platform.shared.event;

import org.jvmxray.platform.shared.logback.codec.XRLogKeypair;
import org.jvmxray.platform.shared.logback.codec.XRLogPairCodec;
import org.jvmxray.platform.shared.property.XRAgentProperties;
import org.jvmxray.platform.shared.property.XRPropertyFactory;
import org.jvmxray.platform.shared.util.XRGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.CodeSource;

/**
 * Utility for logging JVMXRay security events as logback messages.
 *
 * @author Milton Smith
 */
public class XREventLogger {

    /** Max stackframe levels to store in dumps. */
    private static final int STACKTRACE_MAXDEPTH = 80;

    private XREventLogger() {}

    /**
     * Logs JVMXRay security event as a logback log message.  The amount of event
     * metadata included depends upon the log level settings.  TRACE includes most meta
     * like callstack at the time the security event was generated and classloader
     * info, DEBUG includes classloader meta, INFO is only event meta (p1,p2,p3).
     * @param loggerName JVMXRay security event as logback logger name.
     * @param p1 Optionally included metadata.  Depends on event type.
     * @param p2 Optionally included metadata.  Depends on event type.
     * @param p3 Optionally included metadata.  Depends on event type.
     */
    public static final void logEvent(String loggerName, String p1, String p2, String p3) {
        XRAgentProperties properties = null;
        try {
            properties = XRPropertyFactory.getAgentProperties();
        } catch(Exception e) {
            e.printStackTrace();
        }
        String aid = properties.getProperty(XREvent.APPLICATIONID);
        String cat = properties.getProperty(XREvent.CATEGORYID);
        String eid = XRGUID.getID(); // ID of the event being logged, changes w/each event.
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClzLoader = currentThread.getContextClassLoader();
        String classLoaderName = (contextClzLoader!=null) ? contextClzLoader.getName() : "unassigned";
        classLoaderName = (classLoaderName != null ) ? classLoaderName : "unassigned";
        String classLoaderClassName = (contextClzLoader!=null) ? contextClzLoader.getClass().getName() : "unassigned";
        Logger localLogger = LoggerFactory.getLogger(loggerName);
        if( localLogger.isTraceEnabled() ) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            localLogger.trace("{} {} {} {} {} {} {} {}",
                    XRLogKeypair.value(XREvent.APPLICATIONID, aid),
                    XRLogKeypair.value(XREvent.EVENTID, eid),
                    XRLogKeypair.value(XREvent.CATEGORYID, cat),
                    XRLogKeypair.value(XREvent.PARAM1, XRLogPairCodec.encode(p1,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM2, XRLogPairCodec.encode(p2,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM3, XRLogPairCodec.encode(p3,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.CLASSLOADER, classLoaderName + ":" + classLoaderClassName),
                    XRLogKeypair.value(XREvent.DUMPSTACK,formatStackTrace(stackTrace))
            );
        } else if( localLogger.isDebugEnabled() ) {
            localLogger.debug("{} {} {} {} {} {} {}",
                    XRLogKeypair.value(XREvent.APPLICATIONID, aid),
                    XRLogKeypair.value(XREvent.EVENTID, eid),
                    XRLogKeypair.value(XREvent.CATEGORYID, cat),
                    XRLogKeypair.value(XREvent.PARAM1, XRLogPairCodec.encode(p1,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM2, XRLogPairCodec.encode(p2,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM3, XRLogPairCodec.encode(p3,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.CLASSLOADER, classLoaderName + ":" + classLoaderClassName)
            );
        } else {
            localLogger.debug("{} {} {} {} {} {}",
                    XRLogKeypair.value(XREvent.APPLICATIONID, aid),
                    XRLogKeypair.value(XREvent.EVENTID, eid),
                    XRLogKeypair.value(XREvent.CATEGORYID, cat),
                    XRLogKeypair.value(XREvent.PARAM1, XRLogPairCodec.encode(p1,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM2, XRLogPairCodec.encode(p2,Charset.forName("UTF-8"))),
                    XRLogKeypair.value(XREvent.PARAM3, XRLogPairCodec.encode(p3,Charset.forName("UTF-8")))
            );
        }
    }

    /**
     * Builds a human-readable String of stacktrace element data.
     * @param stackTrace Stacktrace elements generate at time security event created.
     * @return String representation of stacktrace.
     */
    public static String formatStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace == null || stackTrace.length == 0) {
            return "unavailable";
        }
        StringBuilder sb = new StringBuilder();
        int max = Math.min(stackTrace.length, STACKTRACE_MAXDEPTH);
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                sb.append("-->");
            }
            StackTraceElement ste = stackTrace[i];
            String location = getLocation(ste.getClassName());
            String encodedFile = (ste.getFileName()!=null) ? XRLogPairCodec.encode(ste.getFileName(), Charset.forName("UTF-8")): "UnknownSrc";
            sb.append(ste.getClassName())
                    .append(".")
                    .append(ste.getMethodName())
                    .append("(")
                    .append(location)
                    .append("[")
                    .append(encodedFile)
                    .append(":")
                    .append(ste.getLineNumber())
                    .append("])");
        }
        if (stackTrace.length > STACKTRACE_MAXDEPTH) {
            sb.append("-->[Truncated").append(STACKTRACE_MAXDEPTH).append("]");
        }
        return sb.toString();
    }

    /**
     * Location of target class.
     * @param className  Target class name.
     * @return String representation of URL location for target class, if available.
     * Returns "Unknown Location" if location data unavailable.
     */
    private static String getLocation(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            CodeSource cs = clazz.getProtectionDomain().getCodeSource();
            return cs != null && cs.getLocation() != null ? cs.getLocation().toString() : "Unknown";
        } catch (ClassNotFoundException e) {
            return "Unknown";
        }
    }

}
