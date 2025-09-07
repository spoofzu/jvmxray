package org.jvmxray.agent.sensor.memory;

import org.jvmxray.agent.proxy.LogProxy;
import org.jvmxray.agent.sensor.AbstractSensor;
import org.jvmxray.agent.sensor.InjectableSensor;
import org.jvmxray.agent.sensor.Sensor;
import org.jvmxray.agent.sensor.Transform;
import org.jvmxray.platform.shared.property.AgentProperties;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * MemorySensor monitors memory allocation, garbage collection, and unsafe
 * memory operations to detect memory-based attacks and resource exhaustion.
 * 
 * @author Milton Smith
 */
public class MemorySensor extends AbstractSensor implements InjectableSensor {

    private static final String NAMESPACE = "org.jvmxray.agent.core.memory.MemorySensor";
    private static final String SENSOR_GUID = "EE882184-342F-478D-A82A-9FE174D76739";

    public MemorySensor(String propertySuffix) {
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
            GarbageCollectionInterceptor.class,
            MemoryQueryInterceptor.class,
            UnsafeMemoryInterceptor.class,
            UnsafeFreeInterceptor.class,
            UnsafeReallocateInterceptor.class,
            DirectBufferInterceptor.class,
            MemoryUtils.class
        };
    }

    @Override
    public Transform[] configure() {
        try {
            // TODO: MemorySensor disabled due to recursive logging issue with ArrayBlockingQueue
            // The sensor instruments Runtime.gc() and System.gc() which may trigger GC operations
            // during ArrayBlockingQueue.put() calls, causing recursion in BUFFERED logging mode.
            // Need to analyze if GC instrumentation interferes with queue operations.
            // Issue: GC operations during logging -> sensor -> log -> queue.put() -> potential GC -> sensor
            return new Transform[0];
            
            /* DISABLED CODE - Enable once recursive logging issue is resolved
            AgentProperties props = AgentInitializer.getInstance().getProperties();
            boolean enabled = props.getBooleanProperty("jvmxray.agent.sensor.memory.enabled", true);
            
            if (!enabled) {
                return new Transform[0];
            }

            List<Transform> transforms = new ArrayList<>();

            // Runtime memory operations
            try {
                Class<?> runtimeClass = Class.forName("java.lang.Runtime");
                transforms.add(new Transform(
                    runtimeClass,
                    GarbageCollectionInterceptor.class,
                    new MethodSpec("gc")
                ));
                transforms.add(new Transform(
                    runtimeClass,
                    MemoryQueryInterceptor.class,
                    new MethodSpec("freeMemory")
                ));
                transforms.add(new Transform(
                    runtimeClass,
                    MemoryQueryInterceptor.class,
                    new MethodSpec("totalMemory")
                ));
                transforms.add(new Transform(
                    runtimeClass,
                    MemoryQueryInterceptor.class,
                    new MethodSpec("maxMemory")
                ));
            } catch (ClassNotFoundException e) {
                // Runtime should always be present
            }

            // System.gc() calls
            try {
                Class<?> systemClass = Class.forName("java.lang.System");
                transforms.add(new Transform(
                    systemClass,
                    GarbageCollectionInterceptor.class,
                    new MethodSpec("gc")
                ));
            } catch (ClassNotFoundException e) {
                // System should always be present
            }

            // Unsafe memory operations (if available)
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                transforms.add(new Transform(
                    unsafeClass,
                    UnsafeMemoryInterceptor.class,
                    new MethodSpec("allocateMemory", long.class)
                ));
                transforms.add(new Transform(
                    unsafeClass,
                    UnsafeFreeInterceptor.class,
                    new MethodSpec("freeMemory", long.class)
                ));
                transforms.add(new Transform(
                    unsafeClass,
                    UnsafeReallocateInterceptor.class,
                    new MethodSpec("reallocateMemory", long.class, long.class)
                ));
            } catch (ClassNotFoundException e) {
                // sun.misc.Unsafe not available, try jdk.internal.misc.Unsafe
                try {
                    Class<?> unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                    transforms.add(new Transform(
                        unsafeClass,
                        UnsafeMemoryInterceptor.class,
                        new MethodSpec("allocateMemory", long.class)
                    ));
                    transforms.add(new Transform(
                        unsafeClass,
                        UnsafeFreeInterceptor.class,
                        new MethodSpec("freeMemory", long.class)
                    ));
                    transforms.add(new Transform(
                        unsafeClass,
                        UnsafeReallocateInterceptor.class,
                        new MethodSpec("reallocateMemory", long.class, long.class)
                    ));
                } catch (ClassNotFoundException e2) {
                    // No Unsafe available, skip
                }
            }

            // ByteBuffer direct memory allocation
            try {
                Class<?> byteBufferClass = Class.forName("java.nio.ByteBuffer");
                transforms.add(new Transform(
                    byteBufferClass,
                    DirectBufferInterceptor.class,
                    new MethodSpec("allocateDirect", int.class)
                ));
            } catch (ClassNotFoundException e) {
                // ByteBuffer should always be present
            }

            return transforms.toArray(new Transform[0]);
            */

        } catch (Exception e) {
            LogProxy.getInstance().logMessage(NAMESPACE, "ERROR", Map.of(
                "message", "MemorySensor configuration error: " + e.getMessage()
            ));
            return new Transform[0];
        }
    }

    @Override
    public void shutdown() {
        // No cleanup required for Byte Buddy sensors
    }
}