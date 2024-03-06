module jvmxray.agent {
    // Project Dependencies
    requires jvmxray.shared;

    // Project External Dependencies
    requires java.desktop;
    requires org.slf4j;
    requires java.instrument;
    requires jdk.attach;
    requires commons.cli;

    // Private Project Exports
    // None

    // Public API Exports
    // None
}