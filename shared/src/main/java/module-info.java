module jvmxray.shared {
    // Project Dependencies
    // None

    // Project External Dependencies
    requires java.rmi;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.datastax.oss.driver.core;
    requires com.datastax.oss.driver.querybuilder;
    requires com.fasterxml.jackson.databind;

    // Private Project Exports
    exports org.jvmxray.platform.shared.classloader to jvmxray.agent, jvmxray.logserver;
    exports org.jvmxray.platform.shared.event to jvmxray.agent, jvmxray.logserver;
    exports org.jvmxray.platform.shared.property to jvmxray.agent, jvmxray.logserver;
    exports org.jvmxray.platform.shared.util to jvmxray.agent, jvmxray.logserver;

    // Public API Exports
    exports org.jvmxray.platform.shared.logback.appender.cassandra;
    exports org.jvmxray.platform.shared.logback.codec;
    exports org.jvmxray.platform.shared.logback.converter;
    exports org.jvmxray.platform.shared.service to jvmxray.agent, jvmxray.logserver;
}