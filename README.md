
# Welcome to JVMXRay

|  |  |
| --- | :--- |
| ![xrayduke](https://user-images.githubusercontent.com/8450615/88954072-af62ef00-d24e-11ea-95f9-734395481248.png) | JVMXRay is a technology for monitoring access to protected system resources by your Java applications like files, sockets, classes, and more. It’s designed with an application security emphasis but there are benefits across other areas like, software diagnostics, usage tracking, and auditing. |
| **RECENT NEWS** | &nbsp; |
| **Feb 20, 2024** Improved architecture. Site docs forthcoming. | Improved documentation for new architecture. |
| **Apr 28, 2023** Documentation updated | Improved documentation for new architecture. |
| **Apr 5, 2023** Architectural overhaul | The system has been simplified to a few components, 1) Injector that delivers a payload to a process based on PID, 2) Java agent designed to deliver code used for monitoring, 3) monitoring code payload. Some initial wiki improvements have been applied with more to come. |
| **Jun 16, 2021** Architecture overhaul | Many improvements delivered. Wiki update in progress to reflect improvements. |
| **Feb 10, 2021** Many improvements | See latest delivery notes and [updated project WIKI...](https://github.com/spoofzu/jvmxray/wiki) |

[Duke: Oracle's Java mascot...](https://wiki.openjdk.java.net/display/duke/Main)

## Many Benefits
Following are a quick list of important benefits.

### :rocket: Monitor & identify application access to protected resources
What is your application doing?  Monitor events of interest related to protected resources like: sockets, files, process execution, software supply chains, and more.

### :rocket: Improve insights into your software supply chain
JVMXRay doesn't require access to applications source code.  JVMXRay monitors your server including 3rd party libraries or commercial application where you may not have source code.

### :rocket: Extreme flexiblity & versitily
Internally JVMXRay supports logback logging.  Use standard logback configuration to specify types events and level of metadata captured, how, and where security events are logged.  Use your present centralized logging solution or handle security events distinctly.  Security event destinations are anywhere supported by the logback framework.  Connect RDBMs via JDBC, rolling text files, Flume, Kafka, JMS, NoSQL DBs like Cassandra/Mongo/CouchDB, SMTP email messges, *NIX syslogs, and your own custom solutions via HTTP, socket appenders, etc.  These are not JVMXRay features but are features of popular logging frameworks and leveraged by JVMXRay for security events.  No reason to reinvent the wheel.

### :rocket: Low entry barrier: no code changes required, extensible, and open
JVMXRay is easy to setup since it uses your current logging frameworks configuration.  Know how to setup a log4j2 or logback configuration file?  Your ready to start!100% Java code so it runs anywhere your Java apps run.

```
INFORMATION:
Consider the project early stage code.
```

## Audience
The anticipated audience for JVMXRay is two-fold,<br/>

**Systems Administrators**
Individuals charged with system security and interested in new methods to gather security inteligence into Java applications.

**Security Developers & Architects**
Indiviudals interested in improved security intelligence about their applications.

<!-- TODO: // Update example
# Deploying JVMXRay with Examples
The following provides some basic information to download and compile JVMXRay source on your computer.  Remainder of the video shows how to get JVMXRay working with Tomcat and work with Tomcat's examples.

[![](http://img.youtube.com/vi/QxgTiTCorow/0.jpg)](http://www.youtube.com/watch?v=QxgTiTCorow "JVMXRay Deploy")
-->
# Security Event Log Fragment...

Security event destinations and formats are flexible but here's some sample messages from the projects unit tests.

```
...
2024.02.12 at 13:03:06 CST | main | org.jvmxray.events.access.threadgroup | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3976b-7ffd CAT=unit-test P1=system P2= P3= CL=xray:org.jvmxray.platform.shared.classloader.XRLoggingClassLoader
2024.02.12 at 13:03:06 CST | main | org.jvmxray.events.access.thread | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3976b-7ffc CAT=unit-test P1=Notification+Thread P2= P3= CL=xray:org.jvmxray.platform.shared.classloader.XRLoggingClassLoader
2024.02.12 at 13:03:06 CST | main | org.jvmxray.events.system.properertiesnamed | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3976b-7ffb CAT=unit-test P1=sun.jnu.encoding P2= P3= CL=xray:org.jvmxray.platform.shared.classloader.XRLoggingClassLoader
2024.02.12 at 13:03:06 CST | main | org.jvmxray.events.permission.check | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3976b-7b4e CAT=unit-test P1=%2Fusr%2Flocal%2FCellar%2Fmaven%2F3.9.5%2Flibexec%2Flib%2Fjavax.inject-1.jar P2=read P3=java.io.FilePermission CL=unassigned:org.codehaus.plexus.classworlds.realm.ClassRealm
2024.02.12 at 13:03:29 CST | main | org.jvmxray.events.io.filedelete | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3ad18-6514 CAT=unit-test P1=%2Fvar%2Ffolders%2Fzb%2Flw89d2ms76x75zfy_8btv4l40000gn%2FT%2Fjansi-2.4.0-5b614d71567410f3-libjansi.jnilib.lck P2= P3= CL=xray:org.jvmxray.platform.shared.classloader.XRLoggingClassLoader
2024.02.12 at 13:03:29 CST | main | org.jvmxray.events.io.filedelete | DEBUG | AID=c2f97677bbccd0c2-6347816e-18d9eb374b8-8000 EID=6f77809c0b5f406f-3ab753c9-18d9eb3ad18-6513 CAT=unit-test P1=%2Fvar%2Ffolders%2Fzb%2Flw89d2ms76x75zfy_8btv4l40000gn%2FT%2Fjansi-2.4.0-5b614d71567410f3-libjansi.jnilib P2= P3= CL=xray:org.jvmxray.platform.shared.classloader.XRLoggingClassLoader
...

```

## How it Works
The Java Virtual Machine provides a robust security framework for controlling access to protected resources.  JVMXRay provides an implementation of the java.lang.SecurityManager component.  JVMXRay provides no policy enforcement but instead monitors activities to protected resources.  When the security manager is called the metadata is put into an event and logged using a logback logger.  It's expected machine learning and log management technologies will provide additional depth and insight into these security events as the project matures.

## Project Leader(s)
Milton Smith

Disclosure(s):  The JVMXRay project is not, approved, endorsed by, or affiliated with Oracle Corporation.  Oracle is a long-time supporter of secure open source software and the Online Web Application Security(OWASP) project.  Milton Smith is also active in the open source community and an employee of Oracle.
