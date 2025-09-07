package org.jvmxray.agent.sensor.serialization;

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
 * SerializationSensor monitors Java serialization/deserialization operations
 * to detect dangerous deserialization attacks, gadget chains, and malicious
 * object injection attempts.
 * 
 * Targets:
 * - java.io.ObjectInputStream.readObject
 * - java.io.ObjectInputStream.resolveClass
 * - Jackson, Gson, and other JSON deserializers
 * - XML deserializers (XStream, etc.)
 * 
 * @author Milton Smith
 */
public class SerializationSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.serialization.SerializationSensor";
    private static final String SENSOR_GUID = "CDFF9E26-2447-4BCB-AB03-3EABBD1CBBED";

    public SerializationSensor(String propertySuffix) {
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
            ObjectInputInterceptor.class,
            ObjectInputResolveInterceptor.class,
            ObjectOutputInterceptor.class,
            JacksonInterceptor.class,
            GsonInterceptor.class,
            XStreamInterceptor.class,
            SerializationUtils.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.serialization.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // Core Java serialization
            try {
                Class<?> objectInputStreamClass = Class.forName("java.io.ObjectInputStream");
                transforms.add(new Transform(
                    objectInputStreamClass,
                    ObjectInputInterceptor.class,
                    new MethodSpec("readObject")
                ));
                transforms.add(new Transform(
                    objectInputStreamClass,
                    ObjectInputResolveInterceptor.class,
                    new MethodSpec("resolveClass", Class.forName("java.io.ObjectStreamClass"))
                ));
            } catch (ClassNotFoundException e) {
                // ObjectInputStream should always be present
            }

            try {
                Class<?> objectOutputStreamClass = Class.forName("java.io.ObjectOutputStream");
                transforms.add(new Transform(
                    objectOutputStreamClass,
                    ObjectOutputInterceptor.class,
                    new MethodSpec("writeObject", Object.class)
                ));
            } catch (ClassNotFoundException e) {
                // ObjectOutputStream should always be present
            }

            // Jackson JSON deserializer (if present)
            try {
                Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                transforms.add(new Transform(
                    objectMapperClass,
                    JacksonInterceptor.class,
                    new MethodSpec("readValue", String.class, Class.class)
                ));
            } catch (ClassNotFoundException e) {
                // Jackson not present, skip
            }

            // Gson deserializer (if present)
            try {
                Class<?> gsonClass = Class.forName("com.google.gson.Gson");
                transforms.add(new Transform(
                    gsonClass,
                    GsonInterceptor.class,
                    new MethodSpec("fromJson", String.class, Class.class)
                ));
            } catch (ClassNotFoundException e) {
                // Gson not present, skip
            }

            // XStream deserializer (if present)
            try {
                Class<?> xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
                transforms.add(new Transform(
                    xstreamClass,
                    XStreamInterceptor.class,
                    new MethodSpec("fromXML", String.class)
                ));
            } catch (ClassNotFoundException e) {
                // XStream not present, skip
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "SerializationSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}