package org.jvmxray.platform.shared.classloader;

import org.jvmxray.platform.shared.event.XREventLogger;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * The logging classloader logs classes loaded to the
 * <code>org.jvmxray.agent.events.class.loaded</code> logger namespace facilitating
 * user configuration via <code>logback.xml</code>.
 *
 * @author Milton Smith
 */
public class XRLoggingClassLoader extends URLClassLoader {

    /**
     * Constructor
     * @param name class loader name; or null if not named
     */
    public XRLoggingClassLoader(String name) {
        this(name, new URL[]{});
    }

    /**
     * Constructor
     * @param name class loader name; or null if not named
     * @param urls array of URLs from which to load classes and resources
     */
    public XRLoggingClassLoader(String name, URL[] urls) {
        this(name,urls,Thread.currentThread().getContextClassLoader());
    }

    /**
     * Constructor
     * @param name class loader name; or null if not named
     * @param urls the URLs from which to load classes and resources
     * @param parent Parent classloader for delegation.
     */
    public XRLoggingClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name,urls, parent);
    }

    /**
     * Logs target class.  Loaded classes are logged to <code>org.jvmxray.events.classloader.load.class</code>
     * where the fully qualified classname logged to logback logger keypair stored in the message. For example,
     * <code>P1=a.b.c.d</code>
     * @param name
     *          The <a href="#binary-name">binary name</a> of the class
     *
     * @return Target class.
     * @throws ClassNotFoundException Thrown if class not found.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        XREventLogger.logEvent("org.jvmxray.events.classloader.load.class", name, "", "");
        return super.findClass(name);
    }

    /**
     * Logs target resource.  Resources are files loaded from the classpath like java or xml property files.
     * Loaded resources are logged to <code>org.jvmxray.events.classloader.load.resource</code>
     *      * where the resource name is logged to logback logger keypair stored in the message. For example,
     *      * <code>P1=program.properties</code>
     * @param name
     *         The resource name
     *
     * @return URL to the resource.
     */
    @Override
    public URL getResource(String name) {
        XREventLogger.logEvent("org.jvmxray.events.classloader.load.resource", name, "", "");
        return super.getResource(name);
    }

}
