package org.jvmxray.agent.sensor.serialization;

import net.bytebuddy.asm.Advice;
import org.jvmxray.agent.proxy.LogProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for serialization operations to detect deserialization attacks,
 * gadget chains, and malicious object injection.
 * 
 * @author Milton Smith
 */
public class SerializationInterceptor {
    
    // Namespace for logging serialization events
    public static final String NAMESPACE = "org.jvmxray.events.serialization";
    public static final LogProxy logProxy = LogProxy.getInstance();

    // Known dangerous classes for deserialization attacks
    private static final String[] DANGEROUS_CLASSES = {
        "java.rmi.server.RemoteObjectInvocationHandler",
        "org.apache.commons.collections.functors.InvokerTransformer",
        "org.apache.commons.collections.functors.ChainedTransformer",
        "org.apache.commons.collections.functors.ConstantTransformer",
        "org.apache.commons.collections.functors.InstantiateTransformer",
        "org.apache.commons.collections4.functors.InvokerTransformer",
        "org.apache.commons.collections4.functors.ChainedTransformer",
        "com.sun.rowset.JdbcRowSetImpl",
        "org.springframework.beans.factory.ObjectFactory",
        "org.apache.xalan.xsltc.trax.TemplatesImpl",
        "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl",
        "org.apache.commons.beanutils.BeanComparator",
        "org.hibernate.engine.spi.TypedValue",
        "org.hibernate.tuple.component.AbstractComponentTuplizer",
        "org.jboss.interceptor.builder.MethodReference",
        "org.apache.wicket.util.upload.DiskFileItem",
        "clojure.core.proxy$clojure.lang.APersistentMap",
        "org.apache.myfaces.context.servlet.FacesContextImpl",
        "org.apache.myfaces.context.servlet.FacesContextImplBase",
        "org.apache.openjpa.ee.RegistryManagedRuntime",
        "org.apache.openjpa.ee.JNDIManagedRuntime"
    };

    /**
     * Intercepts ObjectInputStream.readObject() to detect deserialization attacks.
     */
    @Advice.OnMethodExit
    public static void readObject(@Advice.This Object objectInputStream,
                                @Advice.Return Object result,
                                @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "java_native");
            
            if (result != null) {
                String className = result.getClass().getName();
                metadata.put("deserialized_class", className);
                
                // Check if it's a dangerous class
                String dangerousClass = checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "CRITICAL");
                }
                
                // Check for suspicious patterns
                if (className.contains("Transformer") || 
                    className.contains("Handler") || 
                    className.contains("Factory")) {
                    metadata.put("suspicious_pattern", "true");
                    metadata.put("risk_level", metadata.getOrDefault("risk_level", "HIGH"));
                }
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
                
                // Some attacks cause specific exceptions
                if (throwable instanceof ClassNotFoundException) {
                    metadata.put("potential_attack", "gadget_chain_attempt");
                }
            }
            
            // Analyze call stack for additional context
            String stackTrace = getRelevantStackTrace();
            metadata.put("call_context", analyzeCallContext(stackTrace));
            
            logProxy.logMessage(NAMESPACE + ".deserialize", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently to avoid disrupting deserialization
        }
    }

    /**
     * Intercepts ObjectInputStream.resolveClass() to detect class resolution attacks.
     */
    @Advice.OnMethodEnter
    public static void resolveClass(@Advice.Argument(0) Object objectStreamClass) {
        try {
            if (objectStreamClass != null) {
                String className = objectStreamClass.toString();
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("operation", "resolve_class");
                metadata.put("class_name", className);
                
                // Check for dangerous classes being resolved
                String dangerousClass = checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "CRITICAL");
                    metadata.put("threat_type", "deserialization_gadget");
                }
                
                logProxy.logMessage(NAMESPACE + ".resolve", "INFO", metadata);
            }
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts ObjectOutputStream.writeObject() to monitor serialization.
     */
    @Advice.OnMethodEnter
    public static void writeObject(@Advice.This Object objectOutputStream,
                                 @Advice.Argument(0) Object obj) {
        try {
            if (obj != null) {
                String className = obj.getClass().getName();
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("operation", "serialize");
                metadata.put("serialized_class", className);
                metadata.put("serialization_type", "java_native");
                
                // Check if serializing dangerous objects
                String dangerousClass = checkDangerousClass(className);
                if (dangerousClass != null) {
                    metadata.put("dangerous_class", dangerousClass);
                    metadata.put("risk_level", "HIGH");
                }
                
                logProxy.logMessage(NAMESPACE + ".serialize", "INFO", metadata);
            }
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Jackson ObjectMapper.readValue() for JSON deserialization.
     */
    @Advice.OnMethodExit
    public static void jacksonReadValue(@Advice.Argument(0) Object input,
                                      @Advice.Return Object result,
                                      @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "json_jackson");
            
            if (input != null) {
                metadata.put("input_type", input.getClass().getSimpleName());
                
                // Analyze input for suspicious patterns
                String inputStr = input.toString();
                if (inputStr.length() < 10000) { // Only analyze reasonably sized inputs
                    if (inputStr.contains("@type") || inputStr.contains("@class")) {
                        metadata.put("polymorphic_deserialization", "true");
                        metadata.put("risk_level", "HIGH");
                    }
                    
                    // Check for suspicious class references
                    for (String dangerousClass : DANGEROUS_CLASSES) {
                        if (inputStr.contains(dangerousClass)) {
                            metadata.put("dangerous_class_reference", dangerousClass);
                            metadata.put("risk_level", "CRITICAL");
                            break;
                        }
                    }
                }
            }
            
            if (result != null) {
                metadata.put("result_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
                metadata.put("error_message", throwable.getMessage());
            }
            
            logProxy.logMessage(NAMESPACE + ".json", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

    /**
     * Intercepts Gson.fromJson() for JSON deserialization.
     */
    @Advice.OnMethodExit
    public static void gsonFromJson(@Advice.Argument(0) String json,
                                  @Advice.Return Object result,
                                  @Advice.Thrown Throwable throwable) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("operation", "deserialize");
            metadata.put("serialization_type", "json_gson");
            
            if (json != null && json.length() < 10000) {
                // Analyze JSON for suspicious patterns
                if (json.contains("@type") || json.contains("class")) {
                    metadata.put("polymorphic_deserialization", "true");
                    metadata.put("risk_level", "MEDIUM");
                }
            }
            
            if (result != null) {
                metadata.put("result_class", result.getClass().getName());
            }
            
            if (throwable != null) {
                metadata.put("error", throwable.getClass().getSimpleName());
            }
            
            logProxy.logMessage(NAMESPACE + ".gson", "INFO", metadata);
            
        } catch (Exception e) {
            // Fail silently
        }
    }

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
                for (String dangerousClass : DANGEROUS_CLASSES) {
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

    // Helper methods
    private static String checkDangerousClass(String className) {
        for (String dangerous : DANGEROUS_CLASSES) {
            if (className.contains(dangerous)) {
                return dangerous;
            }
        }
        return null;
    }

    private static String getRelevantStackTrace() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            StringBuilder relevant = new StringBuilder();
            
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("org.jvmxray") && 
                    !className.startsWith("java.io") &&
                    !className.startsWith("sun.") &&
                    relevant.length() < 500) {
                    relevant.append(className).append(".").append(element.getMethodName()).append(";");
                }
            }
            
            return relevant.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String analyzeCallContext(String stackTrace) {
        if (stackTrace.contains("RMI") || stackTrace.contains("rmi")) {
            return "rmi_context";
        } else if (stackTrace.contains("servlet") || stackTrace.contains("http")) {
            return "web_context";
        } else if (stackTrace.contains("jndi") || stackTrace.contains("ldap")) {
            return "jndi_context";
        } else {
            return "general_context";
        }
    }
}