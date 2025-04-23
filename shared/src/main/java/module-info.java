module org.jvmxray.shared {
    // Export shared packages
    exports org.jvmxray.platform.shared.classloader;
    exports org.jvmxray.platform.shared.log;
    exports org.jvmxray.platform.shared.property;
    exports org.jvmxray.platform.shared.util;

    // Require essential JDK modules
    requires java.rmi;

    // Require dependencies
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires logback.json.classic;
    requires logback.jackson;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
}