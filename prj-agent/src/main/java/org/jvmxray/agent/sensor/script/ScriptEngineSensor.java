package org.jvmxray.agent.sensor.script;

import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.platform.shared.property.AgentProperties;
import org.jvmxray.agent.init.AgentInitializer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ScriptEngineSensor monitors script execution through JSR-223 ScriptEngine
 * and related scripting APIs to detect code injection and unauthorized
 * script execution.
 * 
 * @author Milton Smith
 */
public class ScriptEngineSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.script.ScriptEngineSensor";
    private static final String SENSOR_GUID = "314596D7-710E-4E81-9CDC-E3B574768A5F";

    public ScriptEngineSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return SENSOR_GUID;
    }

    @Override
    public void initialize(AgentProperties properties, String agentArgs, Instrumentation inst) {
        // No initialization required for Byte Buddy sensors
    }

    @Override
    public Class<?>[] inject() {
        return new Class<?>[] {
            Sensor.class,
            InjectableSensor.class,
            LogProxy.class,
            ScriptEngineInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.script.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // ScriptEngine operations
            try {
                Class<?> scriptEngineClass = Class.forName("javax.script.ScriptEngine");
                transforms.add(new Transform(
                    scriptEngineClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("eval", String.class)
                ));
                transforms.add(new Transform(
                    scriptEngineClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("eval", String.class, Class.forName("javax.script.ScriptContext"))
                ));
                transforms.add(new Transform(
                    scriptEngineClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("eval", Class.forName("java.io.Reader"))
                ));
            } catch (ClassNotFoundException e) {
                // ScriptEngine not present, skip
            }

            // ScriptEngineManager operations
            try {
                Class<?> scriptManagerClass = Class.forName("javax.script.ScriptEngineManager");
                transforms.add(new Transform(
                    scriptManagerClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("getEngineByName", String.class)
                ));
                transforms.add(new Transform(
                    scriptManagerClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("getEngineByExtension", String.class)
                ));
            } catch (ClassNotFoundException e) {
                // ScriptEngineManager not present, skip
            }

            // Compilable script operations (for JavaScript, etc.)
            try {
                Class<?> compilableClass = Class.forName("javax.script.Compilable");
                transforms.add(new Transform(
                    compilableClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("compile", String.class)
                ));
                transforms.add(new Transform(
                    compilableClass,
                    ScriptEngineInterceptor.class,
                    new MethodSpec("compile", Class.forName("java.io.Reader"))
                ));
            } catch (ClassNotFoundException e) {
                // Compilable not present, skip
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "ScriptEngineSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}