package org.jvmxray.agent.util.sensor;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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