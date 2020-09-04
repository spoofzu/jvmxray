package org.owasp.jvmxray.util;

import java.util.StringTokenizer;

import org.owasp.jvmxray.event.EventFactory;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;
import org.owasp.jvmxray.exception.JVMXRayException;

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
	
	public final IEvent fromJSON( String event ) throws JVMXRayException {
		
		IEvent result = null;
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
		String cl="", st="", ts="",
				td="", et="", id="",
				ca="", pa="", p1="", p2="", p3="";
		int pk = -1; // deprecated, only used for db.
		//TODOMS fix this up
		while( st1.hasMoreTokens() ) {
			StringTokenizer st2 = new StringTokenizer(st1.nextToken(), ":");
			String nm = st2.nextToken();
			String va = st2.nextToken();
			nm = nm.replace("\"","").trim();
			nm = ( nm.indexOf(',')>-1) ? nm.substring(0,nm.indexOf(',')) : nm;
			va = va.replace("\"","").trim();
			va = ( va.indexOf(',')>-1) ? va.substring(0,va.indexOf(',')) : va;
			switch (nm) {
				case "cl":
					cl = va;
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
				case "pa":
					pa = va;
					StringTokenizer pz = new StringTokenizer(pa, "|");
					p1 = pz.hasMoreTokens() ? pz.nextToken() : "";
					p2 = pz.hasMoreTokens() ? pz.nextToken() : "";
					p3 = pz.hasMoreTokens() ? pz.nextToken() : "";
					break;
				default:
					throw new JVMXRayException("Unknown IEvent metadata.  meta="+nm);
			}
		}
		EventFactory f = EventFactory.getInstance();	
		result = f.createEventByEventType( Events.valueOf(et), pk, Integer.parseInt(st), 
				Long.parseLong(ts), td,  id, 
				st, p1, p2, p3);
				
		return result;
	}
	
	
	
	public final String toJSON( IEvent event ) {
		StringBuffer buff = new StringBuffer(1000);
		buff.append("{\"ievent\": {");
		buff.append(EOL);
		_toJSONScalarValue( buff, "cl", event.getClass().getName(), true);
		_toJSONScalarValue( buff, "st", Integer.toString(event.getState()), true );
		_toJSONScalarValue( buff, "ts", Long.toString(event.getTimeStamp()), true );
		_toJSONScalarValue( buff, "td", event.getThreadId(), true );
		_toJSONScalarValue( buff, "et", event.getEventType().toString(), true );
		_toJSONScalarValue( buff, "id", event.getIdentity(), true );
		_toJSONScalarValue( buff, "ca", event.getStackTrace(), true );
		_toJSONMultiValue( buff, "pa", event.getParams() );
		buff.append("}}");
		buff.append(EOL);
		return buff.toString();
	}

	public final void _toJSONScalarValue( StringBuffer buff, String key, String value, boolean trailingcomma ) {
		String c = trailingcomma ? "," : "";
		buff.append("   \""+key+"\": ");
		buff.append("\"");
		buff.append(value);
		buff.append("\"");
		buff.append(c);
		buff.append(EOL);
	}
	
	public final void _toJSONMultiValue( StringBuffer buff, String key, String[] value ) {
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
