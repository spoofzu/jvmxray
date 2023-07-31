package org.jvmxray.agent.ui;

import java.util.Map;

/**
 * Listener for primary xray debug ui.  Used to return the
 * various selection results back to the caller.  Caller
 * implements the listener and receives an update with
 * latests settings at the appropriate time.
 * @author Milton Smith
 */
public interface JVMXRaySettingsListener {
        void onSettingsChange(Map map);
}
