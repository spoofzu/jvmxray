package org.jvmxray.agent.sensor.reflection;

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
 * ReflectionSensor monitors Java reflection operations to detect potential
 * security vulnerabilities including privilege escalation, code injection,
 * and unauthorized access to private members.
 * 
 * @author Milton Smith
 */
public class ReflectionSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.reflection.ReflectionSensor";
    private static final String SENSOR_GUID = "B484BC40-52D6-4EC5-913F-227408780763";

    public ReflectionSensor(String propertySuffix) {
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
            ClassForNameInterceptor.class,
            MethodInvokeInterceptor.class,
            ConstructorInterceptor.class,
            FieldGetInterceptor.class,
            FieldSetInterceptor.class,
            SetAccessibleInterceptor.class,
            ReflectionUtils.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.reflection.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // Class.forName() monitoring
            try {
                Class<?> classClass = Class.forName("java.lang.Class");
                transforms.add(new Transform(
                    classClass,
                    ClassForNameInterceptor.class,
                    new MethodSpec("forName", String.class)
                ));
                transforms.add(new Transform(
                    classClass,
                    ClassForNameInterceptor.class,
                    new MethodSpec("forName", String.class, boolean.class, ClassLoader.class)
                ));
            } catch (ClassNotFoundException e) {
                // Class should always be present
            }

            // Method invocation monitoring
            try {
                Class<?> methodClass = Class.forName("java.lang.reflect.Method");
                transforms.add(new Transform(
                    methodClass,
                    MethodInvokeInterceptor.class,
                    new MethodSpec("invoke", Object.class, Object[].class)
                ));
            } catch (ClassNotFoundException e) {
                // Method should always be present
            }

            // Constructor invocation monitoring
            try {
                Class<?> constructorClass = Class.forName("java.lang.reflect.Constructor");
                transforms.add(new Transform(
                    constructorClass,
                    ConstructorInterceptor.class,
                    new MethodSpec("newInstance", Object[].class)
                ));
            } catch (ClassNotFoundException e) {
                // Constructor should always be present
            }

            // Field access monitoring
            try {
                Class<?> fieldClass = Class.forName("java.lang.reflect.Field");
                transforms.add(new Transform(
                    fieldClass,
                    FieldGetInterceptor.class,
                    new MethodSpec("get", Object.class)
                ));
                transforms.add(new Transform(
                    fieldClass,
                    FieldSetInterceptor.class,
                    new MethodSpec("set", Object.class, Object.class)
                ));
                transforms.add(new Transform(
                    fieldClass,
                    SetAccessibleInterceptor.class,
                    new MethodSpec("setAccessible", boolean.class)
                ));
            } catch (ClassNotFoundException e) {
                // Field should always be present
            }

            // AccessibleObject.setAccessible() monitoring (covers Method, Constructor, Field)
            try {
                Class<?> accessibleObjectClass = Class.forName("java.lang.reflect.AccessibleObject");
                transforms.add(new Transform(
                    accessibleObjectClass,
                    SetAccessibleInterceptor.class,
                    new MethodSpec("setAccessible", boolean.class)
                ));
            } catch (ClassNotFoundException e) {
                // AccessibleObject should always be present
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "ReflectionSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}