package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for XStream.fromXML() for XML deserialization.
 * 
 * @author Milton Smith
 */
public class XStreamInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    /**
     * Intercepts XStream.fromXML() for XML deserialization.
     */
    @Advice.OnMethodExit
    public static void xstreamFromXML(@Advice.Argument(0) String xml,
                                    @Advice.Return Object result,
                                    @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "xml_xstream");
            
            if (xml != null && xml.length() < 10000) {
                // Check for suspicious XML patterns
                if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) {
                    metadata.put("xxe_risk", "true");
                    metadata.put("risk_level", "HIGH");
                }
                
                // Check for dangerous class references in XML
                for (String dangerousClass : SerializationUtils.DANGEROUS_CLASSES) {
                    if (xml.contains(dangerousClass)) {
                        metadata.put("dangerous_class_reference", dangerousClass);
                        metadata.put("risk_level", "CRITICAL");
                        break;
                    }
                }
            }
            
            if (result != null) {
                metadata.put("result_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".xml", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }
}