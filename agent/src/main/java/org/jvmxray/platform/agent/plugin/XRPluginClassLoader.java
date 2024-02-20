package org.jvmxray.platform.agent.plugin;

import org.jvmxray.platform.shared.classloader.XRLoggingClassLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

//TODO: Finish up plugin classloader.

/**
 * Manage JVMXRay plugin classes and resources.  This classloader manages
 * resources for plugins.
 */
public class XRPluginClassLoader extends XRLoggingClassLoader {

    //TODO: implement plugin sandboxing.

    private final File source;

    /**
     * Manage JVMXRay plugin.
     * @param source Base directory where plugin resources(e.g., class, properties) or
     *               JVMXRay plugin jar are located.
     */
    public XRPluginClassLoader(File source) {
        this(source,Thread.currentThread().getContextClassLoader());
    }

    /**
     * Manage JVMXRay plugin.
     * @param source Base directory where plugin resources(e.g., class, properties) or
     *               JVMXRay plugin jar are located.
     * @param parent Parent classloader for delegation.
     */
    public XRPluginClassLoader(File source, ClassLoader parent) {
        this(source,"xrplugin",parent);
    }

    /**
     * Manage JVMXRay plugin.
     * @param source Base directory where plugin resources(e.g., class, properties) or
     *               JVMXRay plugin jar are located.
     * @param loaderName Human-readable classloader name.  Used when examing threads
     *                   and logging durring troubleshooting.
     * @param parent Parent classloader for delegation.
     */
    public XRPluginClassLoader(File source, String loaderName, ClassLoader parent) {
        super(loaderName, parent);
        this.source = source;
    }

    /**
     * Located plugin resources.
     * @param name
     *          The <a href="#binary-name">binary name</a> of the class
     *
     * @return Target class.
     * @throws ClassNotFoundException Thrown if class not found.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            if (source.isDirectory()) {
                return loadClassFromDirectory(name);
            } else if (source.isFile() && source.getName().endsWith(".jar")) {
                return loadClassFromJar(name);
            } else {
                throw new ClassNotFoundException("Unsupported source for class loading");
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("Could not load class " + name, e);
        }
    }

    /**
     * Load classes from directory path.
     * @param name Fully qualified class name.  For example, <code>org.jvmxray.platform.agent.plugin.YourPlugin</code>
     * @return Target class.
     * @throws IOException Thrown on trouble loading.
     */
    private Class<?> loadClassFromDirectory(String name) throws IOException {
        String classPath = name.replace('.', '/') + ".class";
        Path classFilePath = new File(source, classPath).toPath();
        byte[] classData = Files.readAllBytes(classFilePath);
        return defineClass(name, classData, 0, classData.length);
    }

    /**
     * Load classes from JVMXRay jar.
     * @param name Fully qualified class name.  For example, <code>org.jvmxray.platform.agent.plugin.YourPlugin</code>
     * @return Target class.
     * @throws IOException Thrown on trouble loading.
     */
    private Class<?> loadClassFromJar(String name) throws IOException, ClassNotFoundException {
        try (JarFile jar = new JarFile(source)) {
            String classPath = name.replace('.', '/') + ".class";
            ZipEntry entry = jar.getEntry(classPath);
            if (entry == null) {
                throw new ClassNotFoundException("Class " + name + " not found in " + source.getName());
            }
            byte[] classData = jar.getInputStream(entry).readAllBytes();
            return defineClass(name, classData, 0, classData.length);
        }
    }
}
