package org.jvmxray.agent.sensor.configuration;

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
 * ConfigurationSensor monitors system property changes, environment variable
 * access, and configuration file loading to detect privilege escalation and
 * unauthorized system modifications.
 * 
 * @author Milton Smith
 */
public class ConfigurationSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.configuration.ConfigurationSensor";
    private static final String SENSOR_GUID = "4CA769A3-F605-456A-B082-9D39C953E726";

    public ConfigurationSensor(String propertySuffix) {
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
            SystemGetPropertyInterceptor.class,
            SystemSetPropertyInterceptor.class,
            SystemGetEnvInterceptor.class,
            SystemGetEnvAllInterceptor.class,
            PropertiesLoadInterceptor.class,
            PropertiesStoreInterceptor.class,
            PreferencesGetInterceptor.class,
            PreferencesPutInterceptor.class,
            ConfigurationUtils.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.configuration.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // System property operations
            try {
                Class<?> systemClass = Class.forName("java.lang.System");
                transforms.add(new Transform(
                    systemClass,
                    SystemGetPropertyInterceptor.class,
                    new MethodSpec("getProperty", String.class)
                ));
                transforms.add(new Transform(
                    systemClass,
                    SystemGetPropertyInterceptor.class,
                    new MethodSpec("getProperty", String.class, String.class)
                ));
                transforms.add(new Transform(
                    systemClass,
                    SystemSetPropertyInterceptor.class,
                    new MethodSpec("setProperty", String.class, String.class)
                ));
                transforms.add(new Transform(
                    systemClass,
                    SystemGetEnvInterceptor.class,
                    new MethodSpec("getenv", String.class)
                ));
                transforms.add(new Transform(
                    systemClass,
                    SystemGetEnvAllInterceptor.class,
                    new MethodSpec("getenv")
                ));
            } catch (ClassNotFoundException e) {
                // System should always be present
            }

            // Properties file loading
            try {
                Class<?> propertiesClass = Class.forName("java.util.Properties");
                transforms.add(new Transform(
                    propertiesClass,
                    PropertiesLoadInterceptor.class,
                    new MethodSpec("load", Class.forName("java.io.InputStream"))
                ));
                transforms.add(new Transform(
                    propertiesClass,
                    PropertiesLoadInterceptor.class,
                    new MethodSpec("loadFromXML", Class.forName("java.io.InputStream"))
                ));
                transforms.add(new Transform(
                    propertiesClass,
                    PropertiesStoreInterceptor.class,
                    new MethodSpec("store", Class.forName("java.io.OutputStream"), String.class)
                ));
            } catch (ClassNotFoundException e) {
                // Properties should always be present
            }

            // Preferences API monitoring
            try {
                Class<?> preferencesClass = Class.forName("java.util.prefs.Preferences");
                transforms.add(new Transform(
                    preferencesClass,
                    PreferencesGetInterceptor.class,
                    new MethodSpec("get", String.class, String.class)
                ));
                transforms.add(new Transform(
                    preferencesClass,
                    PreferencesPutInterceptor.class,
                    new MethodSpec("put", String.class, String.class)
                ));
            } catch (ClassNotFoundException e) {
                // Preferences should always be present
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "ConfigurationSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}