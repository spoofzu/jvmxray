# AI Documentation Style Guide for JVMXRay

## Overview
This guide ensures consistent documentation when AI assistants maintain JVMXRay docs. Follow these standards for all documentation updates and creation.

## Documentation Standards

### File Naming Convention
```yaml
pattern: "{category}-{topic}.md"
examples:
  - "architecture-sensors.md"
  - "api-events.md"
  - "guide-troubleshooting.md"
  - "reference-commands.md"
```

### Document Structure Template
```markdown
# {Title} - One line description

## Overview
Brief 2-3 sentence description of what this document covers.

## Prerequisites (if applicable)
- Requirement 1
- Requirement 2

## Main Content
Primary documentation content with examples.

## Examples
Always include working examples.

## Troubleshooting (if applicable)
Common issues and solutions.

## See Also
Links to related documentation.
```

### Mermaid Diagram Guidelines

#### When to Use Each Diagram Type:
```yaml
sequence_diagrams:
  use_for: ["API calls", "event flows", "component interactions"]
  example: "Agent → Sensor → LogService → Database flow"

flowcharts:
  use_for: ["process flows", "decision trees", "data pipelines"]
  example: "Event processing pipeline"

class_diagrams:
  use_for: ["code architecture", "inheritance hierarchies", "APIs"]
  example: "Sensor class relationships"

er_diagrams:
  use_for: ["database schemas", "data relationships"]
  example: "Event storage tables"

state_diagrams:
  use_for: ["component lifecycles", "status transitions"]
  example: "Sensor state machine"

graph_diagrams:
  use_for: ["system architecture", "dependency graphs"]
  example: "Overall system components"
```

#### Diagram Formatting Standards:
```yaml
naming:
  nodes: "PascalCase for components, camelCase for actions"
  relationships: "descriptive verbs"
  
colors:
  security_events: "#ff6b6b"
  normal_flow: "#4ecdc4"
  error_conditions: "#ffe66d"
  
layout:
  max_width: "12 nodes per row"
  group_related: "use subgraphs for logical grouping"
```

### Event Documentation Format
```yaml
event_template:
  structure: |
    ### {event.name}
    ```yaml
    event: {event.namespace}
    format: "{logback.pattern}"
    example: "{real.world.example}"
    fields:
      field_name:
        type: "{data.type}"
        description: "{human.readable.description}"
        required: {true|false}
    security:
      cwe: ["{CWE.numbers}"]
      mitre: ["{MITRE.tactics}"]
      severity: {low|medium|high|critical}
    ```

example: |
  ### File Read Event
  ```yaml
  event: org.jvmxray.events.io.fileread
  format: "caller={class}:{line}, target={path}, status={result}"
  example: "caller=java.io.FileInputStream:189, target=/etc/passwd, status=success"
  fields:
    caller:
      type: string
      description: "Calling class and line number"
      required: true
    target:
      type: string
      description: "Absolute file path being accessed"
      required: true
    status:
      type: enum[success|denied|error]
      description: "Operation result"
      required: true
  security:
    cwe: ["CWE-73", "CWE-200"]
    mitre: ["T1005", "T1083"]
    severity: medium
  ```
```

### Code Example Standards
```yaml
code_blocks:
  always_include: ["working example", "expected output", "common variations"]
  languages: ["java", "bash", "yaml", "json", "xml"]
  
example_template: |
  ```java
  // Brief description of what this code does
  public class ExampleSensor extends AbstractSensor {
      @Override
      public void inject(Instrumentation inst) {
          // Implementation with inline comments
      }
  }
  ```
  
  **Expected Output:**
  ```
  2025.09.07 at 10:15:30 CDT | INFO | org.jvmxray.events.example | ...
  ```
```

### Cross-Reference Standards
```yaml
internal_links:
  format: "[Display Text](relative/path/to/doc.md)"
  examples:
    - "[Sensor Development Guide](guides/sensor-development.md)"
    - "[Event Format Reference](reference/all-events.md#file-events)"

external_links:
  format: "[Display Text](https://full.url)"
  always_include: "brief description of external resource"
```

### Update Procedures
```yaml
when_updating_docs:
  1. "Check for affected cross-references"
  2. "Update related diagrams if architecture changed"
  3. "Verify all examples still work"
  4. "Update version numbers if applicable"
  5. "Check for broken links"

version_control:
  major_changes: "Update version in document header"
  backward_compatibility: "Note breaking changes prominently"
  
validation_checklist:
  - "All code examples compile/run"
  - "All Mermaid diagrams render correctly"
  - "All links work"
  - "YAML validation passes"
  - "Consistent terminology used"
```

### AI Assistant Instructions
```yaml
maintenance_tasks:
  routine:
    - "Update examples when code changes"
    - "Add new sensors to reference docs"
    - "Update diagrams when architecture changes"
    - "Fix broken links during refactoring"
  
  comprehensive:
    - "Create missing documentation"
    - "Reorganize for better navigation"
    - "Add troubleshooting sections"
    - "Generate API documentation from code"

response_format:
  when_asked_about_docs: "Always reference specific section and line numbers"
  when_updating_docs: "Explain what changed and why"
  when_creating_docs: "Follow this style guide exactly"
```

## Quality Checklist for AI
Before completing documentation updates:

- [ ] All Mermaid diagrams render without errors
- [ ] Code examples are tested and work
- [ ] Cross-references are valid
- [ ] YAML formatting is valid
- [ ] Consistent terminology throughout
- [ ] Version information is current
- [ ] Related documents are updated
- [ ] Examples match current API

## Terminology Standards

### Consistent Terms
- **JVMXRay Agent** (not "agent", "xray agent", or "jvmx agent")
- **Sensor** (not "monitor", "interceptor", or "watcher")
- **Structured Events** (not "logs", "messages", or "data")
- **LogService** (not "log service", "logging service")
- **MCP Server** (not "mcp-server", "MCP service")
- **Bytecode Injection** (not "code injection", "instrumentation")

### Technical Accuracy
- Always use exact class names: `AbstractSensor`, `InjectableSensor`
- Always use exact namespaces: `org.jvmxray.events.io.fileread`
- Always use exact command syntax: `mvn clean install`
- Always use exact file paths: `prj-agent/target/prj-agent-0.0.1-shaded.jar`

## File Organization Rules

### Directory Structure
```
docs/
├── architecture/     # System design and component relationships
├── api/             # API specifications and interfaces
├── guides/          # Step-by-step instructions
├── reference/       # Complete catalogs and lookups
├── examples/        # Integration examples and tutorials
└── AI-STYLE-GUIDE.md # This file
```

### Cross-File Dependencies
- **Always update related files** when making changes
- **Check reverse dependencies** before removing content
- **Maintain consistency** across all documentation
- **Update index files** when adding new documents