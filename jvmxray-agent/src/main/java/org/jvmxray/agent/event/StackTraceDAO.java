package org.jvmxray.agent.event;
import org.jvmxray.agent.filters.StackDebugLevel;

import java.util.ArrayList;

public class StackTraceDAO implements IStackTrace {

    private static final String EOL = System.getProperties().getProperty("line.separator");

    private String clsnm = "";
    private String filenm = "";
    private String methnm = "";
    private int linenum = -1;
    private String loc = "";
    private String modulenm = "";
    private String modulevr = "";
    private boolean isnative = false;
    private String desc = "";
    private String clsloadernm = "";
    private IStackTrace nextstacktrace = null;
    //private IStackTrace previousstacktrace;

    public StackTraceDAO() {
    }

    public StackTraceDAO(String clsloadernm, String filenm, String clsnm, String methnm, int linenum,
                         String loc, String modulenm, String modulevr, boolean isnative,
                         String desc) {
        this.clsloadernm = clsloadernm;
        this.filenm = filenm;
        this.clsnm = clsnm;
        this.methnm = methnm;
        this.linenum = linenum;
        this.loc = loc;
        this.modulenm = modulenm;
        this.modulevr = modulevr;
        this.isnative = isnative;
        this.desc = desc;
    }

    @Override
    public String getClsnm() {
        return clsnm;
    }

    void setClsnm(String clsnm) {
        this.clsnm = clsnm;
    }

    @Override
    public String getFilenm() {
        return filenm;
    }

    void setFilenm(String filenm) {
        this.filenm = filenm;
    }

    @Override
    public String getMethnm() {
        return methnm;
    }

    void setMethnm(String methnm) {
        this.methnm = methnm;
    }

    @Override
    public int getLinenum() {
        return linenum;
    }

    void setLinenum(int linenum) {
        this.linenum = linenum;
    }

    @Override
    public String getLoc() {
        return loc;
    }

    void setLoc(String loc) {
        this.loc = loc;
    }

    @Override
    public String getModulenm() {
        return modulenm;
    }

    void setModulenm(String modulenm) {
        this.modulenm = modulenm;
    }

    @Override
    public String getModulevr() {
        return modulevr;
    }

    void setModulevr(String modulevr) {
        this.modulevr = modulevr;
    }

    @Override
    public boolean isNative() {
        return isnative;
    }

    void setIsnative(boolean isnative) {
        this.isnative = isnative;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String getClsloadernm() {
        return clsloadernm;
    }

    void setClsloadernm(String clsloadernm) {
        this.clsloadernm = clsloadernm;
    }

    public IStackTrace getNextStackTrace() {
        return nextstacktrace;
    }

    public void setNextstacktrace( IStackTrace value ) {
        nextstacktrace = value;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("classname=");
        buff.append(clsnm);
        buff.append(", ");
        buff.append("filename=");
        buff.append(filenm);
        buff.append(", ");
        buff.append("classloadername=");
        buff.append(clsloadernm);
        buff.append(", ");
        buff.append("methodname=");
        buff.append(methnm);
        buff.append(", ");
        buff.append("linenum=");
        buff.append(linenum);
        buff.append(", ");
        buff.append("loc=");
        buff.append(loc);
        buff.append(", ");
        buff.append("modulenm=");
        buff.append(modulenm);
        buff.append(", ");
        buff.append("modulevr=");
        buff.append(modulevr);
        buff.append(", ");
        buff.append("isnative=");
        buff.append(isnative);
        buff.append(", ");
        buff.append("desc=");
        buff.append(desc);
        IStackTrace lste = getNextStackTrace();
        if( lste != null ) {
            buff.append(EOL);
            buff.append(lste.toString());
        }
        return buff.toString();
    }

    public boolean equals(Object obj) {
        boolean bResult = false;
        if( obj != null ) {
            if (obj instanceof IStackTrace) {
                IStackTrace stackTrace = (IStackTrace)(obj);
                if( clsnm.equals(stackTrace.getClsnm()) &&
                    clsloadernm.equals(stackTrace.getClsloadernm()) &&
                    filenm.equals(stackTrace.getFilenm()) &&
                    methnm.equals(stackTrace.getMethnm()) &&
                    linenum == stackTrace.getLinenum() &&
                    loc.equals(stackTrace.getLoc()) &&
                    modulenm.equals(stackTrace.getModulenm()) &&
                    modulevr.equals(stackTrace.getModulevr()) &&
                    isnative == stackTrace.isNative() &&
                    desc.equals(stackTrace.getDesc()) ) {
                    bResult = true;
                }
            }
        }
        return bResult;
    }
}
