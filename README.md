
# JVMXRay: AI-Enhanced Java Security Monitoring

[![Black Hat Arsenal](https://raw.githubusercontent.com/toolswatch/badges/master/arsenal/usa/2020.svg?sanitize=true)](https://www.toolswatch.org/blackhat-arsenal-us-2020-archive/)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![Build Status](https://github.com/spoofzu/jvmxray/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spoofzu/jvmxray/actions/workflows/maven-build.yml)
[![Contributors](https://img.shields.io/github/contributors/spoofzu/jvmxray.svg)](https://github.com/spoofzu/jvmxray/graphs/contributors)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Development Status](https://img.shields.io/badge/Status-Alpha-red.svg)](ZREADME-ALPHA.md)

<div align="left">
<picture>
  <source srcset="./media/logo-wht-on-blk.png?raw=true" media="(prefers-color-scheme: dark)">
  <source srcset="./media/logo-blk-on-wht.png?raw=true" media="(prefers-color-scheme: light)">
  <img src="https://github.com/spoofzu/jvmxray/blob/master/build/logo-blk-on-wht.png?raw=true" alt="JVMXRay Logo" width="600">
</picture>
</div>

**[Duke history](https://dev.java/duke/)**

**JVMXRay is an AI-enhanced security monitoring platform that watches Java applications in real-time, detecting vulnerabilities and suspicious activity without requiring code changes. Intelligence analysis enriches security events with AI-powered metadata and context for enhanced risk prioritization and compliance reporting. With simple setup and minimal performance impact, JVMXRay provides comprehensive security visibility into applications and third-party dependencies, enhanced by machine learning that improves detection accuracy over time.**

| &nbsp;                                  | &nbsp;                                                                                                                                                                                                                                                                                                                                |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **NEWS**                                | &nbsp;                                                                                                                                                                                                                                                                                                                                |
| **Mar 26, 2026** Sensor Metadata Overhaul | Major sensor improvement: event correlation with scope_chain/parent_scope/scope_depth, TRACE_ID indexed column, 3 P0 sensors enriched (API, Auth, Script), spanning scopes for Process and Crypto, MCCScope added to 24 interceptors, bug fixes. See [CHANGELOG](docs/CHANGELOG.md). |
| **Oct 7, 2025** Minor Fixes             | Sensor meta improvements, developing AI service (work in progress) OWASP Dependency Check integration (NVD metadata, CVSS scoring, etc) for 3rd party libraries (not operational at the moment).                                                                                                                                      |
| **Sep 18, 2025** Major Update           | Significant milestone release featuring AI-powered vulnerability analysis, online/offline integration support with AI MCP clients like Claude Desktop, 15+ sensor types and growing, enhanced documentation suite, multi-database support (SQLite/MySQL/Cassandra), and complete CI/CD pipeline. Suitable for testing and evaluation. |
| **Apr 23, 2025** Platform rearchitected | Architecture improved to remove deprecated SecurityManager and move to byte code injection approach.                                                                                                                                                                                                                                  |

**📰 [View News Archive](ZREADME-NEWS.md)** - Complete history of project announcements and milestones

## ⚠️ Development Status & Security Notice

**🚨 ALPHA SOFTWARE - NOT FOR PRODUCTION USE 🚨**

JVMXRay is currently in **alpha development status** and should **NOT be deployed in production environments**. This software is intended for testing, evaluation, and development purposes only.

**📖 [Learn more about alpha limitations and security considerations →](ZREADME-ALPHA.md)**

---

## Why JVMXRay?

Java applications are under constant attack, but traditional security tools require code changes, create performance overhead, or generate too many false positives. JVMXRay improves upon current state by:

- **Zero Code Changes**: Monitor any Java application without modifications
- **Intelligence Analysis**: AI enriches security events with contextual metadata for risk prioritization and compliance reporting
- **Complete Visibility**: See exactly what applications and dependencies are doing
- **Minimal Impact**: Low performance overhead using well-proven technologies like bytecode injection and Logback enterprise logging
- **Enterprise Ready**: Works with existing logging infrastructure and scales to any size

## Logging Philosophy: Beyond Traditional Approaches

### The Problem with Unstructured Logging
Traditional application logging relies on developers manually adding log statements throughout code. This creates several challenges:
- **Developer-Dependent**: Log quality varies based on individual experience and foresight
- **Needle in Haystack**: Complex parsing tools required to extract meaningful insights from unstructured text
- **Inconsistent Format**: Different developers use different logging patterns and message formats
- **Reactive Coverage**: Only logs what developers anticipated needing, missing unexpected security events

### JVMXRay's Structured Security Events
JVMXRay automatically generates structured security events without developer intervention:
- **System-Generated**: Consistent, comprehensive coverage regardless of developer experience
- **Machine-Readable**: Structured formats enable instant analysis and correlation without complex parsing
- **Predictive Coverage**: Monitors security-relevant operations automatically, capturing events developers might not anticipate
- **AI-Ready**: Structured data enables intelligent analysis, pattern recognition, and automated threat detection

## Who Benefits and How

### 🏢 Operations & IT Teams
- **Datacenter Intelligence**: Keep sensitive security data on-premises while leveraging AI analysis capabilities
- **Zero-Downtime Deployment**: Monitor applications without restarts or maintenance windows
- **Alert Reduction**: Intelligent filtering significantly reduces security alert noise
- **Infrastructure Integration**: Works with existing Splunk, ELK, DataDog, and logging systems

### 👨‍💻 Developers & Engineers  
- **Beyond Traditional Logging**: Eliminate dependency on manual log statements - JVMXRay automatically captures security-relevant operations in structured formats that don't require complex parsing tools to find security needles in log haystacks
- **AI Security Intelligence**: JVMXRay MCP Server provides event-based security intelligence to AI clients instantly, making them security experts on your datacenter. Compatible with Claude Desktop or internal MCP clients.
- **Improved Diagnostics**: Point-in-time system state capture with monitor sensor and uncaught exception handler to diagnose cloud server failures. Eliminates the need to recreate complex test environments for debugging production issues.
- **Real-time Insights**: See file access, network connections, and system calls as they happen

### 🔒 Security Practitioners
- **Advanced Threat Detection**: Machine learning enhances vulnerability identification accuracy
- **MITRE ATT&CK Mapping**: Automatic attack technique identification and timeline analysis
- **Incident Response**: Complete attack context with file, network, and process details
- **Low False Positives**: Context-aware detection based on actual application security events

### 📈 Engineering Leaders & CISOs
- **Risk Reduction**: Proactive vulnerability detection prevents costly data breaches
- **Strategic Investment**: Comprehensive security monitoring that scales with business growth
- **Open Source Foundation**: No vendor lock-in with enterprise support options available
- **Supply Chain Security**: Monitor third-party libraries and detect malicious behavior
- **Compliance Automation**: AI-enriched security event data supporting compliance automation for SOC 2, PCI DSS, and regulatory reporting

<!-- TODO: // Update example
# Deploying JVMXRay with Examples
The following provides some basic information to download and compile JVMXRay source on your computer.  Remainder of the video shows how to get JVMXRay working with Tomcat and work with Tomcat's examples.

[![](http://img.youtube.com/vi/QxgTiTCorow/0.jpg)](http://www.youtube.com/watch?v=QxgTiTCorow "JVMXRay Deploy")
-->

## Video Tutorial

Watch a [video](https://youtu.be/HufPrUo96H8?si=CTwZ1SISg9HW39Jh) to build JVMXRay and integrate with Claude Desktop AI followed by a quick demo or jump directly to the [demo at 3:30](https://youtu.be/HufPrUo96H8?si=toqnh9hu2EgtOrNu&t=211).

## Quick Start

Get up and running in **under 5 minutes**:

1. **Change to GitHub Repository Folder**
   ```bash
   cd {your-github-folder}/
   ```

2. **Clone JVMXRay Repository**
   ```bash
   git clone https://github.com/spoofzu/jvmxray.git
   ```

3. **Build JVMXRay Project**
   ```bash
   mvn clean package
   ```

**Congratulations! You've built JVMXRay successfully!**

The project compiles, tests pass, and includes:
- Complete sensor framework with 19 monitoring sensors
- Multi-database support (SQLite/MySQL/Cassandra)
- AI-enhanced security event analysis
- Enterprise logging integration

### Continue Optional Setup

Ready for advanced features? Continue with:

**📖 [Continue Setup →](docs/setup-continued.md)** - Security event enrichment, AI integration, and vulnerability analysis

**Optional features include:**
- Step 5: Migrate data to enriched format for AI analysis
- Step 6: MCP integration for Claude Desktop AI-powered queries

## Key Capabilities

### 🤖 AI-Enhanced Analysis
- **Structured Security Events**: Unlike traditional unstructured logs that require complex parsing, JVMXRay generates machine-readable security events automatically
- **AI Data Enrichment**: Structured data enables intelligent vulnerability classification with CWE assignment and dynamic CVSS scoring based on attack characteristics
- **Pattern Recognition**: Advanced detection for SQL injection, command injection, path traversal using consistent event formats
- **Intelligence Pipeline**: Rule-based analysis with configurable pattern matching and threat classification on structured data

### 🔧 Extensible Sensor Capabilities
- **File I/O Sensor**: Monitors file system access, reads, writes, and deletions
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.io.fileio |  | caller=java.io.File:1075, file=/tmp/sensitive.data, operation=DELETE, status=deleted, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=unit-test
  ```
- **Network Sensor**: Tracks socket connections, binds, and data transfers
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.net.connect |  | caller=java.net.Socket:189, destination=malicious-site.com:443, status=connected, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **Process Sensor**: Detects system command execution and privilege escalation
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.system.exec |  | caller=java.lang.ProcessBuilder:1029, command=/bin/sh -c rm -rf /, status=blocked, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **Monitor Sensor**: System performance and health monitoring with point-in-time snapshots
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.monitor |  | caller=org.jvmxray.agent.sensor.monitor.MonitorSensor:45, GCCount=1, ThreadRunnable=2, MemoryFree=566.3MB, ProcessCpuLoad=0%, OpenFiles=163, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **Library Sensor**: Dynamic and static JAR loading detection with supply chain visibility
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.system.lib |  | caller=ClassLoader:123, load_type=dynamic, jar_path=/path/to/library.jar, sha256=a1b2c3..., groupId=com.example, artifactId=library, version=1.2.3, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **Serialization Sensor**: Object serialization monitoring for deserialization attacks
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.serialization |  | caller=ObjectInputStream:123, target=UserData.class, status=deserialized, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **SQL Sensor**: Database query monitoring and injection attempt detection
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | main |  INFO | org.jvmxray.events.sql.query |  | caller=java.sql.Statement:142, query=SELECT * FROM users WHERE id = '1 OR 1=1', status=potential_injection, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **Uncaught Exception Sensor**: Application error and crash monitoring with comprehensive diagnostics
  ```
  C:AP | 2025.09.18 at 11:23:34 CDT | payment-processor-1 |  INFO | org.jvmxray.events.system.uncaughtexception |  | caller=com.example.PaymentProcessor:145, thread_name=payment-processor-1, thread_id=42, thread_group=main, exception_type=java.lang.NullPointerException, exception_location=com.example.PaymentProcessor:145, exception_method=processPayment, exception_message=Cannot process null payment, stack_depth=28, memory_pressure=HIGH, heap_used_mb=756.2, command_line=java -jar payment-service.jar --port=8080, jvm_uptime_minutes=47, incident_id=f3d4e5a6-b7c8-4d9e-a1b2-3c4d5e6f7a8b, AID=7KLZZAC0DM9RA1ISVXQY63NTK, CID=production
  ```
- **API Sensor**: REST and web service call monitoring with full request/response metadata (URI, method, host, status code, response time, TLS detection)
- **Authentication Sensor**: 6 specialized interceptors tracking login attempts (JAAS, Spring Security), session operations, and principal queries with success/failure tracking and hashed session IDs
- **ScriptEngine Sensor**: Script execution monitoring with engine identification, content hashing, suspicious pattern detection (Runtime.exec, ProcessBuilder, Class.forName), and risk classification
- **Configuration Sensor**: Application configuration access and modification tracking with security classification
- **HTTP Sensor**: Web request and response pattern analysis with security header analysis
- **Cryptographic Sensor**: Encryption/decryption operations with weak algorithm detection, key strength validation, and spanning scope correlation
- **Reflection Sensor**: Dynamic code loading and class manipulation detection with threat classification
- **Data Transfer Sensor**: Large data movement and export detection
- **Thread Sensor**: Thread lifecycle and synchronization monitoring

### 🔗 Event Correlation
- **Security Stacktraces**: Every event includes `trace_id`, `scope_chain`, `parent_scope`, and `scope_depth` for reconstructing attack chains across sensors
- **Scope Chain Visualization**: See nested sensor activations like `HTTP>Serialization>Reflection>Process` — a deserialization attack signature at a glance
- **Indexed TRACE_ID**: Dedicated database column with index for fast correlation queries across events sharing the same trace
- **Automatic Context Propagation**: HTTP requests, SQL queries, file I/O, crypto operations, and process executions are automatically linked within the same execution context

### 🏗️ Enterprise Architecture
- **Database Support**: SQLite (testing), MySQL, Cassandra (production)
- **Logging Integration**: Logback framework supports any destination (Kafka, JMS, SMTP, etc.)
- **High Performance**: Bytecode injection with minimal overhead
- **Scalable Design**: From single applications to enterprise-wide deployments

## How JMVXRay Works

1. **Deploy Agent**: Add JVMXRay as Java agent: `java -javaagent:xray.jar -jar yourapp.jar` - That's it! Your application won't know it's monitored.
2. **Sensor Installation**: Bytecode injection installs monitoring sensors automatically  
3. **Event Capture**: Sensors monitor file access, network connections, system calls
4. **AI Analysis**: Intelligence pipeline analyzes patterns and assigns vulnerability classifications
5. **Structured Event Generation**: Unlike traditional unstructured logs that require complex analysis tools, JVMXRay automatically generates machine-readable security events that enable instant analysis and AI enhancement
6. **Query & Analyze**: Generate reports with standard BI tools, visualize with Grafana dashboards, or answer ad-hoc queries using AI MCP clients connected to JVMXRay's MCP server  

### Enhanced MCP Query Capabilities ✨ **NEW**

JVMXRay's MCP server enables AI clients like Claude Desktop to become instant security experts on your datacenter. The system provides sophisticated querying capabilities with advanced filtering, pagination, and real-time analysis.

**See it in action:** [JVMXRay AI Integration Demo](https://www.youtube.com/watch?v=O-5oIXijMb8&t=101s) - A video is worth a thousand words!

## Documentation

### Component Documentation
- **[🔧 JVMXRay Agent](docs/prj-agent.md)** - Java agent setup and sensor configuration
- **[📊 Common Components](docs/prj-common.md)** - Database setup and utilities

### Quick Links
- **[📋 Changelog](docs/CHANGELOG.md)** - Detailed release changes and improvements
- **[📰 News Archive](ZREADME-NEWS.md)** - Complete project history

## Project Contributors(s)
Milton Smith - Project creator, leader

Disclosure(s):  The JVMXRay project is not, approved, endorsed by, or affiliated with Oracle Corporation.  Oracle is a long-time supporter of secure open source software and the Online Web Application Security(OWASP) project.  Milton Smith is an active industry open source contributor, OWASP member, and an employee of Oracle.

<!-- Test build with Java version compatibility fix -->
