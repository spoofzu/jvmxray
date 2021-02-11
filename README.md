[![Build Status](https://travis-ci.org/spoofzu/jvmxray.svg?branch=master)](https://travis-ci.org/spoofzu/jvmxray)

# Welcome to JVMXRay

&nbsp; | &nbsp;
------------ | -------------
![xrayduke](https://user-images.githubusercontent.com/8450615/88954072-af62ef00-d24e-11ea-95f9-734395481248.png) | JVMXRay is a technology for monitoring access to system resources within the Java Virtual Machine.  It’s designed with application security emphasis but some will also find it beneficial for software quality processes and diagnostics.

[More about Oracle Java Duke mascot...](https://wiki.openjdk.java.net/display/duke/Main)

NEWS Feb 10, 2021: Many improvements. See latest delivery notes and
[updated project WIKI...](https://github.com/spoofzu/jvmxray/wiki)

## Contact/Chat Group
For regular conversation around the product and development you can find us on OWASP Slack server under the [#owasp-jvmxray](https://owasp.slack.com/archives/C01CEV19DMW) channel.  To join OWASP Slack server,

Navigate to [OWASP Slack signup...](https://owasp-slack.herokuapp.com/)<br/>
Register with your e-mail address.<br/>
When in the Slack, find our channel in the channel list, or type: /join #owasp-jvmxray<br/>

## Benefits
Following is a quick list of some of the more important benefits.

### :rocket: Identify protected resources
Track different types of events of interest related to sockets, files, process execution, and more.  When an event of interest occurs, process it as you wish.  At the moment, adaptors for the system console (e.g., System.out), logback, and Java Logging, are available with others in process. 

### :rocket: No code changes required
JVMXRay does not require any changes to your application source code to work.  The code is pulled into the JVM by a command line option.  The solution is 100% Java code so it runs anywhere.

### :rocket: Supply chain insights
An ancilary benefit of not requiring source code is that JVMXRay provides insight into your applications dependencies including 3rd party libraries (e.g. Jar files).  Events provide the source of origin where your classes where loaded when the event is generated.

### :rocket: Extensible & Open
Don't see an adapter or filter that works for you and know how to code?  Roll up your sleeves and write one.  It's extensible.  Fix a bug and submit a pull requrest.  All the source code is available.

```
INFORMATION:
Project under review for acceptance as an OWASP project. Please
consider this early stage code. 
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

# Sample Output...

The output from this technology can be presented in different ways.  What does the sample log output look like?  Output formats are flexible but following is small idea of what you can capture in a file.

```
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=javax.crypto,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProperty.keystore.type.compat, a=, cn=java.security.SecurityPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST FILE_READ,f=/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/lib/security/cacerts,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=192.30.255.112, p=443,stack=<disabled>
```

## How it Works
The Java Virtual Machine provides a robust security framework for controlling access to protected resources.  JVMXRay provides an implementation of the java.lang.SecurityManager component, called NullSecurityManager.  Ironically, the NullSecurityManager provides no policy enforcement but instead monitors activities to protected resources.  It's expected other cloud log processing tools, big data tools, or cloud secuirty tools will process these events into meaningful contextual information.

## Project Leaders
Milton Smith<br/>
John Melton
