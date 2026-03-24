package org.jvmxray.agent.sensor.data;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import java.util.HashMap;
import java.util.Map;

public class DataTransferInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.data";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void dataTransfer(@Advice.Origin String method, @Advice.Return int bytesRead) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "data_transfer");
            metadata.put("method", method);
            metadata.put("bytes_transferred", String.valueOf(bytesRead));
            logProxy.logMessage(NAMESPACE + ".transfer", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        }
    }
}