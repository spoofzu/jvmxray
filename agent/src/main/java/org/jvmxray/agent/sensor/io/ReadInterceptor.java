package org.jvmxray.agent.sensor.io;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging file read events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the no-argument {@code read} method
 * of the {@link FileInputStream} class, capturing the file descriptor and read result. Events are
 * logged with contextual metadata using the {@link LogProxy} for detailed tracking of file read operations.
 *
 * @author Milton Smith
 */
public class ReadInterceptor {

    // Namespace for logging file read events
    public static final String NAMESPACE = "org.jvmxray.events.io.fileread";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code FileInputStream.read} method.
     * This implementation performs no operations but is included for potential future use.
     *
     * @param fis The {@code FileInputStream} instance being read.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This FileInputStream fis) {
        // No operations required on method entry
    }

    /**
     * Intercepts the exit of the {@code FileInputStream.read} method to log the read result.
     *
     * @param fis    The {@code FileInputStream} instance being read.
     * @param result The result of the read operation (number of bytes read, or -1 for EOF).
     * @throws Exception If an error occurs while accessing the file descriptor.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.This FileInputStream fis, @Advice.Return int result) throws Exception {
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("target", fis.getFD().toString());
        metadata.put("status", (result >= 0) ? "read " + result + " bytes" : "reached EOF");
        // Log the file read event
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }
}