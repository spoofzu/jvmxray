# Library Dependencies

This directory contains JAR files for dependencies that are not available in public Maven repositories.

## Model Context Protocol (MCP) SDK

The following JAR files are required for the MCP client functionality:

- `mcp-0.13.0-SNAPSHOT.jar` - Core MCP SDK
- `mcp-json-jackson2-0.13.0-SNAPSHOT.jar` - JSON serialization support

### Source and Documentation

These JARs are built from the official Anthropic MCP Java SDK:

- **Source Code**: https://github.com/modelcontextprotocol/servers/tree/main/src/java-sdk
- **Documentation**: https://modelcontextprotocol.io/docs/develop/build-client#java
- **Protocol Specification**: https://spec.modelcontextprotocol.io/

### Building from Source

To rebuild these dependencies:

1. Clone the MCP servers repository:
   ```bash
   git clone https://github.com/modelcontextprotocol/servers.git
   cd servers/src/java-sdk
   ```

2. Build the SDK:
   ```bash
   mvn clean install
   ```

3. Copy the built JARs to this directory:
   ```bash
   cp mcp/target/mcp-0.13.0-SNAPSHOT.jar /path/to/jvmxray/libs/
   cp mcp-json-jackson2/target/mcp-json-jackson2-0.13.0-SNAPSHOT.jar /path/to/jvmxray/libs/
   ```

### Version Information

- **Current Version**: 0.13.0-SNAPSHOT
- **Last Updated**: September 2024
- **Compatibility**: Java 11+

### Usage in Project

These dependencies are referenced in `prj-mcp-client/pom.xml` using Maven system scope:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.13.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.parent.basedir}/libs/mcp-0.13.0-SNAPSHOT.jar</systemPath>
</dependency>
```

This approach ensures the build works in both local development and CI environments without requiring external dependencies.