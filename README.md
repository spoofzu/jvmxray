
# JVMXRay - AI-Enhanced Java Security Monitoring

[![Black Hat Arsenal](https://raw.githubusercontent.com/toolswatch/badges/master/arsenal/usa/2020.svg?sanitize=true)](https://www.toolswatch.org/blackhat-arsenal-us-2020-archive/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Java](https://img.shields.io/badge/Java-9+-orange.svg)](https://openjdk.java.net/)

[![Build Status](https://github.com/spoofzu/jvmxray/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spoofzu/jvmxray/actions/workflows/maven-build.yml)
[![Contributors](https://img.shields.io/github/contributors/spoofzu/jvmxray.svg)](https://github.com/spoofzu/jvmxray/graphs/contributors)
[![Last Commit](https://img.shields.io/github/last-commit/spoofzu/jvmxray.svg)](https://github.com/spoofzu/jvmxray/commits/master)

<picture>
  <source srcset="https://github.com/spoofzu/jvmxray/blob/master/build/lightduke.png?raw=true" media="(prefers-color-scheme: dark)">
  <source srcset="https://github.com/spoofzu/jvmxray/blob/master/build/darkduke.png?raw=true" media="(prefers-color-scheme: light)">
  <img src="https://github.com/spoofzu/jvmxray/blob/master/build/lightduke.png" alt="Logo" width="200">
</picture>

[Duke: Oracle's Java mascot...](https://wiki.openjdk.java.net/display/duke/Main)

**JVMXRay is an AI-enhanced security monitoring platform that watches Java applications in real-time, detecting vulnerabilities and suspicious activity without requiring code changes. Intelligence analysis enriches security events with additional metadata like CWE/CVSS scoring for regulatory reporting. With simple setup and minimal performance impact, JVMXRay provides comprehensive security visibility into applications and third-party dependencies, enhanced by machine learning that improves detection accuracy over time.**

| &nbsp;                                  | &nbsp;                                                                                                                                                                                                                                                                                                                                |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **NEWS**                                | &nbsp;                                                                                                                                                                                                                                                                                                                                |
| **Sep 1, 2025** Major Update            | Significant milestone release featuring AI-powered vulnerability analysis, online/offline integration support with AI MCP clients like Claude Desktop, 15+ sensor types and growing, enhanced documentation suite, multi-database support (SQLite/MySQL/Cassandra), and complete CI/CD pipeline. Suitable for testing and evaluation. |
| **Apr 23, 2025** Platform rearchitected | Architecture improved to remove deprecated SecurityManager and move to byte code injection approach.                                                                                                                                                                                                                                  |
| **Feb 20, 2024** Improved architecture  | Improved documentation for new architecture. Site docs forthcoming.                                                                                                                                                                                                                                                                   |

**📰 [View News Archive](README-NEWS.md)** - Complete history of project announcements and milestones

## Why JVMXRay?

Java applications are under constant attack, but traditional security tools require code changes, create performance overhead, or generate too many false positives. JVMXRay improves upon current state by:

- **Zero Code Changes**: Monitor any Java application without modifications
- **Intelligence Analysis**: AI enriches security events with metadata like CWE/CVSS scoring for risk prioritization and compliance reporting
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
- **Compliance Automation**: Security event data enrichment supporting compliance like automatic CWE/CVSS scoring for SOC 2, PCI DSS, and regulatory reporting

<!-- TODO: // Update example
# Deploying JVMXRay with Examples
The following provides some basic information to download and compile JVMXRay source on your computer.  Remainder of the video shows how to get JVMXRay working with Tomcat and work with Tomcat's examples.

[![](http://img.youtube.com/vi/QxgTiTCorow/0.jpg)](http://www.youtube.com/watch?v=QxgTiTCorow "JVMXRay Deploy")
-->

## Quick Start

Get up and running in **under 5 minutes** with SQLite demo data and Claude Desktop AI integration:

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
   **Note**: Test data is generated in a SQLite DB for JVMXRay unit tests. We also use the test data to demonstrate AI integration.


4. **Generate API Key for AI MCP Client (Claude Desktop)**
   ```bash
   ./scripts/generate-api-key.sh
   ```
   **Note**: Paste JVMXRay MCP Server API key into your [MCP Client configuration for Claude Desktop](https://support.anthropic.com/en/articles/10949351-getting-started-with-local-mcp-servers-on-claude-desktop).


5. **Start JVMXRay MCP Server**
   ```bash
   ./scripts/start-mcp-server.sh
   ```

**That's it, you're up and running!** Try these sample phrases:
- *"Show me all critical security events from the last 24 hours"*
- *"Find network connections from IP 192.168.1.100 in the last hour"*
- *"Analyze SQL injection attempts and show attack patterns"*  
- *"Create a security timeline for the payment-gateway application"*
- *"Query ERROR level events from web-server app with limit 50"*

**📖 For complete setup instructions, see [Quick Start Guide](docs/0002-QUICK-START-GUIDE.md)**

## Key Capabilities

### 🤖 AI-Enhanced Analysis
- **Structured Security Events**: Unlike traditional unstructured logs that require complex parsing, JVMXRay generates machine-readable security events automatically
- **AI Data Enrichment**: Structured data enables intelligent vulnerability classification with CWE assignment and dynamic CVSS scoring based on attack characteristics
- **Pattern Recognition**: Advanced detection for SQL injection, command injection, path traversal using consistent event formats
- **Intelligence Pipeline**: Rule-based analysis with configurable pattern matching and threat classification on structured data

### 🔧 Extensible Sensor Capabilities  
- **File I/O Sensor**: Monitors file system access, reads, writes, and deletions
  ```
  2025.04.23 at 16:38:52 CDT | INFO | org.jvmxray.events.io.filedelete | caller=java.io.File:1075, target=/tmp/sensitive.data, status=successfully deleted
  ```
- **Network Sensor**: Tracks socket connections, binds, and data transfers
  ```  
  2025.04.23 at 16:38:51 CDT | INFO | org.jvmxray.events.net.connect | caller=java.net.Socket:189, destination=malicious-site.com:443, status=connected
  ```
- **Process Sensor**: Detects system command execution and privilege escalation
  ```
  2025.04.23 at 16:38:50 CDT | INFO | org.jvmxray.events.system.exec | caller=java.lang.ProcessBuilder:1029, command=/bin/sh -c rm -rf /, status=blocked
  ```
- **Monitor Sensor**: System performance and health monitoring with point-in-time snapshots
  ```
  2025.04.23 at 16:38:51 CDT | INFO | org.jvmxray.events.monitor | GCCount=1, ThreadRunnable=2, MemoryFree=566.3MB, ProcessCpuLoad=0%, OpenFiles=163
  ```
- **Library Sensor**: Dynamic and static JAR loading detection
  ```
  2025.04.23 at 16:38:51 CDT | INFO | org.jvmxray.events.system.lib | method=dynamic, jarPath=/path/to/library.jar, caller=ClassLoader:123
  ```
- **Serialization Sensor**: Object serialization monitoring for deserialization attacks
  ```
  2025.04.23 at 16:38:50 CDT | INFO | org.jvmxray.events.serialization | caller=ObjectInputStream:123, target=UserData.class, status=deserialized
  ```
- **SQL Sensor**: Database query monitoring and injection attempt detection
  ```
  2025.04.23 at 16:38:49 CDT | INFO | org.jvmxray.events.sql.query | query=SELECT * FROM users WHERE id = '1 OR 1=1', status=potential_injection
  ```
- **Uncaught Exception Sensor**: Application error and crash monitoring with comprehensive diagnostics
  ```
  2025.04.23 at 16:38:48 CDT | INFO | org.jvmxray.events.system.uncaughtexception | 
  thread_name=payment-processor-1, thread_id=42, thread_group=main, exception_type=java.lang.NullPointerException,
  exception_location=com.example.PaymentProcessor:145, exception_method=processPayment,
  exception_message=Cannot process null payment, stack_depth=28, memory_pressure=HIGH, heap_used_mb=756.2,
  command_line=java -jar payment-service.jar --port=8080, jvm_uptime_minutes=47, incident_id=f3d4e5a6-b7c8-4d9e-a1b2-3c4d5e6f7a8b
  ```
- **API Sensor**: REST and web service call monitoring
- **Configuration Sensor**: Application configuration access and modification tracking
- **Data Transfer Sensor**: Large data movement and export detection
- **Thread Sensor**: Thread lifecycle and synchronization monitoring
- **Authentication Sensor**: Login attempts and credential usage tracking
- **Cryptographic Sensor**: Encryption/decryption operations and key usage
- **Reflection Sensor**: Dynamic code loading and class manipulation detection
- **HTTP Sensor**: Web request and response pattern analysis

### 🏗️ Enterprise Architecture
- **Database Support**: SQLite (testing), MySQL, Cassandra (production)
- **Logging Integration**: Logback framework supports any destination (Kafka, JMS, SMTP, etc.)
- **High Performance**: Bytecode injection with minimal overhead
- **Scalable Design**: From single applications to enterprise-wide deployments

## How It Works

1. **Deploy Agent**: Add JVMXRay as Java agent: `java -javaagent:xray.jar -jar yourapp.jar` - That's it! Your application won't know it's monitored.
2. **Sensor Installation**: Bytecode injection installs monitoring sensors automatically  
3. **Event Capture**: Sensors monitor file access, network connections, system calls
4. **AI Analysis**: Intelligence pipeline analyzes patterns and assigns vulnerability classifications
5. **Structured Event Generation**: Unlike traditional unstructured logs that require complex analysis tools, JVMXRay automatically generates machine-readable security events that enable instant analysis and AI enhancement
6. **Query & Analyze**: Generate reports with standard BI tools, visualize with Grafana dashboards, or answer ad-hoc queries using AI MCP clients connected to JVMXRay's MCP server  

### Enhanced MCP Query Capabilities ✨ **NEW**

JVMXRay's MCP server enables AI clients like Claude Desktop to become instant security experts on your datacenter. The system provides sophisticated querying capabilities with advanced filtering, pagination, and real-time analysis.

**See it in action:** [JVMXRay AI Integration Demo](https://www.youtube.com/watch?v=O-5oIXijMb8&t=101s) - A video is worth a thousand words!

**📖 For complete query documentation, see [Enhanced MCP Queries Guide](docs/MCP_Enhanced_Queries.md)**

## Documentation

- **[📖 Quick Start Guide](docs/0002-QUICK-START-GUIDE.md)** - Get up and running in under 5 minutes
- **[🔑 Key Management](docs/KEY_MANAGEMENT.md)** - API key and master key management 
- **[📋 Documentation Index](docs/0000-INDEX.md)** - Complete documentation catalog
- **[🚀 Legacy Quick Start](docs/QUICK_START.md)** - Spring Boot focused setup guide

## Project Contributors(s)
Milton Smith - Project creator, leader

Disclosure(s):  The JVMXRay project is not, approved, endorsed by, or affiliated with Oracle Corporation.  Oracle is a long-time supporter of secure open source software and the Online Web Application Security(OWASP) project.  Milton Smith is an active industry open source contributor, OWASP member, and an employee of Oracle.
