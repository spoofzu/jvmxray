package org.jvmxray.platform.agent.bin;

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
import org.jvmxray.platform.agent.ui.injector.XRPrimaryPanel;
import org.jvmxray.platform.agent.ui.injector.XRScreenLocator;
import org.jvmxray.platform.agent.util.XRIdentifyWebServer;
import org.jvmxray.platform.shared.property.XRPropertyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Executable for injecting JVMXRay Agent dynamically into
 * a running process to be monitoried.
 *
 * @author Milton Smith
 */
public class jvmxrayinjector  {

    //TODO:  Needs to be improved to use updated jvmxray properties, XRPropertyBase.

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
    private XRPrimaryPanel primaryPanel = null;
    private String agentPayload = "";
    private String agentBasePath = "";
    private String pidFile = "";
    private Long pidId = Long.valueOf(-1);
    private boolean bDebug = false;
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.agent.bin.jvmxrayinjector");

    /**
     * Java entry point.
     * @param args Program command line arguments.
     */
    public static final void main(final String[] args) {
        jvmxrayinjector injector = new jvmxrayinjector();
        try {
            // Execute
            injector.process(args);
        } catch(Throwable t) {
            logger.error("Unhandled exception.  msg="+t.getMessage(), t);
            System.exit(10);
        }
    }

    /**
     * Opens a GUI if debugging or process command line arguments.
     * @param args Arguments from command line.
     * @throws Exception Thrown on problems.
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
        if (agentBasePath == null || agentBasePath.length()<1 ) {
            throw new Exception("No agentbasepath specified.");
        }
        logger.debug("Agent base path assigned. path={}", agentBasePath);
        logger.info("Debug log session started.");
        // Process remainder of command line options now that simple logging is up.
        processLateCmdArgs(cmd, options);

        // Headed configuration, use desktop debug GUI.
        if(bDebug) {
            // Required by XRPropertyFactrory to find injectors properties.
            System.setProperty("jvmxray.base",agentBasePath);
            XRPropertyFactory.init();
            createAndShowGUI();
            primaryPanel.setJvmxRaySettingsListener(map -> {
                try {
                    Long pid = (Long)map.get("pid");
                    logger.info("PID selection from debug GUI. pid=" + pid);
                    agentPayload = (String)map.get("agentPayload");
                    logger.info("Agent payload selection from debug GUI. agentPayload={}", agentPayload);
                    injectAgent(pid);
                }catch(Exception e) {
                    logger.error("Error injecting agent into target. msg="+e.getMessage(), e);
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
                throw new Exception("No process id (e.g., PID) assigned. pidId="+pidId);
            }
            if( agentPayload ==null || agentPayload.length()<1 ) {
                throw new Exception("No agent injection payload specified. agentPayload="+ agentPayload);
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
            agentBasePath = cmd.getOptionValue(OPT_AGENTBASEPATH_SHORT);
        } else {
            agentBasePath = "";
            System.out.println("Agent base path, -ab --agentbasepath, is a required option.");
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
                agentPayload = cmd.getOptionValue(OPT_AGENTPAYLOAD_SHORT);
                logger.info("Agent payload assigned. agentPayload={}", agentPayload);
            }
        // CLI Options
        } else {
            // Check that either PID file or PID value is provided, but not both.
            if ((cmd.hasOption(OPT_PIDFILE_SHORT) && cmd.hasOption(OPT_PIDID_SHORT)) ||
                    (!cmd.hasOption(OPT_PIDFILE_SHORT) && !cmd.hasOption(OPT_PIDID_SHORT))) {
                logger.info("You must specify either a PID file or a PID value, but not both.");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar jvmxrayinjector.jar", options);
                return;
            }
            if (cmd.hasOption(OPT_PIDFILE_SHORT)) {
                pidFile = cmd.getOptionValue(OPT_PIDFILE_SHORT);
                logger.info("Agent PID file assigned. file={}",pidFile);
            } else {
                String sPid = cmd.getOptionValue(OPT_PIDID_SHORT);
                pidId = Long.parseLong(sPid);
                logger.info("Agent PID value assigned. pidId={}",pidId);
            }
            if (cmd.hasOption(OPT_AGENTPAYLOAD_SHORT)) {
                agentPayload = cmd.getOptionValue(OPT_AGENTPAYLOAD_SHORT);
                logger.info("Agent payload assigned. agentPayload={}",agentPayload);
            }
        }
    }

    /**
     * Retrieve a PID from a *NIX style PID file.
     * @param filename Fully qualified standard PID file.
     * @return PID from target file.
     * @throws IOException Thrown on problems reading target PID file.
     */
    private static Long getPidFromFile(String filename) throws IOException {
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
        File agentJar = new File(agentPayload);
        absolutepath = agentJar.getAbsolutePath();
        if(agentJar==null) {
            throw new IOException("Missing or corrupt agent jar file specified. file="+ agentPayload);
        }
        if( agentJar.exists() ) {
            logger.info("Agent JAR assigned. absolutepath={}",absolutepath);
        } else {
            throw new IOException("Agent JAR not found. absolutepath="+absolutepath);
        }
        if( !agentJar.canRead() ) {
            throw new IOException("Agent JAR is not readable.  Check file/dir permissions. absolutepath="+absolutepath);
        }
        String spid = Long.toString(pid);
        VirtualMachine vm = null;
        try {
            logger.info("Attaching to JVM. spid={}",spid );
            vm = VirtualMachine.attach(spid);
        }catch(Exception e) {
            logger.info("Problem attaching to JVM.  Check PID and try again.  spid={} msg={}",spid,e.getMessage());
            throw e;
        }
        Properties p = new Properties();
        p.setProperty("absolutepath",absolutepath);
        p.setProperty("agentbasepath", agentBasePath);
        p.setProperty("spid",spid);
        StringWriter wout = new StringWriter();
        p.store(wout,null);
        String params = wout.toString();
        try {
            logger.info("Loading agent.  params={}",params);
            vm.loadAgent(absolutepath, params);
            logger.info("Agent successfully loaded.");
        }catch(Exception e) {
            logger.error("Problem loading Java agent.  Check injection payload (e.g., jar).  params={}",params);
            throw e;
        }
        try {
            logger.info("Detaching from JVM.");
            vm.detach();
            logger.info("Successfully detached from JVM.");
        } catch( IOException e) {
            logger.error("Warning, failure detaching from JVM.  msg={}",e.getMessage());
        }
    }

    /**
     * Called on process termination.
     */
    public void shutDown() {
        logger.info("JVM shutdown detected. Finishing up.");
    }

    /**
     * Opens a simple GUI useful during debug/configuration.  Not used
     * during normal headless operation like production operations for
     * example.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("JVMXRay - Injector Debug");
        primaryPanel = new XRPrimaryPanel() {
            @Override
            public List<Object[]> getProcessInformation() {
                List<Object[]> processInformation = new ArrayList<>();
                List<ProcessHandle> processes = ProcessHandle.allProcesses()
                        .collect(Collectors.toList());
                for (ProcessHandle process : processes) {
                    String commandLine = process.info().commandLine().orElse("");
                    if(XRIdentifyWebServer.isJavaProcess(commandLine)) {
                        String wstype= XRIdentifyWebServer.getSupportedWebServerType(commandLine);
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
        primaryPanel.setAgentBasePath(agentBasePath);
        primaryPanel.setAgentPayload(agentPayload);
        primaryPanel.attachWindowListener(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(primaryPanel);
        frame.pack();
        // Can't call primaryPanel.restoreScreenLocation() since frame is not yet visible.
        XRScreenLocator locator = null;
        try {
            locator = primaryPanel.restoreApplicationSettings();
        } catch (Exception e) {
            logger.error("Unable to restore injector property settings", e);
        }
        frame.setLocation(locator.getX(),locator.getY());
        frame.setSize(locator.getWidth(),locator.getHeight());
        frame.setVisible(true);
    }



}
