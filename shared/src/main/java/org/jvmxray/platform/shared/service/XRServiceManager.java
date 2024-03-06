package org.jvmxray.platform.shared.service;

import org.jvmxray.platform.shared.property.XRIProperties;
import org.jvmxray.platform.shared.property.XRServicePlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Extensible service framework.  JVMXRay services reside in a
 * in a services folder beneath the JVMXRay root like the following,
 * <code>{jvmxrayHome}+PATH_SEPARATOR+services</code>
 * The previous path is known as the Services Root.  In this folder
 * exists a property file,
 * <code>svcplatform.properties</code>
 * The svcplatform.properties provides the settings used by the
 * service platform.
 *
 * Services can be client side or service
 * side.  Code that implements this service model will initialize this
 * service manager with an initialized XRIProperties instance.  At a
 * minimum, an initialized properties instance must be specified.  The
 * following properties are used if provided or defaults are assigned.
 * <pre>
 * PROPERTY KEY          DEFAULT VALUE      MEANING
 * service-start         service-main       Property in jar manifest identifying service class (implements XRIEventListener).
 * package-scan-delay    30000              30 seconds, milliseconds to delay before scanning for new service jars.
 * package-scan-period   10000              10 seconds, milliseconds interval for new service jar scanning.
 * </pre>
 *
 * @author Milton Smith
 */
public class XRServiceManager {
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.shared.service.XRServiceManager");
    private static final String PATH_SEPARATOR = System.getProperty("file.separator");
    // Define the valid character sets for directory names based on OS type.
    private static final String COMPLIANT_PATH_CHARS = FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ?
            "[a-zA-Z0-9_/]" : "[a-zA-Z0-9]";
    /** Service Platform Property, Time to delay scanning for new service jars. */
    private static final String PACKAGE_SCAN_DELAY = "package-scan-delay";
    /** Service Platform Property, Period to scan for new service jars. */
    private static final String PACKAGE_SCAN_PERIOD = "package-scan-period";
    /** Thread name assigned to jar scanning worker. */
    private static final String SCAN_WORKER_NAME = "svc-scan";
    /** Service property file name.  Default for all services. */
    public static final String SERVICE_PROPERTIES = "service.properties";
    /** Service Platform Property, services path. */
    public static final String SERVICES_PATH = "services";
    /** Service Property, service name.  This is also the name of the services folder.*/
    public static final String SERVICE_NAME = "service-name";
    /** Service classloader name. */
    private static final String SERVICE_CLZLOADER_NAME = "svc_loader";
    private static final long PACKAGE_SCAN_DELAY_DEFAULT = 30 * 1000; // 30 seconds
    private static final long PACKAGE_SCAN_PERIOD_DEFAULT = 5 * 60 * 1000; // 5 min
    private XRIProperties properties;
    private long serviceScanDelay;
    private long serviceScanPeriod;
    private Path servicesPath;
    private URL[] serviceRoots;
    private XRServiceDispatcher serviceDispatcher = new XRServiceDispatcher();
    private Timer serviceScanTimer;
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private ExecutorService processingExecutor;
    private boolean bInvalidateCache = false;
    private boolean bCompleteRemoval = false;
    private XRServicePlatformProperties platformProperties;
    private Path jvmxrayHome;

    public XRServiceManager(Path jvmxrayHome, URL[] serviceRoots) {
        if( jvmxrayHome == null ) {
            throw new RuntimeException("Service manager init failed.  No JVMXRay home specified.");
        }
        if( serviceRoots == null && serviceRoots.length>0 ) {
            throw new RuntimeException("1 or more service roots must be specified.");
        }
        platformProperties = new XRServicePlatformProperties(jvmxrayHome);
        this.jvmxrayHome = jvmxrayHome;
        this.serviceRoots = serviceRoots;
    }

    /**
     * Initialization the service manager.
     */
    public void init() throws IOException {
        // Initialize platform properties, create services directory
        platformProperties.init();
        serviceScanDelay = platformProperties.getLongProperty(PACKAGE_SCAN_DELAY, PACKAGE_SCAN_DELAY_DEFAULT);
        serviceScanPeriod = platformProperties.getLongProperty(PACKAGE_SCAN_PERIOD, PACKAGE_SCAN_PERIOD_DEFAULT);
        // Form services root, that holds various jars.  A '.' as the service path is equivalent to jvmxrayHome.
        servicesPath = Path.of(platformProperties.getProperty(SERVICES_PATH,
                Path.of(jvmxrayHome.toString(),"services").toAbsolutePath().toString()));
        if( servicesPath.startsWith(".") ) {
            servicesPath = Path.of(jvmxrayHome.toString(), servicesPath.toString().substring(1));
            servicesPath = servicesPath.toAbsolutePath();
        }
        // For now, services root must be a subdirectory of jvmxrayRoot.
        if (!servicesPath.startsWith(jvmxrayHome)) {
            throw new SecurityException("The specified SERVICES_PATH is not within the permitted scope.  path="+servicesPath.toString());
        }
        // If not saved, save a copy of service platform properties.
        if( platformProperties.isModified() ) {
            platformProperties.saveProperties();
        }
        Files.createDirectories(servicesPath,getDirectoryAttributes());
    }

    /**
     * Rebuild cache on the next scan.
     * @param completeRemoval true, invalidate code and configuration.  false, invalidate
     *                        code only, configuration remains.
     */
    public void invalidateServices(boolean completeRemoval) {
        bInvalidateCache = true;
        bCompleteRemoval = completeRemoval;
    }

    /**
     * Start background scanning thread for changes (new/removed)
     * to service jars.
     */
    public void start() {
        this.serviceScanTimer = new Timer();
        this.processingExecutor = Executors.newSingleThreadExecutor();
        serviceScanTimer = new Timer(SCAN_WORKER_NAME);
        serviceScanTimer.schedule(new XRServiceScanWorker(), serviceScanDelay, serviceScanPeriod);
    }

    /**
     * Cleanup in the event of process exit.
     */
    public void shutdown() {
        serviceScanTimer.cancel();
        processingExecutor.shutdownNow();
    }

    /**
     * Background scanning task to identify new jars added or removed.  A single
     * background scanning task is scheduled. If task is running, then the
     * current task exists.
     */
    class XRServiceScanWorker extends TimerTask {
        @Override
        public void run() {
            if (!isTaskRunning.compareAndSet(false, true)) {
                return;
            }
            long startTime = System.currentTimeMillis();
            try {
                if (serviceRoots != null) {
                    try {
                        scanServices(serviceRoots);
                    }catch(Exception e) {
                        logger.error("Service scan error encountered.  Review service roots.",e);
                    }
                }
            } finally {
                isTaskRunning.set(false);
            }
            long finishTime = System.currentTimeMillis();
            logger.info("Service sweep. Elapsed time="+(finishTime-startTime));
        }
    }

    /**
     * Scans remote resources to ensure cache is up to date.
     * @param urls Remote file resources.
     */
    private void scanServices(URL[] urls) {
        for( URL url : urls ) {
            String protocol = url.getProtocol();
            String target = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
            // For now we only allow JARs
            if( !target.endsWith(".jar") ) {
                logger.error("Only fully qualified Jar names are supported for services, skipped.  Continuing. url="+target);
                continue;
            }
            try {
                File targetFile = new File(target);
                // If file not cached -1 is returned.  Causes file to be cached.
                long cachedFileTimestamp = getLocalFileTimestamp(targetFile);
                // Fully qualified jar URLs on remote server.
                if (protocol.startsWith("http")) {
                    long remoteFileTimestamp = getRemoteFileTimestamp(url);
                    if( remoteFileTimestamp > cachedFileTimestamp ) {
                        cacheServiceJar(url,remoteFileTimestamp);
                    }
                // Fully qualified jar URLs on local host.
                } else if (protocol.equals("file")) {
                    File file = new File(url.toURI());
                    if (file.isFile()) {
                        cacheServiceJar(url, getLocalFileTimestamp(file));
                    } else {
                        // Unlikely but added for safety, since we check to see if service url ends with, *.jar.
                        logger.warn("Service directories not supported, skipped.  Specify fully qualified jar.  Continuing. url="+target);
                        continue;
                    }
                } else {
                    logger.warn("Unsupported service jar protocol specified.  protocol="+protocol);
                }
                // Additional protocol handling can be added here
            } catch (URISyntaxException | IOException e) {
                logger.error("Error processing service URL: " + urls, e);
            }
        }
    }

    //TODO Need to continue here.  Initialize services, init(), create a thread and classloader for each and call run().

    private void initAndExecuteServices() {
    }

//    private void loadAndInitializeServices(File source) throws MalformedURLException {
//        ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
//        XRLoggingClassLoader serviceClzLoader = new XRLoggingClassLoader(SERVICE_CLZLOADER_NAME,
//                new URL[]{source.toURI().toURL()},
//                parentLoader);
//        // Get class names from a config file, manifest, or directory scanning
//        for (String className : getClassNamesToLoad(source)) {
//            try {
//                Class<?> pluginClass = serviceClzLoader.loadClass(className);
//                Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
//                // Assuming the init method is a public method with no parameters
//                Method initMethod = pluginClass.getMethod("init");
//                initMethod.invoke(pluginInstance);
//                // Optionally, add the instance to a collection for further management
//            } catch (Exception e) {
//                logger.error("Error loading or initializing service.  source="+source+" class="+className, e);
//            }
//        }
//    }

    /**
     * Retrieve name timestamp of last modification.
     * @param name Fully qualified target name.  For example,
     *             name:///home/username/service.jar
     * @return Timestamp of last modification or -1 if the name does
     * not exist.
     */
    private long getLocalFileTimestamp(File name) throws IOException {
        long result = -1;
        Path fullyQualifiedFile = null;
        if( name==null ) {
            throw new IOException("No filename specified.  name="+name);
        }
        if( !name.isFile() ) {
            throw new IOException("Name parameter must specify a fully qualified name. name="+name.getAbsolutePath());
        }
        fullyQualifiedFile = name.toPath();
        boolean isFileExists = Files.exists(fullyQualifiedFile);
        boolean isCanRead = Files.isReadable(fullyQualifiedFile);
        if (isFileExists && isCanRead) {
            BasicFileAttributes attr = Files.readAttributes(fullyQualifiedFile, BasicFileAttributes.class);
            result = attr.lastModifiedTime().toMillis();
        } else {
            logger.debug("Can't get file timestamp.  File name incorrect, doesn't exist, or no read permissions.  file="+name);
        }
        return result;
    }

    /**
     * Retrieve timestamp of last modification for remote resource.
     * @param target URL of remote resource.  For example, http://www.company.com/path/myjar.jar
     * @return Timestamp of last modification or -1 if the file does
     * not exist.  Note: this is the timestamp on the remote file
     * as specified by the server.  For instance, if the file on the
     * server was a year old then the modification timestamp returned
     * will be as year ago.
     */
    private long getRemoteFileTimestamp(URL target) {
        long serverLastModifiedTime = -1;
        try {
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            serverLastModifiedTime = connection.getLastModified();
            connection.disconnect();
        } catch (IOException e) {
            String msg = (target != null) ? target.toString() : "unknown";
            logger.error("Problem reading last modification from remote server.  file="+msg,e);
        }
        return serverLastModifiedTime;
    }

    /**
     * Cache a remote file on local disk.  Downloads <code>url</code> to
     * a tmp file in the service cache root.  Next, it creates a folder
     * structure based on the host.  For example, a URL like
     * http://www.company.com/path/my.jar will be saved to local cache
     * serviceCacheRoot/com.company.www/path2/path1/my.jar
     * @param url Full path to the remove resource.
     * @param timestamp Cached file modification timestamp.
     * @throws IOException Throw on problems fetching remote resource and
     *   cache management problems.
     */
    private void cacheServiceJar(URL url, long timestamp) throws IOException {
        // For now we only allow fully qualified JARs
        if( !url.getFile().endsWith(".jar") ) {
            logger.warn("Only fully qualified Jar names are supported for services, skipped.  Continuing. url="+url);
            return;
        }
        // Clean cache.
        if(bInvalidateCache) {
            deleteCachedFiles(bCompleteRemoval);
            bInvalidateCache = false;
        }
        // Create a temp file target to download service jar.
        String fileName = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
        String fileNameNoExt = fileName.substring(0,fileName.lastIndexOf('.'));
        File tmpServiceJarFile = File.createTempFile(fileNameNoExt, "tmp");
        if( !tmpServiceJarFile.canWrite() ) {
            throw new IOException("Can't create temp file, check directory permissions.  path="+tmpServiceJarFile.getPath());
        }
        // Download service jar.
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        long bytesWritten = 0;
        logger.debug("Downloading service jar.  url="+url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(tmpServiceJarFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;
            }
        } finally {
            connection.disconnect(); // Close the connection
            endTime = System.currentTimeMillis();
        }
        logger.debug("Service jar downloaded: bytes written {}, elapsed(ms) {},  url={}", bytesWritten, (endTime-startTime), url);
        // Extract the service properties from service jar temp file.
        JarFile tmpServiceJar = null;
        Properties tmpServiceProperties = null;
        Path serviceRoot = null;
        String serviceName = null;
        try {
            tmpServiceJar = new JarFile(tmpServiceJarFile);
            JarEntry entry = tmpServiceJar.getJarEntry(SERVICE_PROPERTIES);
            if (entry != null) {
                try (InputStream inputStream = tmpServiceJar.getInputStream(entry)) {
                    tmpServiceProperties = new Properties();
                    tmpServiceProperties.load(inputStream);
                    serviceName = (String)tmpServiceProperties.get(SERVICE_NAME);
                    if(!Pattern.matches(COMPLIANT_PATH_CHARS +"*",serviceName ) ) {
                        logger.warn("Service name invalid. Service names must only use OS path compliant characters like"+COMPLIANT_PATH_CHARS+", service skipped.  Continuing.  Service name as specified, "+serviceName+" url="+url);
                    }
                    serviceRoot = Path.of(servicesPath.toString(),serviceName);
                }
            } else {
                throw new IOException("Specified service jar contains no "+SERVICE_PROPERTIES+", skipping. url=" + url);
            }
        } finally {
            try {
                if(tmpServiceJar!=null) tmpServiceJar.close();
            } catch (IOException e) {}
        }
        // Create the service directory per user SERVICE_NAME specification.
        Files.createDirectories(serviceRoot, getDirectoryAttributes());
        // Save extracted service properties in service root, if it doesn't exist.
        File serviceProperties = new File(serviceRoot.toFile(),SERVICE_PROPERTIES);
        if( !serviceProperties.exists() ) {
            FileOutputStream out = new FileOutputStream(serviceProperties);
            tmpServiceProperties.store(out,"JVMXRay Service "+serviceName);
        } else {
            logger.debug("Service properties found.  No default properties written.  url="+url);
        }
        // Create or update service jar with cached service jar.
        Path targetFile = Path.of(serviceRoot.toString(),fileName);
        Files.move(tmpServiceJarFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        // Importaint: here we set last modified timestamp to the last modified as reported by the server.
        Files.setLastModifiedTime(targetFile, FileTime.fromMillis(timestamp));
    }

    private static FileAttribute<?>[] getDirectoryAttributes() {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
        return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(permissions) };
    }

    public void deleteCachedFiles(boolean completeRemoval) throws IOException {
        String spec = (completeRemoval) ? ".*" : ".jar";
        try (Stream<Path> paths = Files.walk(servicesPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(spec))
                    .forEach(path -> deleteFile(path));
            if (completeRemoval) {
                deleteSubdirectories(servicesPath);
            }
        }
    }

    public void deleteFile(Path file) {
        try {
            Files.delete(file);
            logger.debug("Deleted cached file.  file=" + file);
        } catch (IOException e) {
            logger.error("Unable to delete cached file. file=" + file + " msg=" + e.getMessage(), e);
        }
    }

    public static void deleteSubdirectories(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isDirectory)
                    .filter(path -> !path.equals(directory))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (file.delete()) {
                            logger.debug("Deleted cached services directory. Directory={}",file);
                        } else {
                            logger.warn("Failed to delete cached services directory, skipping. Directory={}",file);
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to delete cached service directory.  Directory="+directory, e);
        }
    }

}

