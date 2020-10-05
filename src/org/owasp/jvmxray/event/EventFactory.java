package org.owasp.jvmxray.event;

import java.util.Map;

import org.owasp.jvmxray.event.IEvent.Events;
import org.owasp.jvmxray.exception.JVMXRayBadTypeRuntimeException;

public class EventFactory {

	private static EventFactory i = null;
	
	private EventFactory() {}
	
	public synchronized static final EventFactory getInstance() {
		if ( i == null ) {
			i = new EventFactory();
		}
		return i;
	}
	
	public IEvent createEventByEventType( Events type, int id, int state, long timestamp, String tid, String identity, String stacktrace, String p1, String p2, String p3 ) throws JVMXRayBadTypeRuntimeException {
		
		IEvent event = null;
		
		switch (type) {
		case ACCESS_SECURITY:
			event = createAccessSecurityEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case ACCESS_THREAD:
			event = createAccessThreadEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case ACCESS_THREADGROUP:
			event = createAccessThreadGroupEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case CLASSLOADER_CREATE:
			event = createCreateClassLoaderEvent( id, state, timestamp, tid, identity, stacktrace );
			break;
		case EXIT:
			event = createExitEvent( id, state, timestamp, tid, identity, stacktrace, Integer.parseInt(p1) );
			break;
		case FACTORY:
			event = createFactoryEvent( id, state, timestamp, tid, identity, stacktrace);
			break;
		case FILE_DELETE:
			event = createFileDeleteEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case FILE_EXECUTE:
			event = createFileExecuteEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case FILE_READ:
			event = createFileReadEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case FILE_READ_WITH_CONTEXT:
			event = createFileReadWithContextEvent( id, state, timestamp, tid, identity, stacktrace, p1, p2 );
			break;
		case FILE_READ_WITH_FILEDESCRIPTOR:
			event = createFileReadWithFileDescriptorEvent( id, state, timestamp, tid, identity, stacktrace);
			break;
		case FILE_WRITE:
			event = createFileWriteEvent(id, state, timestamp, tid, identity, stacktrace, p1);
			break;
		case FILE_WRITE_WITH_FILEDESCRIPTOR:
			event = createFileWriteWithFileDescriptorEvent( id, state, timestamp, tid, identity, stacktrace);
			break;
		case LINK:
			event = createLinkEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case PACKAGE_ACCESS:
			event = createPackageAccessEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case PACKAGE_DEFINE:
			event = createPackageDefineEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case PERMISSION:
			event = createPermissionEvent( id, state, timestamp, tid, identity, stacktrace, p1, p2 );
			break;
		case PERMISSION_WITH_CONTEXT:
			event = createPermissionWithContextEvent( id, state, timestamp, tid, identity, stacktrace, p1, p2, p3 );
			break;
		case PRINT:
			event = createPrintEvent( id, state, timestamp, tid, identity, stacktrace );
			break;
		case PROPERTIES_ANY:
			event = createPropertiesAnyEvent( id, state, timestamp, tid, identity, stacktrace);
			break;
		case PROPERTIES_NAMED:
			event = createPropertiesNamedEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case SOCKET_ACCEPT:
			event = createSocketAcceptEvent( id, state, timestamp, tid, identity, stacktrace, p1, Integer.parseInt(p2) );
			break;
		case SOCKET_CONNECT:
			event = createSocketConnectEvent( id, state, timestamp, tid, identity, stacktrace, p1, Integer.parseInt(p2) );
			break;
		case SOCKET_CONNECT_WITH_CONTEXT:
			event = createSocketConnectWithContextEvent( id, state, timestamp, tid, identity, stacktrace, p1, Integer.parseInt(p2), p3 );
			break;
		case SOCKET_LISTEN:
			event = createSocketListenEvent( id, state, timestamp, tid, identity, stacktrace, Integer.parseInt(p1) );
			break;
		case SOCKET_MULTICAST:
			event = createSocketMulticastEvent( id, state, timestamp, tid, identity, stacktrace, p1 );
			break;
		case SOCKET_MULTICAST_WITH_TTL:
			event = createSocketMulticastWithTTLEvent( id, state, timestamp, tid, identity, stacktrace, p1, p2 );
			break;
		default:
			throw new JVMXRayBadTypeRuntimeException("Unsupported event type. type="+type);
		}
		
		return event;
		
	}

	public IAccessSecurityEvent createAccessSecurityEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String target) {

			return new AccessSecurityEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 target);
	
	}
	
	public IAccessThreadEvent createAccessThreadEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String threadid) {

			return new AccessThreadtEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 threadid);
	
	}
	
	public IAccessThreadGroupEvent createAccessThreadGroupEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String tg) {

			return new AccessThreadGroupEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 tg);
	
	}
	
	public IClassLoaderEvent createCreateClassLoaderEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace
			 ) {

			return new ClassLoaderEventDAO(id, state, timestamp, tid, identity, stacktrace
					);
	
	}
	
	public IExitEvent createExitEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			int status) {

			return new ExitDAO(id, state, timestamp, tid, identity, stacktrace,
			 status);
	
	}
	
	public IFactoryEvent createFactoryEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace
			) {

			return new FactoryEventDAO(id, state, timestamp, tid, identity, stacktrace
					);
	
	}
	
	public IFileDeleteEvent createFileDeleteEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
		 String file) {

			return new FileDeleteEventDAO(id, state, timestamp, tid, identity, stacktrace,
				file);
	
	}
	
	public IFileExecuteEvent createFileExecuteEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String command) {

			return new FileExecuteEventDAO(id, state, timestamp, tid, identity, stacktrace,
					command);
	
	}
	
	public IFileReadEvent createFileReadEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String file) {

			return new FileReadEventDAO(id, state, timestamp, tid, identity, stacktrace,
					file);
	
	}
	
	public IFileReadWithFileDescriptorEvent createFileReadWithFileDescriptorEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace) {

			return new FileReadWithFileDescriptorEventDAO(id, state, timestamp, tid, identity, stacktrace);
	
	}
	
	public IFileReadWithContextEvent createFileReadWithContextEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String file, String context) {

			return new FileReadWithContextEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 file, context);
	
	}
	
	public IFileWriteEvent createFileWriteEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String file) {

			return new FileWriteEventDAO(id, state, timestamp, tid, identity, stacktrace,
					file);
	
	}
	
	public IFileWriteWithFileDescriptorEvent createFileWriteWithFileDescriptorEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace
		 ) {

			return new FileWriteWithFileDescriptorEventDAO(id, state, timestamp, tid, identity, stacktrace );
	
	}
	
	
	public ILinkEvent createLinkEvent( int id, int state, long timestamp, String identity, String tid, String stacktrace,
		 String lib) {

			return new LinktEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 lib);
	
	}
	
	public IPackageAccessEvent createPackageAccessEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
		 String pkg) {

			return new PackageAccessEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 pkg);
	
	}
	
	public IPackageDefineEvent createPackageDefineEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String pkg) {

			return new SocketPackageDefineDAO(id, state, timestamp, tid, identity, stacktrace,
				 pkg);
	
	}
	
	public IPermissionEvent createPermissionEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String name, String actions) {

			return new PermissionEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 name, actions);
	
	}
	
	public IPermissionWithContextEvent createPermissionWithContextEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String name, String actions, String context) {

			return new PermissionWithContextEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 name, actions, context);
	
	}
	
	public IPrintEvent createPrintEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace ) {

			return new PrintEventDAO(id, state, timestamp, tid, identity, stacktrace);
	
	}
	
	public IPropertiesAnyEvent createPropertiesAnyEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace ) {

			return new PropertiesAnyEventDAO(id, state, timestamp, tid, identity, stacktrace);
	
	}
	
	public IPropertiesNamedEvent createPropertiesNamedEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			String named) {

			return new PropertiesNamedEventDAO(id, state, timestamp, tid, identity, stacktrace,
				named);
	
	}
	
	public ISocketAcceptEvent createSocketAcceptEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
		 String host, int port) {

			return new SocketAcceptEventDAO(id, state, timestamp, tid, identity, stacktrace,
				host, port);
	
	}
	
	public ISocketConnectEvent createSocketConnectEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String host, int port) {

			return new SocketConnectEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 host, port);
	
	}
	
	public ISocketConnectWithContextEvent createSocketConnectWithContextEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String host, int port, String context) {

			return new SocketConnectWithContextEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 host, port, context);
	
	}
	
	public ISocketListenEvent createSocketListenEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 int port) {

			return new SocketListenEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 port);
	
	}
	
	public ISocketMulticastEvent createSocketMulticastEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String addr) {

			return new SocketMulticastEventDAO(id, state, timestamp, tid, identity, stacktrace,
				 addr);
	
	}
	
	public ISocketMulticastWithTTLEvent createSocketMulticastWithTTLEvent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String addr, String ttl) {

			return new SocketMulticastWithTTLEventDAO(id, state, timestamp, tid, identity, stacktrace,
					 addr, ttl);
	
	}

	public IMappedContextEvent createMappedContent( int id, int state, long timestamp, String tid, String identity, String stacktrace,
			 String key, String value) {

			return new MappedContextEventDAO(id, state, timestamp, tid, identity, stacktrace,
					  key, value);
	
	}

	
}
