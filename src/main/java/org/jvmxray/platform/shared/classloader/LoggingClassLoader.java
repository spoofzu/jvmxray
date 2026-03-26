package org.jvmxray.platform.shared.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * The logging classloader logs classes loaded to the
 * <code>org.jvmxray.agent.events.class.loaded</code> logger namespace facilitating
 * user configuration via <code>logback.xml</code>.
 *
 * @author Milton Smith
 */
public class LoggingClassLoader extends URLClassLoader {

    private static final Logger eventLogger = LoggerFactory.getLogger("org.jvmxray.events.classloader.load.class");

    /**
     * Constructor
     * @param name class loader name; or null if not named
     */
    public LoggingClassLoader(String name) {
        this(name, new URL[]{});
    }

    /**
     * Constructor
     * @param name class loader name; or null if not named
     * @param urls array of URLs from which to load classes and resources
     */
    public LoggingClassLoader(String name, URL[] urls) {
        this(name,urls,Thread.currentThread().getContextClassLoader());
    }

    /**
     * Constructor
     * @param name class loader name; or null if not named
     * @param urls the URLs from which to load classes and resources
     * @param parent Parent classloader for delegation.
     */
    public LoggingClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name,urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class<?> clazz = super.loadClass(name);
            // Log the successful load with the actual classloader
            String classloader = clazz.getClassLoader() != null ? clazz.getClassLoader().toString() : "bootstrap";
            eventLogger.info("classloader={} class={} parent={}",classloader, name,getParent());
            return clazz;
        } catch (ClassNotFoundException e) {
            eventLogger.error("class={} parent={}",name,getParent());
            throw e;
        }
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
        Class<?> clazz = super.findClass(name);
        String classloader = clazz.getClassLoader() != null ? clazz.getClassLoader().toString() : "bootstrap";
        eventLogger.info("classloader={} class={} parent={}",classloader, name,getParent());
        return clazz;
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
        URL url = super.getResource(name);
        eventLogger.info("resource={} parent={}",name, getParent());
        return super.getResource(name);
    }

}
