package org.owasp.jvmxray.driver;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.dgc.VMID;
import java.security.Permission;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ServiceConfigurationError;

import org.owasp.jvmxray.event.EventFactory;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;
import org.owasp.jvmxray.exception.JVMXRayConnectionException;
import org.owasp.jvmxray.exception.JVMXRayDBException;
import org.owasp.jvmxray.util.DBUtil;
import org.owasp.jvmxray.util.FileUtil;
import org.owasp.jvmxray.util.LiteLogger;
import org.owasp.jvmxray.util.PropertyUtil;

/**
 * 
 * NullSecurityManager conforms to the java.lang.SecurityManager specifications.  This class intercepts
 * access to protected resources, builds an event, and sends the event to a server for processing.
 *
 * @see java.lang.SecurityManager
 * @author Milton Smith
 *
 */
public class NullSecurityManager  extends SecurityManager {
	
	/** Get logger instance. */
	//private static Logger logger = null; //LoggerFactory.getLogger("org.owasp.jvmxray.driver.NullSecurityManager");
	//private static LoggerContext context;

	// Lock access to NullSecurityManager methods while executing.  Blocked by default until
	// NullSecurityManager is properly initialized.
	private volatile boolean bLocked = true;
	
	// jvmxrayclient.properties
	protected Properties p = new Properties();
	
	// Hold list of filters to process.
	private JVMXRayFilterList rulelist = new JVMXRayFilterList();
	
	// Events to process.
	private EnumSet<Events> usrevents = EnumSet.noneOf(Events.class);
	
	// Server identity
	private String id;

	// JSON end-point for event data.
	private URL webhookurl;

	// Database connection
	private Connection dbconn;

	// Database utilities
	private DBUtil dbutil;

	// Lightweight logger
	private LiteLogger ltlogger = LiteLogger.getLoggerinstance();
	
	/**
	 * Implemented by filters but generally, <br/>
	 * ALLOW events captured on match.
	 * NEUTRAL, events pass through to next filter.
	 * DENY, events removed on match.
	 */
	public enum FilterActions {
		ALLOW,
		NEUTRAL,
		DENY
	}
	
	/**
	 * Callstack options for captured events. <br/>
	 * NONE Do not include stacktrace, default.
	 * LIMITED, Include limited information.  Class call stack, without method or line number references.
	 * FULL, Include full stack trace information.
	 */
	public enum Callstack {
		NONE,
		LIMITED,
		SOURCEPATH,
		FULL
	}

	/**
	 * See core JavaDocs.
	 * @see java.lang.SecurityManager
	 */
	public NullSecurityManager ()  {
				
		//***********************************************
		// Initialization Step 1: Property Initialization
		//***********************************************
		ProtectedTask n1 = new ProtectedTask("Initialization: Property Initialization") {
			@Override
			public boolean execute() throws Exception {
				super.execute();
				initializeFromProperties();
				return true;
			}
			@Override
			public boolean rollback(Exception e) {
				setLocked(false);
				return true;
			}
			@Override
			public boolean preProcess() throws Exception {
				setLocked(true);
				return true;
			}
			@Override
			public boolean postProcess() throws Exception {
				setLocked(false);
				return true;
			}
		};

		//***********************************************
		// Initialization Step 2: DB initialization
		//***********************************************
		ProtectedTask n2 = new ProtectedTask("Initialization: DB Initialization") {
			@Override
			public boolean execute() throws Exception {
				super.execute();
				try {
					initDB();
				}catch(JVMXRayDBException e) {
					ltlogger.info("NullSecurityManager.ctor(): Local db unavailable.");
				}
				return true;
			}
			@Override
			public boolean rollback(Exception e) {
				setLocked(false);
				dbconn = null;
				dbutil = null;
				return true;
			}
			@Override
			public boolean preProcess() throws Exception {
				setLocked(true);
				return true;
			}
			@Override
			public boolean postProcess() throws Exception {
				setLocked(false);
				return true;
			}
		};
		
		//***********************************************
		// Initialization, Execute task chain.
		//***********************************************
		ProtectedTaskModel model = ProtectedTaskModel.getInstance();
		// Connect nodes in chain.
		n1.setNextNode(n2);
		boolean success = model.executeChainedTask(n1);
		if( success ) {
			ltlogger.info("JVMXray Initialization success." ); //logger.info
		} else {
			ltlogger.info("JVMXray Initialization failed.  See logs for details."); //logger.error
			System.exit(30);
		}
			
	}

	private final void initDB() throws SQLException, JVMXRayDBException {
		String sdelay = p.getProperty(PropertyUtil.CONF_PROP_MAXWAIT_INITIALIZATION);
		int delay=45001;
		try {
			delay = Integer.parseInt(sdelay);
		}catch(NumberFormatException e) {
			if( ltlogger.isDebug() ) {
				ltlogger.debug("NullSecurityManager.initDB(): key="+PropertyUtil.CONF_PROP_MAXWAIT_INITIALIZATION+" value="+sdelay);
				e.printStackTrace();
			}
		}

		// Due to the architecture SecurityManager and our use of it
		// beyond it's intended design some features cause exceptions if used
		// prior to full VM initialization.  In the case of JDBC, ServiceConfigurationError
		// exceptions occur if connections are initialized too early.  It's also
		// a problem with early use of other features like JMX and Classloaders.
		// To work around, we attempt to initialize a connection immediately.  This
		// works fine for unit testing but fails in containers.  To support containers
		// we start a background thread and delay event processing for a number
		// of seconds specified by
		// jvmxray.event.nullsecuritymanager.server.delay.initialization
		// or a non-null database connection is returned, whichever comes
		// first.  This allows container initialization
		// will proceed, but any events issued by the service prior to
		// initialization are lost.  If a db connection is established prior
		// to timer elapse, the thread exits successfully.
		//
		// Specific exception thrown is,
		// java.util.ServiceConfigurationError: java.sql.Driver: not accessible to
		// module java.sql during VM init
		FileUtil fiutil = FileUtil.getInstance();
		dbutil = DBUtil.getInstance(p);
		try {
			dbconn = dbutil.createConnection();
			if( dbconn != null ) {
				setLocked(false);
			} else {
				JVMXRayDBException e = new JVMXRayDBException("JDBC connection failed to initialize.  dbconn=null");
				throw e;
			}
		}catch(ServiceConfigurationError e) {
			final int fdelay = delay;
			Thread t = new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					while( System.currentTimeMillis() - start < fdelay ) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {}
						Thread.yield();
						try {
							dbconn = dbutil.createConnection();
						} catch (ServiceConfigurationError e) {

						} catch (SQLException e) {
							e.printStackTrace();
						}
						if ( dbconn != null ) {
							break;
						}
					}
					setLocked(false);
				}
			};
			t.start();
		}
	}

	/**
	 * See core JavaDocs.
	 * @param target
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkSecurityAccess(String target) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_SECURITY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessSecurityEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								target // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param t
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkAccess(Thread t) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREAD) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessThreadEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								t.toString() // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param tg
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkAccess(ThreadGroup tg) {
		if( !isLocked() && isEventEnabled(Events.ACCESS_THREADGROUP) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createAccessThreadGroupEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								tg.toString() // Event parameter
								);
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkCreateClassLoader() {
		if( !isLocked() && isEventEnabled(Events.CLASSLOADER_CREATE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createCreateClassLoaderEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param status
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkExit(int status) {
		if( !isLocked() && isEventEnabled(Events.EXIT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createExitEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								status // exit code
								);
			processEvent(ev);
			setLocked(false);
		}		
	}

	/**
	 * See core JavaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkSetFactory() {
		if( !isLocked() && isEventEnabled(Events.FACTORY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFactoryEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param file
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkDelete(String file) {
		if( !isLocked() &&  isEventEnabled(Events.FILE_DELETE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileDeleteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file ); //  File to delete
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param cmd
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkExec(String cmd) {
		if( !isLocked() && isEventEnabled(Events.FILE_EXECUTE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileExecuteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								cmd ); //  Command to execute
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param file
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkRead(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file ); //  File to read
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs
	 * @param file
	 * @param context
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkRead(String file, Object context) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file, // file to read
								context.toString() ); //  file context
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param fd
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkRead(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_READ_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileReadWithFileDescriptorEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param file
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkWrite(String file) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileWriteEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								file); // File to write
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param fd
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkWrite(FileDescriptor fd) {
		if( !isLocked() && isEventEnabled(Events.FILE_WRITE_WITH_FILEDESCRIPTOR) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createFileWriteWithFileDescriptorEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param lib
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkLink(String lib) {
		if( !isLocked() && isEventEnabled(Events.LINK) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createLinkEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								lib); // library name
			
			processEvent(ev);
			setLocked(false);
		}		
	}
	
	/**
	 * See core JavaDocs.
	 * @param pkg
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPackageAccess(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_ACCESS) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPackageAccessEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								pkg); // package name
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param pkg
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPackageDefinition(String pkg) {
		if( !isLocked() && isEventEnabled(Events.PACKAGE_DEFINE) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPackageDefineEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								pkg); // package name
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param perm
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPermission(Permission perm) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPermissionEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								perm.getName(),  // Permission name
								perm.getActions()); // Permission actions
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param perm
	 * @param context
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPermission(Permission perm, Object context) {
		if( !isLocked() && isEventEnabled(Events.PERMISSION_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPermissionWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later
								perm.getName(),  // Permission name
								perm.getActions(), // Permission actions
								context.toString()); // Context info
			
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPrintJobAccess() {
		if( !isLocked() && isEventEnabled(Events.PRINT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPrintEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later	
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPropertiesAccess() {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_ANY) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPropertiesAnyEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								""); // Stacktrace, assign later	
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param key
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkPropertyAccess(String key) {
		if( !isLocked() && isEventEnabled(Events.PROPERTIES_NAMED) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createPropertiesNamedEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								key); // Property key
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param host
	 * @param port
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkAccept(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_ACCEPT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketAcceptEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port ); // Port number
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param host
	 * @param port
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkConnect(String host, int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketConnectEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port ); // Port number
			processEvent(ev);
			setLocked(false);
		}
	}
	
	/**
	 * See core JavaDocs.
	 * @param host
	 * @param port
	 * @param context
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkConnect(String host, int port, Object context) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_CONNECT_WITH_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketConnectWithContextEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								host,  // Host name
								port, // Port number
								context.toString()); // Context info.
			processEvent(ev);
			setLocked(false);
		}
	}

	/**
	 * See core JavaDocs.
	 * @param port
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkListen(int port) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_LISTEN) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketListenEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								port); // Port number
			processEvent(ev);
			setLocked(false);
		}	
	}

	/**
	 * See core JavaDocs.
	 * @param maddr
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkMulticast(InetAddress maddr) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketMulticastEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later		
								maddr.toString()); // Multicast address
			processEvent(ev);
			setLocked(false);
		}	
	}
	
	/**
	 * See core JavaDocs.
	 * @param maddr
	 * @param ttl
	 * @see java.lang.SecurityManager
	 */
	@Override
	public synchronized void checkMulticast(InetAddress maddr, byte ttl) {
		if( !isLocked() && isEventEnabled(Events.SOCKET_MULTICAST_WITH_TTL) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createSocketMulticastWithTTLEvent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								maddr.toString(), // Multicast address
								String.format("%02X ", ttl)); // Time to live
			processEvent(ev);
			setLocked(false);
		}	
	}

	/**
	 * See core javaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	protected Class<?>[] getClassContext() {
		return super.getClassContext();
	}

	/**
	 * See core javaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public Object getSecurityContext() {
		return super.getSecurityContext();
	}

	/**
	 * See core javaDocs.
	 * @see java.lang.SecurityManager
	 */
	@Override
	public ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	/**
	 * Include related event metadata. For example, the currently
	 * logged on user in a web application.
	 * @param key Metadata name, must be unique.
	 * @param value Metadata value.
	 */
	private synchronized void sendMappedVariable(String key, String value) {
		if( !isLocked() && isEventEnabled(Events.MAPPED_CONTEXT) ) {
			setLocked(true);
			EventFactory factory = EventFactory.getInstance();
			IEvent ev = factory.createMappedContent(
								IEvent.PK_UNUSED, // No PK since this is new event
								IEvent.STATE_UNUSED, // State is not used at this time
								System.currentTimeMillis(),  // timestamp
								getThreadStamp(), // log the thread name and id
								id,  // Cloud service id of this event
								"", // Stacktrace, assign later	
								key, // Keypair keyname
								value); // Keypair value
			processEvent(ev);
			setLocked(false);
		}	
	}
	
	/**
	 * Generate a stack track.  Required depending on JVMXRay property settings.
	 * @param event Active event.
	 * @return  Stack trace based upon user preferences.
	 */
	private String getStackTrace(IEvent event) {
		String stacktrace = "";
		StackTraceElement[] ste = null;
		ste = Thread.currentThread().getStackTrace();
		stacktrace = generateCallStack(rulelist.getCallstackOptions(event), ste);
		return stacktrace;
	}

	private void spoolEvent(IEvent event) throws IOException, SQLException {
		dbutil.insertEvent(dbconn,
				event );
	}

	/**
	 * Process an event.  Required since callers
	 * may trigger additional nested security manager permission
	 * calls resulting in a stack overflow.
	 * @param event Active event.
	 */
	private void processEvent( IEvent event )  {
		try {
			// Note(s):  There is no transaction pooling/cashing just yet.
			//           A client, caller of NullSecurityManager, blocks
			//           unit processEvent() completes.  Probably need
			//           something more robust for production use
			//           eventually.  For example, some configuration
			//           settings to, block, transaction TTL,
			//           or possibly better options.
			//
			if( rulelist.filterEvents( event ) == FilterActions.ALLOW ) {
				boolean eventlogged = false;
				event.setStackTrace(getStackTrace(event));
				boolean server_present = (webhookurl!=null);
				// If server configuration present, send event to remote server.
				if( server_present ) {
					JVMXRayClient client = new JVMXRayClient(webhookurl) {
						// Reassign max tries or use 5 for default.
						protected int MAX_TRIES = 5;
						@Override
						public void startConnection(HttpURLConnection connection) throws Exception {
							connection.setDoOutput(true);
							connection.setRequestMethod("POST");
							connection.setRequestProperty("User-Agent", "JVMXRayV1");
							//connection.setRequestProperty("Accept", "*/*");
							connection.setRequestProperty("Content-Type", "application/json; utf-8");
							//connection.setRequestProperty("Accept-Language", "en-US");
							connection.setRequestProperty("Accept", "text/html");
						}
						@Override
						public IEvent getEvent() throws JVMXRayConnectionException {
							return event;
						}
						@Override
						public void finishConnection(JVMXRayResponse response) {
							int responsecode = response.getResponseCode();
							String responsedata = response.getResponseData();
							ltlogger.debug("NullSecurityManager.processEvent(): status code="+responsecode+
									" server data["+responsedata.length()+"bytes]=" + responsedata);
						}
						//
						// If failed to send to server, try again:
						// Tries: 0   Milliseconds to wait: 0
						//        1                         500
						//        2                         4000
						//        3                         13500
						//        4                         32000
						//
						@Override
						public int retries(int currentAttempt) {
							return 500 * (currentAttempt ^ 3);
						}
					};
					try {
						client.fireEvent();
						eventlogged = true;
					}catch(JVMXRayConnectionException e) {
						// Thrown on bad configuration or server unavailable.  Sink exception, log event to db or console later.
					}
				}
				// If local db propertly configured then log events locally.  Useful for local debugging.
				if( dbconn != null ) {
					spoolEvent(event);
				}
				// If event not logged on server for any reason log to console.  Better than nothing.
				if( !eventlogged) {
					ltlogger.info("NullSecurityManager.processEvent(): Server unavailable.  Log event to console. event="+event.toString());
				// If event logged on server, log to console only if debug logging enabled.
				} else {
					ltlogger.debug("NullSecurityManager.processEvent(): client event debug enabled.  event="+event.toString());
				}
			}
		}catch(Throwable t) {
			ltlogger.info("Unrecoverable error, disregard event and proceed.  msg="+t.getMessage());
			if( ltlogger.isDebug() ) {
				t.printStackTrace();
			}
		}
	}
	
	private boolean isLocked() {
		return bLocked;
	}
	
	private void setLocked(boolean state) {
		bLocked = state;
	}
	
	
	/**
	 * Initialize the NullSecurityManager subclass via property settings from jvmxrayclient.properties.
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
    private void initializeFromProperties() throws ClassNotFoundException, NoSuchMethodException, SecurityException, 
    											   InstantiationException, IllegalAccessException, IllegalArgumentException, 
    											   InvocationTargetException, IOException {
    	
    	PropertyUtil pu = PropertyUtil.getInstance();
    	
		// Load jvmxrayclient.properties
		p = pu.getClientProperties();
    	
    	// Get the assigned server identity or generate a new identity and save it.
		try {
			// Throws exceptions if can't load file with id.
			id = pu.getServerId();
		} catch( Exception e ) {
			
			// Server identity from command line, if assigned by user.
        	String i1 = System.getProperty(PropertyUtil.SYS_PROP_EVENT_SERV_IDENTITY);
        
        	String i3=""; String lid="";
        	// If no id assigned by user, generate a new one.
        	if( i1 == null ) { 
        		String i2 = new VMID().toString();
        		i3 = PropertyUtil.formatVMID(i2);
        		lid=i3;
        	// Id assigned by user, save it.
        	} else {
        		lid=i1;
        	}
		
			// If exception occurs, they bubble up.
			pu.saveServerId(lid);
			id = lid;
		}
		
		// Set the webhook url to remote server.
		try {
			String surl = p.getProperty(PropertyUtil.CONF_PROP_WEBHOOK_URL);
			webhookurl = new URL(surl);
		} catch( Exception e ) {
			webhookurl = null;
		}
    	
    	// Iterate over all the properties
    	for( int i=1; i < 500; i++ ) {
    		
    		// Common settings for all filters.
    		String fclass = p.getProperty("jvmxray.filter"+i+".class");
    		String events = p.getProperty("jvmxray.filter"+i+".events");
    		String strace = p.getProperty("jvmxray.filter"+i+".stacktrace");
    		String defaults = p.getProperty("jvmxray.filter"+i+".default");
    		
    		// No more filters or missing filter.  Continue to look for
    		// next numbered filter.
    		if( fclass == null || events == null || defaults == null )
    			continue;
    		
    		// Collect all properties specific to the filter.
    		Properties np = new Properties();
    		Enumeration<String> e = (Enumeration<String>) p.propertyNames();
    		while (e.hasMoreElements() ) {
    			String key = e.nextElement();
    			String value = p.getProperty(key);
    			String prefix = "jvmxray.filter"+i;
    			if( key.startsWith(prefix) ) {
    				np.put(key,value);
    			}
    		}
    	
             // Take any new events and add it to the list of supported events in
             // usrevents.
    		EnumSet<Events> gvents = EnumSet.noneOf(Events.class);
             String[] sevents = events.split(",");
             for( int i3=0; i3 < sevents.length; i3++ ) {
            	 String levent = sevents[i3].trim();
            	 if ( !usrevents.contains(levent) ) {
            		 gvents.add(Events.valueOf(levent));
            		 usrevents.add(Events.valueOf(levent));

            	 }
             }
             
             // Grab callstackopts for the filter
             Callstack opts = Callstack.NONE;
             opts = Callstack.valueOf(strace);
            	 
        	 // Create instance of specified filter using reflection
    		 Class c = getClass().getClassLoader().loadClass(fclass);
             Constructor cd = c.getConstructor(EnumSet.class, FilterActions.class, Properties.class, Callstack.class);
             FilterActions filteraction = FilterActions.valueOf(defaults);
             JVMXRayFilterRule fdr = (JVMXRayFilterRule)cd.newInstance(gvents,filteraction, np, opts);
             
            		 
             // Add the rule to the list
         	 rulelist.add( fdr );
             
    	}
    	
    }
	
	/**
	 * Test if target type of event handling is enabled.
	 * @param event Event to test if enabled.
	 * @return True, the event is enabled.  False, the event is not enabled.
	 */
    private boolean isEventEnabled(Events event) {
    	boolean iseventenabled = false;
    	for (Events c : usrevents) {
    		if ( event==c ) {
    			iseventenabled = true;
    		}
    	}
    	return iseventenabled;
    }
   
    /**
     * Return a string to identify calling thread suitable for logging.
     * @return Identity of the current thread.
     */
    private String getThreadStamp() {
    	Thread t = Thread.currentThread();
    	return t.getName()+"-"+t.getId();
    }
    
	/**
     * Generate a callstack based upon specification.
     * @param callstackopt Type of callstack to generate.
     * @param stacktrace StackTraceElement[] array of stack trace data.
     */
    String generateCallStack(Callstack callstackopt, StackTraceElement[] stacktrace) {
    	StringBuffer buff = new StringBuffer();
		URL location = null;
    	
    	switch ( callstackopt ) {
    		case LIMITED:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				buff.append(c.getName());
	    				buff.append("->");
      				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case SOURCEPATH:
    			for (StackTraceElement e : stacktrace ) {
    				try {
	    				Class c = Class.forName(e.getClassName());
	    				location = c.getResource('/' + c.getName().replace('.', '/') + ".class");
	    				buff.append(location.toString());
	    				buff.append("->");
    				} catch( ClassNotFoundException e1) {
    					e1.printStackTrace();
    				}
    			}
    			break;
    		case FULL:
    			for (StackTraceElement e : stacktrace ) {
    				buff.append(e.getClassName());
    				buff.append('(');
    				buff.append(e.getMethodName());
    				buff.append(':');
    				buff.append(e.getLineNumber());
    				buff.append(')');
    				buff.append("->");
    			}
    			break;		
    		case NONE:
    			buff.append("disabled");
    			break;

    	}

    	// Chop off trailing ->
		if (buff.length()>0 && buff.toString().endsWith("->"))
			buff.setLength(buff.length()-2);
    	
    	return buff.toString();
    }
}
