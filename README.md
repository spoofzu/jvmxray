[![Build Status](https://travis-ci.org/spoofzu/jvmxray.svg?branch=master)](https://travis-ci.org/spoofzu/jvmxray)

# Welcome to JVMXRay

JVMXRay is a technology for monitoring access to system resources within the Java Virtual Machine.  It’s designed with application security emphasis but some will also find it beneficial for software quality processes and diagnostics.

```
CONSIDERATIONS:
This project is being activty under review for acceptance as an OWASP project. Please
consider this early stage code, with bugs, not suitable for production use. 
There be dragons.  You were warned!
```

## Project Leaders
Milton Smith<br/>
John Melton

## Benefits
Following is a quick list of some of the more important benefits.

### Provides security insight into application access to system resources.
Track different types of events of interest related to sockets, files, process execution, and more.  When an event of interest occurs, you can send it to your centralized logging system.  At the moment, adaptors for the system console (e.g., System.out), logback, and Java Logging are available. 

### No code changes
JVMXRay does not require any changes to your application code to work.

### Supply chain insights
Since no code changes are required to your application, JVMXRay provide insight into 3rd party dependencies.  JVMXRay can classes and keep track of which jars they came from.

## Audience
The anticipated audience for JVMXRay is two-fold, administrators charged with system security, and developers interested in security.  So if you like new security tools this will be interesting to you.  Also if you’re a Java security developer, you can extend JVMXRay API’s to do anything your creative mind can imagine.

## How it Works
The Java Virtual Machine provides security framework for controlling access to protected resources via a policy management framework.  Specifically, JVMXRay provides an implementation of security framework component that extends from java.lang.SecurityManager, called NullSecurityManager.  Unlike most security managers the NullSecurityManager provides no enforcement but instead monitors activities.  Because of the way it works, JVMXRay is incompatible with any solution that uses a security manager.

# Sample Output...
Ok, so what does the sample output look like?  Well it really depends upon the type of adaptor you use to handle the events and settings but the following is small idea of what you could capture if you printed it to a log file.

```
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=javax.crypto,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=javax.crypto,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=java.security.interfaces,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PACKAGE_ACCESS,pkg=javax.crypto.spec,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProperty.jdk.certpath.disabledAlgorithms, a=, cn=java.security.SecurityPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST FILE_READ,f=/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/lib/security/blacklisted.certs,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProperty.keystore.type.compat, a=, cn=java.security.SecurityPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST FILE_READ,f=/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/lib/security/cacerts,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=suppressAccessChecks, a=, cn=java.lang.reflect.ReflectPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=github.com, p=-1,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=https://github.com/, a=GET:, cn=java.net.URLPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProtectionDomain, a=, cn=java.lang.RuntimePermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST PERMISSION,n=getProxySelector, a=, cn=java.net.NetPermission,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=github.com, p=443,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=github.com, p=-1,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=192.30.255.112, p=443,stack=<disabled>
CONSOLEADAPTOR 2020-02-24 14:13:28 PST SOCKET_CONNECT,h=192.30.255.112, p=443,stack=<disabled>
```
