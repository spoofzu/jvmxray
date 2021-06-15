package org.jvmxray.util;

import java.util.StringTokenizer;

import org.jvmxray.exception.JVMXRayException;

/**
 * Features for managing JSON data.
 */
public class JSONUtil {

	public static final String EOL = "\r\n";
	private static JSONUtil ju;

	private JSONUtil() {}

	public static final synchronized JSONUtil getInstance() {
		if ( ju == null ) {
			ju = new JSONUtil();
		}
		return ju;
	}

	public final String fromJSON(String event) throws JVMXRayException {
		String result = null;
		// Return null if event is invalid
		if( event != null && event.length() < 1 ) {
			throw new JVMXRayException("Empty or null event data.  event="+event);
		}
		StringTokenizer t = new StringTokenizer(event, "{");
		String tmp = null;
		if( t.hasMoreTokens() ) {
			t.nextToken();
			tmp = t.nextToken();
		}
		// Return null if event is invalid
		if( tmp == null ) {
			throw new JVMXRayException("Parse error event data.  event="+event+" tmp="+tmp);
		}
		tmp = tmp.substring(0,tmp.length()-2);
		StringTokenizer st1 = new StringTokenizer(tmp, ",");
		String pk="", st="", ts="",
				td="", et="", id="",
				ca="", p1="", p2="", p3="";
		//int pk = -1; // deprecated, only used for db.
		//TODOMS: fix this up
		while( st1.hasMoreTokens() ) {
			StringTokenizer st2 = new StringTokenizer(st1.nextToken(), ":");
			String nm = st2.nextToken();
			String va = st2.nextToken();
			nm = nm.replace("\"","").trim();
			nm = ( nm.indexOf(',')>-1) ? nm.substring(0,nm.indexOf(',')) : nm;
			va = va.replace("\"","").trim();
			va = ( va.indexOf(',')>-1) ? va.substring(0,va.indexOf(',')) : va;
			switch (nm) {
				case "pk":
					pk = va;
					break;
				case "st":
					st = va;
					break;
				case "ts":
					ts = va;
					break;
				case "td":
					td = va;
					break;
				case "et":
					et = va;
					break;
				case "id":
					id = va;
					break;
				case "ca":
					ca = va;
					break;
				case "p1":
					p1 = va;
					break;
				case "p2":
					p2 = va;
					break;
				case "p3":
					p3 = va;
					break;
				default:
					throw new JVMXRayException("Unknown event metadata element.  meta="+nm);
			}
		}
		StringBuffer buff = new StringBuffer();
		buff.append(pk);
		buff.append(',');
		buff.append(st);
		buff.append(',');
		buff.append(ts);
		buff.append(',');
		buff.append(td);
		buff.append(',');
		buff.append(et);
		buff.append(',');
		buff.append(id);
		buff.append(',');
		buff.append(ca);
		buff.append(',');
		buff.append(p1);
		buff.append(',');
		buff.append(p2);
		buff.append(',');
		buff.append(p3);
		result = buff.toString();
		return result;
	}

	public final String toJSON(String event) {
		StringBuffer buff = new StringBuffer(1000);
		buff.append("{\"ievent\": {");
		buff.append(EOL);
		_toJSONScalarValue( buff, "pk", Integer.toString(EventUtil.getInstance().getPK(event)), true);
		_toJSONScalarValue( buff, "st", Integer.toString(EventUtil.getInstance().getState(event)), true );
		_toJSONScalarValue( buff, "ts", Long.toString(EventUtil.getInstance().getTimeStamp(event)), true );
		_toJSONScalarValue( buff, "td", EventUtil.getInstance().getThreadId(event), true );
		_toJSONScalarValue( buff, "et", EventUtil.getInstance().getEventType(event), true );
		_toJSONScalarValue( buff, "id", EventUtil.getInstance().getIdentity(event), true );
		_toJSONScalarValue( buff, "ca", EventUtil.getInstance().getStackTrace(event), true );
		_toJSONScalarValue( buff, "p1", EventUtil.getInstance().getParam1(event), true );
		_toJSONScalarValue( buff, "p2", EventUtil.getInstance().getParam2(event), true );
		_toJSONScalarValue( buff, "p3", EventUtil.getInstance().getParam3(event), false );
		buff.append("}}");
		buff.append(EOL);
		return buff.toString();
	}

	private final void _toJSONScalarValue( StringBuffer buff, String key, String value, boolean trailingcomma ) {
		String c = trailingcomma ? "," : "";
		buff.append("   \""+key+"\": ");
		buff.append("\"");
		buff.append(value);
		buff.append("\"");
		buff.append(c);
		buff.append(EOL);
	}

	private final void _toJSONMultiValue( StringBuffer buff, String key, String[] value ) {
		buff.append("   \""+key+"\": ");
		buff.append("\"");
		String t="";
		for (int i=0; i<value.length; i++) {
			if( i==0 )
				t=value[i];
			else
				t=t+"|"+value[i];
		}
		buff.append(t);
		buff.append("\"");
		buff.append(EOL);
	}
	
}
