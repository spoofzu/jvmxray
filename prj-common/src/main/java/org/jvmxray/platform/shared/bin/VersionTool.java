package org.jvmxray.platform.shared.bin;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Command-line tool for displaying version information from JAR files.
 * Reads MANIFEST.MF entries including git commit, build time, and implementation version.
 *
 * <p><b>Command-Line Options:</b></p>
 * <ul>
 *   <li>{@code --target <jar-file>}: Path to JAR file to inspect (required)</li>
 *   <li>{@code --help}: Display usage information</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * # Display version info from agent JAR
 * java -jar prj-common.jar --target prj-agent/target/prj-agent-0.0.1-shaded.jar
 *
 * # Display help
 * java -jar prj-common.jar --help
 * }</pre>
 *
 * @author Milton Smith
 */
public class VersionTool {

    private static final Logger logger = Logger.getLogger(VersionTool.class.getName());

    // Command-line option constants
    private static final String OPT_TARGET = "target";
    private static final String OPT_HELP = "help";

    /**
     * Main entry point for the VersionTool application.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            new VersionTool().run(args);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VersionTool failed: " + e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Main application logic.
     *
     * @param args Command-line arguments
     * @throws Exception if execution fails
     */
    public void run(String[] args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(OPT_HELP) || args.length == 0) {
                printHelp(options);
                return;
            }

            // Get target JAR file
            String targetJar = cmd.getOptionValue(OPT_TARGET);
            if (targetJar == null) {
                throw new IllegalArgumentException("Missing required option: --target");
            }

            // Display version information
            displayVersionInfo(targetJar);

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(options);
            throw e;
        }
    }

    /**
     * Create command-line options.
     */
    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder()
            .longOpt(OPT_TARGET)
            .hasArg()
            .argName("jar-file")
            .desc("Path to JAR file to inspect")
            .required(false)  // We check manually to allow --help without --target
            .build());

        options.addOption(Option.builder()
            .longOpt(OPT_HELP)
            .desc("Display usage information")
            .build());

        return options;
    }

    /**
     * Display version information from the target JAR file.
     *
     * @param jarPath Path to the JAR file
     * @throws IOException if JAR cannot be read
     */
    private void displayVersionInfo(String jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Manifest manifest = jarFile.getManifest();

            if (manifest == null) {
                System.out.println("No MANIFEST.MF found in: " + jarPath);
                return;
            }

            Attributes attributes = manifest.getMainAttributes();

            // Print header
            System.out.println();
            System.out.println("JVMXRay Version Information");
            System.out.println("===========================");
            System.out.println();
            System.out.println("JAR File: " + jarPath);
            System.out.println();

            // Print version details
            printAttribute(attributes, "Implementation-Version", "Version");
            printAttribute(attributes, "Git-Commit", "Git Commit");
            printAttribute(attributes, "Build-Time", "Build Time");
            printAttribute(attributes, "Premain-Class", "Agent Class");
            printAttribute(attributes, "Can-Redefine-Classes", "Can Redefine");
            printAttribute(attributes, "Can-Retransform-Classes", "Can Retransform");
            printAttribute(attributes, "Build-Jdk-Spec", "Build JDK");
            printAttribute(attributes, "Created-By", "Created By");

            System.out.println();

            // If Git-Commit exists, show GitHub link
            String gitCommit = attributes.getValue("Git-Commit");
            if (gitCommit != null && !gitCommit.isEmpty()) {
                System.out.println("GitHub Source: https://github.com/spoofzu/jvmxray/commit/" + gitCommit);
                System.out.println();
            }

        } catch (IOException e) {
            throw new IOException("Failed to read JAR file: " + jarPath, e);
        }
    }

    /**
     * Print a manifest attribute if it exists.
     *
     * @param attributes Manifest attributes
     * @param key Attribute key
     * @param label Display label
     */
    private void printAttribute(Attributes attributes, String key, String label) {
        String value = attributes.getValue(key);
        if (value != null && !value.isEmpty()) {
            System.out.printf("  %-18s: %s%n", label, value);
        }
    }

    /**
     * Print help information.
     *
     * @param options Command-line options
     */
    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);

        System.out.println("JVMXRay Version Tool");
        System.out.println("====================");
        System.out.println();

        formatter.printHelp(
            "java -jar prj-common.jar [OPTIONS]",
            "\nDisplay version information from JVMXRay JAR files.\n\n",
            options,
            "\nExamples:\n" +
            "  # Display version info from agent JAR\n" +
            "  java -jar prj-common.jar --target prj-agent/target/prj-agent-0.0.1-shaded.jar\n\n" +
            "  # Display help\n" +
            "  java -jar prj-common.jar --help\n"
        );
    }
}
