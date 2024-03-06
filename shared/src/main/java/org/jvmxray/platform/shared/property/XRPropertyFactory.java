package org.jvmxray.platform.shared.property;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.jvmxray.platform.shared.util.XRGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Property factory.  Used to create representation of xray properties.
 *
 * @author Milton Smith
 */
public class XRPropertyFactory {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.platform.shared.property.XRPropertyFactory");
    private static final String systemSlash = System.getProperty("file.separator");

    private static File jvmxrayBase = null;
    private static File jvmxrayHome = null;
    private static File jvmxrayLogHome = null;

    private static XRAgentProperties agentProperties = null;
    private static XRInjectorProperties injectorProperties = null;

    private XRPropertyFactory() {}

    /**
     * Property factory must be initalized prior to use.
     * @throws Exception Thrown on property file problems.
     */
    public static synchronized void init() throws Exception {
        // Obtain jvmxray.jvmxrayBase from user if available or compute it's value.
        //
        String jvmxrayBase0 = System.getProperty("jvmxray.base");
        if( jvmxrayBase0 == null || jvmxrayBase0.length()<1 ) {
            jvmxrayBase0 = System.getProperty("user.home");
            if( jvmxrayBase0 == null || jvmxrayBase0.length()<1 ) {
                throw new IOException("Unable to compute jvmxray.base.  Assign value like, -Djvmxray.base=/Users/milton/");
            }
        }
        jvmxrayBase = new File(jvmxrayBase0);
        jvmxrayBase.mkdirs();
        // Obtain jvmxray.home from user if available or compute it's value based on jvmxray.base.
        //
        String jvmxrayHome0 = System.getProperty("jvmxray.home");
        if( jvmxrayHome0 == null || jvmxrayHome0.length()<1 ) {
            jvmxrayHome = new File(jvmxrayBase, "jvmxray");
        } else {
            jvmxrayHome = new File(jvmxrayBase0);
        }
        jvmxrayHome.mkdirs();
        // Obtain jvmxray.logs from user if available or compute it's value based on jvmxray.home.
        //
        String jvmxrayLogs0 = System.getProperty("jvmxray.logs");
        if( jvmxrayLogs0 == null || jvmxrayLogs0.length()<1 ) {
            jvmxrayLogHome = new File(jvmxrayHome, "logs");
        } else {
            jvmxrayLogHome = new File(jvmxrayLogs0);
        }
        jvmxrayLogHome.mkdirs();
        // jvmxray.logs jvmxrayBase path ends in slash to make logback.xml config
        // cross-platform config.
        String logbackLogHome0 = (jvmxrayLogHome.getAbsolutePath().endsWith(systemSlash)) ?
                jvmxrayLogHome.getAbsolutePath() :
                jvmxrayLogHome.getAbsolutePath() + systemSlash;
        // Sets properties for logback configuration.
        System.setProperty("jvmxray.logs",logbackLogHome0);
        String logbackFileName = "logback-test.xml2";
        String targetFileName = "logback-test.xml";
        boolean bUnitTesting = true;
        InputStream in = XRPropertyFactory.class.getClassLoader().getResourceAsStream(logbackFileName);
        if( in == null ) {
            logbackFileName = "logback.xml2";
            targetFileName = "logback.xml";
            bUnitTesting = false;
            in = XRPropertyFactory.class.getClassLoader().getResourceAsStream(logbackFileName);
        }
        if( in == null ) {
            throw new IOException("Logback configuration not found.");
        }
        // Copy logback file to jvmxray home, if it doesn't exist.  If we are using
        //   unit test configuration then skip copy.
        File logbackTarget = new File(jvmxrayHome, targetFileName);
        if( !bUnitTesting ) {
            if (!logbackTarget.exists()) {
                Files.copy(in, logbackTarget.toPath());
            }
        }
        // Init logback.  Logback can sometimes be intialized prior to agent loading
        //   If that's the case we reintialize it from our configuration.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            context.reset(); // Reinitialize logback.
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // If unit testing load logback config from stream else grab it from
            //   jvmxray home.
            if( bUnitTesting && in!=null ) {
                configurator.doConfigure(in);
            } else {
                configurator.doConfigure(logbackTarget.getAbsoluteFile());
            }
        } catch (JoranException je) {
            throw new Exception("Unable to initialize logback",je);
        }
    }

    public static synchronized XRAgentProperties getAgentProperties() throws Exception {
        if( agentProperties == null ) {
            if( jvmxrayBase == null || jvmxrayHome == null || jvmxrayLogHome == null ) {
                throw new Exception("XRPropertyFactory must be initialized prior to use.");
            }
            agentProperties = new XRAgentProperties(jvmxrayHome.toPath());
            agentProperties.init();
            // If property file created, save a copy.
            if(agentProperties.isModified()) {
                agentProperties.setProperty("AID", XRGUID.getID());
                agentProperties.setProperty("CAT", "unit-test");
                agentProperties.saveProperties("JVMXRay Agent Properties");
            }
        }
        return agentProperties;
    }

    public static synchronized XRInjectorProperties getInjectorProperties() throws Exception {
        if( injectorProperties == null ) {
            if( jvmxrayBase == null || jvmxrayHome == null || jvmxrayLogHome == null ) {
                throw new Exception("XRPropertyFactory must be initialzed prior to use.");
            }
            injectorProperties = new XRInjectorProperties(jvmxrayHome.toPath());
            injectorProperties.init();
            // If property file created, save a copy.
            if(injectorProperties.isModified()) {
                injectorProperties.saveProperties("JVMXRay Injector Properties");
            }
        }
        return injectorProperties;
    }

}
