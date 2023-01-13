package org.jvmxray.agent.event;

public interface IStackTrace {

    public String getClsnm();

    public String getFilenm();

    public String getMethnm();

    public int getLinenum();

    public String getLoc();

    public String getModulenm();

    public String getModulevr();

    public boolean isNative();

    public String getDesc();

    public String getClsloadernm();

    public IStackTrace getNextStackTrace();

//    public IStackTrace getPreviousStackTrace();

    public String toString();

    public boolean equals(Object obj);
}
