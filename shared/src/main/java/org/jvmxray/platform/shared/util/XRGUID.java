package org.jvmxray.platform.shared.util;

import java.rmi.dgc.VMID;

/**
 * Manage globally unique ids.
 */
public class XRGUID {

    private XRGUID() {}

    /**
     * Reformat VMID's
     * @param vmid VMID id format suitable for use with file systems.
     * @return Filtered String.
     */
    private static final String formatID(String vmid) {
        String result = vmid.replace(":-","-");
        result = result.replace(":","-");
        return result;
    }

    /**
     * Globally unique global identifier.  Format suitable for use
     * with file systems like file names.
     * @return Globally unique identifier.
     */
    public static final String getID() {
        return formatID(new VMID().toString());
    }

}
