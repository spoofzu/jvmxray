package org.jvmxray.agent.sensor.crypto;

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
 * CryptoSensor monitors cryptographic operations to detect weak algorithms,
 * improper key management, and potential cryptographic attacks.
 * 
 * @author Milton Smith
 */
public class CryptoSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.crypto.CryptoSensor";
    private static final String SENSOR_GUID = "CE21AA19-81BF-4B65-A4FD-AA724A9C790C";

    public CryptoSensor(String propertySuffix) {
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
            CipherInterceptor.class,
            CipherInitInterceptor.class,
            MessageDigestInterceptor.class,
            KeyStoreInterceptor.class,
            SSLInterceptor.class,
            CryptoUtils.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.crypto.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // Cipher operations
            try {
                Class<?> cipherClass = Class.forName("javax.crypto.Cipher");
                transforms.add(new Transform(
                    cipherClass,
                    CipherInterceptor.class,
                    new MethodSpec("getInstance", String.class)
                ));
                transforms.add(new Transform(
                    cipherClass,
                    CipherInitInterceptor.class,
                    new MethodSpec("init", int.class, Class.forName("java.security.Key"))
                ));
            } catch (ClassNotFoundException e) {
                // Crypto API not present, skip
            }

            // MessageDigest operations
            try {
                Class<?> messageDigestClass = Class.forName("java.security.MessageDigest");
                transforms.add(new Transform(
                    messageDigestClass,
                    MessageDigestInterceptor.class,
                    new MethodSpec("getInstance", String.class)
                ));
            } catch (ClassNotFoundException e) {
                // MessageDigest should always be present
            }

            // KeyStore operations
            try {
                Class<?> keyStoreClass = Class.forName("java.security.KeyStore");
                transforms.add(new Transform(
                    keyStoreClass,
                    KeyStoreInterceptor.class,
                    new MethodSpec("load", Class.forName("java.io.InputStream"), char[].class)
                ));
            } catch (ClassNotFoundException e) {
                // KeyStore should always be present
            }

            // SSL operations
            try {
                Class<?> sslSocketClass = Class.forName("javax.net.ssl.SSLSocket");
                transforms.add(new Transform(
                    sslSocketClass,
                    SSLInterceptor.class,
                    new MethodSpec("setEnabledCipherSuites", String[].class)
                ));
            } catch (ClassNotFoundException e) {
                // SSL not present, skip
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "CryptoSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}