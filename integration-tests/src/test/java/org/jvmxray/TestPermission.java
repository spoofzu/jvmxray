package org.jvmxray;

import java.security.BasicPermission;

public class TestPermission extends BasicPermission {
    private static final long serialVersionUID = 5932223970203786295L;

    TestPermission() {
        super("testpermissionname1");
    }

    public String getActions() {
        // Note: setting permission action fm the two arg constructor
        // does not work.  As a workaround, hard coding a
        // return value.
        return "testaction1";
    }
}
