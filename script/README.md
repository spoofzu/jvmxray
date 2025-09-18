# JVMXRay Scripts Directory

This directory contains organized scripts and configuration files for JVMXRay operations.

## Directory Structure

```
script/
├── bin/                    # Executable scripts
│   ├── services/          # Service management scripts
│   ├── data/              # Data manipulation scripts
│   ├── security/          # Security-related scripts
│   └── misc/              # Miscellaneous utilities
├── config/                # Configuration files
│   └── logback/          # Logging configuration files
└── README.md              # This file
```

## Script Categories

### Services (`bin/services/`)
- `log-service` - Start and manage the JVMXRay log service
- `rest-service` - Start and manage the JVMXRay REST API service

### Data Management (`bin/data/`)
- `generate-test-data` - Generate test event data for development/testing
- `migrate-stage-data` - Migrate data from STAGE0 to STAGE1 format

### Security (`bin/security/`)
- `generate-api-key` - Generate API keys for REST service authentication

### Miscellaneous (`bin/misc/`)
- `webgoat-test` - Launch WebGoat with JVMXRay agent for security testing

## Configuration Files (`config/`)

### Logback Configurations (`config/logback/`)
- `agent-db.xml` - Agent logging configuration for database output
- `common.xml` - Common logging configuration for components

## Usage

All scripts are executable and follow Unix conventions:
- No file extensions (`.sh` removed)
- Use `#!/usr/bin/env bash` for portability
- Include `--help` options where applicable

Example usage:
```bash
# Start the log service
./script/bin/services/log-service

# Generate test data
./script/bin/data/generate-test-data --count 1000

# Generate an API key
./script/bin/security/generate-api-key myapp
```

## Design Principles

All shell scripts follow the **Minimal Wrapper Pattern**:
- Scripts are lightweight wrappers that handle only classpath construction and JVM execution
- All business logic resides in Java code
- Scripts use consistent error handling and logging patterns
- Cross-platform compatibility via portable shebang and path resolution