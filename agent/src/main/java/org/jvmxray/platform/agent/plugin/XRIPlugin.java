package org.jvmxray.platform.agent.plugin;

/**
 * Interface all JVMXRay plugins must implement.  JVMXRay plugin
 * jars manifests include a <code>jvmxray-plugin-main</code> property
 * that specifies the class to launch.  The launchable class must
 * this interface.
 */
public interface XRIPlugin {

    /**
     * One time initialization when plugin loads.
     */
    void init();

    /**
     * Begin execution
     */
    void run();

    /**
     * Event handler for additional types.
     * @param event
     */
    void onEvent(XRIPluginEvent event);

    /**
     * Safe clean-up.  Called on server is shutting down.
     */
    void shutDown();

}
