package org.jvmxray.agent.sensor.api;

import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.MethodSpec;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.init.AgentInitializer;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * APICallSensor monitors REST API calls and web service invocations to detect
 * unauthorized API access and data exfiltration.
 * 
 * @author Milton Smith
 */
public class APICallSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.api.APICallSensor";
    private static final String GUID = "3bIeP9s4mN5wV1z9Un7Ip3";

    public APICallSensor(String propertySuffix) {
        super(propertySuffix);
    }

    @Override
    public String getIdentity() {
        return GUID;
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
            APICallInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.api.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // HTTP client operations
            try {
                Class<?> httpClientClass = Class.forName("java.net.http.HttpClient");
                transforms.add(new Transform(
                    httpClientClass,
                    APICallInterceptor.class,
                    new MethodSpec("send", Class.forName("java.net.http.HttpRequest"), Class.forName("java.net.http.HttpResponse$BodyHandler"))
                ));
            } catch (ClassNotFoundException e) {
                // HttpClient not present, skip
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "APICallSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}