package org.jvmxray.agent.bin;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.agent.sensor.system.AppInitSensor;
import org.jvmxray.agent.sensor.http.HttpSensor;
import org.jvmxray.agent.sensor.io.FileIOSensor;
import org.jvmxray.agent.sensor.monitor.MonitorSensor;
import org.jvmxray.agent.sensor.system.LibSensor;
import org.jvmxray.agent.util.sensor.SensorUtils;
import org.jvmxray.platform.shared.log.JVMXRayLogFactory;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.property.PropertyFactory;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * JVMXRay Agent class responsible for initializing and managing runtime sensors
 * to monitor and instrument Java applications using Byte Buddy.
 *
 * @author Milton Smith
 */
public class jvmxrayagent {

    private static final String EOL = System.lineSeparator();
    // ASCII art banner for JVMXRay
    private static final String BANNER =
            "" + EOL +
                    "      ██╗██╗   ██║███╗   ███╗██╗  ██╗██████╗  █████╗ ██╗   ██╗" + EOL +
                    "      ██║██║   ██║████╗ ████║╚██╗██╔╝██╔══██╗██╔══██╗╚██╗ ██╔╝" + EOL +
                    "      ██║██║   ██║██╔████╔██║ ╚███╔╝ ██████╔╝███████║ ╚████╔╝ " + EOL +
                    " ██   ██║╚██╗ ██╔╝██║╚██╔╝██║ ██╔██╗ ██╔══██╗██╔══██║  ╚██╔╝  " + EOL +
                    " ╚█████╔╝ ╚████╔╝ ██║ ╚═╝ ██║██╔╝ ██╗██║  ██║██║  ██║   ██║   " + EOL +
                    "  ╚════╝   ╚═══╝  ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   " + EOL +
                    "Permission to modify and redistribute is granted under the" + EOL +
                    " terms as described within the project LICENSE and" + EOL +
                    " NOTICE files.";

    // Logger for platform-wide logging
    private static Logger platformLogger;
    // List of registered sensors
    private static final List<Sensor> sensors = new ArrayList<>();

    /**
     * Entry point for the JVMXRay agent, called by the JVM during startup.
     * Initializes properties, logging, sensors, and Byte Buddy instrumentation.
     *
     * @param agentArgs Arguments passed to the agent.
     * @param instrumentation The instrumentation instance provided by the JVM.
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        // Display startup banner
        System.out.println(BANNER);
        System.out.println("jvmxrayagent.premain(): Agent started.");
        try {
            // Initialize property factory
            PropertyFactory propertyFactory = PropertyFactory.getInstance();
            propertyFactory.init();
            AgentProperties properties = propertyFactory.getAgentProperties();

            // Initialize logging
            JVMXRayLogFactory logFactory = JVMXRayLogFactory.getInstance();
            logFactory.init(propertyFactory.getJvmxrayHome());
            platformLogger = logFactory.getLogger("org.jvmxray.agent.bin.jvmxrayagent");
            platformLogger.info(BANNER);
            platformLogger.info("Agent started.");

            // Register sensors
            // TODO: Implement dynamic sensor loading
            sensors.add(new HttpSensor());
            sensors.add(new FileIOSensor());
            sensors.add(new MonitorSensor());
            sensors.add(new AppInitSensor());
            sensors.add(new LibSensor());
            // sensors.add(new SQLSensor()); // TODO: SQLSensor implementation incomplete
            // sensors.add(new SocketSensor()); // TODO: SocketSensor implementation incomplete
            // sensors.add(new UncaughtExceptionSensor()); // TODO: UncaughtExceptionSensor implementation incomplete

            // Initialize all registered sensors
            for (Sensor sensor : sensors) {
                try {
                    sensor.initialize(properties, agentArgs, instrumentation);
                    platformLogger.info("Initialized sensor: " + sensor.getName());
                } catch (Exception e) {
                    platformLogger.error("Failed to initialize sensor " + sensor.getName() + ": " + e.getMessage());
                }
            }

            // Collect injectable sensors for Byte Buddy instrumentation
            List<InjectableSensor> injectableSensors = new ArrayList<>();
            for (Sensor sensor : sensors) {
                if (sensor instanceof InjectableSensor) {
                    injectableSensors.add((InjectableSensor) sensor);
                }
            }

            // Inject classes into bootstrap class loader
            Map<String, Class<?>> classMap = new HashMap<>();
            for (InjectableSensor sensor : injectableSensors) {
                Class<?>[] classes = sensor.inject();
                for (Class<?> clazz : classes) {
                    classMap.put(clazz.getName(), clazz);
                }
            }
            Class<?>[] uniqueClasses = classMap.values().toArray(new Class[0]);
            SensorUtils.injectClasses(instrumentation, "org.jvmxray.agent", uniqueClasses);
            platformLogger.debug("Injected " + uniqueClasses.length + " unique classes into bootstrap loader.");

            // Apply transformations for each injectable sensor
            for (InjectableSensor sensor : injectableSensors) {
                try {
                    // Configure Byte Buddy agent builder
                    AgentBuilder builder = new AgentBuilder.Default()
                            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                            .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                            .ignore(ElementMatchers.nameStartsWith("org.jvmxray.agent"));

                    Transform[] transforms = sensor.configure();
                    if (transforms.length == 0) {
                        platformLogger.warn("No transformations configured for sensor: " + sensor.getName());
                        continue;
                    }

                    // Apply each transformation
                    for (Transform transform : transforms) {
                        builder = builder.type(ElementMatchers.is(transform.getTargetClass()))
                                .transform((bldr, typeDesc, classLoader, module, protectionDomain) -> {
                                    DynamicType.Builder<?> intermediate = bldr;
                                    for (MethodSpec method : transform.getMethods()) {
                                        intermediate = intermediate.visit(
                                                Advice.to(transform.getInterceptor())
                                                        .on(named(method.getMethodName())
                                                                .and(takesArguments(method.getParameterTypes())))
                                        );
                                    }
                                    return intermediate;
                                });
                        platformLogger.info("Configured transform for " + transform.getTargetClass().getName() +
                                " in sensor " + sensor.getName());
                    }

                    // Add listener for transformation events
                    builder.with(new AgentBuilder.Listener() {
                        @Override
                        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                                     boolean loaded, DynamicType dynamicType) {
                            platformLogger.info("Transformed " + typeDescription.getName() + " for sensor " + sensor.getName());
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                              boolean loaded) {}

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                                            Throwable throwable) {
                            platformLogger.error("Error transforming " + typeName + " for sensor " + sensor.getName() +
                                    ": " + throwable.getMessage());
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}
                    }).installOn(instrumentation);

                    platformLogger.info("Transformers installed for sensor: " + sensor.getName());
                } catch (Exception e) {
                    platformLogger.error("Failed to install transformers for sensor " + sensor.getName() +
                            ": " + e.getMessage(), e);
                }
            }

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(jvmxrayagent::shutdown, "jvmxrayagent.shutdownhook"));
            platformLogger.info("Agent premain initialization complete. Agent active.");
            System.out.println("jvmxray.premain(): Agent premain initialization complete. Agent active.");
        } catch (Exception e) {
            System.err.println("jvmxrayagent.premain(): Agent premain failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shuts down the JVMXRay agent, cleaning up resources and shutting down all registered sensors.
     */
    public static void shutdown() {
        // Shut down all sensors
        for (Sensor sensor : sensors) {
            try {
                sensor.shutdown();
                platformLogger.debug("Shut down sensor: " + sensor.getName());
            } catch (Exception e) {
                platformLogger.error("Failed to shut down sensor " + sensor.getName() + ": " + e.getMessage());
            }
        }
        // Shut down logging proxy
        LogProxy.shutdown();
        System.out.println("jvmxrayagent.shutdown(): jvmxrayagent shutting down.");
    }
}