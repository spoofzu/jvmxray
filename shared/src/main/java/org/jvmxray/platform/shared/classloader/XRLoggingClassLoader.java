package org.jvmxray.platform.shared.classloader;

import org.jvmxray.platform.shared.event.XREventLogger;

/**
 * The logging classloader logs classes loaded to the
 * <code>org.jvmxray.agent.events.class.loaded</code> logger namespace facilitating
 * user configuration via <code>logback.xml</code>.
 *
 * @author Milton Smith
 */
public class XRLoggingClassLoader extends ClassLoader {

    public XRLoggingClassLoader(ClassLoader parent) {
        super(parent);
    }

    public XRLoggingClassLoader(String loaderName, ClassLoader parent) {
        super(loaderName, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        XREventLogger.logEvent("org.jvmxray.events.class.loaded", name, "", "");
        return super.findClass(name);
    }

}
