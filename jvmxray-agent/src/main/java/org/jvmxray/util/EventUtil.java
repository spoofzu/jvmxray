package org.jvmxray.util;

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

    public int getPK(String event) {
        String raw = eventFieldFromString(event,0);
        return Integer.parseInt(raw);
    }

    public int getState(String event) {
        String raw = eventFieldFromString(event,1);
        return Integer.parseInt(raw);
    }

    public long getTimeStamp(String event){
        String raw = eventFieldFromString(event,2);
        return Long.parseLong(raw);
    }

    public String getThreadId(String event){
        String raw = eventFieldFromString(event,3);
        return raw;
    }

    public String getEventType(String event){
        String raw = eventFieldFromString(event,4);
        return raw;
    }

    public String getIdentity(String event){
        String raw = eventFieldFromString(event,5);
        return raw;
    }

    public String getStackTrace(String event){
        String raw = eventFieldFromString(event,6);
        return raw;
    }

    public String getParam1(String event){
        String raw = eventFieldFromString(event,7);
        return raw;
    }

    public String getParam2(String event){
        String raw = eventFieldFromString(event,8);
        return raw;
    }

    public String getParam3(String event){
        String raw = eventFieldFromString(event,9);
        return raw;
    }

    public final String getEscapedEventString( String pk, String state, String ts, String thread_id,
                                               String et, String instance_id, String cs,
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
        buff.append(getFilteredString(cs)); // CALLSTACK
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
