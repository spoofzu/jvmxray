#!/bin/bash

# Script to run WebGoat with a JVMXRay Agent
# Author: Milton Smith
# Date: April 23, 2025
# Purpose: Configures and runs WebGoat with the JVMXRay agent, logging output to a specified file.

# Configuration variables
# Use absolute paths or resolve relative to script location
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEBGOAT_DIR="/Users/milton/github/WebGoat"
LOG_DIR="/Users/milton/jvmxray/logs"
LOG_FILE="$LOG_DIR/maven.log"
AGENT_JAR="/Users/milton/github/jvmxray/agent/target/agent-0.0.1-shaded.jar"
MVN_COMMAND="mvn spring-boot:run"
JVM_ARGS="-javaagent:$AGENT_JAR"

# Function to log messages
log_message() {
    local message="$1"
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $message" | tee -a "$LOG_FILE"
}

# Function to check if a file or directory exists
check_path() {
    local path="$1"
    local type="$2"  # 'file' or 'dir'
    if [ "$type" = "file" ] && [ ! -f "$path" ]; then
        log_message "ERROR: File $path does not exist."
        exit 1
    elif [ "$type" = "dir" ] && [ ! -d "$path" ]; then
        log_message "ERROR: Directory $path does not exist."
        exit 1
    fi
}

# Trap errors and exit gracefully
trap 'log_message "ERROR: Script terminated unexpectedly."; exit 1' ERR

# Validate prerequisites
log_message "Starting WebGoat with JVMXRay agent..."

# Check if WebGoat directory exists
check_path "$WEBGOAT_DIR" "dir"

# Check if agent JAR exists
check_path "$AGENT_JAR" "file"

# Check if already in WebGoat directory
CURRENT_DIR="$(pwd)"
if [ "$CURRENT_DIR" != "$WEBGOAT_DIR" ]; then
    log_message "Changing to WebGoat directory: $WEBGOAT_DIR"
    if ! cd "$WEBGOAT_DIR"; then
        log_message "ERROR: Failed to change to $WEBGOAT_DIR"
        exit 1
    fi
else
    log_message "Already in WebGoat directory: $WEBGOAT_DIR"
fi

# Ensure log directory exists
log_message "Creating log directory if it doesn't exist: $LOG_DIR"
if ! mkdir -p "$LOG_DIR"; then
    log_message "ERROR: Failed to create log directory $LOG_DIR"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn >/dev/null 2>&1; then
    log_message "ERROR: Maven is not installed or not found in PATH."
    exit 1
fi

# Run Maven Spring Boot with agent
log_message "Running Maven Spring Boot with JVMXRay agent..."
if ! $MVN_COMMAND -Dspring-boot.run.jvmArguments="$JVM_ARGS" >> "$LOG_FILE" 2>&1; then
    log_message "ERROR: Failed to run Maven Spring Boot."
    exit 1
fi

# Return to parent directory if we changed directories
if [ "$CURRENT_DIR" != "$WEBGOAT_DIR" ]; then
    log_message "Returning to parent directory."
    if ! cd ..; then
        log_message "ERROR: Failed to return to parent directory."
        exit 1
    fi
else
    log_message "No need to return to parent directory (already in WebGoat root)."
fi

log_message "Script completed successfully."
exit 0