package org.jvmxray.agent.util;

import org.jvmxray.agent.event.IStackTrace;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.filters.StackDebugLevel;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Stack;

// Utility function to work w/Events in String form.
public class EventUtil {

    // End of line separator
    public static final String EOL = System.getProperty("line.separator");

    // Allow limited selection of characters.  We include replacement char since it's escaped later.  Slash needed for RegX.
    public static final String ALLOWED_CHAR = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890()=.@-_\\";

    private static EventUtil ep;

    private EventUtil() {}

    public synchronized static EventUtil getInstance() {
        if( ep == null ) {
            ep = new EventUtil();
        }
        return ep;
    }
//
//    public int getPK(String event) {
//        String raw = eventFieldFromString(event,0);
//        return Integer.parseInt(raw);
//    }
//
//    public int getState(String event) {
//        String raw = eventFieldFromString(event,1);
//        return Integer.parseInt(raw);
//    }
//
//    public long getTimeStamp(String event){
//        String raw = eventFieldFromString(event,2);
//        return Long.parseLong(raw);
//    }
//
//    public String getThreadId(String event){
//        String raw = eventFieldFromString(event,3);
//        return raw;
//    }
//
//    public String getEventType(String event){
//        String raw = eventFieldFromString(event,4);
//        return raw;
//    }
//
//    public String getIdentity(String event){
//        String raw = eventFieldFromString(event,5);
//        return raw;
//    }
//
//    public String getParam1(String event){
//        String raw = eventFieldFromString(event,6);
//        return raw;
//    }
//
//    public String getParam2(String event){
//        String raw = eventFieldFromString(event,7);
//        return raw;
//    }
//
//    public String getParam3(String event){
//        String raw = eventFieldFromString(event,8);
//        return raw;
//    }
//
//
//    public String getStackTrace(String event){
//        String raw = eventFieldFromString(event,6);
//        return raw;
//    }

    /**
     * Utility method to create an array of StackTraceDAOs from an array of StackTraceElements.  Metadata
     * included with event depending on configuration settings.
     * @param opts NONE, SOURCEPATH, LIMITED, and FULL.  None, does not include any trace information.
     *             SOURCEPATH, contains the class and source information where it was loaded from (e.g.,
     *             a file or URL for example). LIMITED, contains all SOURCEPATH plus
     * @return
     * @throws ClassNotFoundException
     */
    public StackTraceDAO createStackTraceDAO(StackDebugLevel opts) {
        StackTraceDAO result = null;
        // Return on NONE, nothing to do.
        if (opts == StackDebugLevel.NONE ) return result;
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        StackTraceDAO previousDAO = null;
        StackTraceDAO nextDAO = null;
        int iSz = ste.length;
        boolean bCreateHead = true;
        for (int i = 0; i < iSz; i++) {
            StackTraceElement element = ste[i];
            Class eclass = null;
            try {
                eclass = Class.forName(element.getClassName());
            } catch (ClassNotFoundException e) {
                eclass = Object.class;
                System.err.println("Err: Unable to load class.  class="+element.getClassName());
            }
            // Do this for LIMITED, SOURCEPATH, and FULL.
            String clsloadernm = "";
            String clsnm = element.getClassName();
            String methnm = "";
            int linenum = 0;
            String resourcenm = "";
            String loc = "";
            String modulenm = "";
            String modulevr = "";
            boolean isnative = false;
            String ds = "unavailable";
            String filenm = "unavailable";
            if (opts == StackDebugLevel.SOURCEPATH || opts == StackDebugLevel.LIMITED || opts == StackDebugLevel.FULL ) {
                // Get source file
                filenm = element.getFileName();
                // Set system classloader as prefered by default.
                ClassLoader clsPreferedClassloader = ClassLoader.getSystemClassLoader();
                ClassLoader clSpecifiedClsLoader = eclass.getClassLoader();
                // If the classloader is unspecified get the classname and find
                // the loader and resolve it's name.
                if (clSpecifiedClsLoader == null) {
                    try {
                        clsPreferedClassloader = Class.forName(clsnm).getClassLoader();
                    }catch(ClassNotFoundException e) {}
                    if (clsPreferedClassloader != null) {
                        clsloadernm = clsPreferedClassloader.getClass().getName();
                    } else {
                        clsloadernm = "unspecified";
                        clsPreferedClassloader = ClassLoader.getSystemClassLoader();
                    }
                } else {
                    clsPreferedClassloader = clSpecifiedClsLoader;
                    clsloadernm = clsPreferedClassloader.getClass().getName();
                }
 //               resourcenm = eclass.getName().replace('.', '/') + ".class";
//                loc = clsPreferedClassloader.getResource(resourcenm).toString();
                ProtectionDomain pf = eclass.getProtectionDomain();
                if( pf != null ) {
                    CodeSource cs = pf.getCodeSource();
                    if( cs != null ) {
                        URL tl = cs.getLocation();
                        if( tl != null ) {
                            loc = tl.toString();
                        } else {
                            loc = "unspecified";
                        }
                    } else {
                        loc = "unspecified";
                    }
                } else {
                    loc = "unspecified";
                }
            }
            if ( opts == StackDebugLevel.LIMITED || opts == StackDebugLevel.FULL ) {
                methnm = ( element.getMethodName() == null ) ? "unavailable" : element.getMethodName();
                linenum = element.getLineNumber();
            }
            // Include all information for FULL
            if (opts == StackDebugLevel.FULL ) {
                isnative = element.isNativeMethod();
                ds = ( element.toString() == null ) ? "unavailable" : element.toString();
            }
            // Create the head node
            if( bCreateHead ) {
                previousDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
                                                loc, modulenm, modulevr, isnative, ds);
                result = previousDAO;
                bCreateHead=false;
            // Create decendant nodes
            } else {
                nextDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
                                             loc, modulenm, modulevr, isnative, ds);
                previousDAO.setNextstacktrace(nextDAO);
                previousDAO = nextDAO;
            }
        }
        return result;
    }

    public final String getEscapedEventString( String pk, String state, String ts, String thread_id,
                                               String et, String instance_id,
                                               String param1, String param2, String param3 ) {
        StringBuffer buff = new StringBuffer();
        // Event output format
        // PK,STATE,TIMESTAMP,THREAD-ID,EVENT-TYPE,SERVER-INSTANCE-ID,CALLSTACK,PARAM1,PARAM2,PARAM3
        // EXAMPLE:
        // -1,-1,1616460579243,main-1,PERMISSION,8e024c669b8aa469-37e64854-175d95a6ab1-8000,disabled,setContextClassLoader,,
        buff.append(pk); // PK (NOT USED UNTIL A REC IS STORED ON SERVER, UNUSED, -1)
        buff.append(',');
        buff.append(getFilteredString(state)); // STATE (-1, NOT USED)
        buff.append(',');
        buff.append(getFilteredString(ts)); // TIMESTAMP
        buff.append(',');
        buff.append(getFilteredString(thread_id)); // THREAD-ID
        buff.append(',');
        buff.append(getFilteredString(et)); // EVENT-TYPE
        buff.append(',');
        buff.append(getFilteredString(instance_id)); // SEVER-INSTANCE-ID
        buff.append(',');
        buff.append(getFilteredString(param1)); // PARAM1
        buff.append(',');
        buff.append(getFilteredString(param2)); // PARAM2
        buff.append(',');
        buff.append(getFilteredString(param3)); // PARAM3
        return buff.toString();
    }

    //-1,-1,1622494285740,main-1,PACKAGE_ACCESS,custom-server-id-test,,java.lang,,java.lang
    private final String eventFieldFromString(String event, int index) {
        String result = "";
        if (event == null) {
            result = null;
        } else {
            int fieldcnt = 0;
            int idx = 0;
            int len = event.length();
            int idxOf = event.indexOf(",", 0);
            while (idxOf > 0 && idxOf<len) {
                String field = event.substring(idx, idxOf);
                if (index == fieldcnt) {
                    result = field;
                }
                idx = idxOf + 1;
                fieldcnt++;
                idxOf = event.indexOf(",", idx);
            }
        }
        return result;
    }

    // Encodes any charactes not allowed into an html entity reference.
    private final String getFilteredString(String value) {
        StringBuffer buff = new StringBuffer();
        if (value != null ) {
            if ( value.length() > 0 ) {
                for ( char c : value.toCharArray()) {
                    int idx = ALLOWED_CHAR.indexOf(c);
                    if( idx > -1 ) {
                        buff.append(c);
                    } else {
                        buff.append("&#");
                        buff.append((int)c);
                        buff.append(";");
                    }
                }
            }
        }
        return buff.toString();
    }

}
