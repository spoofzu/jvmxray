package org.jvmxray.agent.sensor.crypto;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for KeyStore.load() to monitor keystore operations.
 *
 * @author Milton Smith
 */
public class KeyStoreInterceptor {

    // Namespace for logging cryptographic events
    public static final String NAMESPACE = "org.jvmxray.events.crypto";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Extracts the file path from a FileInputStream using reflection.
     * Returns null if the stream is not a FileInputStream or if extraction fails.
     */
    private static String extractFilePath(Object inputStream) {
        if (inputStream == null) {
            return null;
        }

        // Check if it's a FileInputStream
        if (inputStream instanceof FileInputStream) {
            try {
                Field pathField = FileInputStream.class.getDeclaredField("path");
                pathField.setAccessible(true);
                return (String) pathField.get(inputStream);
            } catch (Exception e) {
                // Reflection failed - security restriction or JDK variation
                return null;
            }
        }

        return null;
    }

    /**
     * Intercepts KeyStore.load() to monitor keystore operations.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void keyStoreLoad(@Advice.This Object keyStore,
                                  @Advice.Argument(0) Object inputStream,
                                  @Advice.Argument(1) char[] password,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "keyStore_load");
            metadata.put("keystore_class", keyStore.getClass().getName());
            metadata.put("has_inputstream", inputStream != null ? "true" : "false");
            metadata.put("has_password", password != null ? "true" : "false");

            // Extract keystore file path if available
            String filePath = extractFilePath(inputStream);
            if (filePath != null) {
                metadata.put("keystore_file", filePath);
            } else if (inputStream != null) {
                // Log the stream class for debugging when file path unavailable
                metadata.put("stream_class", inputStream.getClass().getName());
            }

            if (password != null && password.length < 8) {
                metadata.put("weak_password", "true");
                metadata.put("risk_level", "HIGH");
            }

            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }

            logProxy.logMessage(NAMESPACE + ".keystore", "INFO", metadata);

        } catch (Exception e) {
            // Fail silently
        }
    }
}