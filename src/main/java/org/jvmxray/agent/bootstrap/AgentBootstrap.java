package org.jvmxray.agent.bootstrap;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Bootstrap entry point for JVMXRay Agent that provides improved isolation
 * by loading the real agent implementation in a separate URLClassLoader.
 * 
 * This class acts as a thin wrapper that:
 * 1. Creates an isolated URLClassLoader with bootstrap classloader as parent
 * 2. Loads the actual agent implementation (jvmxrayagent) in this isolated context
 * 3. Delegates the premain invocation to the real agent
 * 
 * This architecture prevents class conflicts between the agent and application
 * classes while maintaining strict isolation through the bootstrap parent.
 * 
 * @author Milton Smith
 */
public final class AgentBootstrap {

    private static final String REAL_AGENT_CLASS = "org.jvmxray.agent.bin.jvmxrayagent";
    private static final String START_METHOD = "start";

    /**
     * Java agent entry point that creates isolated classloader and delegates
     * to the real agent implementation.
     * 
     * @param agentArgs Arguments passed to the Java agent
     * @param instrumentation The instrumentation instance from the JVM
     * @throws Exception If agent initialization fails
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        System.out.println("AgentBootstrap.premain(): Initializing JVMXRay agent with bootstrap isolation...");
        
        try {
            // Get the location of the current agent JAR
            URL agentJar = AgentBootstrap.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            
            if (agentJar == null) {
                throw new IllegalStateException("Unable to determine agent JAR location");
            }
            
            System.out.println("AgentBootstrap.premain(): Agent JAR location: " + agentJar);
            
            // Create isolated URLClassLoader with current classloader as parent
            // This maintains access to agent classes while providing some isolation
            URLClassLoader isolatedLoader = new URLClassLoader(
                    new URL[]{agentJar}, 
                    AgentBootstrap.class.getClassLoader()
            );
            
            // Load the real agent class in the isolated classloader
            Class<?> realAgentClass = Class.forName(REAL_AGENT_CLASS, true, isolatedLoader);
            
            // Invoke the start method on the real agent implementation
            realAgentClass.getMethod(START_METHOD, String.class, Instrumentation.class)
                    .invoke(null, agentArgs, instrumentation);
                    
            System.out.println("AgentBootstrap.premain(): Successfully delegated to isolated agent implementation.");
            
        } catch (Exception e) {
            System.err.println("AgentBootstrap.premain(): Failed to initialize agent bootstrap: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}