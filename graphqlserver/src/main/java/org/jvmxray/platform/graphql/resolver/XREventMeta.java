package org.jvmxray.platform.graphql.resolver;

public class XREventMeta {
    private String eventid;
    private Integer level;
    private String clzldr;
    private String clzcn;
    private String clzmethnm;
    private String clzmodnm;
    private String clzmodvr;
    private String clzfilenm;
    private Integer clzlineno;
    private String clzlocation;
    private String clznative;

    public XREventMeta() {
    }

    public XREventMeta(String eventid, Integer level, String clzldr, String clzcn, String clzmethnm, String clzmodnm, String clzmodvr, String clzfilenm, Integer clzlineno, String clzlocation, String clznative) {
        this.eventid = eventid;
        this.level = level;
        this.clzldr = clzldr;
        this.clzcn = clzcn;
        this.clzmethnm = clzmethnm;
        this.clzmodnm = clzmodnm;
        this.clzmodvr = clzmodvr;
        this.clzfilenm = clzfilenm;
        this.clzlineno = clzlineno;
        this.clzlocation = clzlocation;
        this.clznative = clznative;
    }

    public String getEventid() {
        return eventid;
    }

    public void setEventid(String eventid) {
        this.eventid = eventid;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getClzldr() {
        return clzldr;
    }

    public void setClzldr(String clzldr) {
        this.clzldr = clzldr;
    }

    public String getClzcn() {
        return clzcn;
    }

    public void setClzcn(String clzcn) {
        this.clzcn = clzcn;
    }

    public String getClzmethnm() {
        return clzmethnm;
    }

    public void setClzmethnm(String clzmethnm) {
        this.clzmethnm = clzmethnm;
    }

    public String getClzmodnm() {
        return clzmodnm;
    }

    public void setClzmodnm(String clzmodnm) {
        this.clzmodnm = clzmodnm;
    }

    public String getClzmodvr() {
        return clzmodvr;
    }

    public void setClzmodvr(String clzmodvr) {
        this.clzmodvr = clzmodvr;
    }

    public String getClzfilenm() {
        return clzfilenm;
    }

    public void setClzfilenm(String clzfilenm) {
        this.clzfilenm = clzfilenm;
    }

    public Integer getClzlineno() {
        return clzlineno;
    }

    public void setClzlineno(Integer clzlineno) {
        this.clzlineno = clzlineno;
    }

    public String getClzlocation() {
        return clzlocation;
    }

    public void setClzlocation(String clzlocation) {
        this.clzlocation = clzlocation;
    }

    public String getClznative() {
        return clznative;
    }

    public void setClznative(String clznative) {
        this.clznative = clznative;
    }

}