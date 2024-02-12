package org.jvmxray.platform.graphql.resolver;

public class XRQLEvent {

    private String eventid;
    private Long ts;
    private String eventtp;
    private String loglevel;
    private String loggernamespace;
    private String aid;
    private String cat;
    private String p1;
    private String p2;
    private String p3;

    public XRQLEvent() {
    }

    public XRQLEvent(String eventid, Long ts, String eventtp, String loglevel, String loggernamespace, String aid, String cat, String p1, String p2, String p3) {
        this.eventid = eventid;
        this.ts = ts;
        this.eventtp = eventtp;
        this.loglevel = loglevel;
        this.loggernamespace = loggernamespace;
        this.aid = aid;
        this.cat = cat;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public String getEventid() {
        return eventid;
    }

    public void setEventid(String eventid) {
        this.eventid = eventid;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getEventtp() {
        return eventtp;
    }

    public void setEventtp(String eventtp) {
        this.eventtp = eventtp;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    public String getLoggernamespace() {
        return loggernamespace;
    }

    public void setLoggernamespace(String loggernamespace) {
        this.loggernamespace = loggernamespace;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public String getP1() {
        return p1;
    }

    public void setP1(String p1) {
        this.p1 = p1;
    }

    public String getP2() {
        return p2;
    }

    public void setP2(String p2) {
        this.p2 = p2;
    }

    public String getP3() {
        return p3;
    }

    public void setP3(String p3) {
        this.p3 = p3;
    }

}
