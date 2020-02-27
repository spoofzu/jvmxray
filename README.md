[![Build Status](https://travis-ci.org/spoofzu/jvmxray.svg?branch=master)](https://travis-ci.org/spoofzu/jvmxray)

# Welcome to JVMXRay

JVMXRay is a technology for monitoring access to system resources within the Java Virtual Machine.  It’s designed with application security emphasis but some will also find it beneficial for software quality processes and diagnostics.

```
CONSIDERATIONS:
This project is being activty under review for acceptance as an OWASP project. Please
consider this early stage code, with bugs, not suitable for production use. 
There be dragons.  You were warned!
```

## Benefits
Following is a quick list of some of the more important benefits.

### Identify application access to protected resources
Track different types of events of interest related to sockets, files, process execution, and more.  When an event of interest occurs, send it to your centralized logging system, or favorite SEIM tool.  At the moment, adaptors for the system console (e.g., System.out), logback, and Java Logging, are available with others in process. 

### No application code changes required
JVMXRay does not require any changes to your application source code to work.  The code is pulled into the JVM by a command line option.  The solution is 100% Java code so it runs anywhere.

### Supply chain insights
An ancilary benefit of not requiring source code is that JVMXRay provides insight into your applications dependencies including 3rd party libraries (e.g. Jar files).  Events provide the source of origin where your classes where loaded when the event is generated.

## Audience
The anticipated audience for JVMXRay is two-fold,<br/>
**Administrators** charged with system security and interested in new methods to gather inteligence.<br/>
**Security Developers & Architects** interested in more security intelligence about their applications.

# Sample Output...

You can fire events to your favorite JMX viewer or use the view built into Java, jconsole.

![jconsole1](https://user-images.githubusercontent.com/8450615/75402724-777c0800-58ba-11ea-8873-22e14a89a468.png)

What does the sample log output look like?  Tt really depends upon the type of adaptor you use to handle the events and settings but the following is small idea of what you could capture if you printed it to a log file.

```
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=javax.crypto,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProperty.keystore.type.compat, a=, cn=java.security.SecurityPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST FILE_READ,f=/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/lib/security/cacerts,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=192.30.255.112, p=443,stack=<disabled>
```
Some of the events appear duplicated since the console adaptor is a simplified output.

## How it Works
The Java Virtual Machine provides a robust security framework for controlling access to protected resources.  JVMXRay provides an implementation of the java.lang.SecurityManager component, called NullSecurityManager.  Ironically, the NullSecurityManager provides no policy enforcement but instead monitors activities to protected resources.  It's expected other cloud log processing tools, big data tools, or cloud secuirty tools will process these events into meaningful contextual information.

## Project Leaders
Milton Smith<br/>
John Melton
