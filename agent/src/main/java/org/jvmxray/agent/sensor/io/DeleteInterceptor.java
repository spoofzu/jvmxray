package org.jvmxray.agent.sensor.io;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor class for monitoring and logging file deletion events within the JVMXRay agent framework.
 * Uses Byte Buddy's {@link Advice} annotations to instrument the {@code delete} method of the {@link File} class,
 * capturing the file path and deletion result. Events are logged with contextual metadata using the
 * {@link LogProxy} for detailed tracking of file deletion operations.
 *
 * @author Milton Smith
 */
public class DeleteInterceptor {

    // Namespace for logging file deletion events
    public static final String NAMESPACE = "org.jvmxray.events.io.filedelete";
    // Singleton instance of LogProxy for logging
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts the entry of the {@code File.delete} method.
     * This implementation performs no operations but is included for potential future use.
     *
     * @param file The {@code File} instance being deleted.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This File file) {
        // No operations required on method entry
    }

    /**
     * Intercepts the exit of the {@code File.delete} method to log the deletion result.
     *
     * @param file   The {@code File} instance being deleted.
     * @param result The result of the deletion operation ({@code true} for success, {@code false} for failure).
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.This File file, @Advice.Return boolean result) {
        // Initialize metadata for logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("target", file.getAbsoluteFile().toString());
        metadata.put("status", result ? "successfully deleted" : "failed to delete");
        // Log the file deletion event
        logProxy.logMessage(NAMESPACE, "INFO", metadata);
    }
}