
# Welcome to JVMXRay

![xrayduke](https://user-images.githubusercontent.com/8450615/88954072-af62ef00-d24e-11ea-95f9-734395481248.png) | JVMXRay is a technology for monitoring access to protected system resources by your Java applications like files, sockets, classes, and more.  It’s designed with an application security emphasis but there are benefits across other areas like, software diagnostics, usage tracking, and auditing.
| ------------- |:-------------|
<b>RECENT NEWS</b> | &nbsp;
Apr 28, 2023 Documentation updated | Improved documentation for new architecture.
Apr 5, 2023 Architectural overhaul | The system has been simplified to a few components, 1) Injector that delivers a payload to a process based on PID, 2) Java agent designed to deliver code used for monitoring, 3) monitoring code payload.  Some initial wiki improvements have been applied with more to come.
Jun 16, 2021  Architecture overhaul | Many improvements delivered.  Wiki update in progress to reflect improvmenets.
Feb 10, 2021  Many improvements | See latest delivery notes and [updated project WIKI...](https://github.com/spoofzu/jvmxray/wiki)

[Duke: Oracle's Java mascot...](https://wiki.openjdk.java.net/display/duke/Main)

## Many Benefits
Following is a quick list of some of the more important benefits.

### :rocket: Monitor & identify application access to protected resources
Do you know what your application is doing?  Monitor events of interest related to sockets, files, process execution, software supply chains, and more.  When a security event occurs, process it as you wish. 

### :rocket: Improve insights into your software supply chain
JVMXRay doesn't require access to your applications source code to work like typical security scanning tools.  This means you can use JVMXRay to monitor 3rd party open source libraries.

### :rocket: Extreme flexiblity & versitily
JVMXRay supports frameworks like log4j2, logback, Java Util logging, you have flexilty about events types captured, how, and where they are logged.  Use your present centralized logging solution or handle security events distinctly.  Security event destinations are anywhere your logging framework Appenders allow like: Cassandra, RDBMs via JDBC, rolling text files, Flume, Kafka, JMS, NoSQL DBs like Mongo/CouchDB, SMTP email messges, *NIX syslogs, and your own custom solutions via HTTP, socket appenders, etc.  These are not JVMXRay features but are features of popular logging frameworks and leveraged by JVMXRay for security events.  No reason to reinvent the wheel.

### :rocket: Low entry barrier: no code changes required, extensible, and open
JVMXRay is easy to setup since it uses your current logging frameworks configuration.  Know how to setup a log4j2 or logback configuration file?  Your ready to start!100% Java code so it runs anywhere your Java apps run.

```
INFORMATION:
Consider the project early stage code. 
Thar be dragons mate.  You were warned!
```

## Audience
The anticipated audience for JVMXRay is two-fold,<br/>

**Systems Administrators**
Individuals charged with system security and interested in new methods to gather security inteligence into Java applications.

**Security Developers & Architects**
Indiviudals interested in improved security intelligence about their applications.

# Deploying JVMXRay with Examples
The following provides some basic information to download and compile JVMXRay source on your computer.  Remainder of the video shows how to get JVMXRay working with Tomcat and work with Tomcat's examples.

[![](http://img.youtube.com/vi/QxgTiTCorow/0.jpg)](http://www.youtube.com/watch?v=QxgTiTCorow "JVMXRay Deploy")

# Security Event Log Fragment...

Security event destinations and formats are flexible but here's some sample messages from the projects unit tests.

```
...
2023-04-25T20:18:00.757Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.classloadercreate, WARN,EVENTTP={CLASSLOADER_CREATE} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-8000} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={} P2={} P3={}
2023-04-25T20:18:00.793Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.io.fileread, WARN,EVENTTP={FILE_READ} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7ffc} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={checkRead1-test-18074656247045790493.tmp} P2={} P3={}
2023-04-25T20:18:00.794Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.classloadercreate, WARN,EVENTTP={CLASSLOADER_CREATE} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7ffb} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={} P2={} P3={}
2023-04-25T20:18:00.796Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.packagedefine, WARN,EVENTTP={PACKAGE_DEFINE} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7ff9} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={commons-cli-1.3.1.jar} P2={} P3={}
2023-04-25T20:18:00.822Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.io.filewrite, WARN,EVENTTP={FILE_WRITE} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7fda} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={checkWrite1-test-1196745006830559472.tmp} P2={} P3={}
2023-04-25T20:18:00.824Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.sox.socketconnect, WARN,EVENTTP={SOCKET_CONNECT} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7fd7} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={www.orange-jupiter.com} P2={2167} P3={}
2023-04-25T20:18:00.826Z,main,version=0.1,org.jvmxray.agent.driver.jvmxraysecuritymanager.events.sox.socketlisten, WARN,EVENTTP={SOCKET_LISTEN} EVENTID={926bbb1e485f7e27-4d00bb7b-187ba10dfb4-7fd4} AID={045db2423ef7485e-40573590-18725e16a5e-8000} CATEGORY={unit-test} P1={1159} P2={} P3={}
...

```

## How it Works
The Java Virtual Machine provides a robust security framework for controlling access to protected resources.  JVMXRay provides an implementation of the java.lang.SecurityManager component, called jvmxraysecuritymanager.  Ironically, jvmxraysecuritymanager provides no policy enforcement but instead monitors activities to protected resources.  When the security manager is called the metadata is put into an event and logged using an SLF4J logger.  SLF4J is a lightweight logging facade that allows JVMXRay support popular logging frameworks.  It's expected machine learning and log management technologies will provide additional depth and insight into these security events as the project matures.

## Project Leader(s)
Milton Smith

Disclosure(s):  The JVMXRay project is not, approved, endorsed by, or affiliated with Oracle Corporation.  Oracle is a long-time supporter of secure open source software and the Online Web Application Security(OWASP) project.  Milton Smith is also active in the open source community and an employee of Oracle.
