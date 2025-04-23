/**
 * Module definition for the JVMXRay agent, a Java agent framework for monitoring
 * JVM activities such as file I/O, network operations, SQL queries, and uncaught
 * exceptions. This module configures exports, opens, and dependencies for core
 * functionality, sensors, and utilities, ensuring compatibility with Byte Buddy
 * instrumentation and JVM internals.
 */
module org.jvmxray.agent {
    // Export main package for agent entry point and public APIs
    exports org.jvmxray.agent.bin;

    // Export sensor packages for external access by other modules
    exports org.jvmxray.agent.sensor;

    // Export utility packages for logging and detection
    exports org.jvmxray.agent.util.log;
    exports org.jvmxray.agent.util.sensor;

    // Require the shared module
    requires org.jvmxray.shared;

    // Open packages to java.base for instrumentation and reflection
    opens org.jvmxray.agent.bin to java.base;
    opens org.jvmxray.agent.sensor to java.base;
    opens org.jvmxray.agent.util.log to java.base;
    opens org.jvmxray.agent.util.sensor to java.base;

    // Require essential JDK modules
    requires java.instrument;            // Java agent and instrumentation APIs
    requires java.management;            // Standard JMX APIs (e.g., MXBeans)
    requires jdk.management;             // For com.sun.management APIs (e.g., OperatingSystemMXBean)
    requires java.logging;               // Logging APIs for SLF4J integration
    requires java.sql;                   // SQL APIs for PreparedStatement instrumentation

    // Require third-party dependencies
    requires net.bytebuddy;              // Byte Buddy for instrumentation
    requires org.slf4j;

    // Optional: Require Servlet APIs (transitive for compatibility)
    // todo Needs further review.
    // requires transitive jakarta.servlet; // Jakarta EE Servlet API
    // requires transitive javax.servlet;   // Java EE Servlet API (fallback)
}