
# Welcome to JVMXRay

[![Black Hat Arsenal](https://raw.githubusercontent.com/toolswatch/badges/master/arsenal/usa/2020.svg?sanitize=true)](https://www.toolswatch.org/blackhat-arsenal-us-2020-archive/)

<picture>
  <source srcset="https://github.com/spoofzu/jvmxray/blob/master/build/lightduke.png?raw=true" media="(prefers-color-scheme: dark)">
  <source srcset="https://github.com/spoofzu/jvmxray/blob/master/build/darkduke.png?raw=true" media="(prefers-color-scheme: light)">
  <img src="https://github.com/spoofzu/jvmxray/blob/master/build/lightduke.png" alt="Logo" width="200">
</picture>

|                 &nbsp;                         |  &nbsp;                                                                                                                                                                                                                                                                                                     |
|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JVMXRay**                             | JVMXRay is a technology for monitoring access to protected system resources by your Java applications like files, sockets, classes, and more. It’s designed with an application security emphasis but there are benefits across other areas like software diagnostics, usage tracking, and auditing. |
| &nbsp;  |  &nbsp;                                                                                                                                                                                                                                                              |
| **NEWS**                                | &nbsp;                                                                                                                                                                                                                                                                                               |
| **Apr 23, 2025** Platform rearchitected | Architecture improved to remove deprecated SecurityManager and move to byte code injection approach.                                                                                                                                                                                                 |
| **Feb 20, 2024** Improved architecture  | Improved documentation for new architecture. Site docs forthcoming.                                                                                                                                                                                                                                  |
| **Apr 28, 2023** Documentation updated  | Improved documentation for new architecture.                                                                                                                                                                                                                                                         |
| **Apr 5, 2023** Architectural overhaul  | The system has been simplified to a few components: 1) Injector that delivers a payload to a process by PID, 2) Java agent for monitoring code delivery, 3) the monitoring code itself. Initial wiki improvements have been applied with more to come.                                               |
| **Jun 16, 2021** Architecture overhaul  | Many improvements delivered. Wiki update in progress to reflect improvements.                                                                                                                                                                                                                        |
| **Feb 10, 2021** Many improvements      | See latest delivery notes and [updated project WIKI...](https://github.com/spoofzu/jvmxray/wiki)                                                                                                                                                                                                     |

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
2025.04.23 at 16:38:52 CDT | jvmxray.sensor-1 |  INFO | org.jvmxray.events.io.filedelete |  | caller=java.io.File:1075, target=/Users/milton/.webgoat-2025.4-SNAPSHOT/webgoat.properties, status=successfully deleted
2025.04.23 at 16:38:51 CDT | jvmxray.sensor-1 |  INFO | org.jvmxray.events.monitor |  | GCCount=1, ThreadNew=0, ThreadWaiting=2, ThreadTerminated=0, NonHeapUsed=11.6MB, GCTime=1ms, DeadlockedThreads=0, ProcessCpuLoad=0%, ThreadBlocked=0, MemoryFree=566.3MB, caller=java.lang.Thread:1575, OpenFiles=163, ThreadRunnable=2, MemoryMax=9GB, MemoryTotal=584MB
2025.04.23 at 16:38:51 CDT | jvmxray.sensor-1 |  INFO | org.jvmxray.events.system.lib |  | method=static, jarPath=/Users/milton/.m2/repository/org/springframework/boot/spring-boot-configuration-metadata/3.4.3/spring-boot-configuration-metadata-3.4.3.jar, caller=sun.instrument.InstrumentationImpl:560
2025.04.23 at 16:38:51 CDT | jvmxray.sensor-1 |  INFO | org.jvmxray.events.system.lib |  | method=dynamic, jarPath=/Users/milton/github/jvmxray/agent/target/agent-0.0.1-shaded.jar, caller=java.lang.Thread:1575
...

```

## How it Works
JVMXRay runs as a Java Agent and is injected at startup into your project via Java command line parameter.  When the Agent initializes, it installs sensors via byte-code injection monitoring access to protected resources (files, http connections, etc) by your program and third party programs. Event metadata is logged as a Logback log message.  Logback is a popular logging platform, very powerful and flexible, and facilites security event handling in many ways.  Tools and tip for configuring Logback are available widely and many books are available.  

## Project Contributors(s)
Milton Smith - Project creator, leader

Disclosure(s):  The JVMXRay project is not, approved, endorsed by, or affiliated with Oracle Corporation.  Oracle is a long-time supporter of secure open source software and the Online Web Application Security(OWASP) project.  Milton Smith is active in the open source community and an employee of Oracle.
