package org.jvmxray.platform.agent.bin;


import org.jvmxray.platform.agent.securitymanager.XRLoggingSecurityManager;
import org.jvmxray.platform.shared.classloader.XRLoggingClassLoader;
import org.jvmxray.platform.shared.property.XRPropertyFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.util.Properties;

/**
 * The JVMXRay Agent sets up the environment to deploy a customized securitymanager
 * on a target JVM and load a custom securitymanager dynamically.  Upon successful
 * deployment of the securitymanager the Agent ends normally.  The securitymanager
 * on the target process is used to monitoring access to protected
 * resources.  Configuring jvmxray monitoring/event selection is handled via standard
 * logback configuration.
 *
 * @author Milton Smith
 */
public class jvmxrayagent  {

    /**
     * agentmain is called when attaching dynamically to a running process to
     * be monitored.
     * @param agentArgs Passed by VM.  Must provide serialized java properties with,
     *                  at a minimum, <code>agentbasepath</code> assigned.  The
     *                  value provided by agentbasepath will be used as ${jvmxray.base}
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent.agentmain(): initializing.");
        if( agentArgs == null || agentArgs.length()<1 ) {
            throw new Exception("jvmxrayagent.agentmain(): agentArgs Properties missing.");
        }
        Properties properties = new Properties();
        StringReader reader = new StringReader(agentArgs);
        try {
            properties.load(reader);
        } catch (IOException e ) {
            throw new IOException("jvmxrayagent.agentmain(): can't deserialize properties",e);
        }
        if( !properties.contains("agentbasepath") ) {
            throw new IOException("jvmxrayagent.agentmain(): agentArgs serialized properties must contain an agentbasepath key/pair assignement.");
        }
        // jvmxray.base is used by XRPropertyFactory.init() to initilize property files
        //    and logging.
        System.setProperty("jvmxray.base",properties.getProperty("agentbasepath"));
        init(agentArgs,  inst);
        System.out.println("jvmxrayagent.agentmain(): intialization complete.");
    }

    /**
     * premain is called when the agent is loaded staticallly via command line
     * switch on the target process to me monitored.
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("jvmxrayagent.agentmain(): initializing.");
        init(agentArgs,  inst);
        System.out.println("jvmxrayagent.agentmain(): intialization complete.");
    }

    /**
     * Deploy custom security manager to monitor target java process.
     * @param agentArgs Passed by VM.
     * @param inst Passed by VM.
     * @throws Exception Thrown on undetermined error.
     */
    private static void init(String agentArgs, Instrumentation inst) throws Exception {
        SecurityManager currentSM = System.getSecurityManager();
        if (currentSM == null) {
            // Initialize properties and logging for JVMXRay.   jvmxray.base should
            // be assigned to the path where jvmxray will create it's home,
            // ${jvmxmray.base}/jvmxray.  If jvmxray.base is undefined, the home
            // directory of the user running this process (user.home) is used instead,
            // ${user.home}/jvmxray
            // Logback logging cannot be used prior to XRPropertyFactory.init().
            //
            XRPropertyFactory.init();
            //
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader classLoader = new XRLoggingClassLoader("xray",originalClassLoader);
            Thread.currentThread().setContextClassLoader(classLoader);
            System.setSecurityManager(new XRLoggingSecurityManager());
        } else {
            throw new Exception("SecurityManager already assigned. sm=" + currentSM.getClass().getName());
        }
    }


}
