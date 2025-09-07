package org.jvmxray.agent.sensor.script;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;
import java.util.HashMap;
import java.util.Map;

public class ScriptEngineInterceptor {
    public static final String NAMESPACE = "org.jvmxray.events.script";
    public static final LogProxy logProxy = LogProxy.getInstance();

    @Advice.OnMethodExit
    public static void scriptExecution(@Advice.Origin String method, @Advice.Argument(0) Object script) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "script_execution");
            metadata.put("method", method);
            metadata.put("script_provided", script != null ? "true" : "false");
            logProxy.logMessage(NAMESPACE + ".execution", "INFO", metadata);
        } catch (Exception e) {
            // Fail silently
        }
    }
}