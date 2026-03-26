package org.jvmxray.agent.sensor.serialization;

/**
 * Utility methods shared by serialization interceptors.
 * Enhanced with CVE references, gadget chain names, and ysoserial payload detection.
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
        "org.apache.openjpa.ee.JNDIManagedRuntime",
        // Additional dangerous classes from metadata-improvements.md
        "org.mozilla.javascript.NativeJavaObject",
        "org.python.core.PyFunction",
        "groovy.util.Expando",
        "org.codehaus.groovy.runtime.MethodClosure",
        "org.apache.commons.io.output.DeferredFileOutputStream",
        "org.apache.commons.fileupload.disk.DiskFileItem",
        "javax.management.BadAttributeValueExpException",
        "org.apache.catalina.tribes.transport.bio.PooledMultiSender",
        "com.mchange.v2.c3p0.WrapperConnectionPoolDataSource",
        "org.jboss.weld.interceptor.builder.MethodReference"
    };

    // CVE mappings for known gadget chains
    private static final java.util.Map<String, String> CVE_MAPPINGS = new java.util.HashMap<>();
    static {
        CVE_MAPPINGS.put("org.apache.commons.collections.functors.InvokerTransformer", "CVE-2015-4852,CVE-2015-7501");
        CVE_MAPPINGS.put("org.apache.commons.collections4.functors.InvokerTransformer", "CVE-2015-4852,CVE-2015-7501");
        CVE_MAPPINGS.put("com.sun.rowset.JdbcRowSetImpl", "CVE-2018-14718,CVE-2017-3248");
        CVE_MAPPINGS.put("org.apache.xalan.xsltc.trax.TemplatesImpl", "CVE-2013-2186,CVE-2017-9805");
        CVE_MAPPINGS.put("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl", "CVE-2013-2186");
        CVE_MAPPINGS.put("org.apache.commons.beanutils.BeanComparator", "CVE-2014-0050,CVE-2019-10086");
        CVE_MAPPINGS.put("org.hibernate.engine.spi.TypedValue", "CVE-2020-25638");
        CVE_MAPPINGS.put("org.springframework.beans.factory.ObjectFactory", "CVE-2011-2894,CVE-2016-1000027");
        CVE_MAPPINGS.put("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource", "CVE-2019-5427");
        CVE_MAPPINGS.put("org.apache.wicket.util.upload.DiskFileItem", "CVE-2020-11976");
        CVE_MAPPINGS.put("org.codehaus.groovy.runtime.MethodClosure", "CVE-2015-3253");
        CVE_MAPPINGS.put("java.rmi.server.RemoteObjectInvocationHandler", "CVE-2017-3241");
    }

    // Gadget chain name mappings
    private static final java.util.Map<String, String> GADGET_CHAIN_NAMES = new java.util.HashMap<>();
    static {
        GADGET_CHAIN_NAMES.put("org.apache.commons.collections.functors.InvokerTransformer", "CommonsCollections1-7");
        GADGET_CHAIN_NAMES.put("org.apache.commons.collections4.functors.InvokerTransformer", "CommonsCollections5-7");
        GADGET_CHAIN_NAMES.put("org.apache.commons.beanutils.BeanComparator", "CommonsBeanutils1");
        GADGET_CHAIN_NAMES.put("org.hibernate.engine.spi.TypedValue", "Hibernate1-2");
        GADGET_CHAIN_NAMES.put("com.sun.rowset.JdbcRowSetImpl", "JRMPClient,JNDI");
        GADGET_CHAIN_NAMES.put("org.apache.xalan.xsltc.trax.TemplatesImpl", "JDK7u21,Jython1");
        GADGET_CHAIN_NAMES.put("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl", "JDK7u21");
        GADGET_CHAIN_NAMES.put("org.springframework.beans.factory.ObjectFactory", "Spring1-2");
        GADGET_CHAIN_NAMES.put("org.codehaus.groovy.runtime.MethodClosure", "Groovy1");
        GADGET_CHAIN_NAMES.put("clojure.core.proxy$clojure.lang.APersistentMap", "Clojure");
        GADGET_CHAIN_NAMES.put("org.apache.myfaces.context.servlet.FacesContextImpl", "Myfaces1-2");
        GADGET_CHAIN_NAMES.put("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource", "C3P0");
    }

    // Ysoserial payload identifiers
    private static final java.util.Map<String, String> YSOSERIAL_PAYLOADS = new java.util.HashMap<>();
    static {
        YSOSERIAL_PAYLOADS.put("org.apache.commons.collections.functors.InvokerTransformer", "CommonsCollections1");
        YSOSERIAL_PAYLOADS.put("org.apache.commons.collections4.functors.InvokerTransformer", "CommonsCollections5");
        YSOSERIAL_PAYLOADS.put("org.apache.commons.beanutils.BeanComparator", "CommonsBeanutils1");
        YSOSERIAL_PAYLOADS.put("org.hibernate.engine.spi.TypedValue", "Hibernate1");
        YSOSERIAL_PAYLOADS.put("com.sun.rowset.JdbcRowSetImpl", "URLDNS,JRMPClient");
        YSOSERIAL_PAYLOADS.put("org.codehaus.groovy.runtime.MethodClosure", "Groovy1");
        YSOSERIAL_PAYLOADS.put("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource", "C3P0");
    }

    /**
     * Checks if a class name matches known dangerous classes and returns metadata.
     */
    public static java.util.Map<String, String> checkDangerousClassWithMetadata(String className) {
        java.util.Map<String, String> result = new java.util.HashMap<>();

        for (String dangerous : DANGEROUS_CLASSES) {
            if (className.contains(dangerous)) {
                result.put("dangerous_class", dangerous);
                result.put("is_dangerous", "true");
                result.put("risk_level", "CRITICAL");

                // Add CVE references if available
                String cves = CVE_MAPPINGS.get(dangerous);
                if (cves != null) {
                    result.put("cve_references", cves);
                }

                // Add gadget chain name if available
                String gadgetChain = GADGET_CHAIN_NAMES.get(dangerous);
                if (gadgetChain != null) {
                    result.put("gadget_chain_name", gadgetChain);
                }

                // Add ysoserial payload if available
                String ysoserial = YSOSERIAL_PAYLOADS.get(dangerous);
                if (ysoserial != null) {
                    result.put("ysoserial_payload", ysoserial);
                }

                return result;
            }
        }

        result.put("is_dangerous", "false");
        return result;
    }

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
     * Gets CVE references for a dangerous class.
     */
    public static String getCVEReferences(String className) {
        for (java.util.Map.Entry<String, String> entry : CVE_MAPPINGS.entrySet()) {
            if (className.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets gadget chain name for a dangerous class.
     */
    public static String getGadgetChainName(String className) {
        for (java.util.Map.Entry<String, String> entry : GADGET_CHAIN_NAMES.entrySet()) {
            if (className.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Extracts the package name from a fully qualified class name.
     */
    public static String getClassPackage(String className) {
        if (className == null) return null;
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Checks if a class is a JDK class.
     */
    public static boolean isJDKClass(String className) {
        if (className == null) return false;
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("sun.") ||
               className.startsWith("com.sun.") ||
               className.startsWith("jdk.");
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

    /**
     * Determines the untrusted source context.
     */
    public static String determineUntrustedSource(String callContext) {
        switch (callContext) {
            case "rmi_context":
                return "remote_rmi_call";
            case "web_context":
                return "http_request";
            case "jndi_context":
                return "jndi_lookup";
            default:
                return "unknown_source";
        }
    }
}