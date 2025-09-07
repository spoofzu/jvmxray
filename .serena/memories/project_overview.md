# JVMXRay Project Overview

## Purpose
JVMXRay is an AI-enhanced security monitoring platform that watches Java applications in real-time, detecting vulnerabilities and suspicious activity without requiring code changes. It provides comprehensive security visibility into applications and third-party dependencies.

## Key Features
- **Zero Code Changes**: Monitor any Java application without modifications using bytecode injection
- **AI-Enhanced Analysis**: Structured security events with CWE/CVSS scoring for risk prioritization
- **Complete Visibility**: Monitors file access, network connections, system calls, and more
- **Minimal Impact**: Low performance overhead using bytecode injection and Logback logging
- **Enterprise Ready**: Works with existing logging infrastructure (Splunk, ELK, DataDog)
- **MCP Server Integration**: AI clients like Claude Desktop can query security events

## Architecture
Multi-module Maven project with Java 11:
- **prj-agent**: Java agent that injects sensors using ByteBuddy
- **prj-service-event-aggregator**: LogService for real-time event aggregation  
- **prj-common**: Shared utilities and models

## Deployment Model
Add as Java agent: `java -javaagent:xray.jar -jar yourapp.jar`

## Key Technologies
- ByteBuddy for bytecode injection
- Logback for enterprise logging
- Maven for build management
- SQLite/MySQL/Cassandra for data persistence
- Socket-based real-time event streaming