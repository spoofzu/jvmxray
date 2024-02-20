package org.jvmxray.platform.shared.property;

import java.io.IOException;
import java.util.Enumeration;

public interface XRIProperties {

    void init() throws IOException;

    boolean isModified();

    String getProperty(String name);

    String getProperty(String name, String defaultvalue);

    void setProperty(String name, String value);

    int getIntProperty(String name) throws NumberFormatException;

    int getIntProperty(String name, int defaultvalue) throws NumberFormatException;

    void setIntProperty(String name, int value);

    long getLongProperty(String name);

    long getLongProperty(String name, long defaultValue);

    void setLongProperty(String name, long value);

    Enumeration<String> getPropertyNames();

    void saveProperties()throws IOException ;
}
