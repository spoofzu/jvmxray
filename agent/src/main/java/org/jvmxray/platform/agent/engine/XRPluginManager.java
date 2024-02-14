package org.jvmxray.platform.agent.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

public class XRPluginManager {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.agent.engine.XRPluginManager");
    private static final String PLUGIN_MAIN_PROP = "jvmxray-plugin-main";
    private static final long PLUGIN_SCAN_DELAY = 30 * 1000;
    private static final long PLUGIN_SCAN_PERIOD = 10 * 1000;
    private String pluginDirectory;
    private Set<String> knownJars;
    private BlockingQueue<File> processingQueue;
    private Timer directoryMonitorTimer;
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private ExecutorService processingExecutor;

    public XRPluginManager() {}

    public void init(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        this.knownJars = ConcurrentHashMap.newKeySet();
        this.processingQueue = new LinkedBlockingQueue<>();
        this.directoryMonitorTimer = new Timer();
        this.processingExecutor = Executors.newSingleThreadExecutor();

        start();
    }

    public void start() {
        directoryMonitorTimer = new Timer();
        directoryMonitorTimer.schedule(new DirectoryMonitorTask(), PLUGIN_SCAN_DELAY, PLUGIN_SCAN_PERIOD);
    }

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

    class DirectoryMonitorTask extends TimerTask {
        @Override
        public void run() {
            if (!isTaskRunning.compareAndSet(false, true)) {
                // Another instance of the task is still running, so exit this one
                return;
            }

            try {
                scanDirectory(new File(pluginDirectory));
            } finally {
                isTaskRunning.set(false);
            }
        }
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                File jar = processingQueue.take();
                processJar(jar);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String value = manifest.getMainAttributes().getValue(PLUGIN_MAIN_PROP);
                if (value != null) {
                    // The JAR has the 'jvmxray-plugin-main' attribute, add further processing here
                    System.out.println("Plugin found in JAR: " + jar.getAbsolutePath());
                    // Perform additional processing as required
                }
            }
        } catch (IOException e) {
            logger.error("Error processing jar: " + jar.getPath(), e);
        }
    }

    public void shutdown() {
        directoryMonitorTimer.cancel();
        processingExecutor.shutdownNow();
    }

}

