package org.jvmxray.agent.sensor.thread;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * ThreadSensor monitors thread creation, synchronization, and lifecycle
 * events to detect threading attacks, deadlocks, and resource exhaustion.
 * 
 * @author Milton Smith
 */
public class ThreadSensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.thread.ThreadSensor";
    private static final String SENSOR_GUID = "BA41325F-3453-4F99-9EDA-83FF7EC16676";

    public ThreadSensor(String propertySuffix) {
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
            ThreadInterceptor.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            // TODO: ThreadSensor disabled due to recursive logging issue with ArrayBlockingQueue
            // The sensor instruments Thread.interrupt() which causes recursion when AgentLogger
            // uses ArrayBlockingQueue.put() in BUFFERED mode. Need to fix instrumentation
            // exclusions or find alternative approach to monitor thread operations.
            // Issue: Thread operations in logging infrastructure trigger sensor -> log -> queue.put() -> Thread.interrupt() -> sensor
            return new Transform[0];
            
            /* DISABLED CODE - Enable once recursive logging issue is resolved
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.thread.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // Thread lifecycle operations
            try {
                Class<?> threadClass = Class.forName("java.lang.Thread");
                transforms.add(new Transform(
                    threadClass,
                    ThreadInterceptor.class,
                    new MethodSpec("start")
                ));
                transforms.add(new Transform(
                    threadClass,
                    ThreadInterceptor.class,
                    new MethodSpec("interrupt")
                ));
                transforms.add(new Transform(
                    threadClass,
                    ThreadInterceptor.class,
                    new MethodSpec("stop")
                ));
                transforms.add(new Transform(
                    threadClass,
                    ThreadInterceptor.class,
                    new MethodSpec("join")
                ));
            } catch (ClassNotFoundException e) {
                // Thread should always be present
            }

            // Executor service operations
            try {
                Class<?> executorClass = Class.forName("java.util.concurrent.Executor");
                transforms.add(new Transform(
                    executorClass,
                    ThreadInterceptor.class,
                    new MethodSpec("execute", Runnable.class)
                ));
            } catch (ClassNotFoundException e) {
                // Executor not present, skip
            }

            return transforms.toArray(new Transform[0]);
            */

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "ThreadSensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}