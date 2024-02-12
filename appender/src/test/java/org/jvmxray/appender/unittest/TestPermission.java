package org.jvmxray.appender.unittest;

import java.security.BasicPermission;

public class TestPermission extends BasicPermission {
    private static final long serialVersionUID = 5932223970203786295L;

    public TestPermission(String name) {
        super( name );
    }
    public TestPermission() {
        this("testpermissionNZ");
    }

    public String getActions() {
        // Note: setting permission action fm the two arg constructor
        // does not work.  As a workaround, hard coding a
        // return value.
        return "testaction";
    }
}
