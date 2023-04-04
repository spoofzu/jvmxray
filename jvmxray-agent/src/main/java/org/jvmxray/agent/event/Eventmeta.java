package org.jvmxray.agent.event;

/**
 * Container for event meta.
 * @author Milton Smith
 */
public class Eventmeta {
    private final String clsloadernm;
    private final String filenm;
    private final String clsnm;
    private final String methnm;
    private final int linenum;
    private final String loc;
    private final String modulenm;
    private final String modulevr;
    private final boolean isnative;
    private final String ds;

    public Eventmeta(String clsloadernm, String filenm, String clsnm, String methnm, int linenum,
                     String loc, String modulenm, String modulevr, boolean isnative, String ds) {
        this.clsloadernm = clsloadernm;
        this.filenm = filenm;
        this.clsnm = clsnm;
        this.methnm = methnm;
        this.linenum = linenum;
        this.loc = loc;
        this.modulenm = modulenm;
        this.modulevr = modulevr;
        this.isnative = isnative;
        this.ds = ds;
    }

    public String getClsloadernm() {
        return clsloadernm;
    }

    public String getFilenm() {
        return filenm;
    }

    public String getClsnm() {
        return clsnm;
    }

    public String getMethnm() {
        return methnm;
    }

    public int getLinenum() {
        return linenum;
    }

    public String getLoc() {
        return loc;
    }

    public String getModulenm() {
        return modulenm;
    }

    public String getModulevr() {
        return modulevr;
    }

    public boolean isIsnative() {
        return isnative;
    }

    public String getDs() {
        return ds;
    }
}
