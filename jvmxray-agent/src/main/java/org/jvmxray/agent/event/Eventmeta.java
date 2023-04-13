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

    public String getClsLoaderNm() {
        return clsloadernm;
    }

    public String getFileNm() {
        return filenm;
    }

    public String getClsNm() {
        return clsnm;
    }

    public String getMethNm() {
        return methnm;
    }

    public int getLineNum() {
        return linenum;
    }

    public String getLoc() {
        return loc;
    }

    public String getModuleNm() {
        return modulenm;
    }

    public String getModuleVr() {
        return modulevr;
    }

    public boolean isIsNative() {
        return isnative;
    }

    public String getDs() {
        return ds;
    }
}
