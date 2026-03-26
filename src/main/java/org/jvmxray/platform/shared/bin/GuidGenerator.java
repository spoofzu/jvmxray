package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;
import org.jvmxray.platform.shared.util.GUID;

/**
 * Standalone CLI utility for generating GUIDs in various formats.
 * Provides command-line interface for generating UUIDs compatible with macOS uuidgen format.
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --help}: Display usage information and exit.</li>
 *   <li>{@code --standard}: Generate standard RFC 4122 UUID format with dashes (uppercase).</li>
 *   <li>{@code --compact}: Generate compact Base36-encoded UUID format.</li>
 *   <li>{@code --short}: Generate short 8-character format for human-readable contexts.</li>
 *   <li>{@code --count <n>}: Generate multiple GUIDs (default: 1).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * java org.jvmxray.platform.shared.bin.GuidGenerator
 * java org.jvmxray.platform.shared.bin.GuidGenerator --standard
 * java org.jvmxray.platform.shared.bin.GuidGenerator --compact
 * java org.jvmxray.platform.shared.bin.GuidGenerator --short
 * java org.jvmxray.platform.shared.bin.GuidGenerator --count 5
 * java org.jvmxray.platform.shared.bin.GuidGenerator --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class GuidGenerator {

    // Command-line option constants
    private static final String OPT_HELP = "help";
    private static final String OPT_STANDARD = "standard";
    private static final String OPT_COMPACT = "compact";
    private static final String OPT_SHORT = "short";
    private static final String OPT_COUNT = "count";

    private String format = "standard"; // default format
    private int count = 1; // default count

    /**
     * Main method to run the GUID generator.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            new GuidGenerator().run(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the GUID generator, parsing command-line arguments and executing the requested action.
     *
     * @param args Command-line arguments.
     */
    private void run(String[] args) throws Exception {
        // Parse command-line arguments
        parseCommandLine(args);

        // Generate and output GUIDs
        for (int i = 0; i < count; i++) {
            String guid = generateGuid();
            System.out.println(guid);
        }
    }

    /**
     * Parses command-line arguments and sets instance variables accordingly.
     *
     * @param args Command-line arguments.
     */
    private void parseCommandLine(String[] args) throws ParseException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Handle help option
        if (cmd.hasOption(OPT_HELP)) {
            displayHelp(options);
            System.exit(0);
        }

        // Determine format (default is standard)
        if (cmd.hasOption(OPT_COMPACT)) {
            format = OPT_COMPACT;
        } else if (cmd.hasOption(OPT_SHORT)) {
            format = OPT_SHORT;
        } else {
            format = OPT_STANDARD; // default or explicitly set
        }

        // Handle count option
        if (cmd.hasOption(OPT_COUNT)) {
            String countStr = cmd.getOptionValue(OPT_COUNT);
            try {
                count = Integer.parseInt(countStr);
                if (count < 1) {
                    throw new ParseException("Count must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid count value: " + countStr);
            }
        }
    }

    /**
     * Creates command-line options.
     *
     * @return Options object containing all available command-line options.
     */
    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h")
                .longOpt(OPT_HELP)
                .desc("Display this help message")
                .build());

        options.addOption(Option.builder("s")
                .longOpt(OPT_STANDARD)
                .desc("Generate standard RFC 4122 UUID format with dashes (uppercase) - DEFAULT")
                .build());

        options.addOption(Option.builder("c")
                .longOpt(OPT_COMPACT)
                .desc("Generate compact Base36-encoded UUID format")
                .build());

        options.addOption(Option.builder("r")
                .longOpt(OPT_SHORT)
                .desc("Generate short 8-character format for human-readable contexts")
                .build());

        options.addOption(Option.builder("n")
                .longOpt(OPT_COUNT)
                .hasArg()
                .argName("number")
                .desc("Generate multiple GUIDs (default: 1)")
                .build());

        return options;
    }

    /**
     * Generates a GUID based on the selected format.
     *
     * @return Generated GUID string
     */
    private String generateGuid() {
        switch (format) {
            case OPT_COMPACT:
                return GUID.generate();
            case OPT_SHORT:
                return GUID.generateShort();
            case OPT_STANDARD:
            default:
                return GUID.generateStandard();
        }
    }

    /**
     * Displays help information for the command-line interface.
     *
     * @param options Available command-line options
     */
    private void displayHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("guid-generator [OPTIONS]",
                "\nJVMXRay GUID Generator - Generate globally unique identifiers\n\n",
                options,
                "\nExamples:\n" +
                "  guid-generator                    Generate standard UUID (default)\n" +
                "  guid-generator --standard         Generate standard RFC 4122 UUID\n" +
                "  guid-generator --compact          Generate compact Base36 format\n" +
                "  guid-generator --short            Generate short 8-character format\n" +
                "  guid-generator --count 5          Generate 5 standard UUIDs\n" +
                "  guid-generator -c -n 3            Generate 3 compact GUIDs\n\n" +
                "Default format matches macOS uuidgen output (uppercase with dashes).\n");
    }
}