package org.jvmxray.agent.util.sensor;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.platform.shared.property.IProperties;
import org.jvmxray.agent.proxy.LogProxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.*;

import static net.bytebuddy.dynamic.loading.ClassInjector.UsingInstrumentation.Target.BOOTSTRAP;

/**
 * Utility class for injecting classes into the JVM's bootstrap class loader within
 * the JVMXRay agent framework. Provides methods to ensure sensor-related classes and
 * their dependencies are available in the bootstrap class loader, enabling instrumentation
 * of core Java classes such as {@code java.io.File} or {@code java.net.Socket}.
 *
 * @author Milton Smith
 */
public class SensorUtils {

    private static final String NAMESPACE = "org.jvmxray.agent.util.sensor.SensorUtils";

    /**
     * Loads sensors from properties with the specified prefix.
     * The suffix of each key (e.g., "http" in "jvmxray.sensor.http") is used as the
     * sensor's display name prefix.
     *
     * @param properties Properties file.
     * @param propertyPrefix The prefix for sensor properties (e.g., "jvmxray.sensor.").
     * @return A list of successfully loaded sensor instances.
     */
    public static final List<Sensor> loadSensors(IProperties properties, String propertyPrefix) {
        List<Sensor> sensors = new ArrayList<>();
        try {
            Enumeration<String> propertyNames = properties.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = propertyNames.nextElement();
                if (key.startsWith(propertyPrefix)) {
                    String className = properties.getProperty(key).trim();
                    if (!className.isEmpty()) {
                        String propertySuffix = key.substring(propertyPrefix.length());
                        Sensor sensor = loadSensor(className, propertySuffix);
                        if (sensor != null) {
                            sensors.add(sensor);
                            LogProxy.getInstance().logMessage(NAMESPACE, "INFO", 
                                    String.format("loadSensors: Loaded sensor %s: %s (display name: %s, identity: %s)",
                                    key, className, sensor.getDisplayName(), sensor.getIdentity()));
                        } else {
                            LogProxy.getInstance().logMessage(NAMESPACE, "WARN", 
                                    String.format("loadSensors: Skipped sensor %s due to error: %s",
                                    key, className));
                        }
                    }
                }
            }
            if (sensors.isEmpty()) {
                LogProxy.getInstance().logMessage(NAMESPACE, "WARN", 
                        String.format("loadSensors: No sensors found for prefix: %s", propertyPrefix));
            }
        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", 
                    String.format("loadSensors: Failed to load sensors for prefix %s: %s",
                    propertyPrefix, e.getMessage()));
        }
        return sensors;
    }

    /**
     * Loads a single sensor by its fully qualified class name and assigns a property suffix.
     *
     * @param className      The fully qualified name of the sensor class.
     * @param propertySuffix The property key suffix (e.g., "http").
     * @return An instance of the sensor, or null if loading fails.
     * @throws IllegalArgumentException if the className is null or empty, or if the class does not implement Sensor.
     */
    private static final Sensor loadSensor(String className, String propertySuffix) {
        if (className == null || className.trim().isEmpty()) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", "loadSensor: Class name cannot be null or empty.");
            throw new IllegalArgumentException("Class name cannot be null or empty.");
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!Sensor.class.isAssignableFrom(clazz)) {
                LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", 
                        String.format("loadSensor: Class %s does not implement Sensor interface.", className));
                throw new IllegalArgumentException("Class " + className + " does not implement Sensor interface.");
            }
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
            return (Sensor) constructor.newInstance(propertySuffix);
        } catch (NoSuchMethodException e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", 
                    String.format("loadSensor: Sensor %s does not have a constructor accepting a property suffix.", className));
            return null;
        } catch (ClassNotFoundException e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", 
                    String.format("loadSensor: Sensor class not found: %s", className));
            return null;
        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", 
                    String.format("loadSensor: Failed to instantiate sensor: %s", className));
            return null;
        }
    }

    /**
     * Injects the specified classes into the bootstrap class loader using the provided
     * {@code Instrumentation} instance. Creates a temporary directory for class loading,
     * reads class bytecode from the classpath, and uses Byte Buddy's {@code ClassInjector}
     * to perform the injection. The temporary directory is marked for deletion on JVM exit.
     *
     * @param inst      The {@code Instrumentation} instance for class manipulation.
     * @param namespace A string identifier for the injection context (currently unused).
     * @param classes   Variable-length array of {@code Class} objects to inject.
     * @throws IOException If a class resource cannot be found, read, or injected.
     */
    public static void injectClasses(Instrumentation inst, String namespace, Class<?>... classes) throws IOException {
        // Create a temporary directory for class loading
        File temp = Files.createTempDirectory("tmp").toFile();
        temp.deleteOnExit();
        // Initialize Byte Buddy class injector for bootstrap class loader
        ClassInjector injector = ClassInjector.UsingInstrumentation.of(temp, BOOTSTRAP, inst);
        // Map classes to their bytecode
        Map<TypeDescription, byte[]> classMap = new HashMap<>();
        for (Class<?> clazz : classes) {
            String classResource = clazz.getName().replace('.', '/') + ".class";
            try (InputStream is = SensorUtils.class.getClassLoader().getResourceAsStream(classResource)) {
                if (is == null) {
                    throw new IOException("Class resource not found: " + classResource);
                }
                // Read class bytecode
                byte[] classBytes = is.readAllBytes();
                classMap.put(new TypeDescription.ForLoadedType(clazz), classBytes);
            }
        }
        // Inject classes into the bootstrap class loader
        injector.inject(classMap);
    }
}