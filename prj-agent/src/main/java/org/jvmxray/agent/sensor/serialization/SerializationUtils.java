package org.jvmxray.agent.sensor.serialization;

/**
 * Utility methods shared by serialization interceptors.
 * 
 * @author Milton Smith
 */
public class SerializationUtils {
    
    // Known dangerous classes for deserialization attacks
    public static final String[] DANGEROUS_CLASSES = {
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
     * Checks if a class name matches known dangerous classes.
     */
    public static String checkDangerousClass(String className) {
        for (String dangerous : DANGEROUS_CLASSES) {
            if (className.contains(dangerous)) {
                return dangerous;
            }
        }
        return null;
    }

    /**
     * Gets a relevant stack trace for analysis.
     */
    public static String getRelevantStackTrace() {
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

    /**
     * Analyzes call context based on stack trace.
     */
    public static String analyzeCallContext(String stackTrace) {
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