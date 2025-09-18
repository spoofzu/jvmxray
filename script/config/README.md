# JVMXRay Configuration Files

This directory contains configuration files used by JVMXRay scripts and services.

## Logback Configurations (`logback/`)

### `agent-db.xml`
- **Purpose**: Agent logging configuration for database output
- **Used by**: Test data generation scripts that simulate agent behavior
- **Features**:
  - Database appender for SQLite test database
  - Console output for debugging
  - File output for log analysis
  - Shaded logback classes for agent compatibility

### `common.xml`
- **Purpose**: Common logging configuration for non-agent components
- **Used by**: Data migration tools, service launchers
- **Features**:
  - Standard logback classes
  - Console and file output
  - Configurable log levels

## Usage in Scripts

Scripts reference these configurations via system properties:
```bash
-Dlogback.configurationFile=$PROJECT_ROOT/script/config/logback/common.xml
-Dlogback.agent.configurationFile=$PROJECT_ROOT/script/config/logback/agent-db.xml
```

## Customization

To customize logging for your environment:
1. Copy the relevant XML file to a new name
2. Modify log levels, appenders, or output formats as needed
3. Update the script to reference your custom configuration

## Important Notes

- Agent configurations use shaded logback classes (`agent.shadow.logback.*`)
- Common configurations use standard logback classes (`ch.qos.logback.*`)
- Database paths are configured as properties within the XML files
- Log file paths are relative to the project root unless absolute paths are specified