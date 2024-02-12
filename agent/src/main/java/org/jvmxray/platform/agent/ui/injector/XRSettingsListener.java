package org.jvmxray.platform.agent.ui.injector;

import java.util.Map;

/**
 * Listener for primary xray debug ui.  Used to return the
 * various selection results back to the caller.  Caller
 * implements the listener and receives an update with
 * latests settings at the appropriate time.
 * @author Milton Smith
 */
public interface XRSettingsListener {
        void onSettingsChange(Map map);
}
