//package org.jvmxray.platform.shared.logback.appender.cassandra;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.AppenderBase;
//import org.jvmxray.platform.shared.event.XREvent;
//import org.jvmxray.platform.shared.logback.codec.XRLogPairCodec;
//
//import java.nio.charset.Charset;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Appender base class for JVMXRay.  Convience class deserializing JVMXRay event data.
// * Log messages in JVMXRay logging to the following namepace,<br/>
// * <code>org.jvmxray.agent.driver.jvmxraysecuritymanager.events</code><br/>
// * To persist JVMXRay events to various media, extend XRAppenderBase and
// * override <code>processEvent()</code> implementing your own backing stores.
// * @author Milton Smith
// */
//public abstract class XRAppenderBase extends AppenderBase<ILoggingEvent> {
//
//    private boolean bAppenderInitialized = false;
//
//    /**
//     * Extensible event handler for JVMXRay Agent events.  Note, event metadata depends upon
//     * event being loggged but also the log level.  For example, lowest levels like TRACE
//     * include stacktrace data, whereas DEBUG only includes classloader data, higher levels
//     * only core event metadata like eventid, timestamp, loglevel, loggernamespace, and p1-p3.
//     * @param eventId Event id.  Example: 0c07057d0c3b7fd6-3549b40a-189550d0635-7fe6
//     * @param ts Timestamp.  Time event was logged on client.  Example: 1689349064265
//     * @param logLevel Logback log level.  Namespace assigned to this event.  Example: org.jvmxray.agent.events.io.filewrite   Note,
//     *                 all events begin with the following, <code>org.jvmxray.agent.events</code>
//     * @param loggerNamespace Logback namespace.  Logging namespace assigned to event.  Example: org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.packagedefine
//     * @param threadName Java thread name.  Example: main
//     * @param aid JVMXRay application id.  GUID assigned to service instance. Example: 045db2423ef7485e-40573590-18725e16a5e-8000
//     * @param cat JVMXRay category id.  Categorize event data like production or test.  Example: unit-test
//     * @param p1 JVMXRay parameter 1, optional.  Defination of the metadata depends on event type(eventtp) value.
//     * @param p2 JVMXRay parameter 2, optional.  Defination of the metadata depends on event type(eventtp) value.
//     * @param p3 JVMXRay parameter 3, optional.  Defination of the metadata depends on event type(eventtp) value.
//     * @param dumpStack Optional Stacktrace data.  Included only on TRACE log level due to possible performance impacts.
//     * @param keyPairs Key value pairs captured as string types.  Keypairs are any user defined meta.
//     * @param event Logback ILoggingEvent, provided for convience. Unmodified log message from
//     *              logback logging framework. Keep in mind, much of the ILoggingEvent
//     *              metadata is not applicable since it's accurate at the point the message
//     *              is logged.  In the case of JVMXRay, the event is logged in the
//     *              jvmxraysecuritymanager.  Most developers are more interested in the
//     *              metadata applicable to the jvmxray event metadata.
//     */
//    protected abstract void processEvent(String eventId, long ts,
//                                String logLevel, String loggerNamespace,
//                                String threadName, String aid, String cat,
//                                String p1, String p2, String p3,
//                                String dumpStack,
//                                Map<String,String> keyPairs,
//                                ILoggingEvent event );
//
//    /**
//     * Initialize appender.  Provides appender developers an opportunity
//     * to initialize appenders.  Called each append() call until initializeAppender()
//     * returns successfully.
//     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
//     */
//    protected abstract void initializeAppender(ILoggingEvent event);
//
//    private synchronized void intializeAppender0(ILoggingEvent event) {
//        try {
//            if(bAppenderInitialized) return;
//            initializeAppender(event);
//            bAppenderInitialized = true;
//        } catch (Throwable t) {
//            addError("Unhandled exception.  msg="+t.getMessage(), t);
//            throw t;
//        }
//    }
//
//    /**
//     * Processes the incomming log message.  Deserialze JVMXRay event data then
//     * call processEvent().
//     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
//     */
//    @Override
//    protected void append(ILoggingEvent event) {
//
//        // Initalize appenders.
//        intializeAppender0(event);
//
//        // Check version of serialized agent event metadata.
//        // TODO: consider checking version only once per agent session instead of each event in future improvements.
//        // TODO: MDC not sent via SocketAppender.  Investigate or workaround by moving version to the msg field.
////        Map mdc = event.getMDCPropertyMap();
////        Object obj = mdc.containsKey(VERSION_TAG) ? mdc.get(VERSION_TAG) : null;
////        if (obj == null) {
////            addError("JVMXRay agent error: no version provided in agent MDC.  err="+VERSION_TAG+" is missing.");
////            return;
////        }
////        String ver = (obj instanceof String ) ? (String)obj : null;
////        if( ver == null ) {
////            addError("JVMXRay agent error: wrong version type.  err="+obj.getClass().getName());
////            return;
////        }
////        if( !ver.startsWith(VERSION_ID) ) {
////            addError( "JVMXRay agent error: unexecpted metadata version.  ver="+ver);
////            return;
////        }
//
//        // Gather static log message metadata and add to the map.
//        //
//        HashMap<String, String> map = new HashMap<String, String>();
//        String rawMsg = event.getFormattedMessage();
//        long timeStamp = event.getTimeStamp();
//        String threadName = event.getThreadName();
//        String loggerName = event.getLoggerName();
//        Level logLevel0 = event.getLevel();
//        String logLevel = logLevel0.toString();
//
//        // XRAppenderBase is only intended for use with JVMXRay log messages.
//        //
//        if (!loggerName.startsWith("org.jvmxray.events")) {
//            addError("Bad configuration: XRAppenderBase is specialized for JVMXRay formatted events only. loggername=" + loggerName + " msg=" + rawMsg);
//            return;
//        }
//
//        // Gather key/pair log message metadata from logbacks message.
//        // For example, AID=xxx CAT=xxx DUMPSTACK=xxx
//        // Note: Values are encoded using XRLogPairCodec which URLEncodes
//        //   data in event data contains reserved characters.
//        String[] fmtpairs = rawMsg.split(" ");
//        int sz = fmtpairs.length;
//        for (int idx = 0; idx < sz; idx++) {
//            String[] keypair = fmtpairs[idx].split("=");
//            int kpsz = keypair.length;
//            String key = keypair[0];
//            String rawval = (kpsz > 1) ? keypair[1] : "";
//            map.put(key, rawval);
//        }
//        String aid = map.get(XREvent.APPLICATIONID);
//        String eid = map.get(XREvent.EVENTID);
//        String cat = map.get(XREvent.CATEGORYID);
//        String p1 = map.get(XREvent.PARAM1);
//        String p2 = map.get(XREvent.PARAM2);
//        String p3 = map.get(XREvent.PARAM3);
//        String dumpStack = map.get(XREvent.DUMPSTACK);
//        // Remove any jvmxray types from the map, which leaves only the user created
//        // types available to downstream custom appender designers.  APPLICATIONID,
//        // EVENTID, CATEGORYID, PARAM1, PARAM2, PARAM3, DUMPSTACK are made available
//        // via processEvent() API.
//        map.remove(XREvent.APPLICATIONID);
//        map.remove(XREvent.CATEGORYID);
//        map.remove(XREvent.PARAM1);
//        map.remove(XREvent.PARAM2);
//        map.remove(XREvent.PARAM3);
//        map.remove(XREvent.DUMPSTACK);
//
//        processEvent(eid, timeStamp,
//                     logLevel, loggerName,
//                     threadName, aid, cat,
//                     p1, p2, p3,
//                     dumpStack, map, event);
//
//
//    }
//
//    /**
//     * Internal method for developers to print raw ILoggingEvent metadata.  Call from your XRAppenderBase
//     * implementation.  Note: output is visible in logs when, 1) this API called by developer, 2) logback.xml
//     * is set to debug (e.g., <code>configuration debug="true"</code>
//     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
//     */
//    protected void debug(ILoggingEvent event) {
//        debug0(event);
//    }
//
//    private void debug0(ILoggingEvent event) {
//        Map mdc = event.getMDCPropertyMap();
//        String rawmsg = event.getFormattedMessage();
//        Level lvl = event.getLevel();
//        String loglevel = lvl.toString();
//        String loggernamespace = event.getLoggerName();
//        String threadname = event.getThreadName();
//        String timestamp = Long.toString(event.getTimeStamp());
//
//        String[] fmtpairs = rawmsg.split(" ");
//        int sz = fmtpairs.length;
//        StringBuilder sb = new StringBuilder();
//        sb.append("event data[ ");
//        sb.append("timestamp=");
//        sb.append(timestamp);
//        sb.append(" ");
//        sb.append("loggername=");
//        sb.append(loggernamespace);
//        sb.append(" ");
//        sb.append("loglevel=");
//        sb.append(loglevel);
//        sb.append(" ");
//        sb.append("threadname=");
//        sb.append(threadname);
//        sb.append(" ");
//        sb.append("mdc=");
//        sb.append(mdc.toString());
//        sb.append(" ");
//        for (int idx = 0; idx < sz; idx++) {
//            String[] keypair = fmtpairs[idx].split("=");
//            int kpsz = keypair.length;
//            String key = keypair[0];
//            String rawval = (kpsz > 1) ? keypair[1] : "";
//            String value = XRLogPairCodec.decode(rawval, Charset.forName("UTF-8"));
//            sb.append("keypair[");
//            sb.append(idx);
//            sb.append("]=");
//            sb.append(key);
//            sb.append(":");
//            sb.append(value);
//            if (idx < sz - 1) {
//                sb.append(" | ");
//            }
//        }
//        sb.append(" ]");
//        addError(sb.toString());
//        //todo does not print event metadata yet.
//    }
//
//}
