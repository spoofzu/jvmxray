package org.jvmxray.platform.agent.plugin;

/**
 * Plugin event specification.
 */
public interface XRIPluginEvent {

    /**
     * Pause execution
     */
    void pause();

    /**
     * Resume execution
     */
    void resume();

}
