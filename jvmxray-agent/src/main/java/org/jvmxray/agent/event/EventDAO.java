package org.jvmxray.agent.event;

public class EventDAO implements IEvent {

    private static final String EOL = System.getProperties().getProperty("line.separator");

    private int id = -1;
    private int st = -1;
    private long ts = System.currentTimeMillis();
    private String tid = "undefined";
    private String et = "ACCESS_SECURITY";
    private String aid = "undefined";
    private IStackTrace str = null;
    private String p1 = "";
    private String p2 = "";
    private String p3 = "";
    private String rulename = "";

    public EventDAO(String mr, int id, int st, long ts, String tid,
                    String et, String aid, IStackTrace str,
                    String p1, String p2, String p3) {
        this.rulename = mr;
        this.id = id;
        this.st = st;
        this.ts = ts;
        this.tid = tid;
        this.et = et;
        this.aid = aid;
        this.str = str;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    @Override
    public String getMatchingRule() {
        return rulename;
    }

    public void setMatchRule( String name ) {
        rulename = name;
    }

    @Override
    public int getPk() {
        return id;
    }

    public void setPk(int value) {
        this.id = value;
    }

    @Override
    public int getSt() {
        return st;
    }

    public void setSt(int value) {
        this.st = value;
    }

    @Override
    public long getTs() {
        return ts;
    }

    public void setTs(long value) {
        this.ts = value;
    }

    @Override
    public String getTid() {
        return tid;
    }

    public void setTid(String value) {
        this.tid = value;
    }

    @Override
    public String getEt() {
        return et;
    }

    public void setEt(String value) {
        this.et = value;
    }

    @Override
    public String getAid() {
        return aid;
    }

    public void setAid(String value) {
        this.aid = value;
    }

    @Override
    public IStackTrace getStackTrace() {
        return str;
    }

    @Override
    public void setStackTrace(IStackTrace value) {
        this.str = value;
    }

    @Override
    public String getP1() {
        return p1;
    }

    public void setP1(String value) {
        p1 = value;
    }

    @Override
    public String getP2() {
        return p2;
    }

    public void setP2(String value) {
        p2 = value;
    }

    @Override
    public String getP3() {
        return p3;
    }

    public void setP3(String value) {
        p3 = value;
    }

    public String toString() {
        String OINDENT = "   ";
        String INDENT = OINDENT;
        StringBuffer buff = new StringBuffer(500);
        buff.append("Event");
        buff.append("[id=");
        buff.append(getPk());
        buff.append("]");
        buff.append(" mr="+getMatchingRule());
        buff.append(", ");
        buff.append("st=" + getSt());
        buff.append(", ");
        buff.append("ts=" + getTs());
        buff.append(", ");
        buff.append("tid=" + getTid());
        buff.append(", ");
        buff.append("et=" + getEt());
        buff.append(", ");
        buff.append("aid=" + getAid());
        buff.append(", ");
        buff.append("p1=" + getP1());
        buff.append(", ");
        buff.append("p2=" + getP2());
        buff.append(", ");
        buff.append("p3=" + getP3());
        buff.append(EOL);
        buff.append("Stacktrace");
        buff.append(EOL);
        int idx = 1;
        IStackTrace tstr1 = getStackTrace();
        if( tstr1==null ) {
            buff.append("*No data*");
        }
        while (tstr1 != null) {
            buff.append(INDENT);
            buff.append("|");
            buff.append(EOL);
            buff.append(INDENT);
            buff.append("--> ");
            buff.append("sl="+idx);
            buff.append(", ");
            buff.append("clsloadernm=" + tstr1.getClsloadernm());
            buff.append(", ");
            buff.append("filenm=" + tstr1.getFilenm());
            buff.append(", ");
            buff.append("clsnm=" + tstr1.getClsnm());
            buff.append(", ");
            buff.append("methnm=" + tstr1.getMethnm());
            buff.append(", ");
            buff.append("linenum=" + tstr1.getLinenum());
            buff.append(", ");
            buff.append("loc=" + tstr1.getLoc());
            buff.append(", ");
            buff.append("modulenm=" + tstr1.getModulenm());
            buff.append(", ");
            buff.append("modulevr=" + tstr1.getModulevr());
            buff.append(", ");
            buff.append("isnative=" + tstr1.isNative());
            buff.append(", ");
            buff.append("desc=" + tstr1.getDesc());
            tstr1 = tstr1.getNextStackTrace();
            idx++;
            INDENT += OINDENT;
        }
        return buff.toString();
    }

    public boolean equals(Object obj) {
        boolean bResult = false;
        if( obj != null ) {
            if ( obj instanceof IEvent ) {
                IEvent eEvent = (IEvent)obj;
                if( id==eEvent.getPk() &&
                    st==eEvent.getSt() &&
                    ts==eEvent.getTs() &&
                    tid.equals(eEvent.getTid()) &&
                    et.equals(eEvent.getEt()) &&
                    aid.equals(eEvent.getAid()) &&
                    p1.equals(eEvent.getP1()) &&
                    p2.equals(eEvent.getP2()) &&
                    p3.equals(eEvent.getP3()) ) {
                        IStackTrace tstr1 = eEvent.getStackTrace();
                        IStackTrace tstr2 = str;
                        while( tstr1!=null && tstr2!=null ) {
                            if (tstr1.equals(tstr2)) {
                                tstr1 = tstr1.getNextStackTrace();
                                tstr2 = tstr2.getNextStackTrace();
                            } else {
                                bResult = false;
                                break;
                            }
                        }
                        // Each level passed equality tests and no more
                        // nodes remaining to be tested.
                        if( tstr1==null && tstr1==null) {
                            bResult = true;
                        }
                }
            }
        }
        return bResult;
    }
}