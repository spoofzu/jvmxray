package org.jvmxray.agent.util;

import java.io.UnsupportedEncodingException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.event.IStackTrace;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.exception.JVMXRayException;

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

	public final IEvent eventFromJSON(String event) throws JVMXRayException, UnsupportedEncodingException {
		IEvent result = null;
		// Return null if event is invalid
		if( event != null && event.length() < 1 ) {
			throw new JVMXRayException("Empty or null event data.  event="+event);
		}
		JSONParser parser = new JSONParser();
		JSONObject jsonEvent = null;
		try {
			jsonEvent = (JSONObject)parser.parse(event);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		boolean bCreateHead = true;
		StackTraceDAO previousDAO = null;
		StackTraceDAO nextDAO = null;
		StackTraceDAO headnodeDAO = null;
		JSONObject jsonStackTrace = null;
		int i=0;
		while(true) {
			String sid="sid"+i;
			if( jsonEvent.containsKey(sid) ) {
				jsonStackTrace = (JSONObject)jsonEvent.get(sid);
				String clsloadernm = (String)jsonStackTrace.get("clsloadernm");
				String filenm = (String)jsonStackTrace.get("filenm");
				String clsnm = (String)jsonStackTrace.get("clsnm");
				String methnm = (String)jsonStackTrace.get("methnm");
				int linenum = ((Number)jsonStackTrace.get("linenum")).intValue();
				String loc = (String)jsonStackTrace.get("loc");
				String modulenm = (String)jsonStackTrace.get("modulenm");
				String modulevr = (String)jsonStackTrace.get("modulevr");
				boolean isnative = (Boolean)jsonStackTrace.get("isnative");
				String desc = (String)jsonStackTrace.get("desc");
				// Create the head node
				if( bCreateHead ) {
					previousDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
							loc, modulenm, modulevr, isnative, desc);
					headnodeDAO = previousDAO;
					bCreateHead = false;
					// Create decendant nodes
				} else {
					nextDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
							loc, modulenm, modulevr, isnative, desc);
					previousDAO.setNextstacktrace(nextDAO);
					previousDAO = nextDAO;
				}
			} else {
				break;
			}
			i++;
		}
		int id = ((Number)jsonEvent.get("id")).intValue();
		String mr = (String)jsonEvent.get("mr");
		int st = ((Number)jsonEvent.get("st")).intValue();
		long ts = ((Number)jsonEvent.get("ts")).longValue();
		String tid = (String)jsonEvent.get("tid");
		String et = (String)jsonEvent.get("et");
		String aid = (String)jsonEvent.get("aid");
		String p1 = (String)jsonEvent.get("p1");
		String p2 = (String)jsonEvent.get("p2");
		String p3 = (String)jsonEvent.get("p3");
		EventDAO eDAO = new EventDAO(mr,id,st,ts,tid,et,aid,headnodeDAO,p1,p2,p3);

//		// remove wht space
//		event = event.replaceAll("\\s", "");
//		StringTokenizer t = new StringTokenizer(event, "{");
//		String tmp = null;
//		if( t.hasMoreTokens() ) {
//			t.nextToken();
//			tmp = t.nextToken();
//		}
//		// Return null if event is invalid
//		if( tmp == null ) {
//			throw new JVMXRayException("Parse error event data.  event="+event+" tmp="+tmp);
//		}
//		tmp = tmp.substring(0,tmp.length()-2);
//		StringTokenizer st1 = new StringTokenizer(tmp, ",");
//		int id=0, st=0;
//		long ts=0;
//		String sId="", sSt="", sTs="",
//			   tid="", et="", aid="",
//			   p1="", p2="", p3="";
//		HashMap <String,String> map = new HashMap<String,String>();
//		while( st1.hasMoreTokens() ) {
//			StringTokenizer st2 = new StringTokenizer(st1.nextToken(), ":");
//			String nm = st2.nextToken();
//			nm=nm.trim();
//			String va = st2.nextToken();
//			va=va.trim();
//			nm = nm.substring(1,nm.length()-1);
//			va = va.substring(1,va.length()-1);
//			va = JSONUtil.unescape(va);
//			if( nm.startsWith("tr") ) {
//				map.put(nm,va);
//			} else {
//				switch (nm) {
//					case "id":
//						sId = va;
//						id = Integer.parseInt(sId);
//						break;
//					case "st":
//						sSt = va;
//						st = Integer.parseInt(sSt);
//						break;
//					case "ts":
//						sTs = va;
//						ts = Long.parseLong(sSt);
//						break;
//					case "tid":
//						tid = va;
//						break;
//					case "et":
//						et = va;
//						break;
//					case "aid":
//						aid = va;
//						break;
//					case "p1":
//						p1 = va;
//						break;
//					case "p2":
//						p2 = va;
//						break;
//					case "p3":
//						p3 = va;
//						break;
//					case "proto-version":
//						// Future compatibilty support.
//						if (!va.equals("0.1")) {
//							throw new JVMXRayException("Illegal protocol version.  nm=" + nm + " va=" + va);
//						}
//						break;
//					default:
//						throw new JVMXRayException("Unknown event metadata element.  meta=" + nm);
//				}
//			}
//		}
//		ArrayList<IStackTrace> tbuff = new ArrayList<IStackTrace>();
//		int i=0;
//		while(true) {
//			String prefix = "tr"+i+"-";
//			if(!map.containsKey(prefix+"clsnm")) {
//				break;
//			}
//			String clsloadernm = map.get(prefix+"clsloadernm");
//			String clsnm = map.get(prefix+"clsnm");
//			String methnm = map.get(prefix+"methnm");
//			String linenum = map.get(prefix+"linenum");
//			int iLinenum = Integer.parseInt(linenum);
//			String loc = map.get(prefix+"loc");
//			String modulenm = map.get(prefix+"modulenm");
//			String modulevr = map.get(prefix+"modulevr");
//			String isnative = map.get(prefix+"isnative");
//			boolean bIsNative = Boolean.getBoolean(isnative);
//			String desc = map.get(prefix+"desc");
//			StackTraceDAO sDAO = new StackTraceDAO(clsloadernm,clsnm,methnm,iLinenum,
//													loc,modulenm,modulevr,bIsNative,desc);
//			tbuff.add(sDAO);
//			i++;
//		}
//		IStackTrace[] traces = tbuff.toArray(new IStackTrace[0]);
//		EventDAO eDAO = new EventDAO(id,st,ts,tid,et,aid,traces,p1,p2,p3);

		return eDAO;
	}

	public final String eventToJSON(IEvent event) {

		final JSONObject jsonEvent = new JSONObject();
		jsonEvent.put("id", event.getPk());
		jsonEvent.put("st", event.getSt());
		jsonEvent.put("ts", event.getTs());
		jsonEvent.put("tid", event.getTid());
		jsonEvent.put("et", event.getEt());
		jsonEvent.put("aid", event.getAid());
		jsonEvent.put("p1", event.getP1());
		jsonEvent.put("p2", event.getP2());
		jsonEvent.put("p3", event.getP3());
		jsonEvent.put("mr", event.getMatchingRule());

		int i=0;
		JSONObject jsonStackTrace = new JSONObject();
		IStackTrace str = event.getStackTrace();
		while( str!= null ) {
			jsonStackTrace.put("clsloadernm", str.getClsloadernm());
			jsonStackTrace.put("filenm", str.getFilenm());
			jsonStackTrace.put("clsnm",str.getClsnm());
			jsonStackTrace.put("methnm",str.getMethnm());
			jsonStackTrace.put("linenum",str.getLinenum());
			jsonStackTrace.put("loc",str.getLoc());
			jsonStackTrace.put("modulenm",str.getModulenm());
			jsonStackTrace.put("modulevr",str.getModulevr());
			jsonStackTrace.put("isnative",str.isNative());
			jsonStackTrace.put("desc",str.getDesc());
			jsonEvent.put("sid"+i, jsonStackTrace);
			str = str.getNextStackTrace();
			jsonStackTrace = new JSONObject();
			i++;
		}
		String json = jsonEvent.toJSONString();
		return json.toString();

//		StringBuffer buff = new StringBuffer(1000);
//		buff.append("{\"ievent\": {");
//		buff.append(EOL);
//		IStackTrace[] aSt = event.getStackTrace();
//		_toJSONScalarValue( buff, "proto-version", "0.1", true );
//		StringBuffer jsonbuff = new StringBuffer(100);
//		// Store the stacktrace data first.
//		for (int i=0; i<aSt.length; i++) {
//			IStackTrace stacktrace = aSt[i];
//			String tr = "tr"+i+"-";
//			_toJSONScalarValue( buff, tr+"clsloadernm", escape(stacktrace.getClsloadernm()), true );
//			_toJSONScalarValue( buff, tr+"clsnm", escape(stacktrace.getClsnm()), true );
//			_toJSONScalarValue( buff, tr+"methnm", escape(stacktrace.getMethnm()), true );
//			_toJSONScalarValue( buff, tr+"linenum", Integer.toString(stacktrace.getLinenum()), true );
//			_toJSONScalarValue( buff, tr+"loc", escape(stacktrace.getLoc()), true );
//			_toJSONScalarValue( buff, tr+"modulenm", escape(stacktrace.getModulenm()), true );
//			_toJSONScalarValue( buff, tr+"modulevr", escape(stacktrace.getModulevr()), true );
//			_toJSONScalarValue( buff, tr+"isnative", stacktrace.isNative()?"TRUE":"FALSE", true );
//			_toJSONScalarValue( buff, tr+"desc", escape(stacktrace.getDesc()), true );
//		}
//		// Followed by event data.
//		_toJSONScalarValue( buff, "id", Integer.toString(event.getPk()), true);
//		_toJSONScalarValue( buff, "st", Integer.toString(event.getSt()), true );
//		_toJSONScalarValue( buff, "ts", Long.toString(event.getTs()), true );
//		_toJSONScalarValue( buff, "tid", escape(event.getTid()), true );
//		_toJSONScalarValue( buff, "et", escape(event.getEt()), true );
//		_toJSONScalarValue( buff, "aid", escape(event.getAid()), true );
//		_toJSONScalarValue( buff, "p1", escape(event.getP1()), true );
//		_toJSONScalarValue( buff, "p2", escape(event.getP2()), true );
//		_toJSONScalarValue( buff, "p3", escape(event.getP3()), false );
//		buff.append("}}");
//		buff.append(EOL);
//		return buff.toString();
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

	/**
	 * Based on sample code from project, https://github.com/fangyidong/json-simple
	 * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
	 * @param s - Must not be null.
	 * @return JSON encoded string.
	 */
	static String escape(String s) {
		if( s==null || s.length()<1 ) return s;
		StringBuffer buff = new StringBuffer(100);
//		final int len = s.length();
//		for(int i=0;i<len;i++){
//			char ch=s.charAt(i);
//			switch(ch){
//				case '"':
//					buff.append("\\\"");
//					break;
//				case '\\':
//					buff.append("\\\\");
//					break;
//				case '\b':
//					buff.append("\\b");
//					break;
//				case '\f':
//					buff.append("\\f");
//					break;
//				case '\n':
//					buff.append("\\n");
//					break;
//				case '\r':
//					buff.append("\\r");
//					break;
//				case '\t':
//					buff.append("\\t");
//					break;
//				case '/':
//					buff.append("\\/");
//					break;
//				default:
//					//Reference: http://www.unicode.org/versions/Unicode5.1.0/
//					if((ch>='\u0000' && ch<='\u001F') || (ch>='\u007F' && ch<='\u009F') || (ch>='\u2000' && ch<='\u20FF')){
//						String ss=Integer.toHexString(ch);
//						buff.append("\\u");
//						for(int k=0;k<4-ss.length();k++){
//							buff.append('0');
//						}
//						buff.append(ss.toUpperCase());
//					}
//					else{
//						buff.append(ch);
//					}
//			}
//		}//for
		JSONObject jo = new JSONObject();
		buff.append(jo);
		return buff.toString();
	}

	static String unescape(String s) throws UnsupportedEncodingException {
		if( s==null || s.length()<1 ) return s;
//		System.out.println("*** escaped, s="+s);
//		final int len = s.length();
		StringBuffer buff = new StringBuffer();
//		for(int i=0;i<len;i++) {
//			char ch=s.charAt(i);
//			switch(ch){
//				case 0:
//				case '\n':
//				case '\r':
//					throw new UnsupportedEncodingException("Unterminated String. raw src="+s);
//				case '\\':
//					ch = s.charAt(i+1);
//					switch (ch) {
//						case 'b':
//							buff.append('\b');
//							break;
//						case 't':
//							buff.append('\t');
//							break;
//						case 'n':
//							buff.append('\n');
//							break;
//						case 'f':
//							buff.append('\f');
//							break;
//						case 'r':
//							buff.append('\r');
//							break;
//						case 'u':
//							String substr = "";
//							try {
//								substr = s.substring(i + 1, 4);
//							}catch(ArrayIndexOutOfBoundsException e) {
//								UnsupportedEncodingException e1 = new UnsupportedEncodingException("Unicode format error. raw src="+s+" sub str="+substr);
//								e1.setStackTrace(e.getStackTrace());
//								throw e1;
//							}
//							try {
//								buff.append((char)Integer.parseInt(substr, 16));
//							} catch (NumberFormatException e) {
//								UnsupportedEncodingException e1 = new UnsupportedEncodingException("Illegal escape. raw src="+s+" sub str="+substr);
//								e1.setStackTrace(e.getStackTrace());
//								throw e1;
//							}
//							break;
//						case '"':
//						case '\'':
//						case '\\':
//						case '/':
//							buff.append(ch);
//							break;
//						default:
//							throw new UnsupportedEncodingException("Illegal escape. raw src="+s);
//					}
//				default:
//					buff.append(ch);
//					break;
//			}
//		}
//		System.out.println("*** unescaped, "+buff.toString());
		JSONParser parser = new JSONParser();
		try {
			parser.parse(s);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		buff.append(parser.toString());
		return buff.toString();
	}
}
