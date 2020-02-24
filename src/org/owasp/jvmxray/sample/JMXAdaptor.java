package org.owasp.jvmxray.sample;

import java.io.FileDescriptor;
import java.lang.management.ManagementFactory;
import java.security.Permission;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.owasp.jvmxray.api.NullSecurityManager;

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

	public void fireEvent(Events event, Object[] obj1, String format, Object ...obj2) {
		
		switch (event) {
			case PERMISSION:
				monitorMBean.setPermissionEvent(event,((Permission)obj1[0]).toString());
				break;
			case PERMISSION_WITH_CONTEXT:
				monitorMBean.setPermissionEvent(event,((Permission)obj1[0]).toString());
				break;
			case FILE_DELETE:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
				break;
			case FILE_EXECUTE:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
				break;
			case FILE_READ:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
		    	break;
			case FILE_READ_WITH_CONTEXT:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
				break;
			case FILE_READ_WITH_FILEDESCRIPTOR:
				monitorMBean.setFileEvent(event,((FileDescriptor)obj1[0]).toString());
				break;
			case FILE_WRITE:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
			case FILE_WRITE_WITH_FILEDESCRIPTOR:
				monitorMBean.setFileEvent(event,(String)obj1[0]);
				break;
		    default:
		    	break;
		}
		
	}


}
