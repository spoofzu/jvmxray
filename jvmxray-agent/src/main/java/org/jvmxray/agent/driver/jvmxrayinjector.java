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
import org.jvmxray.agent.log.SimpleLogging;
import org.jvmxray.agent.ui.JVMXRayPrimaryPanel;
import org.jvmxray.agent.ui.ScreenLocator;
import org.jvmxray.agent.util.IdWebServer;

import javax.swing.JFrame;
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
    private static final String OPT_AGENTPAYLOAD_SHORT = "ap";
    private static final String OPT_AGENTPAYLOAD_LONG = "agentpayload";
    private static final String OPT_DEBUGUI_SHORT = "du";
    private static final String OPT_DEBUGUI_LONG = "debugui";
    private static final String OPT_AGENTBASEPATH_SHORT = "ab";
    private static final String OPT_AGENTBASEPATH_LONG = "agentbasepath";
    private JVMXRayPrimaryPanel primaryPanel = null;
    private String agentpayload = "";
    private String agentbasepath = "";
    private String pidFile = "";
    private Long pidId = Long.valueOf(-1);
    private boolean bDebug = false;
    private SimpleLogging simplelogger = SimpleLogging.getInstance();

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
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread( ()->{
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        // Define command line options and parameters.
        Options options = new Options();
        defineCmdOptions(args, options);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        processEarlyCmdArgs(cmd, options);
        // agentbasepath required, if parameter missing then exit.
        if (agentbasepath == null || agentbasepath.length()<1 ) {
            throw new Exception("jvmxrayagent: No agentbasepath specified.  agentbasepath="+agentbasepath);
        }
        // Initialize simple logger.
        File logpath = new File(agentbasepath, "logs");
        File debuglog = new File(logpath,"jvmxrayinjectordebug.txt");
        simplelogger.defineLog(debuglog,true);
        simplelogger.log("jvmxrayinjector: Debug log session started.");
        // Process remainder of command line options now that simple logging is up.
        processLateCmdArgs(cmd, options);

        // Headed configuration, use desktop debug GUI.
        if(bDebug) {
            createAndShowGUI();
            primaryPanel.setJvmxRaySettingsListener(map -> {
                try {
                    Long pid = (Long)map.get("pid");
                    System.out.println("jvmxrayinjector: PID selection from debug GUI. pid=" + pid);
                    agentpayload = (String)map.get("agentPayload");
                    simplelogger.log("jvmxrayinjector: Agent payload selection from debug GUI. agentPayload=" + agentpayload);
                    injectAgent(pid);
                }catch(Exception e) {
                    simplelogger.log("jvmxrayinjector: Error injecting agent into target. msg="+e.getMessage());
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
            if( agentpayload ==null || agentpayload.length()<1 ) {
                throw new Exception("jvmxrayinjector: No agent injection payload specified. agentPayload="+ agentpayload);
            }
            // All appropriate options processed.  Attempt to inject agent.
            injectAgent(pidId);
        }

    }

    /**
     * Define the command line arguments and parameters.
     * @param args Arguments from the commandline.
     * @param options Options builder.
     */
    private void defineCmdOptions(String[] args, Options options) {
        // HELP Option
        Option helpOption = Option.builder(OPT_HELP_SHORT)
                .longOpt(OPT_HELP_LONG)
                .build();
        options.addOption(helpOption);
        // PID FILE option
        Option pidFileOption = Option.builder(OPT_PIDFILE_SHORT)
                .longOpt(OPT_PIDFILE_LONG)
                .desc("Path to the file containing the PID of the target process.")
                .hasArg()
                .argName("PID FILE")
                .build();
        options.addOption(pidFileOption);
        // PID value option
        Option pidOption = Option.builder(OPT_PIDID_SHORT)
                .longOpt(OPT_PIDID_LONG)
                .desc("PID of the target process.")
                .hasArg()
                .argName("PID VALUE")
                .build();
        options.addOption(pidOption);
        // AGENT PAYLOAD option
        Option agentPayloadOption = Option.builder(OPT_AGENTPAYLOAD_SHORT)
                .longOpt(OPT_AGENTPAYLOAD_LONG)
                .desc("Path to the agent payload JAR file.")
                .hasArg()
                .argName("JAR FILE")
                .build();
        options.addOption(agentPayloadOption);
        // DEBUGUI option.
        Option debugUiOption = Option.builder(OPT_DEBUGUI_SHORT)
                .longOpt(OPT_DEBUGUI_LONG)
                .desc("Open the debug UI.")
                .build();
        options.addOption(debugUiOption);
        // AGENT base path option
        Option agentBasePathOption = Option.builder(OPT_AGENTBASEPATH_SHORT)
                .longOpt(OPT_AGENTBASEPATH_LONG)
                .desc("Base path used by the agent.")
                .hasArg()
                .argName("PATH")
                .build();
        options.addOption(agentBasePathOption);
    }

    private void processEarlyCmdArgs(CommandLine cmd, Options options) {
        // Help options
        if (cmd.hasOption(OPT_HELP_SHORT)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar jvmxrayinjector.jar", options);
            return;
        }
        // Agent base is required.
        if (cmd.hasOption(OPT_AGENTBASEPATH_SHORT)) {
            agentbasepath = cmd.getOptionValue(OPT_AGENTBASEPATH_SHORT);
            System.out.println("jvmxrayinjector: Agent base path assigned. path=" + agentbasepath);
        } else {
            agentbasepath = "";
            System.out.println("jvmxrayinjector: Agent base path, -ab --agentbasepath, is a required option.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar jvmxrayinjector.jar", options);
            return;
        }
    }

    private void processLateCmdArgs(CommandLine cmd, Options options) {
        // DEBUG UI options
        if (cmd.hasOption(OPT_DEBUGUI_SHORT)) {
            bDebug = true;
            if (cmd.hasOption(OPT_AGENTPAYLOAD_SHORT)) {
                agentpayload = cmd.getOptionValue(OPT_AGENTPAYLOAD_SHORT);
                simplelogger.log("jvmxrayinjector: Agent payload assigned. agentPayload=" + agentpayload);
            }
        // CLI Options
        } else {
            // Check that either PID file or PID value is provided, but not both.
            if ((cmd.hasOption(OPT_PIDFILE_SHORT) && cmd.hasOption(OPT_PIDID_SHORT)) ||
                    (!cmd.hasOption(OPT_PIDFILE_SHORT) && !cmd.hasOption(OPT_PIDID_SHORT))) {
                simplelogger.log("jvmxrayinjector: You must specify either a PID file or a PID value, but not both.");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar jvmxrayinjector.jar", options);
                return;
            }
            if (cmd.hasOption(OPT_PIDFILE_SHORT)) {
                pidFile = cmd.getOptionValue(OPT_PIDFILE_SHORT);
                simplelogger.log("jvmxrayinjector: Agent PID file assigned. file=" + pidFile);
            } else {
                String sPid = cmd.getOptionValue(OPT_PIDID_SHORT);
                pidId = Long.parseLong(sPid);
                simplelogger.log("jvmxrayinjector: Agent PID value assigned. pidId=" + pidId);
            }
            if (cmd.hasOption(OPT_AGENTPAYLOAD_SHORT)) {
                agentpayload = cmd.getOptionValue(OPT_AGENTPAYLOAD_SHORT);
                simplelogger.log("jvmxrayinjector: Agent payload assigned. agentPayload=" + agentpayload);
            }
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
     * Inject a Java agent payload into another process.
     * @param pid Process ID of an executing process.  Usually, a web server but could be any Java process.
     * @throws AgentLoadException Thrown by JVM on agent load problems.
     * @throws IOException Throw on problems with the agent payload (e.g., Jar file).
     * @throws AgentInitializationException Thrown by JVM on agent load problems.
     * @throws AttachNotSupportedException Thrown by JVM on agent load problems.
     */
    private void injectAgent(Long pid) throws IOException, AgentLoadException, AttachNotSupportedException, AgentInitializationException {
        String absolutepath = "";
        File agentJar = new File(agentpayload);
        absolutepath = agentJar.getAbsolutePath();
        if(agentJar==null) {
            throw new IOException("Missing or corrupt agent jar file specified. file="+ agentpayload);
        }
        if( agentJar.exists() ) {
            simplelogger.log("jvmxrayinjector: Agent JAR assigned. absolutepath="+absolutepath);
        } else {
            throw new IOException("Agent JAR not found. absolutepath="+absolutepath);
        }
        if( !agentJar.canRead() ) {
            throw new IOException("Agent JAR is not readable.  Check file/dir permissions. absolutepath="+absolutepath);
        }
        String spid = Long.toString(pid);
        VirtualMachine vm = null;
        try {
            simplelogger.log("jvmxrayinjector: Attaching to JVM. spid=" + spid );
            vm = VirtualMachine.attach(spid);
        }catch(Exception e) {
            simplelogger.log("jvmxrayinjector: Problem attaching to JVM.  Check PID and try again.  spid="+spid+" msg="+e.getMessage());
            throw e;
        }
        try {
            simplelogger.log("jvmxrayinjector: Loading agent.  absolutepath="+absolutepath);
            vm.loadAgent(absolutepath, "agentbasepath=" + agentbasepath);
            simplelogger.log("jvmxrayinjector: Agent successfully loaded.");
        }catch(Exception e) {
            simplelogger.log("jvmxrayinjector: Problem loading Java agent.  Check injection payload (e.g., jar).  absolutepath="+absolutepath);
            throw e;
        }
        try {
            simplelogger.log("jvmxrayinjector: Detaching from JVM.");
            vm.detach();
            simplelogger.log("jvmxrayinjector: Successfully detached from JVM.");
        } catch( IOException e) {
            simplelogger.log("jvmxrayinjector: Warning, failure detaching from JVM.  msg="+e.getMessage());
        }
    }

    public void shutDown() {
        // Shutdown gracefully
        simplelogger.log("jvmxrayinjector: JVM shutdown detected. Finishing up.");
    }

    /**
     * Opens a simple GUI useful during debug/configuration.  Not used
     * during normal headless operation like production operations for
     * example.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("JVMXRay - Injector Debug");
        primaryPanel = new JVMXRayPrimaryPanel(simplelogger) {
            @Override
            public List<Object[]> getProcessInformation() {
                List<Object[]> processInformation = new ArrayList<>();
                List<ProcessHandle> processes = ProcessHandle.allProcesses().toList();
                for (ProcessHandle process : processes) {
                    String commandLine = process.info().commandLine().orElse("");
                    if(IdWebServer.isJavaProcess(commandLine)) {
                        String wstype=IdWebServer.getSupportedWebServerType(commandLine);
                        String cmdOnly = commandLine.split(" ")[0];
                        if (wstype==null ) {
                            wstype="";
                        }
                        long pid = process.pid();
                        processInformation.add(new Object[]{wstype, pid, cmdOnly});
                    }
                }
                return processInformation;
            }
        };
        primaryPanel.setAgentBasePath(agentbasepath);
        primaryPanel.setAgentPayload(agentpayload);
        primaryPanel.attachWindowListener(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(primaryPanel);
        frame.pack();
        // Can't call primaryPanel.restoreScreenLocation() since frame is not yet visible.
        ScreenLocator locator = primaryPanel.getSavedLocation();
        frame.setLocation(locator.getX(),locator.getY());
        frame.setSize(locator.getWidth(),locator.getHeight());
        frame.setVisible(true);
    }



}
