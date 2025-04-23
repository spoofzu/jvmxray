package org.jvmxray.platform.shared.util;

import org.jvmxray.platform.shared.property.AgentProperties;

import java.rmi.dgc.VMID;

/**
 * Utility class for generating globally unique identifiers (GUIDs) in the JVMXRay
 * framework. Produces identifiers based on {@link VMID} that are suitable for use
 * in file systems, such as file names, by reformatting the output to replace invalid
 * characters.
 *
 * <p>This class is used to create unique identifiers for components like agent
 * properties (e.g., AID in {@link AgentProperties}).</p>
 *
 * @author Milton Smith
 */
public class GUID {

    /**
     * Private constructor to prevent instantiation.
     */
    private GUID() {
        // Prevent instantiation
    }

    /**
     * Reformats a {@link VMID} string to make it suitable for file system use by
     * replacing colons and colon-hyphen combinations with hyphens.
     *
     * @param vmid The {@link VMID} string to reformat.
     * @return The reformatted string.
     */
    private static final String formatID(String vmid) {
        // Replace ":" with "-"
        String result = vmid.replace(":", "-");
        return result;
    }

    /**
     * Generates a globally unique identifier (GUID) suitable for file system use,
     * such as file names, based on a {@link VMID}.
     *
     * @return The unique identifier as a reformatted string.
     */
    public static final String getID() {
        // Generate and format VMID
        return formatID(new VMID().toString());
    }
}