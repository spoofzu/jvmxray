package org.jvmxray.agent.driver;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Agent payload to injected into processes by jvmxrayinjector.
 * @author Milton Smith
 */
public class jvmxrayagent  {

    /**
     * Called by Java when injecting agent statically via command line switch.
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent: premain() initialized.");
        initialize(agentArgs,inst);
    }

    /**
     * Called when injecting dynamcially via VirtualMachine.attach().
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent: agentmain() initialized.");
        initialize(agentArgs,inst);
    }

    /**
     * Initilaization for agent.
     * @param agentArgs Passed by JVM.
     * @param inst Passed by JVM.
     */
    private static void initialize(String agentArgs, Instrumentation inst) {
        // NOTE: At the moment, the jvmxrayagent does not use Transformers; however,
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
            loadSecurityManager(agentArgs);
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * This code loads the jvmxraysecuritymanager and the prefered logging platform
     * the jvmxraysecuritymanager should use.
     */
    private static void loadSecurityManager(String agentArgs) {
        // If user provides logging framework we use it otherwise default to logback.
        String agentLoggingFramework = "log4j2";  // logback or log4j2(default)
        String[] split = agentArgs.split("=");
        if (split!=null) {
            if( split.length > 1 ) {
                agentLoggingFramework = split[1];
            }
        }
        SecurityManager currentSM = System.getSecurityManager();
        if (currentSM == null || currentSM instanceof jvmxraysecuritymanager ) {
            final String log4j2cfile = "log42.xml";
            final String logbackcfile = "logback.xml";
            final String log4jxmlconfig   = "/Users/milton/jvmxray-agent/"+log4j2cfile;
            final String logbackxmlconfig = "/Users/milton/jvmxray-agent/"+logbackcfile;

            // Inject the securitymanager.
            System.setSecurityManager(new jvmxraysecuritymanager());

            // Log current framework in use on target diagnostic purposes.
            String configMsg = "";
            String fileName = "";
            String clzPath = "";
            String logbackconf = System.getProperty("logback.configurationFile");
            String log4j2conf = System.getProperty("log4j.configurationFile");
            if( logbackconf != null ) {
                configMsg = "jvmxrayagent: preexistconfig ing config, sys:logback.configurationFile=";
                clzPath = "jvmxrayagent: logback config loaded from classpath="+jvmxraysecuritymanager.class.getResource("/"+logbackcfile).toString();
                fileName = logbackconf;
            } else {
                configMsg = "jvmxrayagent: no preexisting logback configuration. sys:logback.configurationFile=null";
                fileName = "";
                clzPath = "";
            }
            System.out.println(configMsg+fileName);
            System.out.println(clzPath);
            if( log4j2conf != null ) {
                configMsg = "jvmxrayagent: preexisting config, sys:log4j.configurationFile=";
                clzPath = "jvmxrayagent: log4j2 config loaded from classpath="+jvmxraysecuritymanager.class.getResource("/"+log4j2cfile).toString();
                fileName = log4j2conf;
            } else {
                configMsg = "jvmxrayagent: no preexisting log4j2 configuration. sys:log4j.configurationFile=null";
                fileName = "";
                clzPath = "";
            }
            System.out.println(configMsg+fileName);
            System.out.println(clzPath);

            // Set slf4j logging preference implementation on the securitymanager.
            if( agentLoggingFramework!= "logback" ) {
                // Load logback configuration file
                System.setProperty("logback.configurationFile",logbackxmlconfig);
                System.out.println("jvmxraysecuritymanager logging configuration set to logback. logback.configurationFile="+logbackxmlconfig);
            } else if( agentLoggingFramework!= "log4j2") {
                // Load log4j2 configuration file
                System.setProperty("log4j.configurationFile",log4jxmlconfig);
                System.out.println("jvmxraysecuritymanager logging configuration set to log4j2. log4j.configurationFile="+log4jxmlconfig);
            } else {
                // No logging agent logging specified.  Use default client config, if available.
                System.out.println("jvmxrayagent: no preexisting log4j2 configuration.  Using application default logging.");
            }
        } else if (currentSM instanceof java.lang.SecurityManager) {
            System.out.println("jvmxrayagent: process already implements a securitymanager.  Not compatible with JVMXRay.  Impl="+currentSM.getClass().getName());
        } else {
            throw new IllegalStateException("Error: unable load jvmxray security manager.  Security manager already installed.  type= " + currentSM.getClass().getName());
        }
    }

}
