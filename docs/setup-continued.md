# JVMXRay Setup - Continued

This guide continues the Quick Start setup with optional advanced features for event exploration and database configuration.

**Prerequisites:** You should have completed steps 1-4 from the [main README](../README.md) Quick Start section.

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
- ✅ Enterprise logging integration

### Explore Further

- **[Component Documentation](.)** - Deep dive into each module
- **[Agent Configuration](prj-agent.md)** - Deploy JVMXRay to monitor your applications
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
