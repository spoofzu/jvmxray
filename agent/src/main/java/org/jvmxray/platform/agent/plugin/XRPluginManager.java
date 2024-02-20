package org.jvmxray.platform.agent.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Manages JVMXRay plugins.  Plugins are JVMXRay's extensibility model for
 * generating/reporting on security events.  Plugins may support streaming
 * security events in near-realtime or logging security events for offline
 * reporting.  Plugins can be a directory; for example, at code development
 * time or stored in self-contained JVMXRay jars where the manifest
 * includes a property <code>jvmxray-plugin-main</code> who value specifies
 * a class conforming to the
 * <code>org.jvmxray.platform.shared.property.XRIProperties</code>
 * specification.
 *
 * @author Milton Smith
 */
public class XRPluginManager {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.agent.plugin.XRPluginManager");
    private static final String PLUGIN_MAIN_PROP = "jvmxray-plugin-main";
    private static final long PLUGIN_SCAN_DELAY = 30 * 1000; // 30 seconds
    private static final long PLUGIN_SCAN_PERIOD = 10 * 1000; // 10 seconds
    private String pluginDirectory;
    private Set<String> knownJars;
    private BlockingQueue<File> processingQueue;
    private Timer directoryMonitorTimer;
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private ExecutorService processingExecutor;

    public XRPluginManager() {}

    /**
     * Initialization the plugin manager.
     * @param pluginDirectory The directory plugins are located.
     */
    public void init(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        this.knownJars = ConcurrentHashMap.newKeySet();
        this.processingQueue = new LinkedBlockingQueue<>();
        this.directoryMonitorTimer = new Timer();
        this.processingExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Start a background thread that scans for new plugins.
     */
    public void start() {
        directoryMonitorTimer = new Timer();
        directoryMonitorTimer.schedule(new DirectoryMonitorTask(), PLUGIN_SCAN_DELAY, PLUGIN_SCAN_PERIOD);
    }

    /**
     * Scan directory for jars.
     * @param directory Directory to scan.
     */
    private void scanDirectory(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                if (knownJars.add(file.getAbsolutePath())) {
                    processingQueue.offer(file);
                }
            }
        }
    }

    /**
     * Background scanning task.  A single background scanning task is scheduled.
     * If task is still running then the current task exists.
     */
    class DirectoryMonitorTask extends TimerTask {
        @Override
        public void run() {
            if (!isTaskRunning.compareAndSet(false, true)) {
                // Exit if another task is running
                return;
            }
            try {
                scanDirectory(new File(pluginDirectory));
            } finally {
                isTaskRunning.set(false);
            }
        }
    }

    /**
     * Process each plugin in the queue.
     */
    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                File source = processingQueue.take();
                loadAndInitializePlugins(source);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void loadAndInitializePlugins(File source) {
        XRPluginClassLoader classLoader = new XRPluginClassLoader(source);
        // Example: Get class names from a config file, manifest, or directory scanning
        for (String className : getClassNamesToLoad(source)) {
            try {
                Class<?> pluginClass = classLoader.loadClass(className);
                Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
                // Assuming the init method is a public method with no parameters
                Method initMethod = pluginClass.getMethod("init");
                initMethod.invoke(pluginInstance);
                // Optionally, add the instance to a collection for further management
            } catch (Exception e) {
                logger.error("Error loading or initializing plugin: " + className, e);
            }
        }
    }

    private List<String> getClassNamesToLoad(File source) {
        // Implement logic to retrieve class names from the source
        // This can be based on scanning the directory/JAR, reading a config file, etc.
        return new ArrayList<>();
    }

    /**
     * Process a plugin.
     * @param jar A jar file conforming to the JVMXRay plugin
     *            specification.
     */
    private void processJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String pluginClassName = manifest.getMainAttributes().getValue(PLUGIN_MAIN_PROP);
                if (pluginClassName != null) {
                    System.out.println("Plugin found in JAR: " + jar.getAbsolutePath());
                    loadAndInitializePlugin(jar, pluginClassName);
                }
            }
        } catch (IOException e) {
            logger.error("Error processing jar: " + jar.getPath(), e);
        }
    }

    private void loadAndInitializePlugin(File jar, String className) {
        try {
            URL jarUrl = jar.toURI().toURL();
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl})) {
                Class<?> pluginClass = Class.forName(className, true, classLoader);
                Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();

                Method initMethod = pluginClass.getMethod("init");
                initMethod.invoke(pluginInstance);
                // TODO call other plugin methods as well.
            }
        } catch (Exception e) {
            logger.error("Error loading or initializing plugin: " + className, e);
        }
    }

    /**
     * Cleanup in the event of process exit.
     */
    public void shutdown() {
        directoryMonitorTimer.cancel();
        processingExecutor.shutdownNow();
    }

}

