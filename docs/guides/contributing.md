# Contributing to JVMXRay

## Welcome Contributors! 

JVMXRay is an open source project that welcomes contributions from developers, security professionals, and anyone interested in Java application security monitoring.

## How to Contribute

### 🐛 Reporting Bugs
- Use the [GitHub Issues](https://github.com/spoofzu/jvmxray/issues) template
- Include JVMXRay version, Java version, and OS
- Provide steps to reproduce the issue
- Include relevant log files and error messages

### ✨ Suggesting Features
- Check existing [feature requests](https://github.com/spoofzu/jvmxray/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement)
- Describe the use case and expected behavior
- Consider if it fits JVMXRay's core mission of security monitoring

### 🔧 Contributing Code
- Fork the repository and create a feature branch
- Follow the development setup guide below
- Write tests for new functionality
- Ensure all tests pass before submitting
- Submit a pull request with clear description

## Development Setup

### Prerequisites
- Java 11+ (JDK)
- Maven 3.6+
- Git
- 4GB RAM recommended
- IDE with Java support (IntelliJ IDEA, Eclipse, VS Code)

### Environment Setup
```bash
# 1. Fork and clone repository
git clone https://github.com/your-username/jvmxray.git
cd jvmxray

# 2. Set up development environment
export JAVA_HOME=/path/to/jdk11
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# 3. Build and test
mvn clean install

# 4. Verify integration tests pass
./test-integration.sh
```

### Project Structure
```
jvmxray/
├── prj-agent/                      # Java agent with sensors
│   ├── src/main/java/             # Agent source code
│   └── src/test/java/             # Agent unit tests
├── prj-service-event-aggregator/   # LogService for events
│   ├── src/main/java/             # LogService source
│   └── src/test/java/             # LogService tests
├── prj-common/                     # Shared utilities & schema
│   ├── src/main/java/             # Common source code
│   └── src/test/java/             # Common unit tests
├── prj-integration/                # Integration tests
└── docs/                           # Documentation
```

## Development Workflow

### 1. Creating a Feature Branch
```bash
# Create branch from main
git checkout main
git pull origin main
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b bugfix/issue-number-description
```

### 2. Making Changes
- Follow existing code patterns and conventions
- Add unit tests for new functionality
- Update documentation as needed
- Ensure builds pass: `mvn clean install`

### 3. Testing Your Changes
```bash
# Run unit tests
mvn test

# Run integration tests
mvn clean install

# Test specific sensor functionality
./test-integration.sh --sensors "file,network"

# Manual testing with sample application
java -javaagent:prj-agent/target/prj-agent-0.0.1-shaded.jar \
     -cp prj-integration/target/test-classes \
     org.jvmxray.integration.TurtleIntegrationTest
```

### 4. Submitting Pull Request
```bash
# Push branch to your fork
git push origin feature/your-feature-name

# Create pull request via GitHub web interface
# Include:
# - Clear description of changes
# - Related issue numbers
# - Test results
# - Breaking changes (if any)
```

## Coding Standards

### Java Code Style
- **Java 11+** features and syntax
- **4 spaces** for indentation (no tabs)
- **120 character** line length maximum
- **CamelCase** for classes, **camelCase** for methods/variables
- **Comprehensive Javadoc** for public APIs

### Sensor Development Standards
```java
// Example sensor class structure
public class ExampleSensor extends AbstractSensor implements InjectableSensor {
    
    private static final String SENSOR_NAME = "ExampleSensor";
    private static final String EVENT_NAMESPACE = "org.jvmxray.events.example";
    
    @Override
    public String getName() {
        return SENSOR_NAME;
    }
    
    @Override
    public void inject(Instrumentation instrumentation) {
        // ByteBuddy injection logic
    }
    
    // Event generation methods with proper error handling
    public void onExampleEvent(String data) {
        try {
            String event = formatEvent("example", data, "success");
            logEvent(EVENT_NAMESPACE + ".example", event);
        } catch (Exception e) {
            // Never break application flow
            logError("Error in example sensor", e);
        }
    }
}
```

### Testing Standards
- **100% test coverage** for new sensor code
- **Integration tests** for all new sensors in TurtleIntegrationTest
- **Performance tests** to ensure <3% overhead
- **Error handling tests** to verify application stability

### Documentation Standards
- Follow [AI Style Guide](docs/AI-STYLE-GUIDE.md)
- Include **Mermaid diagrams** for complex flows
- Provide **working code examples**
- Update **reference documentation** for new features

## Security Guidelines

### Secure Development Practices
- **Never log sensitive data** (passwords, tokens, PII)
- **Validate all inputs** to prevent injection attacks
- **Use parameterized queries** for database operations
- **Handle errors gracefully** without exposing internals
- **Follow principle of least privilege**

### Event Format Security
```java
// Good - Sanitized logging
String sanitizedQuery = query.replaceAll("'[^']*'", "'***'");
logEvent("sql.query", "query=" + sanitizedQuery + ", status=executed");

// Bad - Potential sensitive data exposure
logEvent("sql.query", "query=" + rawQuery + ", password=" + password);
```

## Sensor Development Guide

### Adding New Sensors

#### 1. Plan Your Sensor
- **Identify target methods** to intercept
- **Define event format** with security metadata
- **Consider performance impact**
- **Plan test coverage**

#### 2. Implement Sensor Class
```java
// Extend AbstractSensor and implement InjectableSensor
public class YourSensor extends AbstractSensor implements InjectableSensor {
    
    @Override
    public void inject(Instrumentation instrumentation) {
        new AgentBuilder.Default()
            .type(getTypeMatcher())
            .transform((builder, type, classLoader, module) ->
                builder.method(getMethodMatcher())
                       .intercept(Advice.to(YourInterceptor.class)))
            .installOn(instrumentation);
    }
    
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return ElementMatchers.nameStartsWith("target.package");
    }
    
    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return ElementMatchers.named("targetMethod");
    }
}
```

#### 3. Create Method Interceptor
```java
public class YourInterceptor {
    
    @Advice.OnMethodEnter
    public static void onMethodEnter(
            @Advice.Origin String method,
            @Advice.AllArguments Object[] args) {
        try {
            // Extract relevant data
            // Generate structured event
            YourSensor.getInstance().onEvent(data);
        } catch (Exception e) {
            // Never break application flow
            logSensorError("Interception failed", e);
        }
    }
}
```

#### 4. Add Integration Test
```java
// Add to TurtleIntegrationTest.java
public void testYourSensor() throws Exception {
    // Trigger operations that should be detected
    performTargetOperation();
    
    // Wait for event processing
    Thread.sleep(1000);
    
    // Verify expected events were generated
    assertTrue("Should detect your operation", 
               logContains("org.jvmxray.events.your.event"));
}
```

#### 5. Document Your Sensor
- Add to [Sensor Reference](docs/reference/all-sensors.md)
- Include event format specification
- Provide usage examples
- Document security implications

## Review Process

### Code Review Checklist
- [ ] **Functionality**: Code works as intended
- [ ] **Tests**: Comprehensive test coverage
- [ ] **Performance**: No significant overhead (<3%)
- [ ] **Security**: No sensitive data exposure
- [ ] **Documentation**: Updated and accurate
- [ ] **Compatibility**: Works with Java 11+
- [ ] **Style**: Follows project conventions

### Review Criteria
- **Correctness**: Does it solve the stated problem?
- **Security**: Are there any security implications?
- **Performance**: Impact on monitored applications
- **Maintainability**: Is the code clear and well-structured?
- **Testing**: Are edge cases covered?

## Release Process

### Version Numbering
- **Major.Minor.Patch** (e.g., 1.0.0)
- **Major**: Breaking API changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes, no new features

### Release Checklist
- [ ] All tests pass
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers incremented
- [ ] Integration tests verified
- [ ] Performance benchmarks run

## Community Guidelines

### Code of Conduct
- **Be respectful** and inclusive
- **Welcome newcomers** and help them learn
- **Focus on what's best** for the community
- **Show empathy** towards other community members

### Communication
- **GitHub Issues** for bug reports and feature requests
- **Pull Requests** for code contributions
- **Discussions** for questions and design discussions

### Getting Help
- Review existing [documentation](docs/)
- Search [GitHub Issues](https://github.com/spoofzu/jvmxray/issues)
- Check [troubleshooting guide](docs/guides/troubleshooting.md)
- Ask questions in GitHub Discussions

## Recognition

### Contributors
We recognize all contributors in:
- **README.md** contributors section
- **Release notes** for significant contributions
- **Git commit history** with proper attribution

### Types of Contributions
- **Code contributions** (features, bug fixes, tests)
- **Documentation** improvements
- **Bug reports** with reproduction steps
- **Feature suggestions** with use cases
- **Community support** helping other users

## Additional Resources

### Useful Links
- [Architecture Overview](docs/architecture/README.md)
- [Quick Start Guide](docs/guides/quick-start.md)
- [Complete Command Reference](docs/reference/all-commands.md)
- [Sensor Development Guide](docs/guides/sensor-development.md)

### Development Tools
- **IDE Plugins**: ByteBuddy plugin for IntelliJ
- **Debugging**: Java Flight Recorder for performance analysis
- **Testing**: JUnit 4.13+ for unit tests
- **Build**: Maven 3.6+ with Java 11+

Thank you for contributing to JVMXRay! Your contributions help make Java applications more secure.