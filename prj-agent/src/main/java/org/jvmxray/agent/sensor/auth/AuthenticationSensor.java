package org.jvmxray.agent.sensor.auth;

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
 * AuthenticationSensor monitors authentication-related operations to detect
 * security events such as brute force attacks, authentication bypass, and
 * privilege escalation attempts.
 * 
 * @author Milton Smith
 */
public class AuthenticationSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.auth.AuthenticationSensor";
    private static final String SENSOR_GUID = "2sZvG9rVeD5nM1q9Ld7Zg3";

    public AuthenticationSensor(String propertySuffix) {
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
            AuthenticationInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.auth.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();
            
            // HTTP Session monitoring
            try {
                Class<?> sessionClass = Class.forName("javax.servlet.http.HttpSession");
                transforms.add(new Transform(
                    sessionClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("setAttribute", String.class, Object.class)
                ));
                transforms.add(new Transform(
                    sessionClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("getAttribute", String.class)
                ));
                transforms.add(new Transform(
                    sessionClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("invalidate")
                ));
            } catch (ClassNotFoundException e) {
                // Servlet API not present, skip
            }

            // JAAS LoginContext monitoring
            try {
                Class<?> loginContextClass = Class.forName("javax.security.auth.login.LoginContext");
                transforms.add(new Transform(
                    loginContextClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("login")
                ));
            } catch (ClassNotFoundException e) {
                // JAAS not present, skip
            }

            // Spring Security AuthenticationManager monitoring
            try {
                Class<?> authManagerClass = Class.forName("org.springframework.security.authentication.AuthenticationManager");
                Class<?> authClass = Class.forName("org.springframework.security.core.Authentication");
                transforms.add(new Transform(
                    authManagerClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("authenticate", authClass)
                ));
            } catch (ClassNotFoundException e) {
                // Spring Security not present, skip
            }

            // Principal.getName() monitoring
            try {
                Class<?> principalClass = Class.forName("java.security.Principal");
                transforms.add(new Transform(
                    principalClass,
                    AuthenticationInterceptor.class,
                    new MethodSpec("getName")
                ));
            } catch (ClassNotFoundException e) {
                // Unlikely to happen, but handle gracefully
            }

            return transforms.toArray(new Transform[0]);

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "AuthenticationSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}