package org.jvmxray.agent.sensor.io;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging file write events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code write(int)} method
 * of the {@link FileOutputStream} class, capturing the file descriptor and write status. Events are
 * logged with contextual metadata using the {@link LogProxy} for detailed tracking of file write operations.
 *
 * @author Milton Smith
 */
public class WriteInterceptor {

    // Namespace for logging file write events
    public static final String NAMESPACE = "org.jvmxray.events.io.filewrite";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code FileOutputStream.write(int)} method.
     * This implementation performs no operations but is included for potential future use.
     *
     * @param fos The {@code FileOutputStream} instance being written to.
     * @param b   The integer byte value being written.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This FileOutputStream fos, @Advice.Argument(0) int b) {
        // No operations required on method entry
    }

    /**
     * Intercepts the exit of the {@code FileOutputStream.write(int)} method to log the write result.
     *
     * @param fos The {@code FileOutputStream} instance being written to.
     * @throws Exception If an error occurs while accessing the file descriptor.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.This FileOutputStream fos) throws Exception {
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("target", fos.getFD().toString());
        metadata.put("status", "successfully written");
        // Log the file write event
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }
}