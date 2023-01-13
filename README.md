[![Build Status](https://travis-ci.org/spoofzu/jvmxray.svg?branch=master)](https://travis-ci.org/spoofzu/jvmxray)

# Welcome to JVMXRay

![xrayduke](https://user-images.githubusercontent.com/8450615/88954072-af62ef00-d24e-11ea-95f9-734395481248.png) | JVMXRay is a technology for monitoring access to Java protected system resources like files, sockets, and more, used by your application.  It’s designed with an application security emphasis but there are benefits in other areas like, software diagnostics, usage tracking, and auditing.
| ------------- |:-------------|
<b>RECENT NEWS</b> | &nbsp;
Jun 16, 2021  Architecture overhaul | Many improvements delivered.  Wiki update in progress to reflect improvmenets.
Feb 10, 2021  Many improvements | See latest delivery notes and [updated project WIKI...](https://github.com/spoofzu/jvmxray/wiki)

[More about Oracle Java Duke mascot...](https://wiki.openjdk.java.net/display/duke/Main)

## Contact/Chat Group
New chat information forthcoming.

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
Consider this early stage code. 
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

The output from this technology can be presented in different ways.  What does the sample log output look like?  Output formats are flexible the following is a sample of the type of information JVMXRay captures for you.

```
-1,-1,1623791226603,main-1,EXIT,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,100,,
-1,-1,1623791226603,main-1,LINK,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,mylib.dll,,
-1,-1,1623791226619,main-1,FILE_READ_WITH_FILEDESCRIPTOR,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,java.io.FileDescriptor@6be46e8f,,
-1,-1,1623791226620,main-1,FILE_READ_WITH_CONTEXT,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,redsecuritymanagertest3-11060405012461211247.tmp,java.security.AccessControlContext@363a949e,
-1,-1,1623791226621,main-1,PERMISSION,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,TestPermission,testpermissionname1,testaction1
-1,-1,1623791226628,main-1,PERMISSION_WITH_CONTEXT,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,testpermissionname1,testaction1,org.jvmxray.RedSecurityManagerTest@47f6473
-1,-1,1623791226629,main-1,SOCKET_ACCEPT,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,localhost,1234,
-1,-1,1623791226631,main-1,FILE_DELETE,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,redsecuritymanagertest6-5182052521633743041.tmp,,
-1,-1,1623791226631,main-1,SOCKET_LISTEN,1bd99e2f195004a5-5cb63e19-179fd6e8882-8000,,123,,
```

## How it Works
The Java Virtual Machine provides a robust security framework for controlling access to protected resources.  JVMXRay provides an implementation of the java.lang.SecurityManager component, called NullSecurityManager.  Ironically, the NullSecurityManager provides no policy enforcement but instead monitors activities to protected resources.  It's expected other cloud log processing tools, big data tools, or cloud secuirty tools will process these events into meaningful contextual information.

## Project Leader(s)
Milton Smith
