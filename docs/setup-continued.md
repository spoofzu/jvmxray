# JVMXRay Setup - Continued

This guide continues the Quick Start setup with optional advanced features for event enrichment, AI integration, and vulnerability analysis.

**Prerequisites:** You should have completed steps 1-4 from the [main README](../README.md) Quick Start section.

---

## Step 5: Initialize AI Service Configuration

Initialize the AI Service to create configuration files and directories:

```bash
./script/services/ai-service --init
```

**What This Does:**
- Creates `.jvmxray/aiservice/` directory structure
- Generates `aiservice.properties` configuration file

**Expected Output:**
```
Initializing AI Service configuration...

=================================================
AI Service configuration initialized successfully
=================================================
Configuration directory: /path/to/.jvmxray/aiservice
Configuration file: /path/to/.jvmxray/aiservice/config/aiservice.properties
=================================================

Next steps:
1. Start the AI service:
   ./script/services/ai-service --start --interval 60

Configuration initialization complete!
```

---

## Step 6: Start AI Service

Start the AI Service to begin processing security events:

```bash
./script/services/ai-service --start --interval 60
```

**Expected Output:**
```
Starting AI Service...
Initializing components...
Initializing AI processors...

=================================================
AI Service started successfully
=================================================
Database: jdbc:sqlite:/path/to/jvmxray-test.db
Batch size: 1000 events per cycle
Processing interval: 60 seconds
=================================================
Multi-stage data pipeline processing:
  Stage0: STAGE0_EVENT → STAGE1_EVENT (parsing)
  Stage1: STAGE1_EVENT → STAGE2_LIBRARY (basic enrichment)
  Stage2: STAGE2_LIBRARY enrichment (CVE analysis)
=================================================
Service is now processing events...

[2025-10-01 16:30:00] Processed: 15 stage0→stage1, 12 stage1→stage2, 5 stage2 enriched
```

**Keep the service running** for continuous processing, or stop it with `Ctrl+C` when done.

### 6.1 What the AI Service Does

The AI Service provides a complete multi-stage data processing pipeline:

**Stage 0 - Event Parsing:**
- Reads raw events from `STAGE0_EVENT` (written by agents)
- Parses KEYPAIRS column into normalized format
- Writes to `STAGE1_EVENT` and `STAGE1_EVENT_KEYPAIR` tables
- Uses IS_STABLE flag for data consistency

**Stage 1 - Library Enrichment:**
- Processes library loading events from `STAGE1_EVENT`
- Extracts library information (JAR path, SHA-256 hash, method)
- Generates library name from path
- Writes basic library records to `STAGE2_LIBRARY`

**Stage 2 - CVE Analysis & Metadata:**
- Reads library records from `STAGE2_LIBRARY`
- Performs pattern-based CVE matching against known vulnerabilities
- Identifies CVE vulnerabilities with CVSS scores and severity
- Extracts Maven coordinates and JAR manifest metadata
- Updates `STAGE2_LIBRARY` with enriched data
- Stores CVE records in `STAGE2_LIBRARY_CVE`

**Configuration:**
- Each stage can be enabled/disabled independently
- Configurable batch sizes per stage
- Processing interval applies to all stages
- See `.jvmxray/aiservice/config/aiservice.properties`

### 6.2 Query Enriched Data

**View enriched libraries:**
```bash
sqlite3 .jvmxray/common/data/jvmxray-test.db "SELECT LIBRARY_NAME, GROUP_ID, ARTIFACT_ID, VERSION, SHA256_HASH FROM STAGE2_LIBRARY LIMIT 10;"
```

**Find libraries with vulnerabilities:**
```sql
SELECT l.LIBRARY_NAME, l.VERSION, c.CVE_ID, c.CVSS_SEVERITY, c.CVSS_V3
FROM STAGE2_LIBRARY l
JOIN STAGE2_LIBRARY_CVE c
  ON l.GROUP_ID = c.GROUP_ID
  AND l.ARTIFACT_ID = c.ARTIFACT_ID
  AND l.VERSION = c.VERSION
WHERE c.CVSS_SEVERITY IN ('CRITICAL', 'HIGH');
```

**📖 Full documentation:** [AI Service Guide](prj-service-ai.md)

---

## Step 7: REST Service + Claude Desktop MCP Integration (Optional)

Enable AI-powered security intelligence by connecting Claude Desktop to JVMXRay through the REST API.

### 7.1 Generate API Key

Generate an API key for secure MCP access:

```bash
./script/security/generate-api-key "claude-desktop"
```

**Save the API key** - you'll need it for Claude Desktop configuration.

### 7.2 Start REST Service

Open a **separate terminal** and start the REST service:

```bash
./script/services/rest-service --port 8080
```

**Expected Output:**
```
Starting REST Service...
REST Service started on port 8080
API endpoint: http://localhost:8080
```

**Keep this terminal running** - the REST service must be active for MCP integration.

### 7.3 Configure Claude Desktop MCP

Add this configuration to your Claude Desktop settings file:

**macOS/Linux:** `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "jvmxray": {
      "command": "java",
      "args": [
        "-jar",
        "{YOUR_PROJECT_PATH}/prj-mcp-client/target/prj-mcp-client-bridge.jar",
        "--host=localhost",
        "--port=8080",
        "--api-key={YOUR_GENERATED_API_KEY}",
        "--debug=/tmp/jvmxray-mcp-debug.log",
        "--workers=4",
        "--queue=256"
      ]
    }
  }
}
```

**Replace:**
- `{YOUR_PROJECT_PATH}` with your actual project directory (e.g., `/Users/username/github/jvmxray`)
- `{YOUR_GENERATED_API_KEY}` with the API key from Step 7.1

**Restart Claude Desktop** to activate the MCP integration.

### 7.4 Verify Integration

In Claude Desktop, you should now be able to query your JVMXRay security events:

```
Show me the most recent security events
```

```
List all file I/O operations in the last hour
```

```
Find network connections to external hosts
```

**📖 Full documentation:** [MCP Client Guide](prj-mcp-client.md)

---

## Optional: Explore Test Database

The SQLite test database is created at: `.jvmxray/common/data/jvmxray-test.db`

Use any SQLite client to explore the data:

```bash
# View raw events
sqlite3 .jvmxray/common/data/jvmxray-test.db "SELECT * FROM STAGE0_EVENT LIMIT 10;"

# View enriched events
sqlite3 .jvmxray/common/data/jvmxray-test.db "SELECT * FROM STAGE0_EVENT_KEYPAIR LIMIT 10;"

# View library intelligence
sqlite3 .jvmxray/common/data/jvmxray-test.db "SELECT * FROM STAGE2_LIBRARY LIMIT 10;"
```

**Note:** SQLite is used for development and testing. Production deployments support MySQL and Cassandra databases.

---

## Next Steps

**Congratulations! You've completed the full JVMXRay setup!**

Your system now includes:
- ✅ Complete sensor framework with 15+ monitoring capabilities
- ✅ Multi-database support (SQLite/MySQL/Cassandra)
- ✅ AI-enhanced security event analysis
- ✅ MCP integration for AI-powered queries (if enabled)
- ✅ Vulnerability analysis and library tracking (if enabled)
- ✅ Enterprise logging integration

### Explore Further

- **[Component Documentation](.)** - Deep dive into each module
- **[Agent Configuration](prj-agent.md)** - Deploy JVMXRay to monitor your applications
- **[REST API Guide](prj-service-rest.md)** - Integrate with external systems
- **[Database Schema](prj-common.md)** - Understand data structures

### Production Deployment

Ready to deploy JVMXRay in production? See:
- **[Production Deployment Guide](0002-QUICK-START-GUIDE.md)** - Enterprise setup patterns
- **[Database Configuration](prj-common.md)** - MySQL and Cassandra setup
- **[Security Best Practices](../CLAUDE.md)** - Secure configuration guidelines

---

**Questions or Issues?**

- Review the [main README](../README.md)
- Check component documentation in [docs/](.)
- Report issues at [GitHub Issues](https://github.com/spoofzu/jvmxray/issues)
