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
import org.jvmxray.agent.util.sensor.SensorUtils;
import org.jvmxray.platform.shared.log.JVMXRayLogFactory;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.platform.shared.property.PropertyFactory;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;

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
        "Permission to modify and redistribute is granted under the" +
        " terms as described within the project LICENSE and" +
        " NOTICE files.";
    private static final String NAMESPACE = "org.jvmxray.agent.bin.jvmxrayagent";
    // Must wait to init logProxy until JVMXRayLogFactory is initialized.
    private static LogProxy logProxy = null;
    private static final List<Sensor> sensors = new ArrayList<>();

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println(BANNER);
        System.out.println("jvmxrayagent.premain(): Agent started.");
        try {
            // ********************
            // *** BEGIN SECTION
            // ***
            // *** WARNING:
            // *** Don't modify anything within this section since there's a specific
            // ***   order of initialization.
            // ********************
            //
            // Initialize property factory
            PropertyFactory propertyFactory = PropertyFactory.getInstance();
            propertyFactory.init();
            // Initialize agent property settings.  We do this after init to record problems if any.
            AgentProperties properties = propertyFactory.getAgentProperties();
            // Initialize proxy logging.
            //   Note(s):
            //   - We use a proxy for logging since Sensors can't log using a standard logger.
            //   - We don't need to use a proxy but it's required for sensors so we use it in
            //       core agent code(here) for consistency.  Using the proxy also includes
            //       AID/CID to uniquely identify messages from cloud services.
            JVMXRayLogFactory logFactory = JVMXRayLogFactory.getInstance();
            logFactory.init(propertyFactory.getJvmxrayHome());
            logProxy = LogProxy.getInstance();
            // ********************
            // *** END SECTION
            // ********************
            logProxy.logMessage(NAMESPACE, "INFO", BANNER);
            logProxy.logMessage(NAMESPACE, "INFO", "Agent started.");

            // Register sensors from properties
            Set<Sensor> sensors = new HashSet<>(); // Use Set to help track unique sensors
            List<Sensor> loadedSensors = SensorUtils.loadSensors(properties, "jvmxray.sensor.");
            Map<String, List<Sensor>> identityMap = new HashMap<>();

            // Check for duplicate sensor identities
            for (Sensor sensor : loadedSensors) {
                String identity = sensor.getIdentity();
                identityMap.computeIfAbsent(identity, k -> new ArrayList<>()).add(sensor);
            }

            // Process sensors.  Check for duplicate sensor identities, if found, log and remove them.
            for (Map.Entry<String, List<Sensor>> entry : identityMap.entrySet()) {
                String identity = entry.getKey();
                List<Sensor> sensorsWithIdentity = entry.getValue();
                if (sensorsWithIdentity.size() > 1) {
                    // Log duplicate sensors
                    String sensorNames = sensorsWithIdentity.stream()
                            .map(Sensor::getDisplayName)
                            .collect(Collectors.joining(", "));
                            logProxy.logMessage(NAMESPACE, "INFO", String.format("Duplicate sensor identity detected: ID=%s used by sensors: %s. Removing all duplicates.",identity,sensorNames));
                    continue; // Skip adding duplicates to sensors set
                }
                sensors.add(sensorsWithIdentity.get(0));
            }

            if (sensors.isEmpty()) {
                logProxy.logMessage(NAMESPACE, "WARN", "No sensors loaded from properties. Agent running in NOP mode.");
            }

            // Initialize all registered sensors
            for (Sensor sensor : sensors) {
                try {
                    sensor.initialize(properties, agentArgs, instrumentation);
                    logProxy.logMessage(NAMESPACE, "INFO", String.format("Initialized sensor: %s", sensor.getDisplayName()));
                } catch (Exception e) {
                    logProxy.logMessage(NAMESPACE, "ERROR", String.format("Failed to initialize sensor %s: %s", sensor.getDisplayName(), e.getMessage()));
                }
            }

            // Collect injectable sensors for Byte Buddy instrumentation
            List<InjectableSensor> injectableSensors = new ArrayList<>();
            for (Sensor sensor : sensors) {
                if (sensor instanceof InjectableSensor) {
                    injectableSensors.add((InjectableSensor) sensor);
                    logProxy.logMessage(NAMESPACE, "DEBUG", String.format("Registered injectable sensor: %s", sensor.getDisplayName()));
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
            logProxy.logMessage(NAMESPACE, "DEBUG", String.format("Injected %s unique classes into bootstrap loader.", uniqueClasses.length));

            // Apply transformations for each injectable sensor
            for (InjectableSensor sensor : injectableSensors) {
                try {
                    AgentBuilder builder = new AgentBuilder.Default()
                            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                            .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                            .ignore(ElementMatchers.nameStartsWith("org.jvmxray.agent"));

                    Transform[] transforms = sensor.configure();
                    if (transforms.length == 0) {
                        logProxy.logMessage(NAMESPACE, "WARN", String.format("No transformations configured for sensor: %s", sensor.getDisplayName()));
                        continue;
                    }

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
                        logProxy.logMessage(NAMESPACE, "INFO", String.format("Configured transform for %s in sensor %s",
                                transform.getTargetClass().getName(),
                                sensor.getDisplayName()));
                    }

                    builder.with(new AgentBuilder.Listener() {
                        @Override
                        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                                     boolean loaded, DynamicType dynamicType) {
                            logProxy.logMessage(NAMESPACE, "INFO", String.format("Transformed %s for sensor %s",
                                    typeDescription.getName(),
                                    sensor.getDisplayName()));
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                              boolean loaded) {}

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                                            Throwable throwable) {
                            logProxy.logMessage(NAMESPACE, "ERROR", String.format("Error transforming %s for sensor %s: %s",
                                    typeName,
                                    sensor.getDisplayName(),
                                    throwable.getMessage()));
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}
                    }).installOn(instrumentation);

                    logProxy.logMessage(NAMESPACE, "INFO", String.format("Transformers installed for sensor: %s",
                            sensor.getDisplayName()));

                } catch (Exception e) {
                    logProxy.logMessageWithException(NAMESPACE, "ERROR",  String.format("Failed to install transformers for sensor %s",
                            sensor.getDisplayName()),
                            e);
                }
            }

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(jvmxrayagent::shutdown, "jvmxrayagent.shutdownhook"));
            logProxy.logMessage(NAMESPACE, "INFO", "Agent premain initialization complete. Agent active.");
            System.out.println("jvmxray.premain(): Agent premain initialization complete. Agent active.");
        } catch (Exception e) {
            System.err.println("jvmxrayagent.premain(): Agent premain failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        for (Sensor sensor : sensors) {
            try {
                sensor.shutdown();
                logProxy.logMessage(NAMESPACE, "DEBUG", String.format("Shut down sensor: %s",
                        sensor.getDisplayName()));
            } catch (Exception e) {
                logProxy.logMessageWithException(NAMESPACE, "DEBUG", String.format("Failed to shut down sensor %s",
                        sensor.getDisplayName()),
                        e);
            }
        }
        LogProxy.shutdown();
        System.out.println("jvmxrayagent.shutdown(): jvmxrayagent shutting down.");
    }
}