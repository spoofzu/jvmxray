package org.jvmxray.platform.shared.log.intervallogger;

/**
 * A property class that stores key-value pairs representing byte-based metrics
 * and appends SI unit names (e.g., B, kB, MB) to the value for human-readable
 * output. Extends {@link DefaultIntervalProperty} to inherit basic property
 * management and adds byte-specific formatting logic.
 *
 * <p>This class is used within the JVMXRay framework to log metrics such as file
 * sizes or data transfer amounts with appropriate SI units.</p>
 *
 * @author Milton Smith
 */
public class ByteIntervalProperty extends DefaultIntervalProperty {

    /**
     * Constructs a new {@code ByteIntervalProperty} with the specified key name.
     *
     * @param name The property key name, used to identify the value in logs.
     *             Must not be null or empty.
     * @throws IllegalArgumentException If the name is null or empty.
     */
    public ByteIntervalProperty(String name) {
        // Delegate to parent constructor for name validation and initialization
        super(name);
    }

    /**
     * Returns the property value formatted with SI unit names (e.g., "45.3MB", "62B").
     * If the value is not a valid long, the unformatted value from the parent class
     * is returned.
     *
     * @return The value as a string, with SI units appended if it represents a valid
     *         byte count, or the raw value otherwise.
     * @see #addUnits(String)
     */
    @Override
    public String getValue() {
        // Get the raw value from the parent class
        String results = super.getValue();

        // Attempt to parse the value as a long and append SI units
        try {
            Long.parseLong(results);
            results = addUnits(results);
        } catch (NumberFormatException e) {
            // Ignore exception and return unformatted value
        }

        return results;
    }

    /**
     * Formats a byte value by appending SI unit names based on its magnitude.
     * Converts large byte counts to higher units (kB, MB, GB, etc.) with one
     * decimal place for readability.
     *
     * @param value The byte value as a string, representing a valid long.
     * @return The formatted value with SI units (e.g., "45.3MB", "62B", "27.2GB").
     * @throws NumberFormatException If the value cannot be parsed as a long.
     */
    public String addUnits(String value) {
        // Initialize buffer for formatted output
        StringBuffer buff = new StringBuffer(100);

        // Parse the byte value
        long bytes = Long.parseLong(value);

        if (bytes < 1000000) {
            // Append "B" for small values (< 1MB)
            buff.append(value);
            buff.append("B");
        } else {
            // Calculate appropriate SI unit for larger values
            int unit = 1000;
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = "kMGTPE".charAt(exp - 1) + "";
            // Format value with one decimal place and SI prefix
            String ov = String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
            buff.append(ov);
        }

        return buff.toString();
    }
}