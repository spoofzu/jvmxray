# Welcome to JVMXRay

JVMXRay is a technology for monitoring access to system resources within the Java Virtual Machine.  It’s designed with application security emphasis but some will also find it beneficial for software quality processes and diagnostics.

## Benefits
Following is a quick list of some of the more important benefits.

Provides security insight into application access to system resources.
Track different types of events of interest related to sockets, files, process execution, and more.  When an event of interest occurs, you can send it to your centralized logging system.  At the moment, adaptors for the system console (e.g., System.out), logback, and Java Logging are available. 

## No code changes
JVMXRay does not require any changes to your application code to work.

## Supply chain insights
Since no code changes are required to your application, JVMXRay provide insight into 3rd party dependencies.  JVMXRay can classes and keep track of which jars they came from.

## Audience
The anticipated audience for JVMXRay is two-fold, administrators charged with system security, and developers interested in security.  So if you like new security tools this will be interesting to you.  Also if you’re a Java security developer, you can extend JVMXRay API’s to do anything your creative mind can imagine.

## How it Works
The Java Virtual Machine provides security framework for controlling access to protected resources via a policy management framework.  Specifically, JVMXRay provides an implementation of security framework component that extends from java.lang.SecurityManager, called NullSecurityManager.  Unlike most security managers the NullSecurityManager provides no enforcement but instead monitors activities.  Because of the way it works, JVMXRay is incompatible with any solution that uses a security manager.

## Running JVMXRay
1)	Copy the JVMXRay library to your application server’s classpath.
2)	Add the system property, -Djava.security.manager=<security_manager>.  Where <security_manager> is one of three classes, org.owasp.jvmxray.adaptors.ConsoleAdaptor, org.owasp.jvmxray.adaptors.JavaLoggingAdaptor, or org.owasp.jvmxray.adaptors.LogbackAdaptor
3)	Edit your jvmxray.properties to capture your events of interest.  Properties are served from the classpath root by default or from a web server.  To load from your web server assign the following property, -Djvmxray.configuration=<url>.  Where url is the full url to the property file.

## Sample Command Line...
```code
java -Djava.security.manager=com.owasp.jvmxray.adaptors.LogbackAdaptor -Djvmxray.configuration="https://www.myserver.com/jvmxray/site.properties” com.your.main.class
```

The previous command would start Java with the LogbackAdaptor using the configuration file specified by site.properties when executing your class, com.main.class.  This is a simplified command for illustration.  Specific configuration depends upon which application server your using.  Keep in mind, if you use the LogbackAdaptor or JavaLoggingAdaptor additional configuration may be required.
