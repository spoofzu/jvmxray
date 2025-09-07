package org.jvmxray.agent.sensor.data;

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
 * DataTransferSensor monitors large data transfers and I/O operations to detect
 * data exfiltration and resource abuse.
 * 
 * @author Milton Smith
 */
public class DataTransferSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.data.DataTransferSensor";
    private static final String SENSOR_GUID = "3048F130-5D97-4B00-BBEE-09C8F45AACAE";

    public DataTransferSensor(String propertySuffix) {
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
            DataTransferInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.data.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // InputStream/OutputStream operations
            try {
                Class<?> inputStreamClass = Class.forName("java.io.InputStream");
                transforms.add(new Transform(
                    inputStreamClass,
                    DataTransferInterceptor.class,
                    new MethodSpec("read", byte[].class)
                ));
            } catch (ClassNotFoundException e) {
                // InputStream should always be present
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "DataTransferSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}