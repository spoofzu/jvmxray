package org.jvmxray.platform.shared.service;

import org.jvmxray.platform.shared.property.XRIProperties;
import org.jvmxray.platform.shared.property.XRPropertyBase;

import java.nio.file.Path;

/**
 * JVMXRay service properties.  Each service initialized will include one service.properties
 * file within it's service root.  For unpacked services (e.g., directory) a service.properties
 * is located within the directory.  For packaged services (e.g., jar) the service.properties
 * is located in the jar root.  Regardless of the root, a directory will be created at,
 * <pre>
 * {jvmxrayHome}+{PATH_SEPARATOR}+services+{PATH_SEPARATOR}+{serviceName}
 * </pre>
 * If the service.properties exists at that location it will be used.  If service.properties
 * doesn't exist, new properties are instanced and isModified()==true to indicate the
 * properties should be saved.
 *
 * @author Milton Smith
 */
public class XRServiceProperties extends XRPropertyBase implements XRIProperties {

    private static final String PATH_SEPARATOR = System.getProperty("file.separator");

    /**
     * Initialize XRServiceProperties for packaged services.
     * @param jvmxrayHome Initialized jvmxray home.
     * @param serviceRoot Fully qualified file system path to the services root directory.
     * @param serviceName Service name as it appears in the services <code>service.properties</code>
     *                    provided by the
     */
    public XRServiceProperties(Path jvmxrayHome, Path serviceRoot, String serviceName) {
        super(jvmxrayHome,
                jvmxrayHome.relativize(serviceRoot)+PATH_SEPARATOR+serviceName,
                XRServiceManager.SERVICE_PROPERTIES);
    }

}
