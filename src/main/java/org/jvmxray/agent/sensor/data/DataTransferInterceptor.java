package org.jvmxray.agent.sensor.data;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.util.MCCScope;
import java.util.HashMap;
import java.util.Map;

public class DataTransferInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.data";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void dataTransfer(@Advice.Return int bytesRead) {
        MCCScope.enter("DataTransfer");
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "data_transfer");
            metadata.put("bytes_transferred", String.valueOf(bytesRead));
            logProxy.logMessage(NAMESPACE + ".transfer", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        } finally {
            MCCScope.exit("DataTransfer");
        }
    }
}