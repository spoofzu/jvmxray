package org.owasp.jvmxray.adaptors;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.owasp.jvmxray.api.IJVMXRayEvent;
import org.owasp.jvmxray.api.NullSecurityManager;
import org.owasp.jvmxray.jmx.JVMXRayMonitor;

public class JMXAdaptor extends NullSecurityManager {

    private JVMXRayMonitor monitorMBean = new JVMXRayMonitor();
	private MBeanServer mbs = null;

	public JMXAdaptor() {
		super();
		// Get the platform MBeanServer
        mbs = ManagementFactory.getPlatformMBeanServer();

        // Unique identification of MBeans
        ObjectName objName = null;

        try {
            // Uniquely identify the MBeans and register them with the platform MBeanServer 
            objName = new ObjectName("JVMXRay:name=JVMXRayMonitor");
            mbs.registerMBean(monitorMBean, objName);
        } catch(Exception e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Fire an event.  This is implemented by callers so that events
	 * can be handled by log systems, SIEMS, etc.  This framework 
	 * provides implementation for some popular systems like logback
	 * and Java logging.
	 * @param event actual event being processed
	 */
	protected void fireEvent(IJVMXRayEvent event) {
		monitorMBean.fireEvent( event );
	
	}



}
