package org.jvmxray.agent.driver;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jvmxray.agent.ui.JVMXRayPrimaryPanel;
import org.jvmxray.agent.util.IdWebServer;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;

/**
 * Injects target payload and agent into a target Java process, usually
 * a web server, for monitoring with JVMXRay.
 * @author Milton Smith
 */
public class jvmxrayinjector  {

    private static final String TITLE = "jvmxrayinjector";
    private static final String OPT_HELP_SHORT = "h";
    private static final String OPT_HELP_LONG = "help";
    private static final String OPT_PIDFILE_SHORT = "f";
    private static final String OPT_PIDFILE_LONG = "pidfile";
    private static final String OPT_PIDID_SHORT = "i";
    private static final String OPT_PIDID_LONG = "pid";
    private static final String OPT_LOGFRAMEWORK_SHORT = "l";
    private static final String OPT_LOGFRAMEWORK_LONG = "agentLoggingFramework";
    private static final String OPT_AGENTPAYLOAD_SHORT = "ap";
    private static final String OPT_AGENTPAYLOAD_LONG = "agentpayload";
    private static final String OPT_DEBUGUI_SHORT = "du";
    private static final String OPT_DEBUGUI_LONG = "debugui";

    // Sample fully qualified injection payload.
    // "/Users/milton/github/jvmxray/jvmxray-agent/target/jvmxray-agent-0.0.1.jar";
    // OR
    // "/Users/milton/github/jvmxray/jvmxray-agent/target/jvmxray-agent-0.0.1-jar-with-dependencies.jar";
    //
    private JVMXRayPrimaryPanel primaryPanel = null;
    private String agentPayload = "";
    private String agentLoggingFramework = "";
    private String pidFile = "";
    private Long pidId = Long.valueOf(-1);
    private boolean bDebug = false;

    /**
     * Java entry point.
     * @param args Not used.
     */
    public static final void main(final String[] args) {
        jvmxrayinjector injector = new jvmxrayinjector();
        try {
            // Execute
            injector.process(args);
        } catch(Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace();
            System.exit(10);
        }
    }

    /**
     * Primary logic flow.  Opens a GUI if debugging or process command line
     * arguments.
     * @param args Arguments from command line.
     * @throws Exception
     */
    private void process(final String[] args) throws Exception {
        // Parse command line options and assigns options/variables.
        parseCmdLine(args);
        // Headed configuration, use desktop debug GUI.
        if(bDebug) {
            createAndShowGUI();
            primaryPanel.setJvmxRaySettingsListener(map -> {
                try {
                    Long pid = (Long)map.get("pid");
                    System.out.println("jvmxrayinjector: PID selection from debug GUI. pid=" + pid);
                    agentPayload = (String)map.get("agentPayload");
                    System.out.println("jvmxrayinjector: Agent payload selection from debug GUI. agentPayload=" + agentPayload);
                    agentLoggingFramework = (String)map.get("agentLoggingFramework");
                    System.out.println("jvmxrayinjector: Logging framework selection from debug GUI. loggingFramework=" + agentLoggingFramework);
                    injectAgent(pid);
                }catch(Exception e) {
                    System.err.println("jvmxrayinjector: Error injecting agent into target. msg="+e.getMessage());
                    e.printStackTrace();
                }
            });
        // Headless configuration, process command line options.
        } else {
            // Process pid file
            if( pidFile!=null && pidFile.length()>0 ) {
                pidId = getPidFromFile(pidFile);
            }
            // At this point must have a pid set either explictly or via a pid file.
            // Valid range, 0 - (2^63-1), -1 for unassigned.
            if (pidId.longValue()<0 ) {
                throw new Exception("jvmxrayinjector: No process id (e.g., PID) assigned. pidId="+pidId);
            }
            if( agentPayload==null || agentPayload.length()<1 ) {
                throw new Exception("jvmxrayinjector: No agent injection payload specified. agentPayload="+agentPayload);
            }
            if( agentLoggingFramework ==null || agentLoggingFramework.length()<1 ) {
                throw new Exception("jvmxrayinjector: No agent logging framework specified. loggingFramework="+ agentLoggingFramework);
            }
            // All appropriate options processed.  Attempt to inject agent.
            injectAgent(pidId);
        }

    }

    /**
     * Inject a Java agent payload into another process.
     * @param pid Process ID of an executing process.  Usually, a web server but could be any Java process.
     * @throws AgentLoadException Thrown by JVM on agent load problems.
     * @throws IOException Throw on problems with the agent payload (e.g., Jar file).
     * @throws AgentInitializationException Thrown by JVM on agent load problems.
     * @throws AttachNotSupportedException Thrown by JVM on agent load problems.
     */
    private void injectAgent(Long pid) throws AgentLoadException, IOException,
            AgentInitializationException,
            AttachNotSupportedException {
        String absolutePath = "";
        try {
            File agentJar = new File(agentPayload);
            if(agentJar==null) {
                throw new IOException("Bad agent jar file specified. file="+agentPayload);
            }
            if( agentJar.exists() ) {
                System.out.println("jvmxrayinjector: Agent JAR assigned. absolutePath="+absolutePath);
            } else {
                throw new IOException("Agent JAR not found. absolutePath="+absolutePath);
            }
            absolutePath = agentJar.getAbsolutePath();
            String spid = Long.toString(pid);
            VirtualMachine vm = VirtualMachine.attach(spid);
            System.out.println("jvmxrayinjector: Attempting to load agent. pid="+spid+" absolutePath="+absolutePath);
            vm.loadAgent(absolutePath,"agentLoggingFramework="+agentLoggingFramework);
            vm.detach();
            System.out.println("jvmxrayinjector: Agent payload loaded, successfully detached.  absolutePath="+absolutePath);
        }catch(Exception e) {
            System.err.println("jvmxrayinjector: Problem injecting agent. pid="+pid+" absolutePath="+absolutePath);
            throw e;
        }
    }

    /**
     * Parse commands and arguments.
     * @param args Java executable arguments from the command line.
     */
    private void parseCmdLine(String[] args) {
        // Command line options
        Options options = new Options();
        // HELP Option
        Option opN = Option.builder(OPT_HELP_SHORT)
                .longOpt(OPT_HELP_LONG)
                .desc("Help options. This screen.")
                .hasArg()
                .argName("COMMAND")
                .optionalArg(true)
                .build();
        opN.setDescription("Specifies the list of the available commands or type 'help COMMAND' for detailed information.");
        options.addOption(opN);
        // PID FILE option
        opN = Option.builder(OPT_PIDFILE_SHORT)
                .longOpt(OPT_PIDFILE_LONG)
                .desc("Assign PID file for target web server process.")
                .hasArg()
                .argName("PID FILE")
                .build();
        opN.setDescription("PID FILE specifes the fully qualified file name containing a target PID to inject jvmxray monitoring.");
        options.addOption(opN);
        // PID value option
        opN = Option.builder(OPT_PIDID_SHORT)
                .longOpt(OPT_PIDID_LONG)
                .desc("Assign PID value from command line for targeted agent process.")
                .hasArg()
                .argName("PID VALUE")
                .build();
        opN.setDescription("PID VALUE specifes target process id to inject jvmxray monitoring.");
        options.addOption(opN);
        // AGENT PAYLOAD option
        opN = Option.builder(OPT_AGENTPAYLOAD_SHORT)
                .longOpt(OPT_AGENTPAYLOAD_LONG)
                .desc("Agent payload file (e.g., jar).")
                .hasArg()
                .argName("JAR FILE")
                .build();
        opN.setDescription("JAR FILE is the agent payload to inject for jvmxray monitoring.");
        options.addOption(opN);
        // AGENT logging framework option
        opN = Option.builder(OPT_LOGFRAMEWORK_SHORT)
                .longOpt(OPT_LOGFRAMEWORK_LONG)
                .desc("Agent logging framework selection (e.g., log4j2, logback.")
                .hasArg()
                .argName("LOG FRAMEWORK")
                .build();
        opN.setDescription("LOG FRAMEWORK is the agent logging framework selection for jvmxray event logging. "+
                           "The options are 'log4j2' or 'logback'.  The default is logback.");
        options.addOption(opN);
        // DEBUG UI option
        // AGENT PAYLOAD option
        opN = Option.builder(OPT_DEBUGUI_SHORT)
                .longOpt(OPT_DEBUGUI_LONG)
                .desc("Open debug UI, when not operating headless.")
                .build();
        opN.setDescription("The debug UI is useful for initial setup and tool development in Integrated Development Environments.");
        options.addOption(opN);
        // Parse command line.
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            // Check for help option
            if (cmd.hasOption(OPT_HELP_LONG)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(TITLE, options);
                return;
            }
            // Agent payload option
            if (cmd.hasOption(OPT_AGENTPAYLOAD_LONG)) {
                agentPayload = cmd.getOptionValue(OPT_AGENTPAYLOAD_LONG);
                System.out.println("jvmxrayinjector: Agent payload assigned from command line. agentPayload=" + agentPayload);
            }
            // Agent logging framework preference.
            if (cmd.hasOption(OPT_LOGFRAMEWORK_LONG)) {
                agentLoggingFramework = cmd.getOptionValue(OPT_LOGFRAMEWORK_LONG);
                System.out.println("jvmxrayinjector: Agent loggign framework assigned from command line. agentLoggingFramework=" + agentLoggingFramework);
            }
            // Ensure that either the pid-file or pid-id option is provided, but not both
            if (!(cmd.hasOption(OPT_PIDFILE_LONG) ^ cmd.hasOption(OPT_PIDID_LONG))) {
                System.err.println("jvmxrayinjector: You must specify either a PID file or a PID ID, but not both.");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(TITLE, options);
                return;
            }
            // Handle pid-file and pid-id options
            if (cmd.hasOption(OPT_PIDFILE_LONG)) {
                pidFile = cmd.getOptionValue(OPT_PIDFILE_LONG);
                System.out.println("jvmxrayinjector: PID file assigned from command line.   file=" + pidFile);
            } else if (cmd.hasOption(OPT_PIDID_LONG)) {
                String sPid = cmd.getOptionValue(OPT_PIDID_LONG);
                pidId = Long.valueOf(sPid);
                System.out.println("jvmxrayinjector: PID value assigned from command line.  PID=" + pidId);
            }
            // Open the debug UI?
            if (cmd.hasOption(OPT_DEBUGUI_LONG)) {
                bDebug = true;
            }

        } catch (ParseException e) {
            System.err.println("jvmxrayinjector: command line parse error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(TITLE, options);
        }

    }

    /**
     * Retrieve a PID from a *NIX style PID file.
     * @param filename Fully qualified standard PID file.
     * @return PID from target file.
     * @throws IOException Thrown on problems reading target PID file.
     */
    public static Long getPidFromFile(String filename) throws IOException {
        BufferedReader reader = null;
        Long result = -1L;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String pidString = reader.readLine();
            result = Long.valueOf(pidString);
        } finally {
            if( reader!=null ) reader.close();
        }
        return result;
    }

    /**
     * Opens a simple GUI useful during debug/configuration.  Not used
     * during normal headless operation like production operations for
     * example.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("JVMXRay - Injector Debug");
        primaryPanel = new JVMXRayPrimaryPanel() {
            @Override
            public List<Object[]> getProcessInformation() {
                List<Object[]> processInformation = new ArrayList<>();
                List<ProcessHandle> processes = ProcessHandle.allProcesses().toList();
                for (ProcessHandle process : processes) {
                    String commandLine = process.info().commandLine().orElse("");
                    String wstype=IdWebServer.supportedWebServerType(commandLine);
                    if (wstype!=null) {
                        long pid = process.pid();
                        String cmdOnly = commandLine.split(" ")[0];
                        processInformation.add(new Object[]{wstype, pid, cmdOnly});
                    }
                }
                return processInformation;
            }
        };
        primaryPanel.setAgentPayload(agentPayload);
        primaryPanel.setAgentLoggingFramework(agentLoggingFramework);
        primaryPanel.attachWindowListener(frame);
        Dimension screenlocation = primaryPanel.getSavedLocation();
        frame.setLocation(screenlocation.width,screenlocation.height);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(primaryPanel);
        frame.pack();
        frame.setVisible(true);
    }


}
