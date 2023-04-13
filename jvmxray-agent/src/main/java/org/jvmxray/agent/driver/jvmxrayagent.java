package org.jvmxray.agent.driver;

import org.jvmxray.agent.log.SimpleLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;


/**
 * JVMXRay agent that runs in a target to load a securitymanager
 * dynamically.  Keep in mind, although the securitymanager is
 * injected dynmaically the classpath must contain the logging
 * libraries you plan to use or alterantively you can use
 * the logging framework provided by your server (if one is
 * installed in the classpath by default).  If no logging frameworks
 * are included on the target you can use the jar with
 * dependencies created by the agent build which includes slf4j
 * bindings for logback by default.
 * @author Milton Smith
 */
public class jvmxrayagent  {

    private SimpleLogging simplelogger;

    public jvmxrayagent(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent: Agent initialization arguments, agentArgs="+agentArgs);
        initSimpleLogging(agentArgs);
        // Register shutdownhook.  Stop tasks on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread( ()->{
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        agentInitialize(agentArgs,inst);
        simplelogger.log("jvmxrayagent: Agent successfully deployed on target.");
        loadSecurityManager(agentArgs);
    }

    public void shutDown() {
        // Shutdown gracefully
        simplelogger.log("jvmxrayagent: JVM shutdown detected. Finishing up.");
    }

    /**
     * Called by Java when injecting agent statically via command line switch.
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent: premain() initialized.");
        new jvmxrayagent(agentArgs, inst);
    }

    /**
     * Called when injecting dynamcially via VirtualMachine.attach().
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent: agentmain() initialized.");
        new jvmxrayagent(agentArgs, inst);
    }

    /**
     * Initilaization for agent.
     * @param agentArgs Passed by JVM.
     * @param inst Passed by JVM.
     */
    private void agentInitialize(String agentArgs, Instrumentation inst) {
        // NOTE: At the moment, jvmxrayagent does not use Transformers; however,
        //       a null transformer is required for the agent to load successfully.
        try {
            inst.addTransformer(new ClassFileTransformer() {
                public byte[] transform(ClassLoader loader, String className,
                                        Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain,
                                        byte[] classfileBuffer) throws IllegalClassFormatException {
                    // Do nothing, return null
                    return null;
                }
            });
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private void initSimpleLogging(String agentArgs) throws Exception {
        if (agentArgs == null || agentArgs.length() < 0) {
            throw new Exception("jvmxrayagent: Bad or missing agentArgs.  agentArgs=" + agentArgs);
        }
        String[] splitArgs = agentArgs.trim().split("=");
        if (splitArgs.length != 2) {
            throw new Exception("jvmxrayagent: Bad agentArgs format. Must be parameter=value.  agentArgs=" + agentArgs);
        }
        // Init simple logger.
        File logpath = new File(splitArgs[1], "logs");
        File debuglog = new File(logpath,"jvmxrayagentdebug.txt");
        simplelogger = SimpleLogging.getInstance();
        simplelogger.defineLog(debuglog, true);
        simplelogger.log("jvmxrayagent: Agent memory resident on target with simple logging.  agentArgs=" + agentArgs);
        // Detect which logging framework in use on client.
        Logger logger = LoggerFactory.getLogger(jvmxrayagent.class);
        String loggerClassName = logger.getClass().getName();
        simplelogger.log("jvmxrayagent: Logging framework on target.  loggerClassName="+loggerClassName);
    }

    /**
     * This code loads the jvmxraysecuritymanager and the prefered logging platform
     * the jvmxraysecuritymanager should use.
     */
    private void loadSecurityManager(String agentArgs) throws Exception {
        // Implement securitymanager mojo
        SecurityManager currentSM = System.getSecurityManager();
        // Allow to inject jvmxray over itself or no security manager in use.
        if (currentSM == null || currentSM instanceof jvmxraysecuritymanager) {
            System.setSecurityManager(new jvmxraysecuritymanager());
        } else {
            String msg = "jvmxrayagent: Security manager is already installed.  JVMXRay incompatible.  currentSM="+currentSM.getClass().getName();
            simplelogger.log(msg);
            throw new Exception(msg);
        }
        simplelogger.log("jvmxrayagent: Securitymanager memory resident on target.  securitymanager="+jvmxraysecuritymanager.class.getName());
    }
}
