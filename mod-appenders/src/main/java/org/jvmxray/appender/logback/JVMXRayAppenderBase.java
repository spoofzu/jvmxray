package org.jvmxray.appender.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.jvmxray.util.AppenderLogPairEncoder;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Appender base class for JVMXRay.  Convience class deserializing JVMXRay event data.
 * Log messages in JVMXRay logging to the following namepace,<br/>
 * <code>org.jvmxray.agent.driver.jvmxraysecuritymanager.events</code><br/>
 * To persist JVMXRay events to various media, extend JVMXRayAppenderBase and
 * override <code>processEvent()</code> implementing your own backing stores.
 * @author Milton Smith
 */
public abstract class JVMXRayAppenderBase extends AppenderBase<ILoggingEvent> {

    // Log message version.
    private static final String VERSION_TAG = "jvmxray_format_version";
    private static final String VERSION_ID = "0.01";

    private boolean bAppenderInitialized = false;

    /**
     * User event handler for JVMXRay events.
     * @param eventid  JVMXRay event id.  Example: 0c07057d0c3b7fd6-3549b40a-189550d0635-7fe6
     * @param ts JVMXRay timestamp.  Time event was logged.  Example: 1689349064265
     * @param eventtp  JVMXRay event type.  Around 30 different event types.  Example: PACKAGE_DEFINE
     * @param loglevel Logback log level.  Log level assigned to this event.  Example: WARN
     * @param loggernamespace Logback namespace.  Logging namespace assigned to event.  Example: org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.packagedefine
     * @param threadname Java thread name.  Example: main
     * @param aid JVMXRay application id.  GUID assigned to service instance. Example: 045db2423ef7485e-40573590-18725e16a5e-8000
     * @param cat JVMXRay category id.  Categorize event data like production or test.  Example: unit-test
     * @param p1 JVMXRay parameter 1, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p2 JVMXRay parameter 2, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p3 JVMXRay parameter 3, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param event Logback ILoggingEvent, provided for convience. Unmodified log message from
     *              logback logging framework. Keep in mind, much of the ILoggingEvent
     *              metadata is not applicable since it's accurate at the point the message
     *              is logged.  In the case of JVMXRay, the event is logged in the
     *              jvmxraysecuritymanager.  Most developers are more interested in the
     *              metadata applicable to the jvmxray event metadata.
     */
    protected abstract void processEvent(String eventid, long ts,
                                String eventtp, String loglevel, String loggernamespace,
                                String threadname, String aid, String cat,
                                String p1, String p2, String p3,
                                ILoggingEvent event );

    /**
     * Event stackframe metadata where available.  Extra stackframe metadata is included when logging at
     * info and debug levels.
     * @param eventid  Eventid this event metadata is related to.
     * @param level  Stackframe level, 0 to N.
     * @param clzldr  Classloader used to load this class.
     * @param clzcn  Classname of stackframe.
     * @param clzmethnm  Method name of stackframe.
     * @param clzmodnm  Java module name of stackframe.
     * @param clzmodvr  Java module version number of stackframe.
     * @param clzfilenm  Filename belong to the class.
     * @param clzlineno  Line number when event fired.
     * @param clzlocation  Fully qualifed location where class was loaded.
     * @param clznative  Native method, true or false.
     */
    protected abstract void processEventFrame(String eventid, int level, String clzldr,
                                     String clzcn, String clzmethnm, String clzmodnm,
                                     String clzmodvr, String clzfilenm, int clzlineno,
                                     String clzlocation, String clznative );

    /**
     * Initialize appender.  Provides appender developers an opportunity
     * to initialize appenders.  Called each append() call until initializeAppender()
     * returns successfully.
     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
     */
    protected abstract void initializeAppender(ILoggingEvent event);

    private synchronized void _intializeAppender(ILoggingEvent event) {
        try {
            if(bAppenderInitialized) return;
            initializeAppender(event);
            bAppenderInitialized = true;
        } catch (Throwable t) {
            addError("Unhandled exception.  msg="+t.getMessage(), t);
            throw t;
        }
    }

    /**
     * Processes the incomming log message.  Deserialze JVMXRay event data then
     * call processEvent().
     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
     */
    @Override
    protected void append(ILoggingEvent event) {

        // Initalize appenders.
        _intializeAppender(event);

        // Check version of serialized agent event metadata.
        //TODO: consider checking version only once per agent session instead of each event in future improvements.
        Map mdc = event.getMDCPropertyMap();
        Object obj = mdc.containsKey(VERSION_TAG) ? mdc.get(VERSION_TAG) : null;
        if (obj == null) {
            addError("JVMXRay agent error: no version provided in agent MDC.  err="+VERSION_TAG+" is missing.");
            return;
        }
        String ver = (obj instanceof String ) ? (String)obj : null;
        if( ver == null ) {
            addError("JVMXRay agent error: wrong version type.  err="+obj.getClass().getName());
            return;
        }
        if( !ver.startsWith(VERSION_ID) ) {
            addError( "JVMXRay agent error: unexecpted metadata version.  ver="+ver);
            return;
        }

        String rawmsg = event.getFormattedMessage();
        Level lvl = event.getLevel();
        String loglevel = lvl.toString();
        String loggernamespace = event.getLoggerName();
        String threadname = event.getThreadName();
        long timestamp = event.getTimeStamp();

        // JVMXRayAppenderBase is only intended for use with JVMXRay log messages.
        if (!loggernamespace.startsWith("org.jvmxray.agent.driver.jvmxraysecuritymanager.events")) {
            addError("Improper configuration: JVMXRayAppender is specialized for JVMXRay formatted events only. logger namespace=" + loggernamespace + " message=" + rawmsg);
            return;
        }

        String[] fmtpairs = rawmsg.split(" ");
        int sz = fmtpairs.length;
        HashMap<String, String> map = new HashMap<String, String>();
        for (int idx = 0; idx < sz; idx++) {
            String[] keypair = fmtpairs[idx].split("=");
            int kpsz = keypair.length;
            String key = keypair[0];
            String rawval = (kpsz > 1) ? keypair[1] : "";
            String value = AppenderLogPairEncoder.decode(rawval, Charset.forName("UTF-8"));
            map.put(key, value);
        }

        String eventid = (map.containsKey("EVENTID")) ? map.get("EVENTID") : "";
        String eventtp = (map.containsKey("EVENTTP")) ? map.get("EVENTTP") : "";
        String aid = (map.containsKey("AID")) ? map.get("AID") : "";
        String cat = (map.containsKey("CATEGORY")) ? map.get("CATEGORY") : "";
        String p1 = (map.containsKey("P1")) ? map.get("P1") : "";
        String p2 = (map.containsKey("P2")) ? map.get("P2") : "";
        String p3 = (map.containsKey("P3")) ? map.get("P3") : "";

        boolean isStackFrm = rawmsg.startsWith("STACKFRM");

        processEvent(eventid, timestamp,
                     eventtp, loglevel, loggernamespace,
                     threadname, aid, cat,
                     p1, p2, p3,
                     event);

        if( isStackFrm ) {
            String frmlvl =  (map.containsKey("LVL")) ? map.get("LVL") : "";
            String clzldr =  (map.containsKey("CLZ_LDR")) ? map.get("CLZ_LDR") : "";
            String clzcn =  (map.containsKey("CLZ_CN")) ? map.get("CLZ_CN") : "";
            String clzmethnm =  (map.containsKey("CLZ_METHNM")) ? map.get("CLZ_METHNM") : "";
            String clzmodnm =  (map.containsKey("CLZ_MODNM")) ? map.get("CLZ_MODNM") : "";
            String clzmodvr =  (map.containsKey("CLZ_MODVR")) ? map.get("CLZ_MODVR") : "";
            String clzfilenm =  (map.containsKey("CLZ_FILENM")) ? map.get("CLZ_FILENM") : "";
            String clzlineno =  (map.containsKey("CLZ_LINENO")) ? map.get("CLZ_LINENO") : "";
            String clzlocation =  (map.containsKey("CLZ_LOCATION")) ? map.get("CLZ_LOCATION") : "";
            String clznative =  (map.containsKey("CLZ_NATIVE")) ? map.get("CLZ_NATIVE") : "";
            int iClzlineno =  Integer.parseInt(clzlineno);
            int iFrmlvl = Integer.parseInt(frmlvl);

            processEventFrame(eventid, iFrmlvl, clzldr, clzcn, clzmethnm, clzmodnm, clzmodvr,
                    clzfilenm, iClzlineno, clzlocation, clznative);
        }

    }

    /**
     * Internal method for developers to print raw ILoggingEvent metadata.  Call from your JVMXRayAppenderBase
     * implementation.  Note: output is visible in logs when, 1) this API called by developer, 2) logback.xml
     * is set to debug (e.g., <code>configuration debug="true"</code>
     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
     */
    protected void debug(ILoggingEvent event) {
        _debug(event);
    }

    private void _debug(ILoggingEvent event) {
        Map mdc = event.getMDCPropertyMap();
        String rawmsg = event.getFormattedMessage();
        Level lvl = event.getLevel();
        String loglevel = lvl.toString();
        String loggernamespace = event.getLoggerName();
        String threadname = event.getThreadName();
        String timestamp = Long.toString(event.getTimeStamp());

        String[] fmtpairs = rawmsg.split(" ");
        int sz = fmtpairs.length;
        StringBuilder sb = new StringBuilder();
        sb.append("event data[ ");
        sb.append("timestamp=");
        sb.append(timestamp);
        sb.append(" ");
        sb.append("loggernamepace=");
        sb.append(loggernamespace);
        sb.append(" ");
        sb.append("loglevel=");
        sb.append(loglevel);
        sb.append(" ");
        sb.append("threadname=");
        sb.append(threadname);
        sb.append(" ");
        sb.append("mdc=");
        sb.append(mdc.toString());
        sb.append(" ");
        for (int idx = 0; idx < sz; idx++) {
            String[] keypair = fmtpairs[idx].split("=");
            int kpsz = keypair.length;
            String key = keypair[0];
            String rawval = (kpsz > 1) ? keypair[1] : "";
            String value = AppenderLogPairEncoder.decode(rawval, Charset.forName("UTF-8"));
            sb.append("keypair[");
            sb.append(idx);
            sb.append("]=");
            sb.append(key);
            sb.append(":");
            sb.append(value);
            if (idx < sz - 1) {
                sb.append(" | ");
            }
        }
        sb.append(" ]");
        addError(sb.toString());
        //todo does not print event metadata yet.
    }

}
